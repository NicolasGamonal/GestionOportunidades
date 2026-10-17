package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.data.Oportunidad
import duoc.example.gestionoportunidades.ui.components.StatusChip
import duoc.example.gestionoportunidades.ui.nativo.BotonAgregarCalendario

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
                // Ajustado al modelo de datos oficial ("nombre" en lugar de "nombreEvento")
                text = oportunidad.nombre,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatusChip(estado = estadoActual)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Datos del detalle requeridos en J2 ajustados al modelo oficial[cite: 3]
        DetalleItem(titulo = "Tipo de Evento", valor = oportunidad.tipoEvento)
        // Nota: organizacion y contacto provienen de las claves foráneas, aquí los simulamos
        DetalleItem(titulo = "Comuna", valor = oportunidad.comuna)
        DetalleItem(titulo = "Público Esperado", valor = oportunidad.publicoEsperado.toString())
        DetalleItem(titulo = "Prioridad", valor = oportunidad.prioridad)
        DetalleItem(titulo = "Costo Referencial", valor = "$${oportunidad.costoReferencial}")

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(text = "Fechas del evento:", fontWeight = FontWeight.Bold)
        DetalleItem(titulo = "Inicio", valor = oportunidad.fechaInicio)
        DetalleItem(titulo = "Término", valor = oportunidad.fechaTermino)

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

        // Botón para simular el avance de estado
        Button(
            onClick = {
                estadoActual = if (estadoActual == "Identificada") "Contacto realizado" else "Propuesta enviada"
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

        // Cámara nativa (Tarea J5)[cite: 3]
        duoc.example.gestionoportunidades.ui.nativo.SeccionEvidencia()

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