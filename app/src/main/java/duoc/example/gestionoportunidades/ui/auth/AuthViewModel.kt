package duoc.example.gestionoportunidades.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

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
                    SesionManager.iniciarSesion(usuario)
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
                _uiState.update {
                    it.copy(isLoading = false, errorMensaje = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun cerrarSesion() {
        SesionManager.cerrarSesion()
        _uiState.value = AuthUiState()
    }
}