package duoc.example.gestionoportunidades.ui.supervisor

import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Usuario

data class SupervisorUiState(
    val listaOportunidadesGlobal: List<Oportunidad> = emptyList(),
    val listaEjecutivosDisponibles: List<Usuario> = emptyList(),
    val isLoading: Boolean = false,
    val mensajeExito: String? = null,
    val mensajeError: String? = null
)
