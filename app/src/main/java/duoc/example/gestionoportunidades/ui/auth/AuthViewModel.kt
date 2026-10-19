package duoc.example.gestionoportunidades.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// AuthViewModel: lógica detrás de la pantalla de login (AuthScreen.kt).
//
// Responsabilidad:
// - Validar que los campos no estén vacíos.
// - Llamar a AuthRepository.iniciarSesion() (que consulta Room).
// - Si es exitoso: guardar la sesión en SesionManager y notificar a la Screen.
// - Si falla: mostrar mensaje de error.
// - Permitir cerrar sesión (resetear el estado y borrar la sesión global).
//
// Patrón MVVM:
// - Recibe AuthRepository por inyección (desde AppContainer).
// - Expone un único StateFlow<AuthUiState> que la Screen observa.
// - No conoce Composables ni navegación: solo actualiza el estado.
//   La navegación la hace AuthScreen vía LaunchedEffect(uiState.loginExitoso).
//
// ¿Por qué viewModelScope?
// Porque el login es asíncrono (suspend). viewModelScope.launch() ejecuta la
// consulta en un hilo de fondo y la cancela automáticamente si la pantalla
// se destruye (el usuario presionó "volver" mientras cargaba).
class AuthViewModel(
    // Repositorio de autenticación: se inyecta desde AppContainer.
    // Contiene la lógica de login (comparar username + contraseña contra Room).
    private val authRepository: AuthRepository
) : ViewModel() {

    // Estado mutable (privado): solo el ViewModel puede modificarlo.
    // AuthUiState() por defecto: isLoading=false, errorMensaje=null,
    // loginExitoso=false, usuarioLogueado=null.
    private val _uiState = MutableStateFlow(AuthUiState())

    // Estado inmutable (público): la Screen solo puede LEER (observar), no escribir.
    // asStateFlow() crea una "ventana de solo lectura" sobre el MutableStateFlow.
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Intenta iniciar sesión con el username y contraseña ingresados.
    // Se llama desde AuthScreen cuando el usuario presiona "Ingresar".
    fun intentarLogin(username: String, contrasena: String) {
        // Validación básica: si algún campo está vacío, no se consulta Room.
        // Se muestra el mensaje de error directamente (sin coroutine).
        if (username.isBlank() || contrasena.isBlank()) {
            _uiState.update { it.copy(errorMensaje = "Por favor, completa todos los campos") }
            return  // No se continúa: se sale de la función
        }

        // Se lanza una coroutine en el alcance del ViewModel.
        // Si la pantalla se destruye, esta coroutine se cancela sola.
        viewModelScope.launch {
            // 1. Activar el spinner y limpiar errores anteriores
            _uiState.update { it.copy(isLoading = true, errorMensaje = null) }

            try {
                // 2. Consultar Room (suspend: puede tardar unos ms)
                // Devuelve el Usuario si las credenciales son correctas, o null si no.
                val usuario = authRepository.iniciarSesion(username, contrasena)

                if (usuario != null) {
                    // 3a. LOGIN EXITOSO:
                    // - Guardar la sesión en SesionManager (estado global de la app).
                    //   Esto permite que otras pantallas (Dashboard, Oportunidades, etc.)
                    //   sepan quién está logueado sin tener que pasarlo por parámetros.
                    SesionManager.iniciarSesion(usuario)
                    // - Actualizar el uiState para que AuthScreen navegue al Dashboard.
                    //   loginExitoso = true dispara el LaunchedEffect en AuthScreen.
                    _uiState.update {
                        it.copy(
                            usuarioLogueado = usuario,
                            isLoading = false,
                            loginExitoso = true,
                            errorMensaje = null
                        )
                    }
                } else {
                    // 3b. LOGIN FALLIDO (usuario o contraseña incorrectos):
                    // - Mostrar mensaje de error en la Screen.
                    // - Desactivar el spinner.
                    _uiState.update {
                        it.copy(isLoading = false, errorMensaje = "Credenciales incorrectas")
                    }
                }
            } catch (e: Exception) {
                // 3c. ERROR IMPREVISTO (ej: base de datos corrupta, error de Room):
                // - Mostrar el mensaje del error para depuración.
                // - En producción se mostraría un mensaje genérico.
                _uiState.update {
                    it.copy(isLoading = false, errorMensaje = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    // Cierra la sesión actual.
    // Se llama desde el menú de la app (botón "Cerrar sesión" en el TopAppBar o menú lateral).
    //
    // ¿Qué hace?
    // 1. SesionManager.cerrarSesion(): borra el usuario de la sesión global.
    //    Después de esto, cualquier pantalla que consulte SesionManager.usuarioActual
    //    recibirá null.
    // 2. _uiState.value = AuthUiState(): resetea el estado del ViewModel a su valor inicial.
    //    Esto limpia loginExitoso, usuarioLogueado, etc.
    //
    // ¿Por qué _uiState.value y no _uiState.update?
    // Porque no estamos modificando un campo específico, estamos REEMPLAZANDO todo el estado
    // con una instancia nueva. update {} sería para copiar con cambios parciales.
    //
    // La navegación de vuelta a AuthScreen la hace Navegacion.kt
    // (que observa SesionManager o recibe un callback).
    fun cerrarSesion() {
        SesionManager.cerrarSesion()
        _uiState.value = AuthUiState()
    }
}