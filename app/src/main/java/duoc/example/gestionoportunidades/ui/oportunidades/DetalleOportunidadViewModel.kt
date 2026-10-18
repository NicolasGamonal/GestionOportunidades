package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.EvidenciaRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Evidencia
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Estados
import duoc.example.gestionoportunidades.util.Permisos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleUiState(
    val oportunidad: Oportunidad? = null,
    val organizacion: String = "",
    val responsable: String = "",
    val estadosPermitidos: List<String> = emptyList(),
    val evidencias: List<Evidencia> = emptyList(),
    val sinAcceso: Boolean = false,
    val mensajeExito: String? = null,
    val mensajeError: String? = null,
    val isLoading: Boolean = true
)

private data class Mensajes(val exito: String? = null, val error: String? = null)

class DetalleOportunidadViewModel(
    savedStateHandle: SavedStateHandle,
    private val oportunidadRepository: OportunidadRepository,
    organizacionRepository: OrganizacionRepository,
    authRepository: AuthRepository,
    private val evidenciaRepository: EvidenciaRepository
) : ViewModel() {

    private val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("id"))
    private val _mensajes = MutableStateFlow(Mensajes())

    private val datos = oportunidadRepository.observarPorId(idOportunidad).map { op ->
        if (op == null) return@map DetalleUiState(isLoading = false)

        val usuario = SesionManager.usuarioActual
        if (usuario == null ||
            !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, op.idResponsable)
        ) {
            return@map DetalleUiState(sinAcceso = true, isLoading = false)
        }

        DetalleUiState(
            oportunidad = op,
            organizacion = organizacionRepository.obtenerPorId(op.idOrganizacion)?.nombre
                ?: "Sin organización",
            responsable = op.idResponsable
                ?.let { authRepository.obtenerUsuarioPorId(it)?.nombreCompleto }
                ?: "Sin asignar",
            estadosPermitidos = Estados.todos.filter {
                Permisos.puedeCambiarEstado(usuario.rol, op.estado, it)
            },
            isLoading = false
        )
    }

    val uiState: StateFlow<DetalleUiState> = combine(
        datos,
        evidenciaRepository.observarPorOportunidad(idOportunidad),
        _mensajes
    ) { d, evidencias, m ->
        d.copy(evidencias = evidencias, mensajeExito = m.exito, mensajeError = m.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    fun cambiarEstado(nuevoEstado: String) {
        val usuario = SesionManager.usuarioActual ?: return
        val actual = uiState.value.oportunidad ?: return

        if (!Permisos.puedeCambiarEstado(usuario.rol, actual.estado, nuevoEstado)) {
            _mensajes.value = Mensajes(error = "No tienes permiso para este cambio de estado")
            return
        }

        viewModelScope.launch {
            try {
                oportunidadRepository.cambiarEstado(
                    idOportunidad = actual.idOportunidad,
                    estadoAnterior = actual.estado,
                    nuevoEstado = nuevoEstado,
                    idUsuario = usuario.idUsuario
                )
                _mensajes.value = Mensajes(exito = "Estado actualizado a $nuevoEstado")
            } catch (e: Exception) {
                _mensajes.value = Mensajes(error = "No se pudo actualizar: ${e.localizedMessage}")
            }
        }
    }

    /** Guarda en Room la foto recién capturada, asociada a esta oportunidad. */
    fun registrarEvidencia(rutaArchivo: String) {
        viewModelScope.launch {
            try {
                evidenciaRepository.registrar(
                    Evidencia(
                        rutaArchivo = rutaArchivo,
                        fecha = System.currentTimeMillis(),
                        idOportunidad = idOportunidad
                    )
                )
                _mensajes.value = Mensajes(exito = "Foto guardada en la oportunidad")
            } catch (e: Exception) {
                _mensajes.value = Mensajes(error = "No se pudo guardar la foto: ${e.localizedMessage}")
            }
        }
    }

    fun eliminarEvidencia(evidencia: Evidencia) {
        viewModelScope.launch {
            try {
                evidenciaRepository.eliminar(evidencia)
                _mensajes.value = Mensajes(exito = "Foto eliminada")
            } catch (e: Exception) {
                _mensajes.value = Mensajes(error = "No se pudo eliminar la foto: ${e.localizedMessage}")
            }
        }
    }

    fun limpiarMensajes() {
        _mensajes.value = Mensajes()
    }
}