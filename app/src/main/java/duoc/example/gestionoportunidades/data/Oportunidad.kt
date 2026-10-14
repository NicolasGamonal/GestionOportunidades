package duoc.example.gestionoportunidades.data

/**
 * MODELO DE DATOS: Oportunidad
 *
 * Esta data class empaqueta la información de cada oportunidad de activación
 * o evento corporativo registrado por el ejecutivo comercial, según las
 * especificaciones del caso de negocio.
 */
data class Oportunidad(
    val id: Int = 0,                         // Identificador único de la oportunidad
    val nombreEvento: String,                // Nombre de la actividad u oportunidad
    val organizacion: String,                // Empresa u organización ficticia
    val correoContacto: String,              // Correo del contacto corporativo
    val tipoEvento: String = "Feria",        // Categoría (Feria, Jornada, Actividad Municipal)
    val prioridad: String = "Media",         // Prioridad comercial: Alta, Media, Baja
    val estado: String = "Identificada",     // Estado: Identificada, En Proceso, Confirmada
    val esAltaAfluencia: Boolean = false,    // Indicador de evento masivo (Switch)
    val requiereStand: Boolean = false       // Indicador de montaje en terreno (Checkbox)
)