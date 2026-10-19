package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.components.PanelFiltrosAnimado
import duoc.example.gestionoportunidades.ui.components.StatusChip
import duoc.example.gestionoportunidades.ui.components.TarjetaOportunidad
import duoc.example.gestionoportunidades.util.comoFechaHora

@Composable
fun OportunidadesScreen(viewModel: OportunidadesViewModel, onVerDetalle: (Long) -> Unit, onNueva: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filtros by viewModel.filtros.collectAsStateWithLifecycle()
    var mostrarFiltros by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // bottom = 88.dp deja espacio para que el botón no tape la última tarjeta
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = filtros.busqueda,
                    onValueChange = viewModel::onBusquedaChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar por nombre...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                }
            }
        }
        item {
            PanelFiltrosAnimado(
                visible = mostrarFiltros,
                estadoSeleccionado = filtros.estado,
                prioridadSeleccionada = filtros.prioridad,
                onEstadoChange = viewModel::onEstadoChange,
                onPrioridadChange = viewModel::onPrioridadChange,
                onLimpiar = viewModel::limpiarFiltros
            )
        }

        when {
            uiState.isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.oportunidades.isEmpty() -> item {
                Text(
                    "No hay oportunidades que mostrar",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            else -> items(uiState.oportunidades, key = { it.idOportunidad }) { oportunidad ->
                TarjetaOportunidad(
                    oportunidad = oportunidad,
                    onClick = { onVerDetalle(oportunidad.idOportunidad) }
                )
                }
            }

        }
        ExtendedFloatingActionButton(
            onClick = onNueva,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nueva") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}
