package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.dao.OportunidadDao
import duoc.example.gestionoportunidades.data.local.dao.OrganizacionDao
import duoc.example.gestionoportunidades.data.local.dao.ContactoDao
import duoc.example.gestionoportunidades.data.local.dao.SeguimientoDao
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

class OportunidadRepository(
    private val oportunidadDao: OportunidadDao,
    private val organizacionDao: OrganizacionDao,
    private val contactoDao: ContactoDao,
    private val seguimientoDao: SeguimientoDao
) {

    // --- Módulo Oportunidades ---
    suspend fun guardarOportunidad(oportunidad: Oportunidad): Long {
        return oportunidadDao.insertarOportunidad(oportunidad)
    }

    suspend fun modificarOportunidad(oportunidad: Oportunidad) {
        oportunidadDao.actualizarOportunidad(oportunidad)
    }

    fun obtenerTodasLasOportunidades(busqueda: String): Flow<List<Oportunidad>> {
        return oportunidadDao.obtenerTodasLasOportunidades(busqueda)
    }

    fun filtrarOportunidades(estado: String, prioridad: String, busqueda: String): Flow<List<Oportunidad>> {
        return oportunidadDao.filtrarOportunidades(estado, prioridad, busqueda)
    }

    fun obtenerOportunidadesPorResponsable(idUsuario: Long, busqueda: String): Flow<List<Oportunidad>> {
        return oportunidadDao.obtenerOportunidadesPorResponsable(idUsuario, busqueda)
    }

    fun obtenerEventosProximos(fechaActual: Long): Flow<List<Oportunidad>> {
        return oportunidadDao.obtenerEventosProximos(fechaActual)
    }

    // --- Módulo Organizaciones ---
    suspend fun registrarOrganizacion(organizacion: Organizacion): Long {
        return organizacionDao.insertarOrganizacion(organizacion)
    }

    fun obtenerOrganizaciones(): Flow<List<Organizacion>> {
        return organizacionDao.obtenerOrganizaciones()
    }

    // --- Módulo Contactos ---
    suspend fun registrarContacto(contacto: Contacto): Long {
        return contactoDao.insertarContacto(contacto)
    }

    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>> {
        return contactoDao.obtenerContactosPorOrganizacion(idOrg)
    }

    // --- Módulo Historial y Seguimientos (3FN) ---
    suspend fun registrarSeguimiento(seguimiento: Seguimiento): Long {
        return seguimientoDao.insertarSeguimiento(seguimiento)
    }

    fun obtenerHistorialPorOportunidad(idOportunidad: Long): Flow<List<Seguimiento>> {
        return seguimientoDao.obtenerHistorialPorOportunidad(idOportunidad)
    }

    fun obtenerSeguimientosAtrasados(fechaActual: Long): Flow<List<Seguimiento>> {
        return seguimientoDao.obtenerSeguimientosAtrasados(fechaActual)
    }
}
