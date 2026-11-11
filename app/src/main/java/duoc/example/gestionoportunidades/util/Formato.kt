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
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

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

/** 09:00 del día de esta fecha; sirve para crear eventos de calendario a partir de fechas sin hora. */
fun Long.aInicioDeJornada(): Long {
    val zona = ZoneId.systemDefault()
    return Instant.ofEpochMilli(this).atZone(zona).toLocalDate()
        .atTime(9, 0).atZone(zona).toInstant().toEpochMilli()
}

/** Medianoche de hoy en la zona del teléfono. */
fun inicioDeHoyMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
// "HH:mm" (24 horas) de un timestamp, en la zona horaria del dispositivo. Se usa para mostrar y
// editar el horario de un evento (HU-08).
fun Long.comoHora(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(formatoHora)

// Convierte "HH:mm" a LocalTime; devuelve null si el texto no es una hora válida.
fun String.comoHoraOrNull(): LocalTime? = runCatching { LocalTime.parse(this, formatoHora) }.getOrNull()

// Formatea una hora y minutos como "HH:mm" (lo usa el selector de hora).
fun horaComoTexto(hora: Int, minuto: Int): String = LocalTime.of(hora, minuto).format(formatoHora)
