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

@Composable
fun DetalleOportunidadScreen(
    oportunidad: Oportunidad,
    onVolver: () -> Unit
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
                text = oportunidad.nombreEvento,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatusChip(estado = estadoActual)
        }

        // Usamos HorizontalDivider en lugar de Divider para Material 3
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Datos del detalle requeridos en J2
        DetalleItem(titulo = "Organización", valor = oportunidad.organizacion)
        DetalleItem(titulo = "Contacto", valor = oportunidad.correoContacto)
        DetalleItem(titulo = "Tipo de Evento", valor = oportunidad.tipoEvento)
        DetalleItem(titulo = "Prioridad", valor = oportunidad.prioridad)
        DetalleItem(titulo = "¿Alta Afluencia?", valor = if(oportunidad.esAltaAfluencia) "Sí" else "No")
        DetalleItem(titulo = "¿Requiere Stand?", valor = if(oportunidad.requiereStand) "Sí" else "No")

        Text(
            text = "Observaciones:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para simular el avance de estado (luego usará Permisos.kt de tu compañero)
        Button(
            onClick = {
                // Simulación simple de avance de estado
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
    }
}

@Composable
fun DetalleItem(titulo: String, valor: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(text = titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}