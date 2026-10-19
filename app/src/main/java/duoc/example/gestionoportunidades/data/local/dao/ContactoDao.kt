package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.Contacto
import kotlinx.coroutines.flow.Flow

// DAO (Data Access Object) para la tabla "contactos".
// Contiene todas las consultas SQL que hablan directo con Room/SQLite.
// El repositorio llama a estas funciones; el ViewModel nunca toca este archivo.
@Dao
interface ContactoDao {

    // Inserta un contacto nuevo en la base.
    // Si ya existe uno con el mismo id, lo reemplaza (REPLACE).
    // Devuelve el id del contacto insertado.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarContacto(contacto: Contacto): Long

    // Actualiza un contacto existente (por su id).
    // Solo se usan si los campos del objeto Contacto tienen el mismo id que uno ya guardado.
    @Update
    suspend fun actualizarContacto(contacto: Contacto)

    // Busca un contacto específico por su id.
    // Devuelve el contacto o null si no existe.
    @Query("SELECT * FROM contactos WHERE idContacto = :id")
    suspend fun obtenerPorId(id: Long): Contacto?

    // Devuelve todos los contactos que pertenecen a una organización.
    // Flow: se actualiza solo en la UI cuando la base cambia (reactivo).
    @Query("SELECT * FROM contactos WHERE idOrganizacion = :idOrg ORDER BY nombre")
    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>>

    // Búsqueda por nombre (parcial). Ej: si escribes "Ana", encuentra "Ana Rojas".
    // El LIKE con '%' a ambos lados busca el texto en cualquier posición del nombre.
    @Query("SELECT * FROM contactos WHERE nombre LIKE '%' || :busqueda || '%'")
    fun buscarPorNombre(busqueda: String): Flow<List<Contacto>>

    // Devuelve todos los contactos de la base, ordenados de forma implícita.
    // Se usa cuando el supervisor necesita ver todos sin filtro.
    @Query("SELECT * FROM contactos")
    fun obtenerTodos(): Flow<List<Contacto>>

    // Elimina un contacto por su id.
    // Usar con precaución: si hay seguimientos que apuntan a este contacto (idContacto FK),
    // hay que manejar la restricción de integridad referencial.
    @Query("DELETE FROM contactos WHERE idContacto = :id")
    suspend fun eliminarPorId(id: Long)
}