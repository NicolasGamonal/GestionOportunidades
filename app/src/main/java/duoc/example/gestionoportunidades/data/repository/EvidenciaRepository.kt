package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Evidencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class EvidenciaRepository(db: AppDatabase, private val directorioArchivos: File) {

    private val dao = db.evidenciaDao()

    fun observarPorOportunidad(idOportunidad: Long): Flow<List<Evidencia>> =
        dao.obtenerPorOportunidad(idOportunidad)

    suspend fun registrar(evidencia: Evidencia): Long = dao.insertar(evidencia)

    /** Borra el registro y, después, el archivo de la foto. */
    suspend fun eliminar(evidencia: Evidencia) {
        dao.eliminarPorId(evidencia.idEvidencia)
        withContext(Dispatchers.IO) { File(directorioArchivos, evidencia.rutaArchivo).delete() }
    }
}