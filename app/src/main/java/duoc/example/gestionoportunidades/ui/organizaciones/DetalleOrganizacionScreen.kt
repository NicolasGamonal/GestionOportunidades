package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.ui.components.PantallaMensaje
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleItem

@Composable
fun DetalleOrganizacionScreen(
    viewModel: DetalleOrganizacionViewModel,
    onVolver: () -> Unit,
    onEditar: (Long) -> Unit,
    onNuevoContacto: (Long) -> Unit,
    onEditarContacto: (Long, Long) -> Unit      // (idOrganizacion, idContacto)
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val org = ui.organizacion

    when {
        ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        org == null -> PantallaMensaje("No se encontró la organización", onVolver)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = org.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item {
                Column {
                    DetalleItem("Tipo", org.tipo)
                    DetalleItem("Rubro", org.rubro)
                    DetalleItem("Comuna", org.comuna)
                }
            }
            if (ui.puedeEditar) {
                item {
                    OutlinedButton(onClick = { onEditar(org.idOrganizacion) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Editar organización")
                    }
                }
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Contactos (${ui.contactos.size})", style = MaterialTheme.typography.titleMedium)
            }
            if (ui.contactos.isEmpty()) {
                item {
                    Text(
                        "Esta organización aún no tiene contactos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(ui.contactos, key = { it.idContacto }) { contacto ->
                    ContactoCard(contacto) { onEditarContacto(org.idOrganizacion, contacto.idContacto) }
                }
            }
            item {
                Button(onClick = { onNuevoContacto(org.idOrganizacion) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar contacto")
                }
            }
        }
    }
}

@Composable
private fun ContactoCard(contacto: Contacto, onEditar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(contacto.nombre, style = MaterialTheme.typography.titleMedium)
                Text("${contacto.cargo} · ${contacto.area}", style = MaterialTheme.typography.bodyMedium)
                Text(contacto.correo, style = MaterialTheme.typography.bodySmall)
                Text(contacto.telefono, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar contacto")
            }
        }
    }
}