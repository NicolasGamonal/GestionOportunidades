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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun BotonAgregarCalendario(
    titulo: String,
    descripcion: String,
    fechaString: String, // Formato esperado: "yyyy-MM-dd" o "yyyy-MM-dd HH:mm"
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Button(
        onClick = { agregarEventoCalendario(context, titulo, descripcion, fechaString) },
        modifier = modifier
    ) {
        Icon(Icons.Default.Event, contentDescription = "Calendario")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Agregar al calendario")
    }
}

private fun agregarEventoCalendario(
    context: Context,
    titulo: String,
    descripcion: String,
    fechaString: String
) {
    try {
        // Parsear la fecha manejando la zona horaria correcta
        val formato = if (fechaString.contains(":")) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
        formato.timeZone = TimeZone.getDefault()

        val date = formato.parse(fechaString)
        val timeInMillis = date?.time ?: System.currentTimeMillis()

        // Crear el Intent ACTION_INSERT con CalendarContract[cite: 3]
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, titulo)
            putExtra(CalendarContract.Events.DESCRIPTION, descripcion)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, timeInMillis)
            // Agregamos 1 hora por defecto para el fin del evento
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, timeInMillis + (60 * 60 * 1000))
        }

        context.startActivity(intent)

    } catch (e: ActivityNotFoundException) {
        // Manejar que no haya app de calendario instalada[cite: 3]
        Toast.makeText(context, "No hay una aplicación de calendario instalada.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al procesar la fecha.", Toast.LENGTH_SHORT).show()
    }
}