package duoc.example.gestionoportunidades.util

object Catalogos {
    val tiposEvento = listOf(
        "Evento corporativo",
        "Feria empresarial",
        "Actividad municipal",
        "Jornada de beneficios",
        "Feria institucional",
        "Actividad de trabajadores",
        "Activación comercial",
        "Otra instancia relevante"
    )
    val tiposOrganizacion = listOf(
        "Empresa", "Municipalidad", "Institución pública", "Institución privada",
        "Organizador de eventos", "Centro comercial", "Asociación", "Otra entidad"
    )
    val areasContacto = listOf(
        "Eventos", "Activaciones", "Marketing", "Comunicaciones",
        "Recursos Humanos", "Bienestar", "Beneficios", "Administración"
    )

    const val TIPO_CAMBIO_ESTADO = "Cambio de estado"

    val tiposAccion = listOf(
        "Investigación inicial", "Búsqueda de contacto", "Primer contacto",
        "Envío de presentación", "Envío de propuesta", "Solicitud de antecedentes",
        "Reunión", "Llamada", "Seguimiento", "Recepción de respuesta",
        "Confirmación", "Cierre"
    )
}