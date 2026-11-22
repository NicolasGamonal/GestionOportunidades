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

    // Niveles para los chips del formulario de oportunidad (HU-04 y HU-10)
    val nivelesPotencial = listOf("Alto", "Medio", "Bajo")
    val nivelesAfinidad = listOf("Alta", "Media", "Baja")

    // Medio de contacto preferente de un contacto (HU-13)
    val mediosContacto = listOf("Correo", "Teléfono", "WhatsApp", "Presencial")

    // Evaluación de una activación (HU-24): 1 = mala, 5 = excelente
    val evaluaciones = listOf(1, 2, 3, 4, 5)

    const val TIPO_CAMBIO_ESTADO = "Cambio de estado"
    // Se muestra cuando el servidor no confirmó el guardado (el repositorio devolvió 0L)
    const val MENSAJE_ERROR_GUARDADO = "No se pudo guardar. Revisa tu conexión e inténtalo de nuevo."

    val tiposAccion = listOf(
        "Investigación inicial", "Búsqueda de contacto", "Primer contacto",
        "Envío de presentación", "Envío de propuesta", "Solicitud de antecedentes",
        "Reunión", "Llamada", "Seguimiento", "Recepción de respuesta",
        "Confirmación", "Cierre"
    )
    val tiposNotaMarketing = listOf("Observación", "Material")
}