package duoc.example.gestionoportunidades.ui.auth

import duoc.example.gestionoportunidades.model.Usuario

object SesionManager {
    var usuarioActual: Usuario? = null
        private set

    fun iniciarSesion(usuario: Usuario) {
        usuarioActual = usuario
    }

    fun cerrarSesion() {
        usuarioActual = null
    }

    fun estaLogueado(): Boolean = usuarioActual != null

    fun getRol(): String = usuarioActual?.rol ?: ""
}

/*
Bloqueo de rutas — esto lo implementa Josue en Navegacion.kt, pero le pasas la regla:

// En el NavHost, antes de navegar:
val rol = SesionManager.getRol()

// Ejemplo: la ruta "supervisor" solo es accesible si rol == "SUPERVISOR"
navController.navigate("supervisor") {
    // Si no es supervisor, redirigir a dashboard
}
 */