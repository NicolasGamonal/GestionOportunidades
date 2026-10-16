package duoc.example.gestionoportunidades.ui.supervisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupervisorViewModel(
    private val oportunidadRepository: OportunidadRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupervisorUiState())
    val uiState: StateFlow<SupervisorUiState> = _uiState.asStateFlow()

    /**
     * Carga de forma reactiva todas las oportunidades y los ejecutivos de prueba disponibles.
     */
    fun inicializarPanel(busqueda: String = "") {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // Escucha cambios globales en las oportunidades (Flow)
            oportunidadRepository.obtenerTodasLasOportunidades(busqueda).collect { lista ->
                _uiState.update { it.copy(listaOportunidadesGlobal = lista, isLoading = false) }
            }
        }
    }

    /**
     * Requerimiento crítico de la Checklist: Permitir al supervisor asignar y reasignar responsables.
     * Actualiza el idResponsable en la oportunidad y persiste el cambio en Room.
     */
    fun reasignarResponsable(oportunidad: Oportunidad, idNuevoEjecutivo: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, mensajeExito = null, mensajeError = null) }
            try {
                // Creamos una copia modificando la llave foránea del responsable
                val oportunidadModificada = oportunidad.copy(idResponsable = idNuevoEjecutivo)
                oportunidadRepository.modificarOportunidad(oportunidadModificada)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        mensajeExito = "Responsable reasignado exitosamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        mensajeError = "Error al reasignar: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, mensajeError = null) }
    }
}
