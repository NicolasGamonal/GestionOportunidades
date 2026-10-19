package duoc.example.gestionoportunidades.data.repository

import androidx.room.withTransaction
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.flow.Flow

// Repositorio de organizaciones (empresas, municipalidades, instituciones).
// Capa intermedia entre el ViewModel (OrgContactosViewModel) y el DAO (OrganizacionDao).
// El ViewModel llama a estas funciones sin saber que detrás hay Room/SQLite.
//
// A diferencia de OportunidadRepository (que agrupa 3 módulos), este repositorio
// es dedicado exclusivamente a organizaciones. Tiene una regla de negocio importante
// en eliminar(): no se puede borrar una organización que tenga oportunidades
// o cuyos contactos aparezcan en el historial de gestiones.
class OrganizacionRepository(private val db: AppDatabase) {

    // Referencia al DAO de organizaciones. Se obtiene una sola vez al construir el repositorio.
    private val dao = db.organizacionDao()

    // Devuelve todas las organizaciones sin filtro.
    // Flow: la lista en la UI se actualiza sola cuando se agrega, edita o borra una organización.
    // Se usa en la lista principal de la pantalla de organizaciones.
    fun obtenerTodas(): Flow<List<Organizacion>> = dao.obtenerTodas()

    // Búsqueda parcial por nombre, ordenada alfabéticamente.
    // Ej: si escribes "Andina", encuentra "Empresa Logística Andina SpA".
    // Se usa en el campo de búsqueda de la pantalla de organizaciones.
    fun buscarPorNombre(busqueda: String): Flow<List<Organizacion>> = dao.buscarPorNombre(busqueda)

    // Busca una organización específica por su id (consulta puntual, no reactiva).
    // Se usa al abrir el detalle desde la lista o al asociar una oportunidad a una organización.
    // Devuelve la organización o null si no existe.
    suspend fun obtenerPorId(id: Long): Organizacion? = dao.obtenerPorId(id)

    // Inserta una organización nueva (o la sobrescribe si el id ya existe, por REPLACE).
    // Se llama desde el formulario de crear/editar organización.
    // Devuelve el id asignado por Room.
    suspend fun insertar(organizacion: Organizacion): Long = dao.insertarOrganizacion(organizacion)

    // Actualiza una organización existente (por su id).
    // Se usa al editar nombre, tipo, rubro o comuna desde el formulario.
    suspend fun actualizar(organizacion: Organizacion) = dao.actualizarOrganizacion(organizacion)

    // Observa una organización en tiempo real (Flow).
    // Si se edita desde otra pantalla, la UI se actualiza sola.
    // Se usa en la pantalla de detalle de organización para mantener los datos frescos.
    // Llamar a la función que devuelve Flow
    fun observarPorId(id: Long): Flow<Organizacion?> = dao.observarPorId(id)

    // Elimina una organización con reglas de negocio.
    //
    // Reglas (en orden de prioridad):
    // 1. Si tiene oportunidades asociadas → NO se puede eliminar.
    //    Razón: las oportunidades tienen historial, seguimientos y evidencias que dependen de ella.
    //    Borrarla rompería la integridad referencial de toda esa información.
    // 2. Si alguno de sus contactos aparece en el historial de gestiones → NO se puede eliminar.
    //    Razón: los seguimientos guardan idContacto como FK; borrar la organización borraría
    //    sus contactos (cascada) y los seguimientos quedarían huérfanos.
    // 3. Si pasa ambas verificaciones → se elimina y sus contactos se borran en cascada (Room lo hace solo por la FK).
    //
    // ¿Por qué withTransaction?
    // Porque leemos (contarPorOrganizacion, contarPorContactosDeOrganizacion)
    // y luego escribimos (eliminarPorId). La transacción garantiza que entre la lectura
    // y el borrado no se inserte una oportunidad nueva que "esconda" la referencia.
    //
    // ¿Por qué devuelve String? en vez de Unit?
    // Porque el ViewModel necesita saber SI fue exitoso (null) o SI falló y con qué mensaje
    // para mostrarlo en un Snackbar al usuario.
    suspend fun eliminar(id: Long): String? = db.withTransaction {
        // Verifica si la organización tiene oportunidades asociadas (FK idOrganizacion en Oportunidad)
        val oportunidades = db.oportunidadDao().contarPorOrganizacion(id)
        // Verifica si algún contacto de esta organización aparece en seguimientos
        // (consulta con subquery: SELECT COUNT(*) FROM seguimientos WHERE idContacto IN (contactos de esta org))
        val gestiones = db.seguimientoDao().contarPorContactosDeOrganizacion(id)
        when {
            // Regla 1: tiene oportunidades → no se borra
            oportunidades > 0 ->
                "Tiene $oportunidades oportunidad(es) asociada(s); no se puede eliminar."
            // Regla 2: sus contactos tienen historial de gestiones → no se borra
            gestiones > 0 ->
                "Alguno de sus contactos figura en el historial de gestiones; no se puede eliminar."
            // Regla 3: pasa ambas verificaciones → se elimina
            // Los contactos se borran automáticamente por la cascada de Room (FK idOrganizacion)
            else -> {
                dao.eliminarPorId(id)    // sus contactos se eliminan con ella
                null  // null = éxito
            }
        }
    }
}