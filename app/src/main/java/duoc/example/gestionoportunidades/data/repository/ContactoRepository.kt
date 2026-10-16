package duoc.example.gestionoportunidades.data

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Contacto
import kotlinx.coroutines.flow.Flow

class ContactoRepository(private val db: AppDatabase) {

    private val dao = db.contactoDao()

    fun obtenerTodos(): Flow<List<Contacto>> = dao.obtenerTodos()

    fun obtenerPorOrganizacion(idOrg: Long): Flow<List<Contacto>> = dao.obtenerContactosPorOrganizacion(idOrg)

    fun buscarPorNombre(busqueda: String): Flow<List<Contacto>> = dao.buscarPorNombre(busqueda)

    suspend fun obtenerPorId(id: Long): Contacto? = dao.obtenerPorId(id)

    suspend fun insertar(contacto: Contacto): Long = dao.insertarContacto(contacto)

    suspend fun actualizar(contacto: Contacto) = dao.actualizarContacto(contacto)
}