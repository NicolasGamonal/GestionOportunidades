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
import duoc.example.gestionoportunidades.util.CampoContacto
import duoc.example.gestionoportunidades.util.Catalogos
import duoc.example.gestionoportunidades.util.ValidacionesFormularios

// ViewModel del formulario de crear/editar contacto.
// idOrganizacion: a qué org pertenece (viene de la ruta, obligatorio).
// idEdicion: -1 = crear, > 0 = editar.


// Estado del formulario: 5 campos + errores + estados de pantalla
data class FormularioContactoUiState(
    val nombreOrganizacion: String = "",   // Contexto: a qué org pertenece (no editable)
    val nombre: String = "",
    val cargo: String = "",
    val area: String = "",
    val correo: String = "",
    val telefono: String = "",
    val medioPreferente: String = "Correo",   // Chips: siempre hay uno elegido (HU-13)
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
                            correo = contacto.correo, telefono = contacto.telefono,
                            medioPreferente = contacto.medioPreferente
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
    // Correo y teléfono: la limpieza de lo escrito la define Validaciones.kt
    fun onCorreoChange(v: String) = editar(CampoContacto.CORREO) { it.copy(correo = Validaciones.filtrarCorreo(v)) }
    fun onTelefonoChange(v: String) =
        editar(CampoContacto.TELEFONO) { it.copy(telefono = Validaciones.filtrarTelefono(v)) }

    // Los chips no tienen error que limpiar, por eso no pasa por editar().
    fun onMedioChange(v: String) = _uiState.update { it.copy(medioPreferente = v) }

    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }


    // Valida y guarda en Room (insertar o actualizar)
    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.guardadoExitoso) return

        val errores = ValidacionesFormularios.validarContacto(
            nombre = s.nombre, cargo = s.cargo, area = s.area,
            correo = s.correo, telefono = s.telefono
        )
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
            medioPreferente = s.medioPreferente,
            correo = s.correo.trim(),
            telefono = s.telefono.trim(),
            idOrganizacion = idOrganizacion  // Siempre la org de la ruta
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                // insertar devuelve el id nuevo, o 0L si la API falló (el repositorio atrapa el error).
                // actualizar no informa si falló: eso se arregla en el paso 2b.
                val guardado = if (base == null) {
                    contactoRepository.insertar(contacto) > 0L
                } else {
                    contactoRepository.actualizar(contacto)
                    true
                }
                _uiState.update {
                    if (guardado) it.copy(guardando = false, guardadoExitoso = true)
                    else it.copy(guardando = false, mensajeError = Catalogos.MENSAJE_ERROR_GUARDADO)
                }
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