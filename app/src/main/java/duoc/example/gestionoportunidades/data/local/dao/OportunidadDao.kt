package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import duoc.example.gestionoportunidades.model.Oportunidad
import kotlinx.coroutines.flow.Flow

@Dao
interface OportunidadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOportunidad(oportunidad: Oportunidad): Long

    @Update
    suspend fun actualizarOportunidad(oportunidad: Oportunidad)

    @Query("SELECT * FROM oportunidades WHERE nombre LIKE '%' || :busqueda || '%'")
    fun obtenerTodasLasOportunidades(busqueda: String): Flow<List<Oportunidad>>

    @Query("SELECT * FROM oportunidades WHERE estado = :estado AND prioridad = :prioridad AND nombre LIKE '%' || :busqueda || '%'")
    fun filtrarOportunidades(estado: String, prioridad: String, busqueda: String): Flow<List<Oportunidad>>

    @Query("SELECT * FROM oportunidades WHERE idResponsable = :idUsuario AND nombre LIKE '%' || :busqueda || '%'")
    fun obtenerOportunidadesPorResponsable(idUsuario: Long, busqueda: String): Flow<List<Oportunidad>>

    @Query("SELECT * FROM oportunidades WHERE fechaInicio >= :fechaActual ORDER BY fechaInicio ASC")
    fun obtenerEventosProximos(fechaActual: Long): Flow<List<Oportunidad>>
}
