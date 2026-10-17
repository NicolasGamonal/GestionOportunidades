package duoc.example.gestionoportunidades.util

object Estados {
    val todos = listOf(
        "Identificada",
        "Contacto realizado",
        "Propuesta enviada",
        "En espera de respuesta",
        "Confirmada",
        "Descartada"
    )
    val finales = listOf("Confirmada", "Descartada")
    val prioridades = listOf("Alta", "Media", "Baja")
}