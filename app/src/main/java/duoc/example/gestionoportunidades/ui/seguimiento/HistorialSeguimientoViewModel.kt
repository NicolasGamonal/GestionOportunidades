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

// ViewModel del historial de gestiones de una oportunidad.
// Puramente lectura: observa la oportunidad + sus seguimientos en tiempo real.
// Resuelve los nombres de autores y contactos (FK → String) para mostrar en las tarjetas.

// Un item del historial: el seguimiento + datos resueltos para mostrar
data class SeguimientoItem(
    val seguimiento: Seguimiento,
    val autor: String,           // Nombre del usuario que registró (resuelto desde FK)
    val contacto: String?        // "Nombre · Cargo" o null si la gestión no tiene contacto
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

    // id de la oportunidad (viene de la ruta)
    val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("idOportunidad"))

    // Combina 2 Flows reactivos:
    // 1. La oportunidad (para verificar permisos y mostrar el nombre)
    // 2. El historial de seguimientos (se actualiza al agregar una nueva gestión)
    val uiState: StateFlow<HistorialUiState> = combine(
        oportunidadRepository.observarPorId(idOportunidad),
        seguimientoRepository.obtenerHistorial(idOportunidad)
    ) { oportunidad, historial ->
        // Oportunidad no existe
        if (oportunidad == null) {
            return@combine HistorialUiState(noEncontrada = true, isLoading = false)
        }

        // Verificar permisos (el ejecutivo solo ve historial de las suyas)
        val usuario = SesionManager.usuarioActual
        if (usuario == null ||
            !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, oportunidad.idResponsable)
        ) {
            return@combine HistorialUiState(sinAcceso = true, isLoading = false)
        }

        // Resolver nombres de autores y contactos de forma eficiente:
        // Una consulta por usuario DISTINTO (no una por gestión)
        val autores = historial.map { it.idUsuario }.distinct().associateWith { id ->
            authRepository.obtenerUsuarioPorId(id)?.nombreCompleto ?: "Usuario"
        }
        // Una consulta por contacto DISTINTO
        val contactos = historial.mapNotNull { it.idContacto }.distinct().associateWith { id ->
            contactoRepository.obtenerPorId(id)?.let { "${it.nombre} · ${it.cargo}" }
        }

        // Mapear cada seguimiento a un SeguimientoItem con los datos resueltos
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