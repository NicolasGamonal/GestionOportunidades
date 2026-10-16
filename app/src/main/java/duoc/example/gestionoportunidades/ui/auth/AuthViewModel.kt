package duoc.example.gestionoportunidades.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // Estado interno mutable encapsulado
    private val _uiState = MutableStateFlow(AuthUiState())
    // Estado público de solo lectura expuesto como StateFlow a la UI de Compose
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Procesa las credenciales del formulario local contra las 4 cuentas ficticias de Room.
     */
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
                        it.copy(
                            isLoading = false,
                            errorMensaje = "Credenciales incorrectas de prueba"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMensaje = "Error local: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Limpia la sesión actual para cumplir con el requisito de cierre de sesión.
     */
    fun cerrarSesion() {
        _uiState.value = AuthUiState()
    }
}
