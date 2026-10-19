package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

// Repositorio de seguimientos (gestiones, llamadas, emails, reuniones, cambios de estado).
// Capa intermedia entre los ViewModels (SeguimientoViewModel, DashboardViewModel, SupervisorViewModel)
// y el DAO (SeguimientoDao).
// El ViewModel llama a estas funciones sin saber que detrás hay Room/SQLite.
//
// Este repositorio no tiene reglas de negocio complejas (a diferencia de OportunidadRepository
// o OrganizacionRepository): solo expone las consultas del DAO directamente.
// La lógica de negocio (qué se puede borrar, qué se puede cambiar) vive en los ViewModels
// y en Permisos.kt, no aquí.
class SeguimientoRepository(private val db: AppDatabase) {

    // Referencia al DAO de seguimientos. Se obtiene una sola vez al construir el repositorio.
    private val dao = db.seguimientoDao()

    // Devuelve el historial completo de una oportunidad (todas sus gestiones y cambios de estado),
    // ordenado de más reciente a más antiguo.
    // Flow: si se agrega una nueva gestión, la lista del historial se actualiza sola.
    // Se usa en la pantalla de detalle de oportunidad, sección "Historial".
    fun obtenerHistorial(idOportunidad: Long): Flow<List<Seguimiento>> = dao.obtenerHistorialPorOportunidad(idOportunidad)

    // Devuelve los seguimientos que están ATRASADOS: su fechaProximo ya pasó
    // y no son cambios de estado (solo gestiones reales).
    // Se usa en el Dashboard para la sección "Gestiones atrasadas" (alerta roja).
    // fechaActual: se pasa System.currentTimeMillis() desde el ViewModel.
    fun obtenerAtrasados(fechaActual: Long): Flow<List<Seguimiento>> = dao.obtenerSeguimientosAtrasados(fechaActual)

    // Devuelve los seguimientos PENDIENTES de un usuario: su fechaProximo es hoy o en el futuro.
    // Se usa en el Dashboard para la sección "Seguimientos pendientes" (próximas gestiones a hacer).
    // Ordenados por fechaProximo ASC (los más urgentes primero).
    // idUsuario: el ejecutivo logueado; el supervisor puede pasar 0 si se adapta la query.
    fun obtenerPendientesPorUsuario(idUsuario: Long, fechaActual: Long): Flow<List<Seguimiento>> =
        dao.obtenerPendientesPorUsuario(idUsuario, fechaActual)

    // Devuelve todos los seguimientos registrados por un usuario (histórico completo).
    // Se usa en el panel del supervisor para ver la actividad de un ejecutivo
    // (cuántas gestiones hizo, de qué tipo, cuándo).
    fun obtenerPorUsuario(idUsuario: Long): Flow<List<Seguimiento>> = dao.obtenerPorUsuario(idUsuario)

    // Inserta un seguimiento nuevo (una gestión: llamada, email, reunión).
    // Se llama desde el formulario de "Nueva gestión" en la pantalla de seguimiento.
    // Devuelve el id asignado por Room.
    // Nota: los cambios de estado NO se insertan desde aquí; se insertan desde
    // OportunidadRepository.cambiarEstado() dentro de una transacción.
    suspend fun insertar(seguimiento: Seguimiento): Long = dao.insertarSeguimiento(seguimiento)

    // Actualiza un seguimiento existente (por su id).
    // Se usa si se edita el resultado o la próxima acción de una gestión ya registrada.
    suspend fun actualizar(seguimiento: Seguimiento) = dao.actualizarSeguimiento(seguimiento)

    // Devuelve la ÚLTIMA gestión real (no cambio de estado) de cada oportunidad activa.
    // Devuelve un objeto AlertaSeguimiento (no Seguimiento) con campos agregados:
    // nombre de la oportunidad y nombre del responsable (obtenidos por JOIN).
    // idResponsable = 0 → todo el equipo (supervisor); otro valor → solo ese ejecutivo.
    // Se usa en el Dashboard para el resumen de gestiones activas del equipo.
    fun obtenerUltimasGestiones(idResponsable: Long): Flow<List<AlertaSeguimiento>> =
        dao.obtenerUltimasGestiones(idResponsable)
}