package duoc.example.gestionoportunidades.ui.resultados

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.ResultadoActivacionRepository
import duoc.example.gestionoportunidades.model.ResultadoActivacion
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.CampoResultado
import duoc.example.gestionoportunidades.util.Catalogos
import duoc.example.gestionoportunidades.util.Permisos
import duoc.example.gestionoportunidades.util.Validaciones
import duoc.example.gestionoportunidades.util.ValidacionesFormularios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado del formulario de resultado de una activación (HU-24).
// Los números se guardan como String porque vienen de campos de texto; se convierten a Int al guardar.
data class FormularioResultadoUiState(
    val nombreOportunidad: String = "",
    val asistentes: String = "",
    val contactosGenerados: String = "",
    val inscripciones: String = "",
    val evaluacion: Int = 3,                 // 1 a 5; por defecto 3 para que siempre haya una elegida
    val aprendizajes: String = "",
    val errores: Map<CampoResultado, String> = emptyMap(),
    val esEdicion: Boolean = false,          // true si la oportunidad ya tenía un resultado registrado
    val cargando: Boolean = true,
    val sinAcceso: Boolean = false,          // no es Confirmada o el usuario no la puede ver
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class FormularioResultadoViewModel(
    savedStateHandle: SavedStateHandle,
    oportunidadRepository: OportunidadRepository,
    private val resultadoRepository: ResultadoActivacionRepository
) : ViewModel() {

    // Argumento de la ruta "formulario_resultado/{idOportunidad}"
    private val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("idOportunidad"))

    // Resultado ya existente (null si es la primera vez). Sirve para conservar su idResultado al editar.
    private var existente: ResultadoActivacion? = null

    private val _uiState = MutableStateFlow(FormularioResultadoUiState())
    val uiState: StateFlow<FormularioResultadoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val oportunidad = oportunidadRepository.obtenerPorId(idOportunidad)
            val usuario = SesionManager.usuarioActual
            if (oportunidad == null || usuario == null ||
                !Permisos.puedeRegistrarResultado(usuario.rol, usuario.idUsuario, oportunidad.idResponsable, oportunidad.estado)
            ) {
                _uiState.update { it.copy(cargando = false, sinAcceso = true) }
                return@launch
            }

            val previo = resultadoRepository.obtenerPorOportunidad(idOportunidad)
            existente = previo
            _uiState.update {
                if (previo == null) {
                    it.copy(cargando = false, nombreOportunidad = oportunidad.nombre)
                } else {
                    it.copy(
                        cargando = false,
                        nombreOportunidad = oportunidad.nombre,
                        esEdicion = true,
                        asistentes = previo.asistentes.toString(),
                        contactosGenerados = previo.contactosGenerados.toString(),
                        inscripciones = previo.inscripciones.toString(),
                        evaluacion = previo.evaluacion,
                        aprendizajes = previo.aprendizajes
                    )
                }
            }
        }
    }

    // Aplica el cambio y borra el error del campo editado (igual que en los otros formularios).
    private fun editar(campo: CampoResultado?, cambio: (FormularioResultadoUiState) -> FormularioResultadoUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            if (campo != null) nuevo.copy(errores = nuevo.errores - campo) else nuevo
        }

    fun onAsistentesChange(v: String) =
        editar(CampoResultado.ASISTENTES) { it.copy(asistentes = Validaciones.filtrarSoloDigitos(v, 7)) }

    fun onContactosChange(v: String) =
        editar(CampoResultado.CONTACTOS) { it.copy(contactosGenerados = Validaciones.filtrarSoloDigitos(v, 6)) }

    fun onInscripcionesChange(v: String) =
        editar(CampoResultado.INSCRIPCIONES) { it.copy(inscripciones = Validaciones.filtrarSoloDigitos(v, 6)) }

    fun onEvaluacionChange(v: Int) = editar(null) { it.copy(evaluacion = v) }

    fun onAprendizajesChange(v: String) =
        editar(CampoResultado.APRENDIZAJES) { it.copy(aprendizajes = v) }

    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.guardadoExitoso) return

        val errores = ValidacionesFormularios.validarResultado(
            asistentes = s.asistentes,
            contactosGenerados = s.contactosGenerados,
            inscripciones = s.inscripciones,
            aprendizajes = s.aprendizajes
        )
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        val usuario = SesionManager.usuarioActual
        if (usuario == null) {
            _uiState.update { it.copy(mensajeError = "Tu sesión expiró. Vuelve a iniciar sesión.") }
            return
        }

        val resultado = ResultadoActivacion(
            idResultado = existente?.idResultado ?: 0,   // 0 = nuevo; el id existente = edición
            idOportunidad = idOportunidad,
            asistentes = s.asistentes.toInt(),
            contactosGenerados = s.contactosGenerados.toInt(),
            inscripciones = s.inscripciones.toInt(),
            evaluacion = s.evaluacion,
            aprendizajes = s.aprendizajes.trim(),
            fechaRegistro = System.currentTimeMillis(),
            idUsuario = usuario.idUsuario
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                val id = resultadoRepository.guardar(resultado)
                _uiState.update {
                    if (id > 0L) it.copy(guardando = false, guardadoExitoso = true)
                    else it.copy(guardando = false, mensajeError = Catalogos.MENSAJE_ERROR_GUARDADO)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, mensajeError = "No se pudo guardar: ${e.localizedMessage}")
                }
            }
        }
    }
}
