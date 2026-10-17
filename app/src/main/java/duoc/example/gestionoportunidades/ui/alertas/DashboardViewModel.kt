package duoc.example.gestionoportunidades.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val oportunidadRepository: OportunidadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var jobProximos: Job? = null
    private var jobAtrasados: Job? = null

    fun cargarAlertasYMetricas() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        // Cancela las colecciones anteriores para no acumular colectores
        jobProximos?.cancel()
        jobAtrasados?.cancel()

        val fechaActual = System.currentTimeMillis()

        jobProximos = viewModelScope.launch {
            oportunidadRepository.obtenerEventosProximos(fechaActual).collect { proximos ->
                _uiState.update { it.copy(eventosProximos = proximos, isLoading = false) }
            }
        }

        jobAtrasados = viewModelScope.launch {
            oportunidadRepository.obtenerSeguimientosAtrasados(fechaActual).collect { atrasados ->
                _uiState.update { it.copy(seguimientosAtrasados = atrasados) }
            }
        }
    }
}