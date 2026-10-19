package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.Organizacion

// Lista de organizaciones (pestaña "Organizaciones" del menú inferior).
// Búsqueda por nombre + lista de tarjetas + botón "Nueva".
// Sin filtros (a diferencia de OportunidadesScreen que tiene estado y prioridad).
@Composable
fun OrganizacionesScreen(
    viewModel: OrganizacionesViewModel,
    onVerDetalle: (Long) -> Unit,   // Navegar al detalle de la organización
    onNueva: () -> Unit             // Navegar al formulario de creación
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // bottom = 88.dp: espacio para que el FAB no tape la última tarjeta
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Campo de búsqueda (cada tecla → ViewModel → DAO con debounce)
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = viewModel::onBusquedaChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar organización...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    singleLine = true
                )
            }

            when {
                // Cargando
                ui.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                // Estado vacío: mensaje distinto según haya búsqueda o no
                ui.organizaciones.isEmpty() -> item {
                    Text(
                        text = if (busqueda.isBlank()) "Aún no hay organizaciones registradas"
                        else "Ninguna organización coincide con la búsqueda",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                // Lista normal
                else -> items(ui.organizaciones, key = { it.idOrganizacion }) { org ->
                    OrganizacionCard(org) { onVerDetalle(org.idOrganizacion) }
                }
            }
        }

        // Botón flotante "Nueva"
        ExtendedFloatingActionButton(
            onClick = onNueva,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nueva") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

// Tarjeta individual de una organización (solo se usa en esta pantalla → private).
// Muestra: nombre, tipo · rubro, comuna.
@Composable
private fun OrganizacionCard(organizacion: Organizacion, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Ícono + nombre
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = organizacion.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            // Tipo + rubro
            Text(
                text = "${organizacion.tipo} · ${organizacion.rubro}",
                style = MaterialTheme.typography.bodyMedium
            )
            // Comuna
            Text(
                text = organizacion.comuna,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}