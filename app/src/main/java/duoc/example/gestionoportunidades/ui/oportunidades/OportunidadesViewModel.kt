package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Regiones
import duoc.example.gestionoportunidades.util.fechaCortaAMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalTime

// Filtros activos de la lista de oportunidades (HU-22).
// Convención: "" (o 0 en idResponsable) significa "sin filtrar por ese criterio".
data class FiltrosOportunidad(
    val busqueda: String = "",
    val estado: String = "",
    val prioridad: String = "",
    val tipoEvento: String = "",
    val region: String = "",
    val comuna: String = "",
    val idResponsable: Long = 0L,     // Solo lo usa el supervisor (el ejecutivo ya ve solo lo suyo)
    val fechaDesde: String = "",      // "dd/MM/yyyy": fecha de inicio del evento desde...
    val fechaHasta: String = ""       // ...hasta (ambas inclusive)
) {
    // true si hay algún filtro activo distinto de la búsqueda por nombre (muestra "Limpiar filtros")
    fun hayFiltros(): Boolean = estado.isNotEmpty() || prioridad.isNotEmpty() || tipoEvento.isNotEmpty() ||
            region.isNotEmpty() || comuna.isNotEmpty() || idResponsable != 0L ||
            fechaDesde.isNotEmpty() || fechaHasta.isNotEmpty()
}

data class OportunidadesUiState(
    val oportunidades: List<Oportunidad> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class OportunidadesViewModel(
    private val repository: OportunidadRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _filtros = MutableStateFlow(FiltrosOportunidad())
    val filtros: StateFlow<FiltrosOportunidad> = _filtros.asStateFlow()

    // Ejecutivos del equipo, para el filtro "Responsable" (solo se muestra al supervisor).
    val ejecutivos: StateFlow<List<Usuario>> = authRepository.obtenerEjecutivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Cada vez que cambian los filtros, flatMapLatest cancela la consulta anterior y lanza una nueva.
    // Room resuelve estado, prioridad, nombre y responsable (rápido, con SQL). Los demás filtros
    // (tipo, región, comuna y fechas) se aplican después en memoria: la lista es pequeña y la región
    // no está en la base de datos (se deriva de la comuna), así que no se puede filtrar con SQL.
    val uiState: StateFlow<OportunidadesUiState> = _filtros
        .flatMapLatest { f ->
            // Marketing solo trabaja con oportunidades confirmadas
            val estado = if (SesionManager.getRol() == "MARKETING") "Confirmada" else f.estado
            repository.buscar(estado, f.prioridad, f.busqueda, idResponsableDeLaSesion(f))
                .map { lista -> OportunidadesUiState(aplicarFiltrosLocales(lista, f), isLoading = false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OportunidadesUiState())

    fun onBusquedaChange(valor: String) = _filtros.update { it.copy(busqueda = valor) }
    fun onEstadoChange(valor: String) = _filtros.update { it.copy(estado = valor) }
    fun onPrioridadChange(valor: String) = _filtros.update { it.copy(prioridad = valor) }
    fun onTipoEventoChange(valor: String) = _filtros.update { it.copy(tipoEvento = valor) }

    // Al cambiar la región se reinicia la comuna (la comuna elegida podría ser de otra región).
    fun onRegionChange(valor: String) = _filtros.update { it.copy(region = valor, comuna = "") }
    fun onComunaChange(valor: String) = _filtros.update { it.copy(comuna = valor) }
    fun onResponsableChange(id: Long) = _filtros.update { it.copy(idResponsable = id) }
    fun onFechaDesdeChange(valor: String) = _filtros.update { it.copy(fechaDesde = valor) }
    fun onFechaHastaChange(valor: String) = _filtros.update { it.copy(fechaHasta = valor) }

    // Limpia todos los filtros pero conserva lo escrito en el buscador por nombre.
    fun limpiarFiltros() = _filtros.update { FiltrosOportunidad(busqueda = it.busqueda) }

    // Seguridad por rol: el supervisor ve todo (0) y el ejecutivo solo lo suyo (su id).
    // Si no hay sesión, -1 no coincide con ningún responsable y la lista queda vacía.
    private fun idResponsableDeLaSesion(f: FiltrosOportunidad): Long {
        val usuario = SesionManager.usuarioActual ?: return -1L
        return when (usuario.rol) {
            "SUPERVISOR" -> f.idResponsable   // 0 = todo el equipo
            "MARKETING" -> 0L                 // ve las confirmadas de todo el equipo
            else -> usuario.idUsuario         // el ejecutivo solo ve las suyas
        }
    }

    private fun aplicarFiltrosLocales(lista: List<Oportunidad>, f: FiltrosOportunidad): List<Oportunidad> {
        val desde = f.fechaDesde.comoMillisOrNull(LocalTime.MIDNIGHT)
        val hasta = f.fechaHasta.comoMillisOrNull(LocalTime.of(23, 59, 59))
        return lista.filter { op ->
            (f.tipoEvento.isEmpty() || op.tipoEvento == f.tipoEvento) &&
                    (f.region.isEmpty() || Regiones.regionDe(op.comuna) == f.region) &&
                    (f.comuna.isEmpty() || op.comuna == f.comuna) &&
                    (desde == null || op.fechaInicio >= desde) &&
                    (hasta == null || op.fechaInicio <= hasta)
        }
    }

    // "dd/MM/yyyy" -> milisegundos; null si el texto está vacío o no es una fecha válida.
    private fun String.comoMillisOrNull(hora: LocalTime): Long? =
        if (isBlank()) null else runCatching { fechaCortaAMillis(hora) }.getOrNull()
}
