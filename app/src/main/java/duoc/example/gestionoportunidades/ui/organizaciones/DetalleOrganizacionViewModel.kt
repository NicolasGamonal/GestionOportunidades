package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import kotlinx.coroutines.flow.*

data class DetalleOrganizacionUiState(
    val organizacion: Organizacion? = null,
    val contactos: List<Contacto> = emptyList(),
    val puedeEditar: Boolean = false,
    val isLoading: Boolean = true
)

class DetalleOrganizacionViewModel(
    savedStateHandle: SavedStateHandle,
    organizacionRepository: OrganizacionRepository,
    contactoRepository: ContactoRepository
) : ViewModel() {

    private val idOrganizacion: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    val uiState: StateFlow<DetalleOrganizacionUiState> = combine(
        organizacionRepository.observarPorId(idOrganizacion),
        contactoRepository.obtenerPorOrganizacion(idOrganizacion)
    ) { organizacion, contactos ->
        DetalleOrganizacionUiState(
            organizacion = organizacion,
            contactos = contactos,
            puedeEditar = Permisos.puedeEditarOrganizacion(SesionManager.getRol()),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleOrganizacionUiState())
}