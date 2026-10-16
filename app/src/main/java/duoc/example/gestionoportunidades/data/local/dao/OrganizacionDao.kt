package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOrganizacion(organizacion: Organizacion): Long

    @Query("SELECT * FROM organizaciones")
    fun obtenerOrganizaciones(): Flow<List<Organizacion>>
}
