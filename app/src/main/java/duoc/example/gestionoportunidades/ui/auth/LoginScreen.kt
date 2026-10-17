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

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginExitoso: (rol: String, idUsuario: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var username by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    var errorUsername by remember { mutableStateOf<String?>(null) }
    var errorContrasena by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.loginExitoso) {
        if (uiState.loginExitoso && uiState.usuarioLogueado != null) {
            val usuario = uiState.usuarioLogueado!!
            onLoginExitoso(usuario.rol, usuario.idUsuario)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Acceso al Sistema CRM",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Campo: Nombre de Usuario Ficticio
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorUsername = Validaciones.validarObligatorio(it, "El usuario")
            },
            label = { Text("Usuario ficticio (ejecutivo1 / supervisor1)") },
            isError = errorUsername != null,
            supportingText = { errorUsername?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo: Contraseña Ficticia
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

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
        }

        uiState.errorMensaje?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                errorUsername = Validaciones.validarObligatorio(username, "El usuario")
                errorContrasena = Validaciones.validarObligatorio(contrasena, "La contraseña")

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
