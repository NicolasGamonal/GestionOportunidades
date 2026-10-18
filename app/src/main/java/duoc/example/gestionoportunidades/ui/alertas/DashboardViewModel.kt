package duoc.example.gestionoportunidades.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.inicioDeHoyMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val TODO_EL_EQUIPO = 0L
private const val NADIE = -1L

class DashboardViewModel(
    oportunidadRepository: OportunidadRepository,
    seguimientoRepository: SeguimientoRepository
) : ViewModel() {

    // Ejecutivo: solo lo suyo. Supervisor: todo el equipo. Sin sesión: nada.
    private val idResponsable: Long = idResponsableDeLaSesion()

    val uiState: StateFlow<DashboardUiState> = combine(
        oportunidadRepository.obtenerEventosProximos(inicioDeHoyMillis(), idResponsable),
        seguimientoRepository.obtenerUltimasGestiones(idResponsable)
    ) { proximos, gestiones ->
        val ahora = System.currentTimeMillis()
        DashboardUiState(
            isLoading = false,
            eventosProximos = proximos,
            seguimientosPendientes = gestiones.filter { it.fechaProximo >= ahora },
            seguimientosAtrasados = gestiones.filter { it.fechaProximo < ahora }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private fun idResponsableDeLaSesion(): Long {
        val usuario = SesionManager.usuarioActual ?: return NADIE
        return if (usuario.rol == "SUPERVISOR") TODO_EL_EQUIPO else usuario.idUsuario
    }
}