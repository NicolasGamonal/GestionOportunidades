package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Usuario

class AuthRepository(private val db: AppDatabase) {

    private val usuarioDao = db.usuarioDao()

    suspend fun iniciarSesion(username: String, contrasena: String): Usuario? {
        if (username.isBlank() || contrasena.isBlank()) return null
        return usuarioDao.login(username, contrasena)
    }

    suspend fun obtenerUsuarioPorId(idUsuario: Long): Usuario? {
        return usuarioDao.obtenerUsuarioPorId(idUsuario)
    }

    fun obtenerEjecutivos() = usuarioDao.obtenerEjecutivos()
}