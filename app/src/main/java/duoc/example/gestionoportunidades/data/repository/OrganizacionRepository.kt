package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.flow.Flow

class OrganizacionRepository(private val db: AppDatabase) {

    private val dao = db.organizacionDao()

    fun obtenerTodas(): Flow<List<Organizacion>> = dao.obtenerTodas()

    fun buscarPorNombre(busqueda: String): Flow<List<Organizacion>> = dao.buscarPorNombre(busqueda)

    suspend fun obtenerPorId(id: Long): Organizacion? = dao.obtenerPorId(id)

    suspend fun insertar(organizacion: Organizacion): Long = dao.insertarOrganizacion(organizacion)

    suspend fun actualizar(organizacion: Organizacion) = dao.actualizarOrganizacion(organizacion)

    fun observarPorId(id: Long): Flow<Organizacion?> = dao.observarPorId(id)
}