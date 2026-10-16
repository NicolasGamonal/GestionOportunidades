package duoc.example.gestionoportunidades.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
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

    /**
     * Consulta Room 3 de forma reactiva utilizando el timestamp actual del sistema
     * para clasificar qué elementos requieren atención prioritaria en los Dashboards.
     */
    fun cargarAlertasYMetricas() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val fechaActualMilis = System.currentTimeMillis()

                // 1. Escucha eventos/oportunidades próximas de la checklist
                oportunidadRepository.obtenerEventosProximos(fechaActualMilis).collect { proximos ->
                    _uiState.update { it.copy(eventosProximos = proximos) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar tableros: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        viewModelScope.launch {
            try {
                val fechaActualMilis = System.currentTimeMillis()

                // 2. Escucha bitácoras o gestiones comerciales que ya vencieron sin resolución
                oportunidadRepository.obtenerSeguimientosAtrasados(fechaActualMilis).collect { atrasados ->
                    _uiState.update { it.copy(seguimientosAtrasados = atrasados) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar seguimientos: ${e.localizedMessage}") }
            }
        }
    }
}
