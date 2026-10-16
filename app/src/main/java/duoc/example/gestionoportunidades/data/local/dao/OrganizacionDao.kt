package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOrganizacion(organizacion: Organizacion): Long

    @Update
    suspend fun actualizarOrganizacion(organizacion: Organizacion)

    @Query("SELECT * FROM organizaciones WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Organizacion?

    @Query("SELECT * FROM organizaciones")
    fun obtenerTodas(): Flow<List<Organizacion>>

    @Query("SELECT * FROM organizaciones WHERE nombre LIKE '%' || :busqueda || '%'")
    fun buscarPorNombre(busqueda: String): Flow<List<Organizacion>>
}