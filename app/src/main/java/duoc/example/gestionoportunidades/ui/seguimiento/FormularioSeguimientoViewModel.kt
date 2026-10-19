package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.Contacto
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

// ViewModel del formulario de nueva gestión.
// Solo crea (no edita ni elimina: el historial es inmutable).
// Verifica que el usuario pueda ver la oportunidad antes de permitir registrar.

enum class CampoSeguimiento { TIPO, RESULTADO, PROXIMA_ACCION, FECHA_PROXIMA }

data class FormularioSeguimientoUiState(
    val nombreOportunidad: String = "",       // Contexto: a qué oportunidad se asocia
    val contactos: List<Contacto> = emptyList(), // Contactos de la org (para el dropdown opcional)
    val idContacto: Long? = null,             // Contacto seleccionado (null = "Sin contacto")
    val tipoAccion: String = "",
    val resultado: String = "",
    val proximaAccion: String = "",
    val fechaProximo: String = "",
    val errores: Map<CampoSeguimiento, String> = emptyMap(),
    val cargando: Boolean = true,
    val sinAcceso: Boolean = false,           // true si el usuario no puede ver esta oportunidad
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class FormularioSeguimientoViewModel(
    savedStateHandle: SavedStateHandle,
    private val seguimientoRepository: SeguimientoRepository,
    oportunidadRepository: OportunidadRepository,
    contactoRepository: ContactoRepository
) : ViewModel() {

    // id de la oportunidad (viene de la ruta: "form_seguimiento/{idOportunidad}")
    private val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("idOportunidad"))

    private val _uiState = MutableStateFlow(FormularioSeguimientoUiState())
    val uiState: StateFlow<FormularioSeguimientoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Cargar la oportunidad y verificar permisos
            val oportunidad = oportunidadRepository.obtenerPorId(idOportunidad)
            val usuario = SesionManager.usuarioActual
            if (oportunidad == null || usuario == null ||
                !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, oportunidad.idResponsable)
            ) {
                _uiState.update { it.copy(cargando = false, sinAcceso = true) }
            } else {
                _uiState.update { it.copy(cargando = false, nombreOportunidad = oportunidad.nombre) }
                // Cargar contactos de la org (reactivo: si se agrega uno, el dropdown se actualiza)
                contactoRepository.obtenerPorOrganizacion(oportunidad.idOrganizacion).collect { lista ->
                    _uiState.update { it.copy(contactos = lista) }
                }
            }
        }
    }

    // Actualiza un campo y limpia su error
    private fun editar(campo: CampoSeguimiento, cambio: (FormularioSeguimientoUiState) -> FormularioSeguimientoUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(errores = nuevo.errores - campo)
        }

    fun onTipoChange(v: String) = editar(CampoSeguimiento.TIPO) { it.copy(tipoAccion = v) }
    fun onResultadoChange(v: String) = editar(CampoSeguimiento.RESULTADO) { it.copy(resultado = v) }
    fun onProximaAccionChange(v: String) = editar(CampoSeguimiento.PROXIMA_ACCION) { it.copy(proximaAccion = v) }
    fun onFechaProximaChange(v: String) = editar(CampoSeguimiento.FECHA_PROXIMA) { it.copy(fechaProximo = v) }
    // Contacto es opcional: no tiene validación, no limpia errores
    fun onContactoChange(id: Long?) = _uiState.update { it.copy(idContacto = id) }
    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

    // Valida los 4 campos obligatorios
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
            // Fecha debe ser válida Y >= hoy (no se puede programar un seguimiento en el pasado)
            CampoSeguimiento.FECHA_PROXIMA to (
                    Validaciones.validarFecha(s.fechaProximo, "La fecha del próximo seguimiento")
                        ?: Validaciones.validarProximoSeguimiento(hoyComoFechaCorta(), s.fechaProximo)
                    )
        )
        return resultados.mapNotNull { (campo, error) -> error?.let { campo to it } }.toMap()
    }

    // Valida y guarda en Room
    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.guardadoExitoso) return

        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        val usuario = SesionManager.usuarioActual
        if (usuario == null) {
            _uiState.update { it.copy(mensajeError = "Tu sesión expiró. Vuelve a iniciar sesión.") }
            return
        }

        // Construye el Seguimiento:
        // - fechaHora = ahora (cuándo se registró la gestión)
        // - fechaProximo = la fecha elegida con hora 23:59 (fin del día, para que el dashboard
        //   la considere "pendiente" durante todo ese día)
        val seguimiento = Seguimiento(
            idOportunidad = idOportunidad,
            idUsuario = usuario.idUsuario,
            tipoAccion = s.tipoAccion,
            fechaHora = System.currentTimeMillis(),
            resultado = s.resultado.trim(),
            proximaAccion = s.proximaAccion.trim(),
            fechaProximo = s.fechaProximo.fechaCortaAMillis(LocalTime.of(23, 59)),
            idContacto = s.idContacto   // nullable: puede ser "Sin contacto"
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