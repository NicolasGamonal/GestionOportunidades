package duoc.example.gestionoportunidades.ui.alertas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val oportunidadRepository = OportunidadRepository(db)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun cargarAlertasYMetricas() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val fechaActual = System.currentTimeMillis()
            oportunidadRepository.obtenerEventosProximos(fechaActual).collect { proximos ->
                _uiState.update { it.copy(eventosProximos = proximos, isLoading = false) }
            }
        }

        viewModelScope.launch {
            val fechaActual = System.currentTimeMillis()
            oportunidadRepository.obtenerSeguimientosAtrasados(fechaActual).collect { atrasados ->
                _uiState.update { it.copy(seguimientosAtrasados = atrasados) }
            }
        }
    }
}