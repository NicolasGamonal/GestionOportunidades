package duoc.example.gestionoportunidades.ui.alertas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import duoc.example.gestionoportunidades.ui.auth.SesionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onVerOportunidad: (Long) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rol = SesionManager.getRol()

    LaunchedEffect(Unit) {
        viewModel.cargarAlertasYMetricas()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    if (rol == "SUPERVISOR") "Panel General" else "Mi Dashboard"
                )
            })
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Eventos próximos ---
                item {
                    Text(
                        "Eventos Próximos",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (uiState.eventosProximos.isEmpty()) {
                    item {
                        Card {
                            Text(
                                "No hay eventos próximos",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(uiState.eventosProximos) { oportunidad ->
                        DashboardCard(
                            titulo = oportunidad.nombre,
                            detalle = "${oportunidad.comuna} · ${oportunidad.estado}",
                            icono = Icons.Default.Event,
                            onClick = { onVerOportunidad(oportunidad.idOportunidad) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- Seguimientos atrasados ---
                item {
                    Text(
                        "Gestiones Atrasadas",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (uiState.seguimientosAtrasados.isEmpty()) {
                    item {
                        Card {
                            Text(
                                "No hay gestiones atrasadas",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(uiState.seguimientosAtrasados) { seguimiento ->
                        DashboardCard(
                            titulo = seguimiento.tipoAccion,
                            detalle = "Vencido · Próxima: ${seguimiento.fechaProximo}",
                            icono = Icons.Default.Warning,
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    titulo: String,
    detalle: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}