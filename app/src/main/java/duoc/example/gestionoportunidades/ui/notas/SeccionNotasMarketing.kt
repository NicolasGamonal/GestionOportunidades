package duoc.example.gestionoportunidades.ui.notas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import duoc.example.gestionoportunidades.ui.AppViewModelProvider
import duoc.example.gestionoportunidades.ui.components.CampoFormulario
import duoc.example.gestionoportunidades.ui.components.SelectorChips
import duoc.example.gestionoportunidades.util.Catalogos
import duoc.example.gestionoportunidades.util.comoFechaHora

// Material y observaciones de marketing sobre una oportunidad confirmada (HU-27).
// Crea su propio ViewModel: al vivir dentro de la pantalla de detalle, recibe el mismo
// argumento "id" de la ruta.
@Composable
fun SeccionNotasMarketing(
    puedeRegistrar: Boolean,
    viewModel: NotasMarketingViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Material y observaciones de marketing (${ui.notas.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (ui.notas.isEmpty()) {
            Text(
                "Aún no hay notas registradas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ui.notas.forEach { nota ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${nota.tipo} · ${nota.fechaHora.comoFechaHora()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(nota.texto, style = MaterialTheme.typography.bodyLarge)
                    Text("Registrada por: ${nota.autor}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (puedeRegistrar) {
            SelectorChips(
                titulo = "Tipo de nota",
                opciones = Catalogos.tiposNotaMarketing,
                seleccionada = ui.tipo,
                onSeleccion = viewModel::onTipoChange
            )
            CampoFormulario(
                valor = ui.texto,
                onValorChange = viewModel::onTextoChange,
                etiqueta = "Describe el material u observación",
                error = ui.error,
                singleLine = false,
                minLines = 3
            )
            Button(onClick = viewModel::agregar, modifier = Modifier.fillMaxWidth()) {
                Text("Agregar nota")
            }
        }
    }
}