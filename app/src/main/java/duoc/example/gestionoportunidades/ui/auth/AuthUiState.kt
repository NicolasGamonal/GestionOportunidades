package duoc.example.gestionoportunidades.ui.auth

import duoc.example.gestionoportunidades.model.Usuario

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMensaje: String? = null,
    val loginExitoso: Boolean = false,
    val usuarioLogueado: Usuario? = null
)