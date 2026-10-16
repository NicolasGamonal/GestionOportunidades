package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import duoc.example.gestionoportunidades.model.Contacto
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarContacto(contacto: Contacto): Long

    @Update
    suspend fun actualizarContacto(contacto: Contacto)

    @Query("SELECT * FROM contactos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Contacto?

    @Query("SELECT * FROM contactos WHERE idOrganizacion = :idOrg")
    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>>

    @Query("SELECT * FROM contactos WHERE nombre LIKE '%' || :busqueda || '%'")
    fun buscarPorNombre(busqueda: String): Flow<List<Contacto>>

    @Query("SELECT * FROM contactos")
    fun obtenerTodos(): Flow<List<Contacto>>
}