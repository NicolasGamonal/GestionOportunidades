package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
}