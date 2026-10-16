package duoc.example.gestionoportunidades.ui.alertas

import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Seguimiento

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val eventosProximos: List<Oportunidad> = emptyList(),
    val seguimientosAtrasados: List<Seguimiento> = emptyList()
)