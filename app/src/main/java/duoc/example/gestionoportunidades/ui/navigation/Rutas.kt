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

    // El id es opcional: sin id (-1) se crea, con id se edita
    object FormularioOportunidad : Ruta("formulario_oportunidad?id={id}", "Oportunidad") {
        fun crear() = "formulario_oportunidad"
        fun editar(id: Long) = "formulario_oportunidad?id=$id"
    }

    object DetalleOrganizacion : Ruta("organizaciones/{id}", "Organización") {
        fun crear(id: Long) = "organizaciones/$id"
    }
    object FormularioOrganizacion : Ruta("formulario_organizacion?id={id}", "Organización") {
        fun crear() = "formulario_organizacion"
        fun editar(id: Long) = "formulario_organizacion?id=$id"
    }
    object FormularioContacto : Ruta("formulario_contacto/{idOrganizacion}?id={id}", "Contacto") {
        fun crear(idOrganizacion: Long) = "formulario_contacto/$idOrganizacion"
        fun editar(idOrganizacion: Long, id: Long) = "formulario_contacto/$idOrganizacion?id=$id"
    }
    object HistorialSeguimiento : Ruta("seguimiento/{idOportunidad}", "Historial de gestión") {
        fun crear(idOportunidad: Long) = "seguimiento/$idOportunidad"
    }
    object FormularioSeguimiento : Ruta("formulario_seguimiento/{idOportunidad}", "Nueva gestión") {
        fun crear(idOportunidad: Long) = "formulario_seguimiento/$idOportunidad"
    }
}