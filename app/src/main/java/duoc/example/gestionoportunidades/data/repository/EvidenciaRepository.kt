package duoc.example.gestionoportunidades.data.repository

import android.util.Base64
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.EvidenciaRemote
import duoc.example.gestionoportunidades.data.remote.OportunidadRef
import duoc.example.gestionoportunidades.data.remote.RetrofitClient
import duoc.example.gestionoportunidades.model.Evidencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

class EvidenciaRepository(db: AppDatabase, private val directorioArchivos: File) {

    private val api: ApiService = RetrofitClient.apiService
    private val zonaHoraria = ZoneId.systemDefault()

    private fun String?.toDateTimeMillis(): Long {
        if (this.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            LocalDateTime.parse(this.take(19))
                .atZone(zonaHoraria)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun observarPorOportunidad(idOportunidad: Long): Flow<List<Evidencia>> = flow {
        try {
            val lista = api.listarEvidencias(idOportunidad).map { remote ->
                Evidencia(
                    idEvidencia = remote.id ?: 0L,
                    rutaArchivo = remote.descripcion ?: "",
                    fecha = remote.fechaCaptura.toDateTimeMillis(),
                    idOportunidad = remote.oportunidad.id
                )
            }.sortedByDescending { it.fecha }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun registrar(evidencia: Evidencia): Long = withContext(Dispatchers.IO) {
        try {
            val archivo = File(directorioArchivos, evidencia.rutaArchivo)
            val base64 = if (archivo.exists()) {
                Base64.encodeToString(archivo.readBytes(), Base64.NO_WRAP)
            } else {
                ""
            }

            val remote = EvidenciaRemote(
                descripcion = evidencia.rutaArchivo,
                imagenBase64 = base64,
                oportunidad = OportunidadRef(id = evidencia.idOportunidad)
            )
            val response = api.crearEvidencia(remote)
            if (response.isSuccessful && response.body()?.id != null) {
                response.body()!!.id!!
            } else {
                0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    suspend fun eliminar(evidencia: Evidencia) {
        try {
            api.eliminarEvidencia(evidencia.idEvidencia)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        withContext(Dispatchers.IO) {
            File(directorioArchivos, evidencia.rutaArchivo).delete()
        }
    }
}
