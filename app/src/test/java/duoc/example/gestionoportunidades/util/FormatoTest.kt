package duoc.example.gestionoportunidades.util

import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeParseException

class FormatoTest {

    // Fija America/Santiago y es-CL: en noviembre Chile está en UTC-3 y en julio en UTC-4.
    @Rule @JvmField val zonaHoraria = ZonaHorariaRule()

    private fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    // --- Long -> texto ---

    @Test
    fun `comoFechaHora usa la zona de Santiago en horario de verano`() {
        assertEquals("22/11/2026 14:30", millis("2026-11-22T17:30:00Z").comoFechaHora())
    }

    @Test
    fun `comoFechaHora usa la zona de Santiago en horario de invierno`() {
        assertEquals("15/07/2026 08:00", millis("2026-07-15T12:00:00Z").comoFechaHora())
    }

    @Test
    fun `comoFechaCorta cambia de dia segun la zona`() {
        // 02:00 UTC del 23 sigue siendo el 22 a las 23:00 en Santiago.
        assertEquals("22/11/2026", millis("2026-11-23T02:00:00Z").comoFechaCorta())
        assertEquals("23/11/2026", millis("2026-11-23T03:00:00Z").comoFechaCorta())
    }

    @Test
    fun `comoHora entrega HH mm de 24 horas`() {
        assertEquals("14:30", millis("2026-11-22T17:30:00Z").comoHora())
        assertEquals("00:00", millis("2026-11-22T03:00:00Z").comoHora())
        assertEquals("23:59", millis("2026-11-23T02:59:00Z").comoHora())
    }

    // --- Pesos ---

    @Test
    fun `comoPesos usa punto como separador de miles y sin decimales`() {
        assertEquals("\$1.234.567", 1234567.0.comoPesos())
        assertEquals("\$1.000", 1000.0.comoPesos())
        assertEquals("\$999", 999.0.comoPesos())
        assertEquals("\$0", 0.0.comoPesos())
    }

    @Test
    fun `comoPesos redondea los decimales`() {
        assertEquals("\$1.235", 1234.6.comoPesos())
        assertEquals("\$1.234", 1234.4.comoPesos())
    }

    // --- Hoy ---

    @Test
    fun `hoyComoFechaCorta tiene formato dd MM yyyy`() {
        assertTrue(Regex("\\d{2}/\\d{2}/\\d{4}").matches(hoyComoFechaCorta()))
    }

    @Test
    fun `inicioDeHoyMillis es medianoche de hoy y no esta en el futuro`() {
        val inicio = inicioDeHoyMillis()
        assertEquals("00:00", inicio.comoHora())
        assertTrue(inicio <= System.currentTimeMillis())
        assertTrue(System.currentTimeMillis() - inicio < 25L * 60 * 60 * 1000)
    }

    // --- Texto -> Long ---

    @Test
    fun `fechaCortaAMillis convierte a la hora indicada en la zona de Santiago`() {
        assertEquals(millis("2026-01-01T03:00:00Z"), "01/01/2026".fechaCortaAMillis(LocalTime.MIDNIGHT))
        assertEquals(millis("2026-07-15T16:30:00Z"), "15/07/2026".fechaCortaAMillis(LocalTime.of(12, 30)))
    }

    @Test
    fun `fechaCortaAMillis con texto invalido lanza excepcion`() {
        try {
            "no-es-fecha".fechaCortaAMillis(LocalTime.NOON)
            fail("Debía lanzar DateTimeParseException")
        } catch (e: DateTimeParseException) {
            // esperado
        }
    }

    @Test
    fun `ida y vuelta fecha a millis a texto conserva fecha y hora`() {
        val ms = "22/11/2026".fechaCortaAMillis(LocalTime.of(14, 30))
        assertEquals("22/11/2026 14:30", ms.comoFechaHora())
    }

    // --- DatePicker (medianoche UTC) ---

    @Test
    fun `deDatePickerAFechaCorta lee la fecha en UTC aunque Santiago este un dia atras`() {
        // Medianoche UTC del 22 es el 21 a las 21:00 en Santiago, pero el DatePicker quiso decir el 22.
        assertEquals("22/11/2026", millis("2026-11-22T00:00:00Z").deDatePickerAFechaCorta())
    }

    @Test
    fun `fechaCortaADatePicker devuelve medianoche UTC`() {
        assertEquals(millis("2026-11-22T00:00:00Z"), "22/11/2026".fechaCortaADatePicker())
    }

    @Test
    fun `fechaCortaADatePicker con texto invalido devuelve null`() {
        assertNull("x".fechaCortaADatePicker())
        assertNull("".fechaCortaADatePicker())
        assertNull("2026-11-22".fechaCortaADatePicker())
    }

    @Test
    fun `DatePicker ida y vuelta conserva la fecha`() {
        assertEquals("05/03/2026", "05/03/2026".fechaCortaADatePicker()!!.deDatePickerAFechaCorta())
    }

    // --- Inicio de jornada ---

    @Test
    fun `aInicioDeJornada devuelve las 09 00 del mismo dia`() {
        val tarde = "22/11/2026".fechaCortaAMillis(LocalTime.of(15, 45))
        assertEquals(millis("2026-11-22T12:00:00Z"), tarde.aInicioDeJornada())
        assertEquals("22/11/2026 09:00", tarde.aInicioDeJornada().comoFechaHora())
    }

    @Test
    fun `aInicioDeJornada no cambia de dia a las 23 59 ni a medianoche`() {
        val casiMedianoche = "22/11/2026".fechaCortaAMillis(LocalTime.of(23, 59))
        val medianoche = "22/11/2026".fechaCortaAMillis(LocalTime.MIDNIGHT)
        assertEquals("22/11/2026 09:00", casiMedianoche.aInicioDeJornada().comoFechaHora())
        assertEquals("22/11/2026 09:00", medianoche.aInicioDeJornada().comoFechaHora())
    }

    // --- Horas ---

    @Test
    fun `comoHoraOrNull convierte horas validas`() {
        assertEquals(LocalTime.of(9, 30), "09:30".comoHoraOrNull())
        assertEquals(LocalTime.of(0, 0), "00:00".comoHoraOrNull())
        assertEquals(LocalTime.of(23, 59), "23:59".comoHoraOrNull())
    }

    @Test
    fun `comoHoraOrNull devuelve null con horas invalidas`() {
        assertNull("25:00".comoHoraOrNull())
        assertNull("12:60".comoHoraOrNull())
        assertNull("9:00".comoHoraOrNull())
        assertNull("".comoHoraOrNull())
        assertNull("abc".comoHoraOrNull())
    }

    @Test
    fun `horaComoTexto rellena con ceros`() {
        assertEquals("09:05", horaComoTexto(9, 5))
        assertEquals("00:00", horaComoTexto(0, 0))
        assertEquals("23:59", horaComoTexto(23, 59))
    }

    @Test
    fun `horaComoTexto con hora fuera de rango lanza excepcion`() {
        try {
            horaComoTexto(24, 0)
            fail("Debía lanzar DateTimeException")
        } catch (e: DateTimeException) {
            // esperado
        }
    }
}
