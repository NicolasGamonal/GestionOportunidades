package duoc.example.gestionoportunidades.util

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import duoc.example.gestionoportunidades.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Pruebas de RecordatorioReceiver (HU-21) con Robolectric.
 * Se llama a onReceive directamente con un Intent como el que arma Recordatorios.programarSeguimiento
 * y se revisa qué notificación queda publicada en el NotificationManager simulado.
 *
 * robolectric.properties fija sdk=35, o sea Android 13+, donde la notificación exige el permiso
 * POST_NOTIFICATIONS: cada prueba lo concede o lo niega de forma explícita.
 */
@RunWith(RobolectricTestRunner::class)
class RecordatorioReceiverTest {

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val notificaciones: NotificationManager
        get() = app.getSystemService(NotificationManager::class.java)

    private fun concederPermiso() = shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    private fun negarPermiso() = shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

    private fun intentDeRecordatorio(
        titulo: String? = "Seguimiento: Feria del Emprendedor",
        texto: String? = "Enviar propuesta",
        id: Long? = 7L
    ) = Intent().apply {
        if (titulo != null) putExtra(Recordatorios.EXTRA_TITULO, titulo)
        if (texto != null) putExtra(Recordatorios.EXTRA_TEXTO, texto)
        if (id != null) putExtra(Recordatorios.EXTRA_ID, id)
    }

    private fun recibir(intent: Intent) = RecordatorioReceiver().onReceive(app, intent)

    private fun publicadas() = shadowOf(notificaciones).allNotifications

    // =========================================================================
    // Casos en que no se muestra nada
    // =========================================================================

    @Test
    fun `sin titulo en el intent no muestra ninguna notificacion`() {
        concederPermiso()

        recibir(intentDeRecordatorio(titulo = null))

        assertTrue(publicadas().isEmpty())
    }

    @Test
    fun `sin el permiso de notificaciones no muestra nada`() {
        negarPermiso()

        recibir(intentDeRecordatorio())

        assertTrue(publicadas().isEmpty())
    }

    // =========================================================================
    // Notificación publicada
    // =========================================================================

    @Test
    fun `con permiso publica la notificacion con titulo y texto`() {
        concederPermiso()

        recibir(intentDeRecordatorio(titulo = "Seguimiento: Feria del Emprendedor", texto = "Enviar propuesta"))

        assertEquals(1, publicadas().size)
        val notificacion = publicadas().single()
        assertEquals(
            "Seguimiento: Feria del Emprendedor",
            notificacion.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        )
        assertEquals("Enviar propuesta", notificacion.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun `la notificacion usa el canal de recordatorios y se cierra al tocarla`() {
        concederPermiso()

        recibir(intentDeRecordatorio())

        val notificacion = publicadas().single()
        assertEquals(Recordatorios.CANAL_ID, notificacion.channelId)
        assertTrue((notificacion.flags and Notification.FLAG_AUTO_CANCEL) != 0)
    }

    @Test
    fun `usa el id del seguimiento como id de la notificacion`() {
        concederPermiso()

        recibir(intentDeRecordatorio(id = 7L))

        assertNotNull(shadowOf(notificaciones).getNotification(7))
    }

    @Test
    fun `sin id en el intent usa el id 0`() {
        concederPermiso()

        recibir(intentDeRecordatorio(id = null))

        assertNotNull(shadowOf(notificaciones).getNotification(0))
    }

    @Test
    fun `sin texto en el intent publica la notificacion con el texto vacio`() {
        concederPermiso()

        recibir(intentDeRecordatorio(texto = null))

        val texto = publicadas().single().extras.getCharSequence(Notification.EXTRA_TEXT)
        assertEquals("", texto?.toString().orEmpty())
    }

    @Test
    fun `dos seguimientos distintos publican dos notificaciones`() {
        concederPermiso()

        recibir(intentDeRecordatorio(id = 1L))
        recibir(intentDeRecordatorio(id = 2L))

        assertEquals(2, publicadas().size)
    }

    @Test
    fun `al tocar la notificacion se abre MainActivity`() {
        concederPermiso()

        recibir(intentDeRecordatorio())

        val abrir = publicadas().single().contentIntent
        assertNotNull(abrir)
        val pendiente = shadowOf(abrir)
        assertTrue(pendiente.isActivityIntent)
        val intent = pendiente.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        val banderas = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        assertEquals(banderas, intent.flags and banderas)
    }

    // =========================================================================
    // Canal
    // =========================================================================

    @Test
    fun `antes de notificar se asegura de que exista el canal`() {
        concederPermiso()
        notificaciones.deleteNotificationChannel(Recordatorios.CANAL_ID)
        assertNull(notificaciones.getNotificationChannel(Recordatorios.CANAL_ID))

        recibir(intentDeRecordatorio())

        assertNotNull(notificaciones.getNotificationChannel(Recordatorios.CANAL_ID))
    }
}