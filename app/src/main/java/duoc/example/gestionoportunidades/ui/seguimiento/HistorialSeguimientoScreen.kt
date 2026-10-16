package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.data.Seguimiento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialSeguimientoScreen(
    viewModel: SeguimientoViewModel,
    onNavigateToNuevoSeguimiento: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Recolectamos el estado respetando el ciclo de vida, como pide el checklist[cite: 4]
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de la Oportunidad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNuevoSeguimiento) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nueva Gestión")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Indicador de carga[cite: 4]
                uiState.estaCargando -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // Estado vacío[cite: 4]
                uiState.seguimientos.isEmpty() -> {
                    Text(
                        text = "Aún no hay gestiones registradas.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                // Lista de seguimientos
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.seguimientos) { seguimiento ->
                            SeguimientoCard(seguimiento)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeguimientoCard(seguimiento: Seguimiento) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = seguimiento.tipoAccion,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fecha: ${seguimiento.fechaHora}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Resultado: ${seguimiento.resultado}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Próxima acción: ${seguimiento.proximaAccion}",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Programado para: ${seguimiento.fechaProximo}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}