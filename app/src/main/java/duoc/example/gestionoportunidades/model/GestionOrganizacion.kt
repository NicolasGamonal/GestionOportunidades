package duoc.example.gestionoportunidades.model

// Fila del historial de gestiones de una organización (HU-14).
// Resultado de una consulta con JOIN (seguimientos + oportunidades + usuarios + contactos).
// Los alias de la consulta en SeguimientoDao deben coincidir con estos nombres.
data class GestionOrganizacion(
    val idSeguimiento: Long,
    val fechaHora: Long,
    val tipoAccion: String,
    val resultado: String,
    val idOportunidad: Long,
    val nombreOportunidad: String,
    val autor: String,
    val nombreContacto: String?   // null si la gestión no se asoció a un contacto
)
