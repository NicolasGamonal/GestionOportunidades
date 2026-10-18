package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class FiltrosOportunidad(
    val busqueda: String = "",
    val estado: String = "",      // "" = todos
    val prioridad: String = ""    // "" = todas
)

data class OportunidadesUiState(
    val oportunidades: List<Oportunidad> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class OportunidadesViewModel(
    private val repository: OportunidadRepository
) : ViewModel() {

    private val _filtros = MutableStateFlow(FiltrosOportunidad())
    val filtros: StateFlow<FiltrosOportunidad> = _filtros.asStateFlow()

    val uiState: StateFlow<OportunidadesUiState> = _filtros
        .flatMapLatest { f ->
            repository.buscar(f.estado, f.prioridad, f.busqueda, idResponsableDeLaSesion())
                .map { lista -> OportunidadesUiState(lista, isLoading = false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OportunidadesUiState())

    fun onBusquedaChange(valor: String) = _filtros.update { it.copy(busqueda = valor) }
    fun onEstadoChange(valor: String) = _filtros.update { it.copy(estado = valor) }
    fun onPrioridadChange(valor: String) = _filtros.update { it.copy(prioridad = valor) }

    private fun idResponsableDeLaSesion(): Long {
        val usuario = SesionManager.usuarioActual ?: return -1L   // sin sesión: no se muestra nada
        return if (usuario.rol == "SUPERVISOR") 0L else usuario.idUsuario
    }
    fun limpiarFiltros() = _filtros.update { it.copy(estado = "", prioridad = "") }
}