package duoc.example.gestionoportunidades.testutil

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Fechas de prueba en la zona de Santiago, independientes de la zona del computador.
 * En marzo de 2026 Chile está en horario de verano (UTC-3).
 */
object FechasPrueba {

    val SANTIAGO: ZoneId = ZoneId.of("America/Santiago")

    fun instante(iso: String): Long = Instant.parse(iso).toEpochMilli()

    /** Medianoche (00:00) de Santiago del día "yyyy-MM-dd", en milisegundos. */
    fun dia(fecha: String): Long =
        LocalDate.parse(fecha).atStartOfDay(SANTIAGO).toInstant().toEpochMilli()

    /** Fecha y hora de Santiago "yyyy-MM-ddTHH:mm:ss", en milisegundos. */
    fun hora(fechaHora: String): Long =
        LocalDateTime.parse(fechaHora).atZone(SANTIAGO).toInstant().toEpochMilli()

    // Constantes escritas a mano (00:00 de Santiago = 03:00 UTC en marzo) para no depender
    // del mismo código que se está probando.
    val MARZO_01: Long = instante("2026-03-01T03:00:00Z")
    val MARZO_15: Long = instante("2026-03-15T03:00:00Z")
    val MARZO_16: Long = instante("2026-03-16T03:00:00Z")

    /** 14:30 de Santiago del 10/03/2026 = 17:30 UTC. */
    val MARZO_10_1430: Long = instante("2026-03-10T17:30:00Z")
}