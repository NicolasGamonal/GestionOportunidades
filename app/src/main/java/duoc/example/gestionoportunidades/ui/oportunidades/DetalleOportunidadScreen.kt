package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.data.Oportunidad
import duoc.example.gestionoportunidades.ui.components.StatusChip
import duoc.example.gestionoportunidades.ui.nativo.BotonAgregarCalendario
import duoc.example.gestionoportunidades.ui.nativo.SeccionEvidencia

@Composable
fun DetalleOportunidadScreen(
    oportunidad: Oportunidad,
    onVolver: () -> Unit,
    onVerHistorial: () -> Unit = {}
) {
    // Estado local simulado para el cambio de estado que pide J2
    var estadoActual by remember { mutableStateOf(oportunidad.estado) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Encabezado con título y chip de estado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = oportunidad.nombre,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatusChip(estado = estadoActual)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Datos del detalle (organización y contacto vendrán de las claves foráneas en J2)
        DetalleItem(titulo = "Tipo de Evento", valor = oportunidad.tipoEvento)
        DetalleItem(titulo = "Comuna", valor = oportunidad.comuna)
        DetalleItem(titulo = "Público Esperado", valor = oportunidad.publicoEsperado.toString())
        DetalleItem(titulo = "Prioridad", valor = oportunidad.prioridad)
        DetalleItem(titulo = "Costo Referencial", valor = "$${oportunidad.costoReferencial}")

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(text = "Fechas del evento:", fontWeight = FontWeight.Bold)
        DetalleItem(titulo = "Inicio", valor = oportunidad.fechaInicio)
        DetalleItem(titulo = "Término", valor = oportunidad.fechaTermino)

        // Botón de calendario nativo (J6)
        BotonAgregarCalendario(
            titulo = "Evento: ${oportunidad.nombre}",
            descripcion = "Activación corporativa en ${oportunidad.comuna}. Tipo: ${oportunidad.tipoEvento}",
            fechaString = oportunidad.fechaInicio,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Observaciones:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(text = oportunidad.observaciones, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para simular el avance de estado (luego usará Permisos.kt)
        Button(
            onClick = {
                estadoActual =
                    if (estadoActual == "Identificada") "Contacto realizado" else "Propuesta enviada"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Avanzar Estado")
        }

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver a la lista")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Cámara nativa (J5)
        SeccionEvidencia()

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onVerHistorial,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Ver Historial de Gestión")
        }
    }
}

@Composable
fun DetalleItem(titulo: String, valor: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(text = titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}