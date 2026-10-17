package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

class SeguimientoRepository(private val db: AppDatabase) {

    private val dao = db.seguimientoDao()

    fun obtenerHistorial(idOportunidad: Long): Flow<List<Seguimiento>> = dao.obtenerHistorialPorOportunidad(idOportunidad)

    fun obtenerAtrasados(fechaActual: Long): Flow<List<Seguimiento>> = dao.obtenerSeguimientosAtrasados(fechaActual)

    fun obtenerPendientesPorUsuario(idUsuario: Long, fechaActual: Long): Flow<List<Seguimiento>> =
        dao.obtenerPendientesPorUsuario(idUsuario, fechaActual)

    fun obtenerPorUsuario(idUsuario: Long): Flow<List<Seguimiento>> = dao.obtenerPorUsuario(idUsuario)

    suspend fun insertar(seguimiento: Seguimiento): Long = dao.insertarSeguimiento(seguimiento)

    suspend fun actualizar(seguimiento: Seguimiento) = dao.actualizarSeguimiento(seguimiento)
}