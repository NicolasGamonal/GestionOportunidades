package duoc.example.gestionoportunidades.data

// Modelo de datos para Seguimiento según planificación
data class Seguimiento(
    val id: Int = 0,
    val tipoAccion: String,      // Ej: "Llamada", "Reunión", "Cambio de estado"
    val fechaHora: String,
    val resultado: String,
    val proximaAccion: String,
    val fechaProximo: String,
    val idOportunidad: Int,      // FK
    val idUsuario: Int,          // FK
    val idContacto: Int? = null  // FK (Opcional)
)