package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.GestionOrganizacion
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import kotlinx.coroutines.flow.*

// DetalleOrganizacionViewModel: lógica detrás de la pantalla de detalle de organización.
//
// Responsabilidad:
// - Cargar la organización en tiempo real (Flow reactivo desde Room).
// - Cargar los contactos de esa organización en tiempo real (Flow reactivo desde Room).
// - Calcular si el usuario puede editar la organización (permiso por rol).
// - Exponer todo como un único StateFlow que la Screen observa.
//
// Patrón MVVM:
// - Recibe 2 repositorios por inyección (desde AppViewModelProvider.Factory → AppContainer).
// - Expone un único StateFlow<DetalleOrganizacionUiState> que la Screen observa.
// - No conoce Composables ni navegación.
//
// ¿Por qué SavedStateHandle?
// Porque la ruta de navegación incluye un argumento "id" (ej: "detalle_organizacion/3").
// Navigation Compose lo pasa al ViewModel vía SavedStateHandle.
// El ViewModel lo extrae para saber QUÉ organización cargar.
// checkNotNull: si el argumento no existe por un bug, es mejor crashear que mostrar vacío.

// Estado que la Screen observa.
data class DetalleOrganizacionUiState(
    val organizacion: Organizacion? = null,
    val contactos: List<Contacto> = emptyList(),
    // Historial de gestiones hechas con esta organización (HU-14)
    val gestiones: List<GestionOrganizacion> = emptyList(),
    val puedeEditar: Boolean = false,
    val isLoading: Boolean = true
)

class DetalleOrganizacionViewModel(
    savedStateHandle: SavedStateHandle,

    organizacionRepository: OrganizacionRepository,

    contactoRepository: ContactoRepository,

    // Para consultar las gestiones previas con la organización (HU-14)
    seguimientoRepository: SeguimientoRepository
) : ViewModel() {

    private val idOrganizacion: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    // Mismo criterio de permisos que el resto de la app: el supervisor ve las gestiones de todo el
    // equipo (0 = sin filtrar) y el ejecutivo solo las de sus propias oportunidades.
    // Sin sesión se usa -1, que no coincide con ningún responsable (lista vacía).
    private val idResponsableFiltro: Long = SesionManager.usuarioActual?.let { usuario ->
        if (usuario.rol == "SUPERVISOR") 0L else usuario.idUsuario
    } ?: -1L

    val uiState: StateFlow<DetalleOrganizacionUiState> = combine(
        organizacionRepository.observarPorId(idOrganizacion),
        contactoRepository.obtenerPorOrganizacion(idOrganizacion),
        seguimientoRepository.obtenerHistorialPorOrganizacion(idOrganizacion, idResponsableFiltro)
    ) { organizacion, contactos, gestiones ->
        DetalleOrganizacionUiState(
            organizacion = organizacion,
            contactos = contactos,
            gestiones = gestiones,
            puedeEditar = Permisos.puedeEditarOrganizacion(SesionManager.getRol()),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleOrganizacionUiState())
}
