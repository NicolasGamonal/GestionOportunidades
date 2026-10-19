package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel del formulario de crear/editar contacto.
// idOrganizacion: a qué org pertenece (viene de la ruta, obligatorio).
// idEdicion: -1 = crear, > 0 = editar.

// Identifica cada campo para el mapa de errores
enum class CampoContacto { NOMBRE, CARGO, AREA, CORREO, TELEFONO }

// Estado del formulario: 5 campos + errores + estados de pantalla
data class FormularioContactoUiState(
    val nombreOrganizacion: String = "",   // Contexto: a qué org pertenece (no editable)
    val nombre: String = "",
    val cargo: String = "",
    val area: String = "",
    val correo: String = "",
    val telefono: String = "",
    val errores: Map<CampoContacto, String> = emptyMap(),
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val noEncontrado: Boolean = false,    // true si el contacto no existe o no pertenece a esta org
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null,
    val eliminado: Boolean = false
)

class FormularioContactoViewModel(
    savedStateHandle: SavedStateHandle,
    private val contactoRepository: ContactoRepository,
    organizacionRepository: OrganizacionRepository,
) : ViewModel() {

    // id de la organización (obligatorio, viene de la ruta)
    private val idOrganizacion: Long = checkNotNull(savedStateHandle.get<Long>("idOrganizacion"))
    // -1 = crear, > 0 = editar
    private val idEdicion: Long = savedStateHandle.get<Long>("id") ?: -1L
    // Contacto original (para conservar id al editar)
    private var original: Contacto? = null

    private val _uiState = MutableStateFlow(
        FormularioContactoUiState(esEdicion = idEdicion > 0, cargando = idEdicion > 0)
    )
    val uiState: StateFlow<FormularioContactoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Cargar el nombre de la organización (para mostrar como contexto)
            val org = organizacionRepository.obtenerPorId(idOrganizacion)
            _uiState.update { it.copy(nombreOrganizacion = org?.nombre ?: "") }

            // Si es edición: cargar los datos del contacto
            if (idEdicion > 0) {
                val contacto = contactoRepository.obtenerPorId(idEdicion)
                // Verificar que el contacto exista Y pertenezca a esta org (seguridad)
                if (contacto == null || contacto.idOrganizacion != idOrganizacion) {
                    _uiState.update { it.copy(cargando = false, noEncontrado = true) }
                } else {
                    original = contacto
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            nombre = contacto.nombre, cargo = contacto.cargo, area = contacto.area,
                            correo = contacto.correo, telefono = contacto.telefono
                        )
                    }
                }
            }
        }
    }

    // Actualiza un campo y limpia su error
    private fun editar(campo: CampoContacto, cambio: (FormularioContactoUiState) -> FormularioContactoUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(errores = nuevo.errores - campo)
        }

    // Callbacks de cada campo
    fun onNombreChange(v: String) = editar(CampoContacto.NOMBRE) { it.copy(nombre = v) }
    fun onCargoChange(v: String) = editar(CampoContacto.CARGO) { it.copy(cargo = v) }
    fun onAreaChange(v: String) = editar(CampoContacto.AREA) { it.copy(area = v) }
    // Correo: elimina espacios (no son válidos en un email)
    fun onCorreoChange(v: String) = editar(CampoContacto.CORREO) { it.copy(correo = v.replace(" ", "")) }
    // Teléfono: solo dígitos, + y espacios. Máx 16 caracteres.
    fun onTelefonoChange(v: String) =
        editar(CampoContacto.TELEFONO) { it.copy(telefono = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16)) }

    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

    // Valida los 5 campos con Validaciones.kt
    private fun validar(s: FormularioContactoUiState): Map<CampoContacto, String> {
        val resultados = listOf<Pair<CampoContacto, String?>>(
            CampoContacto.NOMBRE to (
                    Validaciones.validarObligatorio(s.nombre, "El nombre")
                        ?: Validaciones.validarLongitudMin(s.nombre.trim(), 3, "El nombre")
                        ?: Validaciones.validarLongitudMax(s.nombre, 60, "El nombre")
                    ),
            CampoContacto.CARGO to (
                    Validaciones.validarObligatorio(s.cargo, "El cargo")
                        ?: Validaciones.validarLongitudMax(s.cargo, 60, "El cargo")
                    ),
            CampoContacto.AREA to Validaciones.validarObligatorio(s.area, "El área"),
            CampoContacto.CORREO to Validaciones.validarCorreo(s.correo.trim()),
            CampoContacto.TELEFONO to Validaciones.validarTelefono(s.telefono.trim())
        )
        return resultados.mapNotNull { (campo, error) -> error?.let { campo to it } }.toMap()
    }

    // Valida y guarda en Room (insertar o actualizar)
    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.guardadoExitoso) return

        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }

        val base = original
        val contacto = Contacto(
            idContacto = base?.idContacto ?: 0,
            nombre = s.nombre.trim(),
            cargo = s.cargo.trim(),
            area = s.area,
            correo = s.correo.trim(),
            telefono = s.telefono.trim(),
            idOrganizacion = idOrganizacion  // Siempre la org de la ruta
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                if (base == null) contactoRepository.insertar(contacto) else contactoRepository.actualizar(contacto)
                _uiState.update { it.copy(guardando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, mensajeError = "No se pudo guardar: ${e.localizedMessage}")
                }
            }
        }
    }

    // Elimina el contacto (el repositorio verifica que no tenga historial antes de borrar)
    fun eliminar() {
        val s = _uiState.value
        if (!s.esEdicion || s.guardando || s.guardadoExitoso || s.eliminado) return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                val motivo = contactoRepository.eliminar(idEdicion)
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