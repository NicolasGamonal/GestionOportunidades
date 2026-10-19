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

// BotonAgregarCalendario: botón reutilizable que abre la app de calendario del dispositivo
// para crear un evento con las fechas de una oportunidad o de un seguimiento.
//
// ¿Dónde se usa?
// - DetalleOportunidadScreen: "Agregar al calendario" con las fechas del evento
//   (fechaInicio y fechaTermino de la oportunidad).
// - SeguimientoScreen (formulario de nueva gestión): "Agregar al calendario" con la
//   fechaProximo del seguimiento.
//
// ¿Por qué está en ui/nativo/ y no en ui/components/?
// Porque usa APIs del sistema Android (Intent, CalendarContract, ActivityNotFoundException).
// Los componentes de ui/components/ son puramente visuales (Compose).
// ui/nativo/ agrupa todo lo que interactúa con el hardware o servicios del sistema
// (cámara, calendario).
//
// ¿Por qué no está dentro de cada pantalla?
// Porque se usa en 2 pantallas (detalle de oportunidad + seguimiento).
// La lógica del Intent es idéntica en ambas: solo cambian los parámetros
// (título, descripción, fechas).
//
// Cumple el checklist:
// - "Implementar integración con el calendario del dispositivo" → ACTION_INSERT
// - "Utilizar correctamente los datos de fecha y hora al crear eventos de calendario" → milisegundos
// - "Gestionar correctamente archivos, URIs, permisos y cancelaciones cuando corresponda" → try/catch

// Constante: duración por defecto del evento si no se puede calcular desde las fechas.
// 1 hora en milisegundos (60 min × 60 seg × 1000 ms).
// Se usa cuando finMillis <= inicioMillis (eventos de un solo día o fechas inválidas).
private const val UNA_HORA = 60 * 60 * 1000L

// Botón composable que abre el calendario del sistema para crear un evento.
//
// Parámetros:
// - titulo: título del evento en el calendario (ej: "Feria del Emprendedor Valle Verde").
// - descripcion: descripción del evento (ej: "Comuna: Providencia · Público: 2500").
// - inicioMillis: fecha y hora de inicio en milisegundos (timestamp).
// - finMillis: fecha y hora de fin en milisegundos (timestamp).
//   Si es <= inicioMillis, el evento dura 1 hora por defecto.
// - modifier: para que el padre controle el tamaño/posición del botón.
//
// ¿Por qué milisegundos y no Date?
// Porque CalendarContract.EXTRA_EVENT_BEGIN_TIME y EXTRA_EVENT_END_TIME
// esperan Long en milisegundos desde epoch (1970). Es el formato nativo de Android.
// Las fechas en Room ya se guardan como Long (milisegundos), así que no hay conversión.
@Composable
fun BotonAgregarCalendario(
    titulo: String,
    descripcion: String,
    inicioMillis: Long,
    finMillis: Long,
    modifier: Modifier = Modifier
) {
    // Context de Android: necesario para lanzar Intents (abrir otra app).
    // LocalContext.current: obtiene el Context del Composable (Activity o Fragment).
    val context = LocalContext.current

    // Botón Material 3 con ícono de calendario + texto.
    // Al presionar, llama a la función privada que construye el Intent.
    Button(
        onClick = { agregarEventoCalendario(context, titulo, descripcion, inicioMillis, finMillis) },
        modifier = modifier
    ) {
        // Ícono de evento/calendario a la izquierda del texto
        Icon(Icons.Default.Event, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Agregar al calendario")
    }
}

// Función privada que construye y lanza el Intent para crear un evento en el calendario.
//
// ¿Por qué es private?
// Porque solo se usa desde BotonAgregarCalendario (mismo archivo).
// No es un Composable: es una función de Android "pura" que trabaja con Intents.
//
// Flujo:
// 1. Calcular la fecha de fin (si es inválida, usar inicio + 1 hora).
// 2. Construir un Intent con ACTION_INSERT y la URI de CalendarContract.Events.
// 3. Agregar los extras (título, descripción, inicio, fin).
// 4. Lanzar el Intent (abre la app de calendario del sistema).
// 5. Si no hay app de calendario → catch → Toast de error.
//
// ¿Por qué try/catch con ActivityNotFoundException?
// Porque si el dispositivo no tiene ninguna app de calendario instalada
// (algunos emuladores sin Google APIs no la tienen), startActivity() lanza
// ActivityNotFoundException. El checklist pide "Manejar que no haya app de calendario".
// El Toast informa al usuario sin crashear la app.
//
// ¿Por qué no se pide permiso?
// Porque ACTION_INSERT con CalendarContract no requiere permiso de escritura.
// El Intent se resuelve a la app de calendario del usuario, que pide su propio
// permiso al usuario (si lo necesita). Nuestra app no escribe directo en la base
// de datos del calendario.
private fun agregarEventoCalendario(
    context: Context,
    titulo: String,
    descripcion: String,
    inicioMillis: Long,
    finMillis: Long
) {
    // Validación de fechas: si fin <= inicio (mismo día o fecha inválida),
    // el evento dura 1 hora por defecto.
    // Esto evita que el calendario muestre un evento de duración 0 o negativa.
    val fin = if (finMillis > inicioMillis) finMillis else inicioMillis + UNA_HORA

    // Intent para insertar un evento nuevo en el calendario.
    // ACTION_INSERT: le dice al sistema "quiero crear algo nuevo".
    // CalendarContract.Events.CONTENT_URI: especifica que es un evento de calendario
    // (vs. una tarea, un recordatorio, etc.).
    val intent = Intent(Intent.ACTION_INSERT).apply {
        // URI del proveedor de contenido de eventos de calendario
        data = CalendarContract.Events.CONTENT_URI
        // Título del evento (se ve en la vista de agenda del calendario)
        putExtra(CalendarContract.Events.TITLE, titulo)
        // Descripción (se ve al abrir el detalle del evento en el calendario)
        putExtra(CalendarContract.Events.DESCRIPTION, descripcion)
        // Fecha de inicio en milisegundos desde epoch
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicioMillis)
        // Fecha de fin en milisegundos desde epoch
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fin)
    }

    try {
        // Lanza el Intent: el sistema resuelve a la app de calendario del usuario.
        // El usuario ve el formulario de "nuevo evento" pre-llenado con nuestros datos.
        // Puede modificarlo o cancelar (no hay callback: no sabemos si guardó o no).
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No hay ninguna app de calendario instalada en el dispositivo.
        // Se muestra un Toast informativo (no crashea la app).
        // En la práctica, esto solo pasa en emuladores sin Google APIs.
        // El plan dice: "El emulador de Josue debe tener una app de calendario instalada
        // (imagen con Google APIs o Play) para probar J6."
        Toast.makeText(context, "No hay una aplicación de calendario instalada.", Toast.LENGTH_LONG).show()
    }
}