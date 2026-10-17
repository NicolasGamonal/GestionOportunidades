package duoc.example.gestionoportunidades.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.util.Estados

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelFiltrosAnimado(
    visible: Boolean,
    estadoSeleccionado: String,
    prioridadSeleccionada: String,
    onEstadoChange: (String) -> Unit,
    onPrioridadChange: (String) -> Unit,
    onLimpiar: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text("Estado", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Estados.todos.forEach { estado ->
                    FilterChip(
                        selected = estado == estadoSeleccionado,
                        // tocar el chip activo lo desmarca
                        onClick = { onEstadoChange(if (estado == estadoSeleccionado) "" else estado) },
                        label = { Text(estado) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Prioridad", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Estados.prioridades.forEach { prioridad ->
                    FilterChip(
                        selected = prioridad == prioridadSeleccionada,
                        onClick = { onPrioridadChange(if (prioridad == prioridadSeleccionada) "" else prioridad) },
                        label = { Text(prioridad) }
                    )
                }
            }

            if (estadoSeleccionado.isNotEmpty() || prioridadSeleccionada.isNotEmpty()) {
                TextButton(onClick = onLimpiar) { Text("Limpiar filtros") }
            }
        }
    }
}