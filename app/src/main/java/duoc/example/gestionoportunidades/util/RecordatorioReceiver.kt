package duoc.example.gestionoportunidades.util

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import duoc.example.gestionoportunidades.MainActivity

// Se ejecuta cuando salta la alarma programada por Recordatorios.programarSeguimiento().
// Construye y muestra la notificación del seguimiento (HU-21).
class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(Recordatorios.EXTRA_TITULO) ?: return
        val texto = intent.getStringExtra(Recordatorios.EXTRA_TEXTO).orEmpty()
        val id = intent.getLongExtra(Recordatorios.EXTRA_ID, 0L)

        // En Android 13+ hace falta el permiso POST_NOTIFICATIONS; sin él no se muestra nada.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        Recordatorios.crearCanal(context)

        // Al tocar la notificación se abre la app.
        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = Notification.Builder(context, Recordatorios.CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(id.toInt(), notificacion)
    }
}
