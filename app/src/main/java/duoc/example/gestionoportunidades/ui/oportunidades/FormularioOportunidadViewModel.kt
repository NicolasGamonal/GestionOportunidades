package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import duoc.example.gestionoportunidades.util.Validaciones
import duoc.example.gestionoportunidades.util.comoFechaCorta
import duoc.example.gestionoportunidades.util.fechaCortaAMillis
import duoc.example.gestionoportunidades.util.hoyComoFechaCorta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

enum class CampoOportunidad {
    NOMBRE, TIPO, ORGANIZACION, DETECCION, INICIO, TERMINO, COMUNA, PUBLICO, COSTO, OBSERVACIONES
}

data class FormularioUiState(
    val nombre: String = "",
    val tipoEvento: String = "",
    val idOrganizacion: Long? = null,
    val fechaDeteccion: String = "",
    val fechaInicio: String = "",
    val fechaTermino: String = "",
    val comuna: String = "",
    val publicoEsperado: String = "",
    val prioridad: String = "Media",
    val costoReferencial: String = "",
    val observaciones: String = "",
    val errores: Map<CampoOportunidad, String> = emptyMap(),
    val organizaciones: List<Organizacion> = emptyList(),
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val sinAcceso: Boolean = false,
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null,
    val puedeEliminar: Boolean = false,
    val eliminado: Boolean = false
)

