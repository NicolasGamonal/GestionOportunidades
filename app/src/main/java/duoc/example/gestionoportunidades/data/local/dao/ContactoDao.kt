package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import duoc.example.gestionoportunidades.model.Contacto
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarContacto(contacto: Contacto): Long

    @Query("SELECT * FROM contactos WHERE idOrganizacion = :idOrg")
    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>>
}
