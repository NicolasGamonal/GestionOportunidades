package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Evidencia
import kotlinx.coroutines.flow.Flow

class EvidenciaRepository(db: AppDatabase) {

    private val dao = db.evidenciaDao()

    fun observarPorOportunidad(idOportunidad: Long): Flow<List<Evidencia>> =
        dao.obtenerPorOportunidad(idOportunidad)

    suspend fun registrar(evidencia: Evidencia): Long = dao.insertar(evidencia)
}