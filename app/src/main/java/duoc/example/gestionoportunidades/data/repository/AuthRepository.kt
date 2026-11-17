package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.remote.LoginRequestRemote
import duoc.example.gestionoportunidades.data.remote.RetrofitClient
import duoc.example.gestionoportunidades.data.remote.UsuarioRemote
import duoc.example.gestionoportunidades.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import duoc.example.gestionoportunidades.data.remote.ApiService


class AuthRepository(private val db: AppDatabase) {

    private val api: ApiService = RetrofitClient.apiService

    // Convierte el Usuario que viene del JSON de Spring Boot al modelo Usuario de Android
    private fun UsuarioRemote.toLocalModel(): Usuario {
        return Usuario(
            idUsuario = this.id,
            username = this.correo,
            contrasena = this.password ?: "",
            nombreCompleto = this.nombre,
            rol = this.rol,
            area = this.area ?: "",
            zona = this.sucursal ?: ""
        )
    }

    // Inicia sesión contra POST /api/auth/login en Spring Boot
    suspend fun iniciarSesion(username: String, contrasena: String): Usuario? {
        if (username.isBlank() || contrasena.isBlank()) return null
        return try {
            val response = api.login(LoginRequestRemote(correo = username.trim(), password = contrasena))
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.toLocalModel()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Obtiene un usuario por su id desde GET /api/usuarios
    suspend fun obtenerUsuarioPorId(idUsuario: Long): Usuario? {
        return try {
            api.listarUsuarios()
                .find { it.id == idUsuario }
                ?.toLocalModel()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Devuelve la lista de ejecutivos (rol = 'EJECUTIVO') como Flow para el panel del supervisor
    fun obtenerEjecutivos(): Flow<List<Usuario>> = flow {
        try {
            val ejecutivos = api.listarUsuarios()
                .filter { it.rol.equals("EJECUTIVO", ignoreCase = true) && it.activo }
                .map { it.toLocalModel() }
            emit(ejecutivos)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
