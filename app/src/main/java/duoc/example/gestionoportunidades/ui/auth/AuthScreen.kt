package duoc.example.gestionoportunidades.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// AuthScreen: pantalla de login (la primera que se ve al abrir la app).
//
// Responsabilidad:
// - Mostrar 2 campos (usuario y contraseña) con validación visual.
// - Llamar a AuthViewModel.intentarLogin() al presionar "Ingresar".
// - Si el login es exitoso, navegar a la siguiente pantalla (Dashboard) vía callback.
// - Si falla, mostrar un mensaje de error debajo de los campos.
//
// Patrón MVVM:
// - Observa el uiState del AuthViewModel con collectAsStateWithLifecycle().
// - No tiene lógica de negocio: la validación de credenciales la hace el ViewModel
//   (que llama a AuthRepository → UsuarioDao.login()).
// - La navegación se delega al padre (Navegacion.kt) vía onLoginExitoso.
//
// ¿Por qué los campos usan remember { mutableStateOf("") } y no el UiState?
// Porque el valor que el usuario está escribiendo es estado LOCAL de la pantalla
// (cambia en cada tecla). El UiState del ViewModel solo guarda el resultado
// (isLoading, errorMensaje, loginExitoso, usuarioLogueado).
// Si se guardara en el UiState, cada tecla dispararía una actualización del StateFlow,
// lo cual es innecesario y pesado.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    // Callback: se llama cuando el login es exitoso con el Usuario logueado.
    // El padre (Navegacion.kt) usa este callback para:
    // 1. Guardar el usuario en SesionManager (estado global de la sesión).
    // 2. Navegar al Dashboard (o a la ruta inicial según rol).
    onLoginExitoso: (usuario: duoc.example.gestionoportunidades.model.Usuario) -> Unit,
    // ViewModel por defecto: si el padre no lo inyecta, se crea uno automáticamente.
    // En la práctica, Navegacion.kt lo inyecta desde AppContainer para que use
    // el AuthRepository correcto (con la base de datos ya creada).
    viewModel: AuthViewModel = viewModel()
) {
    // Observa el estado del ViewModel (isLoading, errorMensaje, loginExitoso, usuarioLogueado).
    // Se actualiza solo cuando el ViewModel cambia el StateFlow.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Estado LOCAL de los campos de texto.
    // remember: el valor se conserva si la composición se re-ejecuta (ej: rotación de pantalla).
    // mutableStateOf: Compose se re-componen cuando cambian (cada tecla presionada).
    var username by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    // Navegar si el login fue exitoso.
    // LaunchedEffect: se re-ejecuta cuando uiState.loginExitoso cambia.
    // Si es true y hay un usuarioLogueado, se llama al callback del padre.
    // El padre (Navegacion.kt) guarda la sesión y navega al Dashboard.
    //
    // ¿Por qué LaunchedEffect y no un onClick directo?
    // Porque el login es asíncrono (suspend): el usuario presiona "Ingresar",
    // el ViewModel consulta Room (que puede tardar unos ms), y CUANDO termina
    // actualiza el uiState. La navegación no puede hacerse en el onClick porque
    // todavía no se sabe si fue exitoso.
    LaunchedEffect(uiState.loginExitoso) {
        if (uiState.loginExitoso) {
            uiState.usuarioLogueado?.let { onLoginExitoso(it) }
        }
    }

    // Scaffold: estructura base de Material 3 con TopAppBar + contenido.
    // El padding que devuelve se aplica al contenido para que no se solape con la barra.
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gestión de Oportunidades") })
        }
    ) { padding ->
        // Column centrada vertical y horizontalmente, con scroll (por si la pantalla es pequeña).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)          // Respetar el padding del Scaffold (TopAppBar)
                .padding(24.dp)            // Margen interno para que no toque los bordes
                .verticalScroll(rememberScrollState()),  // Scroll si el contenido no cabe
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center     // Centrar verticalmente
        ) {
            // Título de la pantalla
            Text("Iniciar Sesión", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(32.dp))

            // --- CAMPO DE USUARIO ---
            // OutlinedTextField: campo de texto con borde (Material 3).
            // leadingIcon: ícono de persona a la izquierda (indica que es un nombre/usuario).
            // singleLine: no permite saltos de línea.
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },  // Actualiza el estado local en cada tecla
                label = { Text("Usuario") },         // Texto que flota arriba cuando hay contenido
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- CAMPO DE CONTRASEÑA ---
            // Igual que el de usuario, pero con:
            // - PasswordVisualTransformation: oculta los caracteres (muestra puntos).
            // - KeyboardType.Password: muestra el teclado con teclas de contraseña.
            // - leadingIcon de candado (indica que es secreto).
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- MENSAJE DE ERROR (solo si hay error) ---
            // uiState.errorMensaje es nullable: si es null, el let no se ejecuta (no se muestra nada).
            // Si el login falla (usuario o contraseña incorrectos), el ViewModel la llena
            // con "Usuario o contraseña incorrectos" y aquí se muestra en rojo.
            uiState.errorMensaje?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,  // Color semántico de error (del tema)
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- BOTÓN DE LOGIN ---
            // enabled = !uiState.isLoading: se deshabilita mientras se consulta Room
            // (evita que el usuario presione 2 veces y dispare 2 consultas).
            Button(
                onClick = { viewModel.intentarLogin(username, contrasena) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Si está cargando, se muestra un spinner dentro del botón
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Ingresar")
            }

            // --- CUENTAS DE PRUEBA (referencia rápida para el docente) ---
            // Muestra las credenciales ficticias para que quien pruebe la app
            // no tenga que buscarlas en el README.
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Cuentas de prueba:\nejecutivo1 / 123456\nsupervisor1 / admin123",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}