package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.OrganizacionRemote
import duoc.example.gestionoportunidades.data.remote.RetrofitClient
import duoc.example.gestionoportunidades.model.Organizacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class OrganizacionRepository(private val db: AppDatabase) {

    private val api: ApiService = RetrofitClient.apiService

    private fun OrganizacionRemote.toLocalModel(): Organizacion {
        return Organizacion(
            idOrganizacion = this.id ?: 0L,
            nombre = this.nombre,
            tipo = this.tipo,
            rubro = this.rubro ?: "",
            comuna = this.comuna ?: ""
        )
    }

    private fun Organizacion.toRemoteModel(incluirId: Boolean = false): OrganizacionRemote {
        return OrganizacionRemote(
            id = if (incluirId && this.idOrganizacion > 0) this.idOrganizacion else null,
            nombre = this.nombre,
            tipo = this.tipo,
            rubro = this.rubro,
            comuna = this.comuna,
            region = "Metropolitana",
            sitioWeb = "",
            publicoAsociado = "",
            observaciones = ""
        )
    }

    fun obtenerTodas(): Flow<List<Organizacion>> = flow {
        try {
            val lista = api.listarOrganizaciones().map { it.toLocalModel() }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun buscarPorNombre(busqueda: String): Flow<List<Organizacion>> = flow {
        try {
            val filtradas = api.listarOrganizaciones()
                .filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
                .sortedBy { it.nombre.lowercase() }
                .map { it.toLocalModel() }
            emit(filtradas)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun obtenerPorId(id: Long): Organizacion? {
        return try {
            api.listarOrganizaciones()
                .find { it.id == id }
                ?.toLocalModel()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertar(organizacion: Organizacion): Long {
        return try {
            val response = api.crearOrganizacion(organizacion.toRemoteModel(incluirId = false))
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

    suspend fun actualizar(organizacion: Organizacion) {
        try {
            api.actualizarOrganizacion(
                id = organizacion.idOrganizacion,
                org = organizacion.toRemoteModel(incluirId = true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observarPorId(id: Long): Flow<Organizacion?> = flow {
        emit(obtenerPorId(id))
    }

    suspend fun eliminar(id: Long): String? {
        return try {
            val response = api.eliminarOrganizacion(id)
            if (response.isSuccessful) {
                null // null = éxito
            } else {
                val raw = response.errorBody()?.string() ?: "No se puede eliminar la organización."
                try {
                    JSONObject(raw).optString("mensaje", raw)
                } catch (_: Exception) {
                    raw
                }
            }
        } catch (e: Exception) {
            "Error de conexión al eliminar: ${e.message}"
        }
    }
}
