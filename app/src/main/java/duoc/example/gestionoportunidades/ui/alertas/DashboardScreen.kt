package duoc.example.gestionoportunidades.ui.alertas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.ui.theme.colores
import duoc.example.gestionoportunidades.util.comoFechaCorta

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onVerOportunidad: (Long) -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val esSupervisor = SesionManager.getRol() == "SUPERVISOR"
    val nombre = SesionManager.usuarioActual?.nombreCompleto ?: ""

    if (ui.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(
                        text = if (esSupervisor) "Resumen del equipo" else "Hola, $nombre",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (esSupervisor) "Ves las oportunidades de todo el equipo"
                        else "Estas son tus gestiones y eventos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            seccion("Gestiones atrasadas", ui.seguimientosAtrasados, "No hay gestiones atrasadas") { alerta ->
                DashboardCard(
                    titulo = alerta.nombreOportunidad,
                    detalle = detalleAlerta(alerta, esSupervisor, "venció el"),
                    icono = Icons.Default.Warning,
                    colorIcono = MaterialTheme.colorScheme.error,
                    onClick = { onVerOportunidad(alerta.idOportunidad) }
                )
            }

            seccion("Seguimientos pendientes", ui.seguimientosPendientes, "No hay seguimientos pendientes") { alerta ->
                DashboardCard(
                    titulo = alerta.nombreOportunidad,
                    detalle = detalleAlerta(alerta, esSupervisor, "para el"),
                    icono = Icons.Default.Schedule,
                    colorIcono = MaterialTheme.colores.advertencia,
                    onClick = { onVerOportunidad(alerta.idOportunidad) }
                )
            }

            seccion("Eventos próximos", ui.eventosProximos, "No hay eventos próximos") { oportunidad ->
                DashboardCard(
                    titulo = oportunidad.nombre,
                    detalle = "${oportunidad.comuna} · ${oportunidad.estado} · " +
                            oportunidad.fechaInicio.comoFechaCorta(),
                    icono = Icons.Default.Event,
                    colorIcono = MaterialTheme.colorScheme.primary,
                    onClick = { onVerOportunidad(oportunidad.idOportunidad) }
                )
            }
        }
    }
}

/** Un bloque del dashboard: título con contador, estado vacío y las tarjetas. */
private fun <T> LazyListScope.seccion(
    titulo: String,
    elementos: List<T>,
    textoVacio: String,
    tarjeta: @Composable (T) -> Unit
) {
    item {
        Text("$titulo (${elementos.size})", style = MaterialTheme.typography.titleMedium)
    }
    if (elementos.isEmpty()) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = textoVacio,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        items(elementos) { tarjeta(it) }
    }
}

private fun detalleAlerta(alerta: AlertaSeguimiento, mostrarResponsable: Boolean, verbo: String): String {
    val base = "${alerta.proximaAccion} · $verbo ${alerta.fechaProximo.comoFechaCorta()}"
    // El supervisor necesita saber de quién es cada gestión
    return if (mostrarResponsable) "$base · ${alerta.nombreResponsable ?: "Sin asignar"}" else base
}

@Composable
fun DashboardCard(
    titulo: String,
    detalle: String,
    icono: ImageVector,
    colorIcono: Color,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = colorIcono)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}