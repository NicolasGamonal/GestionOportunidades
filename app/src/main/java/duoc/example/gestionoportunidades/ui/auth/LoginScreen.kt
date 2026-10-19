package duoc.example.gestionoportunidades.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.util.Validaciones

// LoginScreen: pantalla de login (versión alternativa a AuthScreen.kt).
//
// Diferencias con AuthScreen:
// - Usa Validaciones.validarObligatorio() en cada campo (isError + supportingText).
//   Esto cumple el checklist: "Mostrar mensajes específicos debajo de los campos incorrectos"
//   e "Utilizar indicadores visuales de error, supportingText e íconos apropiados".
// - El callback onLoginExitoso pasa (rol, idUsuario) en vez del objeto Usuario completo.
//   El padre (Navegacion.kt) usa esos datos para guardar la sesión y navegar.
// - No usa Scaffold/TopAppBar: es una Column simple centrada (diseño más minimalista).
// - No muestra las cuentas de prueba en pantalla (están en el README).
//
// Patrón MVVM:
// - Observa el uiState del AuthViewModel con collectAsState().
// - La validación de campos se hace en la Screen (Validaciones.kt) ANTES de llamar
//   al ViewModel. El ViewModel también valida (doble seguridad).
// - Los errores de validación son estado LOCAL (remember), no van en el UiState.
//   Solo el error de credenciales (que viene de Room) va en el UiState.
@Composable
fun LoginScreen(
    // ViewModel inyectado desde el padre (Navegacion.kt o AppContainer).
    viewModel: AuthViewModel,
    // Callback: se llama cuando el login es exitoso.
    // Pasa el rol ("EJECUTIVO" o "SUPERVISOR") y el idUsuario.
    // El padre usa estos datos para guardar la sesión y decidir a qué pantalla navegar.
    onLoginExitoso: (rol: String, idUsuario: Long) -> Unit
) {
    // Observa el estado del ViewModel.
    // Nota: usa collectAsState() (no collectAsStateWithLifecycle()).
    // En una pantalla de login esto no es crítico (no hay consultas pesadas en background),
    // pero en pantallas con datos de Room se prefiere collectAsStateWithLifecycle().
    val uiState by viewModel.uiState.collectAsState()

    // Estado LOCAL de los campos de texto (no van en el UiState del ViewModel).
    // remember: se conserva si la composición se re-ejecuta.
    var username by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    // Estado LOCAL de los errores de validación por campo.
    // Son nullable: null = sin error, String = mensaje de error a mostrar.
    // Se actualizan en tiempo real (cada tecla) y al presionar el botón.
    var errorUsername by remember { mutableStateOf<String?>(null) }
    var errorContrasena by remember { mutableStateOf<String?>(null) }

    // Navegar si el login fue exitoso.
    // LaunchedEffect: se re-ejecuta cuando uiState.loginExitoso cambia.
    // Si es true y hay un usuarioLogueado, se extraen rol e id y se llaman al callback.
    // El padre (Navegacion.kt) guarda la sesión y navega al Dashboard.
    LaunchedEffect(uiState.loginExitoso) {
        if (uiState.loginExitoso && uiState.usuarioLogueado != null) {
            val usuario = uiState.usuarioLogueado!!
            onLoginExitoso(usuario.rol, usuario.idUsuario)
        }
    }

    // Column centrada vertical y horizontalmente.
    // Sin Scaffold: diseño minimalista sin TopAppBar.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título de la pantalla
        Text(
            text = "Acceso al Sistema CRM",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // --- CAMPO: USUARIO ---
        // OutlinedTextField con validación en tiempo real:
        // - onValueChange: cada tecla actualiza el valor Y valida (validarObligatorio).
        //   Si el campo queda vacío, errorUsername se llena con "El usuario es obligatorio".
        //   Si tiene contenido, errorUsername = null (se limpia el error).
        // - isError: pinta el borde en rojo si hay error (indicador visual, checklist).
        // - supportingText: muestra el mensaje de error debajo del campo (checklist).
        // - label: texto descriptivo que incluye los usernames de prueba como referencia.
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                // Validaciones.validarObligatorio() devuelve String? (mensaje o null).
                // Si el campo está vacío → "El usuario es obligatorio".
                // Si tiene contenido → null (sin error).
                errorUsername = Validaciones.validarObligatorio(it, "El usuario")
            },
            label = { Text("Usuario ficticio (ejecutivo1 / supervisor1)") },
            isError = errorUsername != null,
            supportingText = { errorUsername?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- CAMPO: CONTRASEÑA ---
        // Igual que el de usuario, pero con:
        // - PasswordVisualTransformation: oculta los caracteres (puntos).
        // - KeyboardType.Password: teclado de contraseña.
        // - Validación con Validaciones.validarObligatorio() en cada tecla.
        OutlinedTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                errorContrasena = Validaciones.validarObligatorio(it, "La contraseña")
            },
            label = { Text("Contraseña local") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorContrasena != null,
            supportingText = { errorContrasena?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- INDICADOR DE CARGA ---
        // Se muestra mientras el ViewModel consulta Room (login asíncrono).
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
        }

        // --- MENSAJE DE ERROR GENERAL (credenciales incorrectas) ---
        // Diferente de los errores por campo (que son de validación).
        // Este viene del ViewModel cuando Room no encuentra el usuario/contraseña.
        // uiState.errorMensaje es nullable: si es null, no se muestra nada.
        uiState.errorMensaje?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,  // Color semántico de error (del tema)
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // --- BOTÓN DE LOGIN ---
        // Al presionar:
        // 1. Re-valida ambos campos (por si el usuario borró algo sin que se dispare onValueChange).
        // 2. Si NO hay errores de validación → llama a viewModel.intentarLogin().
        // 3. Si SÍ hay errores → no hace nada (los campos ya muestran el error).
        //
        // enabled = !uiState.isLoading: se deshabilita durante la consulta
        // (evita dobles submits).
        // height(50.dp): altura fija para que sea fácil de tocar (accessibilidad).
        Button(
            onClick = {
                // Re-validar al presionar (doble seguridad)
                errorUsername = Validaciones.validarObligatorio(username, "El usuario")
                errorContrasena = Validaciones.validarObligatorio(contrasena, "La contraseña")

                // Solo si ambos campos son válidos se intenta el login
                if (errorUsername == null && errorContrasena == null) {
                    viewModel.intentarLogin(username, contrasena)
                }
            },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Iniciar Sesión")
        }
    }
}