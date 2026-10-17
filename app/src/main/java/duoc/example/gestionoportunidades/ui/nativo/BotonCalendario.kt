package duoc.example.gestionoportunidades.ui.nativo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val UNA_HORA = 60 * 60 * 1000L

@Composable
fun BotonAgregarCalendario(
    titulo: String,
    descripcion: String,
    inicioMillis: Long,
    finMillis: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Button(
        onClick = { agregarEventoCalendario(context, titulo, descripcion, inicioMillis, finMillis) },
        modifier = modifier
    ) {
        Icon(Icons.Default.Event, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Agregar al calendario")
    }
}

private fun agregarEventoCalendario(
    context: Context,
    titulo: String,
    descripcion: String,
    inicioMillis: Long,
    finMillis: Long
) {
    // Si el término no es posterior al inicio, el evento dura una hora
    val fin = if (finMillis > inicioMillis) finMillis else inicioMillis + UNA_HORA

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, titulo)
        putExtra(CalendarContract.Events.DESCRIPTION, descripcion)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicioMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fin)
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay una aplicación de calendario instalada.", Toast.LENGTH_LONG).show()
    }
}