package duoc.example.gestionoportunidades.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val localeChile = Locale.forLanguageTag("es-CL")
private val formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", localeChile)
private val formatoFechaCorta = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun Long.comoFechaHora(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(formatoFechaHora)

fun Long.comoFechaCorta(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(formatoFechaCorta)

fun Double.comoPesos(): String = String.format(localeChile, "\$%,.0f", this)

fun hoyComoFechaCorta(): String = LocalDate.now().format(formatoFechaCorta)

/** "dd/MM/yyyy" a milisegundos, a la hora indicada y en la zona horaria del teléfono. */
fun String.fechaCortaAMillis(hora: LocalTime): Long =
    LocalDate.parse(this, formatoFechaCorta)
        .atTime(hora)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

/** El DatePicker entrega la fecha elegida como medianoche UTC. */
fun Long.deDatePickerAFechaCorta(): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().format(formatoFechaCorta)

/** Para precargar el DatePicker con la fecha que ya tiene el campo. */
fun String.fechaCortaADatePicker(): Long? = runCatching {
    LocalDate.parse(this, formatoFechaCorta).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}.getOrNull()