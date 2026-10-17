package duoc.example.gestionoportunidades.ui.navigation

sealed class Ruta(val ruta: String, val titulo: String) {
    object Login : Ruta("login", "Iniciar Sesión")
    object Dashboard : Ruta("dashboard", "Dashboard")
    object Oportunidades : Ruta("oportunidades", "Oportunidades")
    object Organizaciones : Ruta("organizaciones", "Organizaciones")
    object PanelSupervisor : Ruta("supervisor", "Panel Supervisor")

    object DetalleOportunidad : Ruta("oportunidades/{id}", "Detalle") {
        fun crear(id: Long) = "oportunidades/$id"
    }
}