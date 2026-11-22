package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.ContactoRemote
import duoc.example.gestionoportunidades.data.remote.OrganizacionRef
import duoc.example.gestionoportunidades.data.remote.RetrofitClient
import duoc.example.gestionoportunidades.model.Contacto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class ContactoRepository(private val api: ApiService = RetrofitClient.apiService) {


    private fun ContactoRemote.toLocalModel(): Contacto {
        return Contacto(
            idContacto = this.id ?: 0L,
            nombre = this.nombre,
            cargo = this.cargo ?: "",
            area = this.area ?: "",
            correo = this.correo,
            telefono = this.telefono,
            idOrganizacion = this.organizacion.id,
            medioPreferente = this.medioPreferente ?: "Correo"
        )
    }

    private fun Contacto.toRemoteModel(incluirId: Boolean = false): ContactoRemote {
        return ContactoRemote(
            id = if (incluirId && this.idContacto > 0) this.idContacto else null,
            nombre = this.nombre,
            cargo = this.cargo,
            area = this.area,
            correo = this.correo,
            telefono = this.telefono,
            medioPreferente = this.medioPreferente,
            estado = "Activo",
            observaciones = "",
            organizacion = OrganizacionRef(id = this.idOrganizacion)
        )
    }

    fun obtenerTodos(): Flow<List<Contacto>> = flow {
        try {
            val lista = api.listarTodosContactos().map { it.toLocalModel() }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun obtenerPorOrganizacion(idOrg: Long): Flow<List<Contacto>> = flow {
        try {
            val lista = api.listarContactos(idOrg).map { it.toLocalModel() }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun buscarPorNombre(busqueda: String): Flow<List<Contacto>> = flow {
        try {
            val filtrados = api.listarTodosContactos()
                .filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
                .sortedBy { it.nombre.lowercase() }
                .map { it.toLocalModel() }
            emit(filtrados)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun obtenerPorId(id: Long): Contacto? {
        return try {
            api.listarTodosContactos()
                .find { it.id == id }
                ?.toLocalModel()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertar(contacto: Contacto): Long {
        return try {
            val response = api.crearContacto(contacto.toRemoteModel(incluirId = false))
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

    suspend fun actualizar(contacto: Contacto) {
        try {
            api.actualizarContacto(
                id = contacto.idContacto,
                contacto = contacto.toRemoteModel(incluirId = true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun eliminar(id: Long): String? {
        return try {
            val response = api.eliminarContacto(id)
            if (response.isSuccessful) {
                null // null = éxito
            } else {
                val raw = response.errorBody()?.string() ?: "No se puede eliminar el contacto."
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
