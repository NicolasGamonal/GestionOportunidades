package duoc.example.gestionoportunidades.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import duoc.example.gestionoportunidades.util.comoHoraOrNull
import duoc.example.gestionoportunidades.util.horaComoTexto
import java.time.LocalTime

// Campo de hora (HU-08). Funciona igual que CampoFecha: es de solo lectura, y al tocarlo abre un
// diálogo con el reloj de Material 3 (TimePicker). El valor se maneja como texto "HH:mm" (24 horas).
//
// Se usa un Dialog propio (en vez de AlertDialog) porque el reloj es ancho y el AlertDialog
// estándar puede recortarlo en pantallas angostas.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    valor: String,                          // "HH:mm" o vacío
    etiqueta: String,
    error: String?,
    onHoraElegida: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    CampoSelector(
        valor = valor,
        etiqueta = etiqueta,
        error = error,
        icono = Icons.Filled.Schedule,
        onClick = { abierto = true },
        modifier = modifier
    )

    if (abierto) {
        val inicial = valor.comoHoraOrNull() ?: LocalTime.of(9, 0)
        val estado = rememberTimePickerState(
            initialHour = inicial.hour,
            initialMinute = inicial.minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { abierto = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(etiqueta, style = MaterialTheme.typography.titleMedium)
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        TimePicker(state = estado)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { abierto = false }) { Text("Cancelar") }
                        TextButton(onClick = {
                            onHoraElegida(horaComoTexto(estado.hour, estado.minute))
                            abierto = false
                        }) { Text("Aceptar") }
                    }
                }
            }
        }
    }
}
