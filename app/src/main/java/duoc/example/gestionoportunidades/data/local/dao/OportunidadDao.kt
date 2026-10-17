package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.Oportunidad
import kotlinx.coroutines.flow.Flow

@Dao
interface OportunidadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOportunidad(oportunidad: Oportunidad): Long

    @Update
    suspend fun actualizarOportunidad(oportunidad: Oportunidad)

    @Query("SELECT * FROM oportunidades WHERE idOportunidad = :id")
    suspend fun obtenerPorId(id: Long): Oportunidad?

    @Query("SELECT * FROM oportunidades")
    fun obtenerTodas(): Flow<List<Oportunidad>>

    @Query("SELECT * FROM oportunidades WHERE nombre LIKE '%' || :busqueda || '%'")
    fun buscarPorNombre(busqueda: String): Flow<List<Oportunidad>>

    @Query("SELECT * FROM oportunidades WHERE estado = :estado AND prioridad = :prioridad AND nombre LIKE '%' || :busqueda || '%'")
    fun filtrarOportunidades(estado: String, prioridad: String, busqueda: String): Flow<List<Oportunidad>>

    @Query("SELECT * FROM oportunidades WHERE idResponsable = :idUsuario")
    fun obtenerPorResponsable(idUsuario: Long): Flow<List<Oportunidad>>

    @Query("SELECT * FROM oportunidades WHERE fechaInicio >= :fechaActual ORDER BY fechaInicio ASC")
    fun obtenerEventosProximos(fechaActual: Long): Flow<List<Oportunidad>>

    @Query("UPDATE oportunidades SET estado = :estado WHERE idOportunidad = :id")
    suspend fun actualizarEstado(id: Long, estado: String)
}