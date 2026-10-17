package duoc.example.gestionoportunidades.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val localeChile = Locale.forLanguageTag("es-CL")
private val formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", localeChile)

fun Long.comoFechaHora(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(formatoFechaHora)

fun Double.comoPesos(): String = String.format(localeChile, "\$%,.0f", this)