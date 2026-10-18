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

enum class CampoContacto { NOMBRE, CARGO, AREA, CORREO, TELEFONO }

data class FormularioContactoUiState(
    val nombreOrganizacion: String = "",
    val nombre: String = "",
    val cargo: String = "",
    val area: String = "",
    val correo: String = "",
    val telefono: String = "",
    val errores: Map<CampoContacto, String> = emptyMap(),
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val noEncontrado: Boolean = false,
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class FormularioContactoViewModel(
    savedStateHandle: SavedStateHandle,
    private val contactoRepository: ContactoRepository,
    organizacionRepository: OrganizacionRepository
) : ViewModel() {

    private val idOrganizacion: Long = checkNotNull(savedStateHandle.get<Long>("idOrganizacion"))
    private val idEdicion: Long = savedStateHandle.get<Long>("id") ?: -1L
    private var original: Contacto? = null

    private val _uiState = MutableStateFlow(
        FormularioContactoUiState(esEdicion = idEdicion > 0, cargando = idEdicion > 0)
    )
    val uiState: StateFlow<FormularioContactoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val org = organizacionRepository.obtenerPorId(idOrganizacion)
            _uiState.update { it.copy(nombreOrganizacion = org?.nombre ?: "") }

            if (idEdicion > 0) {
                val contacto = contactoRepository.obtenerPorId(idEdicion)
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

    private fun editar(campo: CampoContacto, cambio: (FormularioContactoUiState) -> FormularioContactoUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(errores = nuevo.errores - campo)
        }

    fun onNombreChange(v: String) = editar(CampoContacto.NOMBRE) { it.copy(nombre = v) }
    fun onCargoChange(v: String) = editar(CampoContacto.CARGO) { it.copy(cargo = v) }
    fun onAreaChange(v: String) = editar(CampoContacto.AREA) { it.copy(area = v) }
    fun onCorreoChange(v: String) = editar(CampoContacto.CORREO) { it.copy(correo = v.replace(" ", "")) }
    fun onTelefonoChange(v: String) =
        editar(CampoContacto.TELEFONO) { it.copy(telefono = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16)) }
    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

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
            idOrganizacion = idOrganizacion
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
}