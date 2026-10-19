package duoc.example.gestionoportunidades.data.repository

import androidx.room.withTransaction
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Contacto
import kotlinx.coroutines.flow.Flow

// Repositorio de contactos.
// Capa intermedia entre el ViewModel (OrgContactosViewModel) y el DAO (ContactoDao).
// El ViewModel llama a estas funciones sin saber que detrás hay Room/SQLite.
// Aquí se centraliza la lógica de negocio que no es puramente SQL
// (por ejemplo, la regla de no borrar un contacto que tiene historial).
class ContactoRepository(private val db: AppDatabase) {

    // Referencia al DAO de contactos. Se obtiene una sola vez al construir el repositorio.
    private val dao = db.contactoDao()

    // Devuelve todos los contactos de la base.
    // Flow: la lista en la UI se actualiza sola cuando se agrega, edita o borra un contacto.
    fun obtenerTodos(): Flow<List<Contacto>> = dao.obtenerTodos()

    // Devuelve solo los contactos que pertenecen a una organización específica.
    // Se usa en la pantalla de detalle de organización para mostrar su equipo de contacto.
    fun obtenerPorOrganizacion(idOrg: Long): Flow<List<Contacto>> = dao.obtenerContactosPorOrganizacion(idOrg)

    // Búsqueda parcial por nombre.
    // Se usa en el campo de búsqueda de la pantalla de organizaciones y contactos.
    fun buscarPorNombre(busqueda: String): Flow<List<Contacto>> = dao.buscarPorNombre(busqueda)

    // Busca un contacto específico por su id (consulta puntual, no reactiva).
    // Se usa al abrir el detalle de un contacto desde la lista.
    suspend fun obtenerPorId(id: Long): Contacto? = dao.obtenerPorId(id)

    // Inserta un contacto nuevo. Devuelve el id asignado por Room.
    suspend fun insertar(contacto: Contacto): Long = dao.insertarContacto(contacto)

    // Actualiza un contacto existente (por su id).
    suspend fun actualizar(contacto: Contacto) = dao.actualizarContacto(contacto)

    // Elimina un contacto, PERO con una regla de negocio:
    // Antes de borrar, verifica si el contacto aparece en algún seguimiento (historial).
    // Si tiene gestiones asociadas → NO se borra y devuelve un mensaje de error (String).
    // Si no tiene gestiones → se borra y devuelve null (éxito).
    //
    // ¿Por qué withTransaction?
    // Porque leemos (contarPorContacto) y luego escribimos (eliminarPorId) en la misma operación.
    // La transacción garantiza que entre la lectura y el borrado no se inserte un seguimiento
    // nuevo que "esconda" la referencia. Es atómica: todo pasa o nada pasa.
    //
    // ¿Por qué devuelve String? en vez de Unit?
    // Porque el ViewModel necesita saber SI fue exitoso (null) o SI falló y con qué mensaje
    // para mostrarlo en un Snackbar o AlertDialog al usuario.
    suspend fun eliminar(id: Long): String? = db.withTransaction {
        // Cuenta cuántos seguimientos apuntan a este contacto (FK idContacto)
        val usos = db.seguimientoDao().contarPorContacto(id)
        if (usos > 0) {
            // Hay historial asociado: se impide el borrado para no perder el historial
            "Este contacto figura en $usos gestión(es) del historial; no se puede eliminar para conservarlo."
        } else {
            // No hay seguimientos que lo referencien: se puede borrar en seguridad
            dao.eliminarPorId(id)
            null  // null = éxito, no hay mensaje de error
        }
    }
}