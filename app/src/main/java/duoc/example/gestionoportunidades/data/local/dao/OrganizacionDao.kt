package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOrganizacion(organizacion: Organizacion): Long

    @Update
    suspend fun actualizarOrganizacion(organizacion: Organizacion)

    @Query("SELECT * FROM organizaciones WHERE idOrganizacion = :id")
    suspend fun obtenerPorId(id: Long): Organizacion?

    @Query("SELECT * FROM organizaciones")
    fun obtenerTodas(): Flow<List<Organizacion>>

    @Query("SELECT * FROM organizaciones WHERE nombre LIKE '%' || :busqueda || '%' ORDER BY nombre")
    fun buscarPorNombre(busqueda: String): Flow<List<Organizacion>>

    @Query("SELECT * FROM organizaciones WHERE idOrganizacion = :id")
    fun observarPorId(id: Long): Flow<Organizacion?>
}