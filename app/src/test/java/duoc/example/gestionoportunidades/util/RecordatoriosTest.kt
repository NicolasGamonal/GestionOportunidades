package duoc.example.gestionoportunidades.util

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.test.core.app.ApplicationProvider
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * Pruebas de Recordatorios (HU-21) con Robolectric: AlarmManager y NotificationManager son
 * "shadows" de Robolectric, así que se puede ver qué alarma se programó sin un teléfono real.
 *
 * Recordatorios decide con la hora actual del sistema (avisa a las 09:00 del día del seguimiento,
 * o en un minuto si hoy ya pasaron las 09:00). Para que las pruebas no dependan de la hora a la
 * que se ejecutan, [fijarHoraLocal] cambia la zona horaria por defecto a una en la que "ahora"
 * es la hora local pedida. ZonaHorariaRule devuelve la zona original al terminar cada prueba.
 */
@RunWith(RobolectricTestRunner::class)
class RecordatoriosTest {

    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val notificaciones: NotificationManager
        get() = app.getSystemService(NotificationManager::class.java)
    private val alarmas get() = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms

    // =========================================================================
    // Ayudas de tiempo
    // =========================================================================

    /** Deja la zona horaria por defecto en un desfase fijo tal que la hora local de ahora sea [hora]:00. */
    private fun fijarHoraLocal(hora: Int) {
        val ahora = System.currentTimeMillis()
        val minutosUtcDelDia = Math.floorMod(ahora, MS_POR_DIA) / 60_000L
        // Desfase en minutos llevado al rango [-12 h, +12 h).
        val desfase = Math.floorMod(hora * 60L - minutosUtcDelDia + 12 * 60, 24 * 60L) - 12 * 60
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.ofTotalSeconds((desfase * 60).toInt())))
    }

    private fun hoy(): LocalDate =
        Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate()

    /** Un instante cualquiera del día indicado (23:59 de la zona actual). */
    private fun finDelDia(dia: LocalDate): Long =
        dia.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun aLas9(dia: LocalDate): Long =
        dia.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun programar(id: Long, dia: LocalDate, nombre: String = "Feria del Emprendedor", accion: String = "Enviar propuesta") =
        Recordatorios.programarSeguimiento(
            context = app,
            idSeguimiento = id,
            nombreOportunidad = nombre,
            proximaAccion = accion,
            fechaProximoMillis = finDelDia(dia)
        )

    // =========================================================================
    // Constantes (contrato con RecordatorioReceiver)
    // =========================================================================

    @Test
    fun `las constantes publicas tienen los valores que usa el receptor`() {
        assertEquals("recordatorios_seguimiento", Recordatorios.CANAL_ID)
        assertEquals("idSeguimiento", Recordatorios.EXTRA_ID)
        assertEquals("titulo", Recordatorios.EXTRA_TITULO)
        assertEquals("texto", Recordatorios.EXTRA_TEXTO)
    }

    // =========================================================================
    // crearCanal
    // =========================================================================

    @Test
    fun `crearCanal crea el canal con su nombre importancia y descripcion`() {
        notificaciones.deleteNotificationChannel(Recordatorios.CANAL_ID)
        assertNull(notificaciones.getNotificationChannel(Recordatorios.CANAL_ID))

        Recordatorios.crearCanal(app)

        val canal = notificaciones.getNotificationChannel(Recordatorios.CANAL_ID)
        assertNotNull(canal)
        assertEquals("Recordatorios de seguimiento", canal.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, canal.importance)
        assertEquals("Avisos de los próximos seguimientos de tus oportunidades", canal.description)
    }

    @Test
    fun `crearCanal varias veces deja un solo canal`() {
        Recordatorios.crearCanal(app)
        Recordatorios.crearCanal(app)

        val canales = notificaciones.notificationChannels.filter { it.id == Recordatorios.CANAL_ID }
        assertEquals(1, canales.size)
    }

    // =========================================================================
    // programarSeguimiento: cuándo se programa
    // =========================================================================

    @Test
    fun `un seguimiento de un dia futuro programa la alarma a las 09 00 de ese dia`() {
        fijarHoraLocal(15)
        val dia = hoy().plusDays(3)

        programar(id = 7L, dia = dia)

        assertEquals(1, alarmas.size)
        assertEquals(aLas9(dia), alarmas.single().triggerAtTime)
    }

    @Test
    fun `la alarma es inexacta y despierta el telefono`() {
        fijarHoraLocal(15)

        programar(id = 7L, dia = hoy().plusDays(1))

        assertEquals(AlarmManager.RTC_WAKEUP, alarmas.single().type)
    }

    @Test
    fun `un seguimiento de hoy antes de las 09 00 se programa a las 09 00 de hoy`() {
        fijarHoraLocal(3)
        val dia = hoy()

        programar(id = 7L, dia = dia)

        assertEquals(1, alarmas.size)
        assertEquals(aLas9(dia), alarmas.single().triggerAtTime)
    }

    @Test
    fun `un seguimiento de hoy cuando ya pasaron las 09 00 avisa en un minuto`() {
        fijarHoraLocal(15)

        val antes = System.currentTimeMillis()
        programar(id = 7L, dia = hoy())
        val despues = System.currentTimeMillis()

        assertEquals(1, alarmas.size)
        val disparo = alarmas.single().triggerAtTime
        assertTrue(
            "La alarma debe saltar un minuto después de programarla",
            disparo in (antes + 60_000L)..(despues + 60_000L)
        )
    }

    @Test
    fun `un seguimiento de un dia anterior no programa nada`() {
        fijarHoraLocal(15)

        programar(id = 7L, dia = hoy().minusDays(1))

        assertTrue(alarmas.isEmpty())
    }

    // =========================================================================
    // programarSeguimiento: qué se programa
    // =========================================================================

    @Test
    fun `la alarma dispara un broadcast al RecordatorioReceiver con titulo texto e id`() {
        fijarHoraLocal(15)

        programar(id = 7L, dia = hoy().plusDays(2), nombre = "Feria del Emprendedor", accion = "Enviar propuesta")

        val pendiente = shadowOf(alarmas.single().operation)
        assertTrue(pendiente.isBroadcastIntent)
        val intent = pendiente.savedIntent
        assertEquals(RecordatorioReceiver::class.java.name, intent.component?.className)
        assertEquals("Seguimiento: Feria del Emprendedor", intent.getStringExtra(Recordatorios.EXTRA_TITULO))
        assertEquals("Enviar propuesta", intent.getStringExtra(Recordatorios.EXTRA_TEXTO))
        assertEquals(7L, intent.getLongExtra(Recordatorios.EXTRA_ID, -1L))
    }

    @Test
    fun `el PendingIntent usa el id del seguimiento como requestCode y es inmutable`() {
        fijarHoraLocal(15)

        programar(id = 7L, dia = hoy().plusDays(2))

        val pendiente = shadowOf(alarmas.single().operation)
        assertEquals(7, pendiente.requestCode)
        assertTrue((pendiente.flags and PendingIntent.FLAG_IMMUTABLE) != 0)
    }

    @Test
    fun `cada seguimiento tiene su propia alarma`() {
        fijarHoraLocal(15)

        programar(id = 1L, dia = hoy().plusDays(2), nombre = "Feria A")
        programar(id = 2L, dia = hoy().plusDays(4), nombre = "Feria B")

        assertEquals(2, alarmas.size)
        val codigos = alarmas.map { shadowOf(it.operation).requestCode }.toSet()
        assertEquals(setOf(1, 2), codigos)
    }

    private companion object {
        const val MS_POR_DIA = 86_400_000L
    }
}