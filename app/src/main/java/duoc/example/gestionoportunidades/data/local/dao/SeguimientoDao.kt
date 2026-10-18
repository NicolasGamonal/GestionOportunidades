package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface SeguimientoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSeguimiento(seguimiento: Seguimiento): Long

    @Update
    suspend fun actualizarSeguimiento(seguimiento: Seguimiento)

    @Query("SELECT * FROM seguimientos WHERE idOportunidad = :idOportunidad ORDER BY fechaHora DESC")
    fun obtenerHistorialPorOportunidad(idOportunidad: Long): Flow<List<Seguimiento>>

    @Query("SELECT * FROM seguimientos WHERE fechaProximo < :fechaActual AND tipoAccion != 'Cambio de estado'")
    fun obtenerSeguimientosAtrasados(fechaActual: Long): Flow<List<Seguimiento>>

    @Query("SELECT * FROM seguimientos WHERE idUsuario = :idUsuario AND fechaProximo >= :fechaActual ORDER BY fechaProximo ASC")
    fun obtenerPendientesPorUsuario(idUsuario: Long, fechaActual: Long): Flow<List<Seguimiento>>

    @Query("SELECT * FROM seguimientos WHERE idUsuario = :idUsuario ORDER BY fechaHora DESC")
    fun obtenerPorUsuario(idUsuario: Long): Flow<List<Seguimiento>>

    /**
     * Última gestión (sin contar cambios de estado) de cada oportunidad no finalizada.
     * idResponsable: 0 = todo el equipo; otro valor = solo esa persona.
     */
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

    @Query("SELECT COUNT(*) FROM seguimientos WHERE idOportunidad = :idOportunidad")
    suspend fun contarPorOportunidad(idOportunidad: Long): Int

    @Query("SELECT COUNT(*) FROM seguimientos WHERE idContacto = :idContacto")
    suspend fun contarPorContacto(idContacto: Long): Int

    @Query("""
    SELECT COUNT(*) FROM seguimientos
    WHERE idContacto IN (SELECT idContacto FROM contactos WHERE idOrganizacion = :idOrganizacion)
""")
    suspend fun contarPorContactosDeOrganizacion(idOrganizacion: Long): Int
}