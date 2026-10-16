package duoc.example.gestionoportunidades.data

// Modelo de datos para Contacto según planificación[cite: 1]
data class Contacto(
    val id: Int = 0,
    val nombre: String,
    val cargo: String,
    val area: String,
    val correo: String,
    val telefono: String,
    val idOrganizacion: Int // Clave foránea que apunta a la Organización[cite: 1]
)