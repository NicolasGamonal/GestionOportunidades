package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Usuario

// Repositorio de autenticación.
// Es la capa intermedia entre el ViewModel (AuthViewModel) y el DAO (UsuarioDao).
// El ViewModel nunca llama directo al DAO; siempre pasa por este repositorio.
// Aquí se centraliza la lógica de login y la obtención de usuarios.
class AuthRepository(private val db: AppDatabase) {

    // Referencia al DAO de usuarios. Se obtiene una sola vez al construir el repositorio.
    private val usuarioDao = db.usuarioDao()

    // Inicia sesión: verifica username y contraseña contra la base.
    // Primero valida que no vengan campos vacíos (evita consulta innecesaria a Room).
    // Devuelve el Usuario si las credenciales son correctas, o null si fallan.
    // Se llama desde AuthViewModel.iniciarSesion().
    suspend fun iniciarSesion(username: String, contrasena: String): Usuario? {
        if (username.isBlank() || contrasena.isBlank()) return null
        return usuarioDao.login(username, contrasena)
    }

    // Obtiene un usuario por su id.
    // Se usa para recuperar los datos del usuario logueado (nombre, rol)
    // y mostrarlos en la UI (ej: "Hola, Juan Pérez" en el dashboard)
    // o para verificar permisos en Permisos.kt.
    suspend fun obtenerUsuarioPorId(idUsuario: Long): Usuario? {
        return usuarioDao.obtenerUsuarioPorId(idUsuario)
    }

    // Devuelve la lista de ejecutivos (rol = 'EJECUTIVO').
    // Se usa en el panel del supervisor para mostrar el dropdown de asignación
    // (seleccionar a qué ejecutivo se le asigna o reasigna una oportunidad).
    // Devuelve Flow: si se agrega un ejecutivo nuevo, la lista se actualiza sola.
    fun obtenerEjecutivos() = usuarioDao.obtenerEjecutivos()
}