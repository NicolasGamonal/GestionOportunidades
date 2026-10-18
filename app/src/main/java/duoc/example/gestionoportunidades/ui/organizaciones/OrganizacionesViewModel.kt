package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class OrganizacionesUiState(
    val organizaciones: List<Organizacion> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizacionesViewModel(repository: OrganizacionRepository) : ViewModel() {

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    val uiState: StateFlow<OrganizacionesUiState> = _busqueda
        .flatMapLatest { repository.buscarPorNombre(it) }
        .map { OrganizacionesUiState(it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizacionesUiState())

    fun onBusquedaChange(valor: String) {
        _busqueda.value = valor
    }
}