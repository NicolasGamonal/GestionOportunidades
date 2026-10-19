package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.flow.Flow

// DAO para la tabla "organizaciones" (empresas, municipalidades, instituciones).
// Es la entidad más simple: no tiene claves foráneas entrantes desde otras tablas
// (los contactos y oportunidades apuntan a ella, pero ella no apunta a nadie).
// El repositorio llama a estas funciones; el ViewModel nunca toca este archivo.
@Dao
interface OrganizacionDao {

    // Inserta una organización nueva.
    // REPLACE: si ya existe una con el mismo id, la sobrescribe (útil al editar).
    // Devuelve el id de la organización insertada.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOrganizacion(organizacion: Organizacion): Long

    // Actualiza una organización existente (por su id).
    // Se usa al editar nombre, tipo, rubro o comuna desde el formulario.
    @Update
    suspend fun actualizarOrganizacion(organizacion: Organizacion)

    // Busca una organización específica por su id.
    // Devuelve la organización o null si no existe.
    // Se usa al abrir el detalle desde la lista o al asociar una oportunidad.
    @Query("SELECT * FROM organizaciones WHERE idOrganizacion = :id")
    suspend fun obtenerPorId(id: Long): Organizacion?

    // Devuelve todas las organizaciones sin filtro.
    // Se usa en la lista principal de la pantalla de organizaciones.
    // Flow: se actualiza solo en la UI cuando se agrega, edita o borra una organización.
    @Query("SELECT * FROM organizaciones")
    fun obtenerTodas(): Flow<List<Organizacion>>

    // Búsqueda por nombre (parcial), ordenada alfabéticamente.
    // Ej: si escribes "Andina", encuentra "Empresa Logística Andina SpA".
    // Se usa en el campo de búsqueda de la pantalla de organizaciones.
    @Query("SELECT * FROM organizaciones WHERE nombre LIKE '%' || :busqueda || '%' ORDER BY nombre")
    fun buscarPorNombre(busqueda: String): Flow<List<Organizacion>>

    // Observa una organización en tiempo real (Flow).
    // Si se edita desde otra pantalla, la UI se actualiza sola.
    // Se usa en la pantalla de detalle de organización para mantener los datos frescos.
    @Query("SELECT * FROM organizaciones WHERE idOrganizacion = :id")
    fun observarPorId(id: Long): Flow<Organizacion?>

    // Elimina una organización por su id.
    // Ojo: hay que borrar primero los contactos y oportunidades asociados (FK).
    // El repositorio maneja el orden de borrado en cascada.
    @Query("DELETE FROM organizaciones WHERE idOrganizacion = :id")
    suspend fun eliminarPorId(id: Long)
}