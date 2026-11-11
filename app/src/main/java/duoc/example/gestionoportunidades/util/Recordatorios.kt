package duoc.example.gestionoportunidades.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Recordatorios locales de seguimientos (HU-21). No usa internet ni servidores: la app le pide al
// sistema (AlarmManager) que la avise el día del próximo seguimiento, y en ese momento
// RecordatorioReceiver muestra la notificación.
//
// Se usa AlarmManager.set() (alarma "inexacta"): no necesita permisos especiales y el sistema puede
// adelantar o atrasar la alarma unos minutos para ahorrar batería, algo aceptable para un recordatorio.
// Limitación conocida: las alarmas se pierden si el teléfono se reinicia.
object Recordatorios {

    const val CANAL_ID = "recordatorios_seguimiento"
    const val EXTRA_ID = "idSeguimiento"
    const val EXTRA_TITULO = "titulo"
    const val EXTRA_TEXTO = "texto"

    // Hora del día a la que se avisa (el seguimiento se programa por fecha, no por hora).
    private const val HORA_AVISO = 9
    private const val UN_MINUTO = 60_000L

    // Las notificaciones (Android 8+) deben pertenecer a un "canal". Crearlo varias veces es seguro.
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_ID,
            "Recordatorios de seguimiento",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de los próximos seguimientos de tus oportunidades"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /**
     * Programa un aviso para el día del próximo seguimiento, a las 09:00.
     * - Si esa hora ya pasó y el seguimiento es para hoy, avisa en un minuto.
     * - Si ya pasó y es de un día anterior, no programa nada.
     */
    fun programarSeguimiento(
        context: Context,
        idSeguimiento: Long,
        nombreOportunidad: String,
        proximaAccion: String,
        fechaProximoMillis: Long
    ) {
        val zona = ZoneId.systemDefault()
        val ahora = System.currentTimeMillis()
        val dia = Instant.ofEpochMilli(fechaProximoMillis).atZone(zona).toLocalDate()

        var disparo = dia.atTime(HORA_AVISO, 0).atZone(zona).toInstant().toEpochMilli()
        if (disparo <= ahora) {
            if (dia != LocalDate.now(zona)) return
            disparo = ahora + UN_MINUTO
        }

        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(EXTRA_ID, idSeguimiento)
            putExtra(EXTRA_TITULO, "Seguimiento: $nombreOportunidad")
            putExtra(EXTRA_TEXTO, proximaAccion)
        }
        // requestCode = id del seguimiento: cada seguimiento tiene su propia alarma.
        val pendiente = PendingIntent.getBroadcast(
            context,
            idSeguimiento.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.RTC_WAKEUP, disparo, pendiente)
    }
}
