package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.dao.UsuarioDao
import duoc.example.gestionoportunidades.model.Usuario

class AuthRepository(private val usuarioDao: UsuarioDao) {

    /**
     * Intenta iniciar sesión localmente comparando credenciales ficticias.
     * @return El objeto [Usuario] con su rol asignado, o null si la combinación es incorrecta.
     */
    suspend fun iniciarSesion(username: String, contrasena: String): Usuario? {
        if (username.isBlank() || contrasena.isBlank()) return null
        return usuarioDao.login(username, contrasena)
    }

    /**
     * Recupera un usuario por su ID para mantener la persistencia relacional en 3FN.
     */
    suspend fun obtenerUsuarioPorId(idUsuario: Long): Usuario? {
        return usuarioDao.obtenerUsuarioPorId(idUsuario)
    }
}
