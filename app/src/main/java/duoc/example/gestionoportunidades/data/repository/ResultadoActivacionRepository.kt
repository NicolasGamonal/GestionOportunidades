package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.ResultadoActivacion
import kotlinx.coroutines.flow.Flow

// Repositorio del resultado de una activación (HU-24). Es la única puerta de entrada a la tabla
// resultados_activacion: los ViewModels no conocen el DAO.
class ResultadoActivacionRepository(db: AppDatabase) {

    private val dao = db.resultadoActivacionDao()

    fun observarPorOportunidad(idOportunidad: Long): Flow<ResultadoActivacion?> =
        dao.observarPorOportunidad(idOportunidad)

    suspend fun obtenerPorOportunidad(idOportunidad: Long): ResultadoActivacion? =
        dao.obtenerPorOportunidad(idOportunidad)

    suspend fun guardar(resultado: ResultadoActivacion): Long = dao.guardar(resultado)
}
