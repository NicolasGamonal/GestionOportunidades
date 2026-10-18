package duoc.example.gestionoportunidades.ui.alertas

import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.model.Oportunidad

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val eventosProximos: List<Oportunidad> = emptyList(),
    val seguimientosPendientes: List<AlertaSeguimiento> = emptyList(),
    val seguimientosAtrasados: List<AlertaSeguimiento> = emptyList()
)