package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

// ViewModel de la lista de organizaciones.
// Búsqueda por nombre con debounce implícito (flatMapLatest cancela la query anterior).

data class OrganizacionesUiState(
    val organizaciones: List<Organizacion> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizacionesViewModel(repository: OrganizacionRepository) : ViewModel() {

    // Estado de la búsqueda ("" = sin filtro, devuelve todas)
    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    // flatMapLatest: cada vez que cambia la búsqueda, cancela la query anterior
    // y lanza una nueva. Evita que una búsqueda vieja sobrescriba los resultados de la nueva.
    // Si busqueda = "", el DAO hace LIKE '%%' que coincide con todo → devuelve todas.
    val uiState: StateFlow<OrganizacionesUiState> = _busqueda
        .flatMapLatest { repository.buscarPorNombre(it) }
        .map { OrganizacionesUiState(it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizacionesUiState())

    // Se llama desde la Screen en cada tecla del campo de búsqueda
    fun onBusquedaChange(valor: String) {
        _busqueda.value = valor
    }
}