package duoc.example.gestionoportunidades.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.inicioDeHoyMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// DashboardViewModel: lógica detrás de la pantalla Dashboard (ui/alertas/DashboardScreen.kt).
//
// Responsabilidad:
// - Consultar 2 fuentes de datos en tiempo real (eventos próximos + últimas gestiones).
// - Filtrar las gestiones en "pendientes" (fechaProximo >= hoy) y "atrasadas" (fechaProximo < hoy).
// - Exponer el resultado como un StateFlow<DashboardUiState> que la Screen observa.
//
// ¿Por qué no usa 3 consultas separadas?
// Porque las gestiones pendientes y atrasadas vienen de la MISMA consulta del DAO
// (obtenerUltimasGestiones). Filtrarlas en el ViewModel evita 2 consultas a Room
// y garantiza que ambas listas siempre sean consistentes entre sí.
//
// Patrón MVVM:
// - Recibe los repositorios por inyección de dependencias (desde AppContainer).
// - No conoce Room, DAOs ni Composables. Solo habla con repositorios.
// - Expone un único StateFlow: la Screen no necesita saber de dónde vienen los datos.

// Constante: 0L significa "todo el equipo" en las queries del DAO.
// El DAO usa el truco (:idResponsable = 0 OR idResponsable = :idResponsable)
// para que 0 devuelva todas las oportunidades (supervisor) y un id real devuelva solo las de ese ejecutivo.
private const val TODO_EL_EQUIPO = 0L

// Constante: -1L significa "nadie" (no hay sesión activa).
// Si por algún motivo no hay usuario logueado, el dashboard no muestra nada
// (las queries con idResponsable = -1 no coinciden con ningún registro).
private const val NADIE = -1L

class DashboardViewModel(
    // Repositorio de oportunidades: se usa para obtener los eventos próximos.
    // Se inyecta desde AppContainer (no se crea aquí).
    oportunidadRepository: OportunidadRepository,
    // Repositorio de seguimientos: se usa para obtener las últimas gestiones del equipo.
    seguimientoRepository: SeguimientoRepository
) : ViewModel() {

    // Determina qué idResponsable usar en las consultas, según el rol del usuario logueado:
    // - SUPERVISOR → 0L (TODO_EL_EQUIPO): ve todas las oportunidades y gestiones.
    // - EJECUTIVO → su propio idUsuario: solo ve las suyas.
    // - Sin sesión → -1L (NADIE): no ve nada (caso de seguridad, no debería ocurrir).
    //
    // Se calcula UNA SOLA VEZ al construir el ViewModel (no es un StateFlow)
    // porque el rol no cambia durante la vida de la pantalla (si cambia, hay que re-login).
    private val idResponsable: Long = idResponsableDeLaSesion()

    // El estado que la Screen observa. Se construye combinando 2 Flows:
    //
    // 1. oportunidadRepository.obtenerEventosProximos(inicioDeHoyMillis(), idResponsable)
    //    → Flow<List<Oportunidad>>: oportunidades con fechaInicio >= hoy que no estén descartadas.
    //    → Si idResponsable = 0, devuelve todas; si es un id, solo las de ese ejecutivo.
    //
    // 2. seguimientoRepository.obtenerUltimasGestiones(idResponsable)
    //    → Flow<List<AlertaSeguimiento>>: última gestión real de cada oportunidad activa (JOIN con usuarios).
    //    → Mismo truco: 0 = todo el equipo, id = solo ese ejecutivo.
    //
    // combine(): se re-ejecuta cada vez que CUALQUIERA de los 2 Flows emite un valor nuevo.
    // Si se agrega una oportunidad, se cambia un estado, se registra una gestión...
    // el combine se dispara y recalcula el DashboardUiState completo.
    //
    // Dentro del combine:
    // - "ahora" se calcula en cada emisión (no se hardcodea) para que el filtro
    //   de pendientes/atrasadas siempre use la fecha actual.
    // - gestiones.filter { it.fechaProximo >= ahora } → pendientes (hoy o futuro)
    // - gestiones.filter { it.fechaProximo < ahora } → atrasadas (ya vencieron)
    //
    // stateIn(): convierte el Flow combinado en un StateFlow (cachea el último valor).
    // - SharingStarted.WhileSubscribed(5_000): el Flow se cancela 5 segundos después
    //   de que la última Screen se desuscribe (ahorra recursos si el usuario navega lejos).
    // - DashboardUiState(): valor inicial (isLoading = true por defecto en el data class).
    val uiState: StateFlow<DashboardUiState> = combine(
        oportunidadRepository.obtenerEventosProximos(inicioDeHoyMillis(), idResponsable),
        seguimientoRepository.obtenerUltimasGestiones(idResponsable)
    ) { proximos, gestiones ->
        val ahora = System.currentTimeMillis()
        DashboardUiState(
            isLoading = false,  // Ya hay datos: se desactiva el CircularProgressIndicator
            eventosProximos = proximos,
            seguimientosPendientes = gestiones.filter { it.fechaProximo >= ahora },
            seguimientosAtrasados = gestiones.filter { it.fechaProximo < ahora }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    // Determina el idResponsable según la sesión actual.
    // Se llama una sola vez en el initializer de la propiedad idResponsable.
    private fun idResponsableDeLaSesion(): Long {
        // Si no hay usuario logueado (no debería pasar, pero es una guarda de seguridad)
        val usuario = SesionManager.usuarioActual ?: return NADIE
        // Supervisor → ve todo el equipo (0L)
        // Ejecutivo → solo las suyas (su propio id)
        return if (usuario.rol == "SUPERVISOR") TODO_EL_EQUIPO else usuario.idUsuario
    }
}