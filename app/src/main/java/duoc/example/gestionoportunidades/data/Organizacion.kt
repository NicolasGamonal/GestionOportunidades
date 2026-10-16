package duoc.example.gestionoportunidades.data

// Modelo de datos para Organización según planificación[cite: 1]
data class Organizacion(
    val id: Int = 0,
    val nombre: String,
    val tipo: String,
    val rubro: String,
    val comuna: String
)