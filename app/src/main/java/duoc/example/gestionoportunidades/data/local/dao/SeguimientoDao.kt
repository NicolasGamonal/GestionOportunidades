package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

// DAO para la tabla "seguimientos" (gestiones, llamadas, emails, reuniones, cambios de estado).
// Es la tabla que mantiene el historial de cada oportunidad.
// Cada cambio de estado también se guarda aquí como tipoAccion = "Cambio de estado".
// El repositorio llama a estas funciones; el ViewModel nunca toca este archivo.
@Dao
interface SeguimientoDao {

    // Inserta un seguimiento nuevo (una gestión o un cambio de estado).
    // REPLACE: si ya existe uno con el mismo id, lo sobrescribe.
    // Devuelve el id del seguimiento insertado.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSeguimiento(seguimiento: Seguimiento): Long

    // Actualiza un seguimiento existente (por su id).
    // Se usa si se edita el resultado o la próxima acción de una gestión ya registrada.
    @Update
    suspend fun actualizarSeguimiento(seguimiento: Seguimiento)

    // Devuelve el historial completo de una oportunidad (todas sus gestiones y cambios de estado),
    // ordenado de más reciente a más antiguo (fechaHora DESC).
    // Se usa en la pantalla de detalle de oportunidad, sección "Historial".
    @Query("SELECT * FROM seguimientos WHERE idOportunidad = :idOportunidad ORDER BY fechaHora DESC")
    fun obtenerHistorialPorOportunidad(idOportunidad: Long): Flow<List<Seguimiento>>

    // Devuelve los seguimientos que están ATRASADOS: su fechaProximo ya pasó
    // y no son cambios de estado (solo gestiones reales como llamadas, emails, reuniones).
    // Se usa en el Dashboard para mostrar "gestiones atrasadas" (alerta roja).
    @Query("SELECT * FROM seguimientos WHERE fechaProximo < :fechaActual AND tipoAccion != 'Cambio de estado'")
    fun obtenerSeguimientosAtrasados(fechaActual: Long): Flow<List<Seguimiento>>

    // Devuelve los seguimientos PENDIENTES de un usuario: su fechaProximo es hoy o en el futuro.
    // Se usa en el Dashboard para mostrar "seguimientos pendientes" (próximas gestiones a hacer).
    // Ordenados por fechaProximo ASC (los más urgentes primero).
    @Query("SELECT * FROM seguimientos WHERE idUsuario = :idUsuario AND fechaProximo >= :fechaActual ORDER BY fechaProximo ASC")
    fun obtenerPendientesPorUsuario(idUsuario: Long, fechaActual: Long): Flow<List<Seguimiento>>

    // Devuelve todos los seguimientos registrados por un usuario (histórico completo).
    // Se usa en el panel del supervisor para ver la actividad de un ejecutivo.
    @Query("SELECT * FROM seguimientos WHERE idUsuario = :idUsuario ORDER BY fechaHora DESC")
    fun obtenerPorUsuario(idUsuario: Long): Flow<List<Seguimiento>>

    // Consulta compleja (JOIN): obtiene la ÚLTIMA gestión real (no cambio de estado)
    // de cada oportunidad que no esté en estado final (Confirmada o Descartada).
    // Devuelve un objeto AlertaSeguimiento con: nombre de la oportunidad, responsable,
    // tipo de acción, próxima acción y fecha próxima.
    // idResponsable = 0 → todo el equipo (supervisor); otro valor → solo ese ejecutivo.
    // Se usa en el Dashboard para mostrar el resumen de gestiones activas.
    @Query("""
    SELECT s.idOportunidad AS idOportunidad,
           o.nombre AS nombreOportunidad,
           u.nombreCompleto AS nombreResponsable,
           s.tipoAccion AS tipoAccion,
           s.proximaAccion AS proximaAccion,
           s.fechaProximo AS fechaProximo
    FROM seguimientos s
    INNER JOIN oportunidades o ON o.idOportunidad = s.idOportunidad
    LEFT JOIN usuarios u ON u.idUsuario = o.idResponsable
    WHERE s.tipoAccion != 'Cambio de estado'
      AND s.fechaHora = (
          SELECT MAX(s2.fechaHora) FROM seguimientos s2
          WHERE s2.idOportunidad = s.idOportunidad
            AND s2.tipoAccion != 'Cambio de estado'
      )
      AND o.estado NOT IN ('Confirmada', 'Descartada')
      AND (:idResponsable = 0 OR o.idResponsable = :idResponsable)
    ORDER BY s.fechaProximo ASC
""")
    fun obtenerUltimasGestiones(idResponsable: Long): Flow<List<AlertaSeguimiento>>

    // Cuenta cuántos seguimientos tiene una oportunidad.
    // Se usa para mostrar un contador en la UI (ej: "5 gestiones")
    // o para validar antes de borrar la oportunidad.
    @Query("SELECT COUNT(*) FROM seguimientos WHERE idOportunidad = :idOportunidad")
    suspend fun contarPorOportunidad(idOportunidad: Long): Int

    // Cuenta cuántos seguimientos están asociados a un contacto específico.
    // Se usa antes de borrar un contacto para saber si tiene historial.
    @Query("SELECT COUNT(*) FROM seguimientos WHERE idContacto = :idContacto")
    suspend fun contarPorContacto(idContacto: Long): Int

    // Cuenta cuántos seguimientos están asociados a CUALQUIER contacto de una organización.
    // Se usa antes de borrar una organización: si sus contactos tienen seguimientos,
    // hay que manejar la cascada.
    @Query("""
    SELECT COUNT(*) FROM seguimientos
    WHERE idContacto IN (SELECT idContacto FROM contactos WHERE idOrganizacion = :idOrganizacion)
""")
    suspend fun contarPorContactosDeOrganizacion(idOrganizacion: Long): Int
}