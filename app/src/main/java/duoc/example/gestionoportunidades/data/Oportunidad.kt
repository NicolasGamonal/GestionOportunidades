package duoc.example.gestionoportunidades.data

/**
 * MODELO DE DATOS: Oportunidad
 *
 * Alineado al 100% con el modelo de datos del checklist oficial[cite: 3].
 */
data class Oportunidad(
    val id: Int = 0,
    val nombre: String,
    val tipoEvento: String,
    val fechaDeteccion: String,
    val fechaInicio: String,
    val fechaTermino: String,
    val comuna: String,
    val publicoEsperado: Int,
    val prioridad: String,
    val estado: String,
    val costoReferencial: Int,
    val observaciones: String,

    // Claves foráneas para relacionar con Room más adelante[cite: 3]
    val idOrganizacion: Int,
    val idResponsable: Int? = null
)