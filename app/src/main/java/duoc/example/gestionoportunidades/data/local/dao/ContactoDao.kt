package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.Contacto
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarContacto(contacto: Contacto): Long

    @Update
    suspend fun actualizarContacto(contacto: Contacto)

    @Query("SELECT * FROM contactos WHERE idContacto = :id")
    suspend fun obtenerPorId(id: Long): Contacto?

    @Query("SELECT * FROM contactos WHERE idOrganizacion = :idOrg ORDER BY nombre")
    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>>

    @Query("SELECT * FROM contactos WHERE nombre LIKE '%' || :busqueda || '%'")
    fun buscarPorNombre(busqueda: String): Flow<List<Contacto>>

    @Query("SELECT * FROM contactos")
    fun obtenerTodos(): Flow<List<Contacto>>

    @Query("DELETE FROM contactos WHERE idContacto = :id")
    suspend fun eliminarPorId(id: Long)


}