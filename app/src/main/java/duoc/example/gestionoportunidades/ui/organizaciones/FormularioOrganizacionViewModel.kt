package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import duoc.example.gestionoportunidades.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoOrganizacion { NOMBRE, TIPO, RUBRO, COMUNA }

data class FormularioOrganizacionUiState(
    val nombre: String = "",
    val tipo: String = "",
    val rubro: String = "",
    val comuna: String = "",
    val errores: Map<CampoOrganizacion, String> = emptyMap(),
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val noEncontrada: Boolean = false,
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null,
    val sinPermiso: Boolean = false,
    val puedeEliminar: Boolean = false,
    val eliminado: Boolean = false
)

class FormularioOrganizacionViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: OrganizacionRepository
) : ViewModel() {

    private val idEdicion: Long = savedStateHandle.get<Long>("id") ?: -1L
    private var original: Organizacion? = null

    private val _uiState = MutableStateFlow(
        FormularioOrganizacionUiState(esEdicion = idEdicion > 0, cargando = idEdicion > 0)
    )
    val uiState: StateFlow<FormularioOrganizacionUiState> = _uiState.asStateFlow()

    val sinPermiso: Boolean = false

    init {
        if (idEdicion > 0) {
            if (!Permisos.puedeEditarOrganizacion(SesionManager.getRol())) {
                _uiState.update { it.copy(cargando = false, sinPermiso = true) }
            } else {
                viewModelScope.launch {
                    val org = repository.obtenerPorId(idEdicion)
                    if (org == null) {
                        _uiState.update { it.copy(cargando = false, noEncontrada = true) }
                    } else {
                        original = org
                        _uiState.update {
                            it.copy(
                                cargando = false,
                                nombre = org.nombre, tipo = org.tipo,
                                rubro = org.rubro, comuna = org.comuna
                            )
                        }
                        _uiState.update {
                            it.copy(
                                cargando = false,
                                nombre = org.nombre, tipo = org.tipo,
                                rubro = org.rubro, comuna = org.comuna,
                                puedeEliminar = Permisos.puedeEliminarOrganizacion(SesionManager.getRol())
                            )
                        }
                    }
                }
            }
        }
    }

    private fun editar(campo: CampoOrganizacion, cambio: (FormularioOrganizacionUiState) -> FormularioOrganizacionUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(errores = nuevo.errores - campo)
        }

    fun onNombreChange(v: String) = editar(CampoOrganizacion.NOMBRE) { it.copy(nombre = v) }
    fun onTipoChange(v: String) = editar(CampoOrganizacion.TIPO) { it.copy(tipo = v) }
    fun onRubroChange(v: String) = editar(CampoOrganizacion.RUBRO) { it.copy(rubro = v) }
    fun onComunaChange(v: String) = editar(CampoOrganizacion.COMUNA) { it.copy(comuna = v) }
    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

    private fun validar(s: FormularioOrganizacionUiState): Map<CampoOrganizacion, String> {
        val resultados = listOf<Pair<CampoOrganizacion, String?>>(
            CampoOrganizacion.NOMBRE to (
                    Validaciones.validarObligatorio(s.nombre, "El nombre")
                        ?: Validaciones.validarLongitudMin(s.nombre.trim(), 3, "El nombre")
                        ?: Validaciones.validarLongitudMax(s.nombre, 80, "El nombre")
                    ),
            CampoOrganizacion.TIPO to Validaciones.validarObligatorio(s.tipo, "El tipo de organización"),
            CampoOrganizacion.RUBRO to (
                    Validaciones.validarObligatorio(s.rubro, "El rubro")
                        ?: Validaciones.validarLongitudMax(s.rubro, 50, "El rubro")
                    ),
            CampoOrganizacion.COMUNA to (
                    Validaciones.validarObligatorio(s.comuna, "La comuna")
                        ?: Validaciones.validarLongitudMax(s.comuna, 40, "La comuna")
                    )
        )
        return resultados.mapNotNull { (campo, error) -> error?.let { campo to it } }.toMap()
    }

    fun guardar() {
        val s = _uiState.value
        if (s.guardando || s.guardadoExitoso) return

        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }   // los valores escritos se conservan
            return
        }

        val base = original
        val organizacion = Organizacion(
            idOrganizacion = base?.idOrganizacion ?: 0,
            nombre = s.nombre.trim(),
            tipo = s.tipo,
            rubro = s.rubro.trim(),
            comuna = s.comuna.trim()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                if (base == null) repository.insertar(organizacion) else repository.actualizar(organizacion)
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
                val motivo = repository.eliminar(idEdicion)
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