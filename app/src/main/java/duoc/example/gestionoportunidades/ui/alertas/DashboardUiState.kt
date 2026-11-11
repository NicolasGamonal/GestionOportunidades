package duoc.example.gestionoportunidades.ui.alertas

import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.model.ConteoEstado
import duoc.example.gestionoportunidades.model.Oportunidad

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val eventosProximos: List<Oportunidad> = emptyList(),
    val seguimientosPendientes: List<AlertaSeguimiento> = emptyList(),
    val seguimientosAtrasados: List<AlertaSeguimiento> = emptyList(),
    // HU-20: oportunidades activas que nunca han tenido una gestión real
    val sinSeguimiento: List<Oportunidad> = emptyList(),
    // HU-20: oportunidades activas cuya organización no tiene ningún contacto registrado
    val sinContacto: List<Oportunidad> = emptyList(),
    // HU-25: indicadores para el supervisor (se muestran solo si el rol es SUPERVISOR)
    val indicadores: IndicadoresEquipo = IndicadoresEquipo()
)

// Resumen numérico del equipo (HU-25).
data class IndicadoresEquipo(
    val total: Int = 0,
    val porEstado: List<ConteoEstado> = emptyList(),
    // Porcentaje de oportunidades Confirmadas sobre el total (null si todavía no hay oportunidades)
    val conversionPorcentaje: Int? = null
)