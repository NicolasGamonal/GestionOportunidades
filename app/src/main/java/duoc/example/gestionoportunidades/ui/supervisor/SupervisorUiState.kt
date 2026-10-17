package duoc.example.gestionoportunidades.ui.supervisor

import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Usuario

data class SupervisorUiState(
    val isLoading: Boolean = true,
    val listaOportunidadesGlobal: List<Oportunidad> = emptyList(),
    val ejecutivos: List<Usuario> = emptyList(),
    val mensajeExito: String? = null,
    val mensajeError: String? = null
)