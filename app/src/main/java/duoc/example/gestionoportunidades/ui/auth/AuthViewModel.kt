package duoc.example.gestionoportunidades.ui.auth

// 1. IMPORTS (cambia ViewModel por AndroidViewModel, agrega Application y AppDatabase)
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 2. DECLARACIÓN DE LA CLASE (cambia ViewModel() por AndroidViewModel(application))
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // 3. AQUÍ VAN LA BD Y EL REPO (dentro de la clase, antes de los StateFlows)
    private val db = AppDatabase.getDatabase(application)
    private val authRepository = AuthRepository(db)

    // 4. RESTO IGUAL A LO QUE YA TENÍAS
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun intentarLogin(username: String, contrasena: String) {
        if (username.isBlank() || contrasena.isBlank()) {
            _uiState.update { it.copy(errorMensaje = "Por favor, completa todos los campos") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMensaje = null) }
            try {
                val usuario = authRepository.iniciarSesion(username, contrasena)
                if (usuario != null) {
                    _uiState.update {
                        it.copy(
                            usuarioLogueado = usuario,
                            isLoading = false,
                            loginExitoso = true,
                            errorMensaje = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMensaje = "Credenciales incorrectas")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMensaje = "Error: ${e.localizedMessage}") }
            }
        }
    }

    fun cerrarSesion() {
        _uiState.value = AuthUiState()
    }
}