class FormularioOportunidadViewModel(
    savedStateHandle: SavedStateHandle,
    private val oportunidadRepository: OportunidadRepository,
    organizacionRepository: OrganizacionRepository
) : ViewModel() {

    // -1 = crear una nueva; un id positivo = editar esa oportunidad
    private val idEdicion: Long = savedStateHandle.get<Long>("id") ?: -1L
    private var original: Oportunidad? = null

    private val _uiState = MutableStateFlow(
        FormularioUiState(
            esEdicion = idEdicion > 0,
            cargando = idEdicion > 0,
            fechaDeteccion = hoyComoFechaCorta()
        )
    )
    val uiState: StateFlow<FormularioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            organizacionRepository.obtenerTodas().collect { lista ->
                _uiState.update { it.copy(organizaciones = lista) }
            }
        }
        if (idEdicion > 0) cargarParaEdicion()
    }

    private fun cargarParaEdicion() {
        viewModelScope.launch {
            val op = oportunidadRepository.obtenerPorId(idEdicion)
            val usuario = SesionManager.usuarioActual
            if (op == null || usuario == null ||
                !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, op.idResponsable)
            ) {
                _uiState.update { it.copy(cargando = false, sinAcceso = true) }
                return@launch
            }
            original = op
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = op.nombre,
                    tipoEvento = op.tipoEvento,
                    idOrganizacion = op.idOrganizacion,
                    fechaDeteccion = op.fechaDeteccion.comoFechaCorta(),
                    fechaInicio = op.fechaInicio.comoFechaCorta(),
                    fechaTermino = op.fechaTermino.comoFechaCorta(),
                    comuna = op.comuna,
                    publicoEsperado = op.publicoEsperado.toString(),
                    prioridad = op.prioridad,
                    costoReferencial = op.costoReferencial.toLong().toString(),
                    observaciones = op.observaciones,
                    puedeEliminar = op.estado == "Identificada" &&
                            Permisos.puedeEliminarOportunidad(usuario.rol, usuario.idUsuario, op.idResponsable)
                )
            }
        }
    }

    // --- Eventos de la pantalla: al editar un campo se borra su error ---

    private fun editar(campo: CampoOportunidad?, cambio: (FormularioUiState) -> FormularioUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            if (campo != null) nuevo.copy(errores = nuevo.errores - campo) else nuevo
        }

    fun onNombreChange(v: String) = editar(CampoOportunidad.NOMBRE) { it.copy(nombre = v) }
    fun onTipoChange(v: String) = editar(CampoOportunidad.TIPO) { it.copy(tipoEvento = v) }
    fun onOrganizacionChange(id: Long) = editar(CampoOportunidad.ORGANIZACION) { it.copy(idOrganizacion = id) }
    fun onDeteccionChange(v: String) = editar(CampoOportunidad.DETECCION) { it.copy(fechaDeteccion = v) }
    fun onInicioChange(v: String) = editar(CampoOportunidad.INICIO) { it.copy(fechaInicio = v) }
    fun onTerminoChange(v: String) = editar(CampoOportunidad.TERMINO) { it.copy(fechaTermino = v) }
    fun onComunaChange(v: String) = editar(CampoOportunidad.COMUNA) { it.copy(comuna = v) }
    fun onPublicoChange(v: String) =
        editar(CampoOportunidad.PUBLICO) { it.copy(publicoEsperado = v.filter(Char::isDigit).take(7)) }
    fun onPrioridadChange(v: String) = editar(null) { it.copy(prioridad = v) }
    fun onCostoChange(v: String) =
        editar(CampoOportunidad.COSTO) { it.copy(costoReferencial = v.filter(Char::isDigit).take(10)) }
    fun onObservacionesChange(v: String) =
        editar(CampoOportunidad.OBSERVACIONES) { it.copy(observaciones = v) }

    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

    // --- Validación (usa util/Validaciones.kt) ---

    private fun validar(s: FormularioUiState): Map<CampoOportunidad, String> {
        val resultados = listOf<Pair<CampoOportunidad, String?>>(
            CampoOportunidad.NOMBRE to (
                    Validaciones.validarObligatorio(s.nombre, "El nombre")
                        ?: Validaciones.validarLongitudMin(s.nombre.trim(), 3, "El nombre")
                        ?: Validaciones.validarLongitudMax(s.nombre, 80, "El nombre")
                    ),
            CampoOportunidad.TIPO to Validaciones.validarObligatorio(s.tipoEvento, "El tipo de evento"),
            CampoOportunidad.ORGANIZACION to
                    if (s.idOrganizacion == null) "Selecciona una organización" else null,
            CampoOportunidad.DETECCION to (
                    Validaciones.validarFecha(s.fechaDeteccion, "La fecha de detección")
                        ?: Validaciones.validarDeteccionAntesInicio(s.fechaDeteccion, s.fechaInicio)
                    ),
            CampoOportunidad.INICIO to Validaciones.validarFecha(s.fechaInicio, "La fecha de inicio"),
            CampoOportunidad.TERMINO to (
                    Validaciones.validarFecha(s.fechaTermino, "La fecha de término")
                        ?: Validaciones.validarFechaTerminoDespuesInicio(s.fechaInicio, s.fechaTermino)
                    ),
            CampoOportunidad.COMUNA to (
                    Validaciones.validarObligatorio(s.comuna, "La comuna")
                        ?: Validaciones.validarLongitudMax(s.comuna, 40, "La comuna")
                    ),
            CampoOportunidad.PUBLICO to Validaciones.validarNumeroPositivo(s.publicoEsperado, "El público esperado"),
            CampoOportunidad.COSTO to Validaciones.validarNumeroNoNegativo(s.costoReferencial, "El costo referencial"),
            CampoOportunidad.OBSERVACIONES to Validaciones.validarLongitudMax(s.observaciones, 500, "Las observaciones")
        )
        return resultados.mapNotNull { (campo, error) -> error?.let { campo to it } }.toMap()
    }

    // --- Guardado ---

    // Si la fecha no cambió respecto a la guardada, se conserva su hora original
    private fun aMillis(fecha: String, original: Long?, horaPorDefecto: LocalTime): Long =
        if (original != null && original.comoFechaCorta() == fecha) original
        else fecha.fechaCortaAMillis(horaPorDefecto)

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.guardadoExitoso) return

        val errores = validar(s)
        if (errores.isNotEmpty()) {
            // Se marcan los errores y se conservan todos los valores escritos
            _uiState.update { it.copy(errores = errores) }
            return
        }

        val usuario = SesionManager.usuarioActual
        if (usuario == null) {
            _uiState.update { it.copy(mensajeError = "Tu sesión expiró. Vuelve a iniciar sesión.") }
            return
        }

        val base = original
        val oportunidad = Oportunidad(
            idOportunidad = base?.idOportunidad ?: 0,
            nombre = s.nombre.trim(),
            tipoEvento = s.tipoEvento,
            fechaDeteccion = aMillis(s.fechaDeteccion, base?.fechaDeteccion, LocalTime.MIDNIGHT),
            fechaInicio = aMillis(s.fechaInicio, base?.fechaInicio, LocalTime.of(9, 0)),
            fechaTermino = aMillis(s.fechaTermino, base?.fechaTermino, LocalTime.of(18, 0)),
            comuna = s.comuna.trim(),
            publicoEsperado = s.publicoEsperado.toInt(),
            prioridad = s.prioridad,
            estado = base?.estado ?: "Identificada",
            costoReferencial = s.costoReferencial.toDouble(),
            observaciones = s.observaciones.trim(),
            idOrganizacion = checkNotNull(s.idOrganizacion),
            // El ejecutivo que crea queda como responsable; el supervisor la deja sin asignar
            idResponsable = base?.idResponsable
                ?: if (usuario.rol == "EJECUTIVO") usuario.idUsuario else null
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                if (base == null) {
                    oportunidadRepository.guardarOportunidad(oportunidad)
                } else {
                    oportunidadRepository.modificarOportunidad(oportunidad)
                }
                _uiState.update { it.copy(guardando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, mensajeError = "No se pudo guardar: ${e.localizedMessage}")
                }
            }
        }
    }
    fun eliminar() {
        val s = _uiState.value
        if (!s.esEdicion || !s.puedeEliminar || s.guardando || s.guardadoExitoso || s.eliminado) return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                val motivo = oportunidadRepository.eliminar(idEdicion)
                _uiState.update {
                    if (motivo == null) it.copy(guardando = false, eliminado = true)
                    else it.copy(guardando = false, mensajeError = motivo)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, mensajeError = "No se pudo eliminar: ${e.localizedMessage}")
                }
            }
        }
    }
}