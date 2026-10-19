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

// ViewModel del panel del supervisor.
// Gestiona: lista global de oportunidades (con búsqueda), lista de ejecutivos,
// asignación/reasignación, confirmación y descarte.
// Solo accesible para rol SUPERVISOR (Navegacion.kt bloquea el acceso al ejecutivo).

class SupervisorViewModel(
    private val oportunidadRepository: OportunidadRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupervisorUiState())
    val uiState: StateFlow<SupervisorUiState> = _uiState.asStateFlow()

    // Job para cancelar la query anterior al cambiar la búsqueda (evita resultados obsoletos)
    private var jobPanel: Job? = null

    init {
        // Cargar la lista de ejecutivos (reactivo: si se agrega uno, se actualiza solo)
        viewModelScope.launch {
            authRepository.obtenerEjecutivos().collect { lista ->
                _uiState.update { it.copy(ejecutivos = lista) }
            }
        }
    }

    // Carga todas las oportunidades filtradas por búsqueda.
    // Se llama desde la Screen cuando cambia el campo de búsqueda (vía LaunchedEffect).
    // Cancela el job anterior para que una búsqueda vieja no sobrescriba la nueva.
    fun inicializarPanel(busqueda: String = "") {
        _uiState.update { it.copy(isLoading = true) }
        jobPanel?.cancel()
        jobPanel = viewModelScope.launch {
            oportunidadRepository.obtenerTodasLasOportunidades(busqueda).collect { lista ->
                _uiState.update { it.copy(listaOportunidadesGlobal = lista, isLoading = false) }
            }
        }
    }

    // Reasigna el idResponsable de una oportunidad a otro ejecutivo.
    // Verifica permiso antes de actuar (doble seguridad: la UI ya lo restringe).
    fun reasignarResponsable(oportunidad: Oportunidad, idNuevoEjecutivo: Long) {
        if (!Permisos.puedeAsignar(SesionManager.getRol())) {
            _uiState.update { it.copy(mensajeError = "Solo el supervisor puede asignar responsables") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, mensajeExito = null, mensajeError = null) }
            try {
                // copy() crea una copia con el idResponsable cambiado (no se muta el original)
                val oportunidadModificada = oportunidad.copy(idResponsable = idNuevoEjecutivo)
                oportunidadRepository.modificarOportunidad(oportunidadModificada)
                _uiState.update {
                    it.copy(isLoading = false, mensajeExito = "Responsable reasignado exitosamente")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, mensajeError = "Error al reasignar: ${e.localizedMessage}")
                }
            }
        }
    }

    // Limpia los mensajes (se llama desde la Screen después de mostrar el Snackbar)
    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, mensajeError = null) }
    }

    // Cambia el estado a "Confirmada" (final)
    fun confirmarOportunidad(oportunidad: Oportunidad) =
        cambiarAEstadoFinal(oportunidad, "Confirmada", "Oportunidad confirmada")

    // Cambia el estado a "Descartada" (final)
    fun descartarOportunidad(oportunidad: Oportunidad) =
        cambiarAEstadoFinal(oportunidad, "Descartada", "Oportunidad descartada")

    // Lógica común para confirmar/descartar:
    // 1. Verifica permiso con Permisos.puedeCambiarEstado (solo supervisor llega a finales)
    // 2. Llama a oportunidadRepository.cambiarEstado() (transacción: actualiza + registra historial)
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