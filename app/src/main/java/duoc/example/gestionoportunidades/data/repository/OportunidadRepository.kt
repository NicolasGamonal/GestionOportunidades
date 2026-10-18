package duoc.example.gestionoportunidades.data.repository

import androidx.room.withTransaction
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

class OportunidadRepository(private val db: AppDatabase) {

    private val oportunidadDao = db.oportunidadDao()
    private val organizacionDao = db.organizacionDao()
    private val contactoDao = db.contactoDao()
    private val seguimientoDao = db.seguimientoDao()

    // --- Módulo Oportunidades ---

    suspend fun guardarOportunidad(oportunidad: Oportunidad): Long {
        return oportunidadDao.insertarOportunidad(oportunidad)
    }

    suspend fun modificarOportunidad(oportunidad: Oportunidad) {
        oportunidadDao.actualizarOportunidad(oportunidad)
    }

    suspend fun obtenerPorId(id: Long): Oportunidad? {
        return oportunidadDao.obtenerPorId(id)
    }

    fun obtenerTodasLasOportunidades(busqueda: String): Flow<List<Oportunidad>> {
        return oportunidadDao.buscarPorNombre(busqueda)
    }

    fun filtrarOportunidades(estado: String, prioridad: String, busqueda: String): Flow<List<Oportunidad>> {
        return oportunidadDao.filtrarOportunidades(estado, prioridad, busqueda)
    }

    fun obtenerPorResponsable(idUsuario: Long): Flow<List<Oportunidad>> {
        return oportunidadDao.obtenerPorResponsable(idUsuario)
    }

    fun obtenerEventosProximos(desde: Long, idResponsable: Long): Flow<List<Oportunidad>> {
        return oportunidadDao.obtenerEventosProximos(desde, idResponsable)
    }

    suspend fun actualizarEstado(id: Long, estado: String) {
        oportunidadDao.actualizarEstado(id, estado)
    }

    // --- Módulo Organizaciones ---

    suspend fun guardarOrganizacion(organizacion: Organizacion): Long {
        return organizacionDao.insertarOrganizacion(organizacion)
    }

    fun obtenerOrganizaciones(): Flow<List<Organizacion>> {
        return organizacionDao.obtenerTodas()
    }

    // --- Módulo Contactos ---

    suspend fun guardarContacto(contacto: Contacto): Long {
        return contactoDao.insertarContacto(contacto)
    }

    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>> {
        return contactoDao.obtenerContactosPorOrganizacion(idOrg)
    }

    fun buscar(estado: String, prioridad: String, busqueda: String, idResponsable: Long): Flow<List<Oportunidad>> =
        oportunidadDao.buscar(estado, prioridad, busqueda, idResponsable)

    fun observarPorId(id: Long): Flow<Oportunidad?> = oportunidadDao.observarPorId(id)

    suspend fun cambiarEstado(
        idOportunidad: Long,
        estadoAnterior: String,
        nuevoEstado: String,
        idUsuario: Long
    ) {
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            oportunidadDao.actualizarEstado(idOportunidad, nuevoEstado)
            seguimientoDao.insertarSeguimiento(
                Seguimiento(
                    idOportunidad = idOportunidad,
                    idUsuario = idUsuario,
                    tipoAccion = "Cambio de estado",
                    fechaHora = ahora,
                    resultado = "$estadoAnterior → $nuevoEstado",
                    proximaAccion = "",
                    fechaProximo = ahora
                )
            )
        }
    }

    suspend fun eliminar(id: Long): String? = db.withTransaction {
        val oportunidad = oportunidadDao.obtenerPorId(id)
            ?: return@withTransaction "La oportunidad ya no existe."
        when {
            oportunidad.estado != "Identificada" ->
                "Solo se pueden eliminar oportunidades en estado Identificada. Las demás se descartan para conservar su historial."
            seguimientoDao.contarPorOportunidad(id) > 0 ->
                "Tiene gestiones registradas; no se puede eliminar para conservar el historial."
            db.evidenciaDao().contarPorOportunidad(id) > 0 ->
                "Tiene fotos de evidencia; elimínalas primero desde el detalle."
            else -> {
                oportunidadDao.eliminarPorId(id)
                null
            }
        }
    }
}