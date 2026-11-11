package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.NotaMarketing
import duoc.example.gestionoportunidades.model.NotaMarketingConAutor
import kotlinx.coroutines.flow.Flow

class NotaMarketingRepository(db: AppDatabase) {
    private val dao = db.notaMarketingDao()

    fun observarPorOportunidad(idOportunidad: Long): Flow<List<NotaMarketingConAutor>> =
        dao.observarPorOportunidad(idOportunidad)

    suspend fun insertar(nota: NotaMarketing): Long = dao.insertar(nota)
}