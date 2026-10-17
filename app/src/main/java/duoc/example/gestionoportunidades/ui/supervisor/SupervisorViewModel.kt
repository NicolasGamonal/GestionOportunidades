package duoc.example.gestionoportunidades.ui.supervisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos

class SupervisorViewModel(
    private val oportunidadRepository: OportunidadRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupervisorUiState())

    private var jobPanel: Job? = null
    val uiState: StateFlow<SupervisorUiState> = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            authRepository.obtenerEjecutivos().collect { lista ->
                _uiState.update { it.copy(ejecutivos = lista) }
            }
        }
    }

    /**
     * Carga de forma reactiva todas las oportunidades y los ejecutivos de prueba disponibles.
     */
    fun inicializarPanel(busqueda: String = "") {
        _uiState.update { it.copy(isLoading = true) }
        jobPanel?.cancel()
        jobPanel = viewModelScope.launch {
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
        if (!Permisos.puedeAsignar(SesionManager.getRol())) {
            _uiState.update { it.copy(mensajeError = "Solo el supervisor puede asignar responsables") }
            return
        }
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
    fun confirmarOportunidad(oportunidad: Oportunidad) =
        cambiarAEstadoFinal(oportunidad, "Confirmada", "Oportunidad confirmada")

    fun descartarOportunidad(oportunidad: Oportunidad) =
        cambiarAEstadoFinal(oportunidad, "Descartada", "Oportunidad descartada")
    private fun cambiarAEstadoFinal(oportunidad: Oportunidad, estado: String, mensaje: String) {
        val usuario = SesionManager.usuarioActual
        if (usuario == null || !Permisos.puedeCambiarEstado(usuario.rol, oportunidad.estado, estado)) {
            _uiState.update { it.copy(mensajeError = "No tienes permiso para esta acción") }
            return
        }
        viewModelScope.launch {
            try {
                oportunidadRepository.cambiarEstado(
                    oportunidad.idOportunidad, oportunidad.estado, estado, usuario.idUsuario
                )
                _uiState.update { it.copy(mensajeExito = mensaje) }
            } catch (e: Exception) {
                _uiState.update { it.copy(mensajeError = "Error: ${e.localizedMessage}") }
            }

        }
    }
}
