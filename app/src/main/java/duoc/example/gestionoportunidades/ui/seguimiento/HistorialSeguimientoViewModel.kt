package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.Seguimiento
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SeguimientoItem(
    val seguimiento: Seguimiento,
    val autor: String,
    val contacto: String?          // "Nombre · Cargo", o null si la gestión no tiene contacto
)

data class HistorialUiState(
    val nombreOportunidad: String = "",
    val items: List<SeguimientoItem> = emptyList(),
    val sinAcceso: Boolean = false,
    val noEncontrada: Boolean = false,
    val isLoading: Boolean = true
)

class HistorialSeguimientoViewModel(
    savedStateHandle: SavedStateHandle,
    oportunidadRepository: OportunidadRepository,
    seguimientoRepository: SeguimientoRepository,
    authRepository: AuthRepository,
    contactoRepository: ContactoRepository
) : ViewModel() {

    val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("idOportunidad"))

    val uiState: StateFlow<HistorialUiState> = combine(
        oportunidadRepository.observarPorId(idOportunidad),
        seguimientoRepository.obtenerHistorial(idOportunidad)
    ) { oportunidad, historial ->
        if (oportunidad == null) {
            return@combine HistorialUiState(noEncontrada = true, isLoading = false)
        }

        val usuario = SesionManager.usuarioActual
        if (usuario == null ||
            !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, oportunidad.idResponsable)
        ) {
            return@combine HistorialUiState(sinAcceso = true, isLoading = false)
        }

        // Una consulta por usuario y por contacto distinto, no una por gestión
        val autores = historial.map { it.idUsuario }.distinct().associateWith { id ->
            authRepository.obtenerUsuarioPorId(id)?.nombreCompleto ?: "Usuario"
        }
        val contactos = historial.mapNotNull { it.idContacto }.distinct().associateWith { id ->
            contactoRepository.obtenerPorId(id)?.let { "${it.nombre} · ${it.cargo}" }
        }

        HistorialUiState(
            nombreOportunidad = oportunidad.nombre,
            items = historial.map {
                SeguimientoItem(
                    seguimiento = it,
                    autor = autores.getValue(it.idUsuario),
                    contacto = it.idContacto?.let { id -> contactos[id] }
                )
            },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())
}