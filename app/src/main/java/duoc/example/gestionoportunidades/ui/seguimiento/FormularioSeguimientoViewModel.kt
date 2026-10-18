package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.Seguimiento
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import duoc.example.gestionoportunidades.util.Validaciones
import duoc.example.gestionoportunidades.util.fechaCortaAMillis
import duoc.example.gestionoportunidades.util.hoyComoFechaCorta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

enum class CampoSeguimiento { TIPO, RESULTADO, PROXIMA_ACCION, FECHA_PROXIMA }

data class FormularioSeguimientoUiState(
    val nombreOportunidad: String = "",
    val tipoAccion: String = "",
    val resultado: String = "",
    val proximaAccion: String = "",
    val fechaProximo: String = "",
    val errores: Map<CampoSeguimiento, String> = emptyMap(),
    val cargando: Boolean = true,
    val sinAcceso: Boolean = false,
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class FormularioSeguimientoViewModel(
    savedStateHandle: SavedStateHandle,
    private val seguimientoRepository: SeguimientoRepository,
    oportunidadRepository: OportunidadRepository
) : ViewModel() {

    private val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("idOportunidad"))

    private val _uiState = MutableStateFlow(FormularioSeguimientoUiState())
    val uiState: StateFlow<FormularioSeguimientoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val oportunidad = oportunidadRepository.obtenerPorId(idOportunidad)
            val usuario = SesionManager.usuarioActual
            if (oportunidad == null || usuario == null ||
                !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, oportunidad.idResponsable)
            ) {
                _uiState.update { it.copy(cargando = false, sinAcceso = true) }
            } else {
                _uiState.update { it.copy(cargando = false, nombreOportunidad = oportunidad.nombre) }
            }
        }
    }

    private fun editar(campo: CampoSeguimiento, cambio: (FormularioSeguimientoUiState) -> FormularioSeguimientoUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(errores = nuevo.errores - campo)
        }

    fun onTipoChange(v: String) = editar(CampoSeguimiento.TIPO) { it.copy(tipoAccion = v) }
    fun onResultadoChange(v: String) = editar(CampoSeguimiento.RESULTADO) { it.copy(resultado = v) }
    fun onProximaAccionChange(v: String) = editar(CampoSeguimiento.PROXIMA_ACCION) { it.copy(proximaAccion = v) }
    fun onFechaProximaChange(v: String) = editar(CampoSeguimiento.FECHA_PROXIMA) { it.copy(fechaProximo = v) }
    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

    private fun validar(s: FormularioSeguimientoUiState): Map<CampoSeguimiento, String> {
        val resultados = listOf<Pair<CampoSeguimiento, String?>>(
            CampoSeguimiento.TIPO to Validaciones.validarObligatorio(s.tipoAccion, "El tipo de acción"),
            CampoSeguimiento.RESULTADO to (
                    Validaciones.validarObligatorio(s.resultado, "El resultado")
                        ?: Validaciones.validarLongitudMin(s.resultado.trim(), 5, "El resultado")
                        ?: Validaciones.validarLongitudMax(s.resultado, 300, "El resultado")
                    ),
            CampoSeguimiento.PROXIMA_ACCION to (
                    Validaciones.validarObligatorio(s.proximaAccion, "La próxima acción")
                        ?: Validaciones.validarLongitudMax(s.proximaAccion, 120, "La próxima acción")
                    ),
            CampoSeguimiento.FECHA_PROXIMA to (
                    Validaciones.validarFecha(s.fechaProximo, "La fecha del próximo seguimiento")
                    // la gestión se registra hoy: el próximo seguimiento no puede ser anterior
                        ?: Validaciones.validarProximoSeguimiento(hoyComoFechaCorta(), s.fechaProximo)
                    )
        )
        return resultados.mapNotNull { (campo, error) -> error?.let { campo to it } }.toMap()
    }

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.guardadoExitoso) return

        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }     // los valores escritos se conservan
            return
        }

        val usuario = SesionManager.usuarioActual
        if (usuario == null) {
            _uiState.update { it.copy(mensajeError = "Tu sesión expiró. Vuelve a iniciar sesión.") }
            return
        }

        val seguimiento = Seguimiento(
            idOportunidad = idOportunidad,
            idUsuario = usuario.idUsuario,
            tipoAccion = s.tipoAccion,
            fechaHora = System.currentTimeMillis(),
            resultado = s.resultado.trim(),
            proximaAccion = s.proximaAccion.trim(),
            // vence al terminar el día elegido
            fechaProximo = s.fechaProximo.fechaCortaAMillis(LocalTime.of(23, 59))
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                seguimientoRepository.insertar(seguimiento)
                _uiState.update { it.copy(guardando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, mensajeError = "No se pudo guardar: ${e.localizedMessage}")
                }
            }
        }
    }
}