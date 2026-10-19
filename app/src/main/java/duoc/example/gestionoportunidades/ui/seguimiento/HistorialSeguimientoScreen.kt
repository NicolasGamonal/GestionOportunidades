package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.ui.components.PantallaMensaje
import duoc.example.gestionoportunidades.ui.nativo.BotonAgregarCalendario
import duoc.example.gestionoportunidades.util.Catalogos
import duoc.example.gestionoportunidades.util.aInicioDeJornada
import duoc.example.gestionoportunidades.util.comoFechaCorta
import duoc.example.gestionoportunidades.util.comoFechaHora

// Historial de gestiones de una oportunidad (timeline).
// Se llega desde DetalleOportunidadScreen (botón "Ver historial de gestión").
// Muestra cada gestión como una tarjeta. Los "Cambios de estado" se distinguen
// visualmente (fondo secondaryContainer + ícono de swap).
// Solo se puede AGREGAR gestiones (FAB "Nueva gestión"): no se editan ni eliminan.

private const val UNA_HORA = 60 * 60 * 1000L

@Composable
fun HistorialSeguimientoScreen(
    viewModel: HistorialSeguimientoViewModel,
    onVolver: () -> Unit,
    onNuevaGestion: (Long) -> Unit   // Navega al formulario de nueva gestión
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ui.sinAcceso -> PantallaMensaje("No tienes permiso para ver esta oportunidad", onVolver)
        ui.noEncontrada -> PantallaMensaje("No se encontró la oportunidad", onVolver)
        else -> Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nombre de la oportunidad (contexto)
                item {
                    Text(
                        text = ui.nombreOportunidad,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Estado vacío o lista de tarjetas
                if (ui.items.isEmpty()) {
                    item {
                        Text("Aún no hay gestiones registradas.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    items(ui.items, key = { it.seguimiento.idSeguimiento }) { item ->
                        SeguimientoCard(item, ui.nombreOportunidad)
                    }
                }
            }
            // FAB: navegar al formulario de nueva gestión
            ExtendedFloatingActionButton(
                onClick = { onNuevaGestion(viewModel.idOportunidad) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva gestión") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }
}

// Tarjeta individual de una gestión.
// Los "Cambios de estado" se ven distintos: fondo secondaryContainer + ícono SwapHoriz.
// No muestran "Próxima acción" ni botón de calendario (no programan una gestión).
@Composable
private fun SeguimientoCard(item: SeguimientoItem, nombreOportunidad: String) {
    val s = item.seguimiento
    val esCambioEstado = s.tipoAccion == Catalogos.TIPO_CAMBIO_ESTADO

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Fondo distinto para cambios de estado (los distingue visualmente del resto)
        colors = if (esCambioEstado) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Tipo de acción (con ícono si es cambio de estado)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (esCambioEstado) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = s.tipoAccion, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            // Fecha + autor (nombre del usuario que registró)
            Text(text = "${s.fechaHora.comoFechaHora()} · ${item.autor}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            // Resultado (sin prefijo "Resultado:" si es cambio de estado, ya que el texto ya lo indica)
            Text(
                text = if (esCambioEstado) s.resultado else "Resultado: ${s.resultado}",
                style = MaterialTheme.typography.bodyMedium
            )
            // Contacto (solo si hay uno asociado)
            item.contacto?.let { contacto ->
                Text(text = "Contacto: $contacto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Próxima acción + botón de calendario: solo para gestiones reales (no cambios de estado)
            if (!esCambioEstado) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(text = "Próxima acción: ${s.proximaAccion}", style = MaterialTheme.typography.labelLarge)
                Text(text = "Programada para: ${s.fechaProximo.comoFechaCorta()}", style = MaterialTheme.typography.bodySmall)
                // Solo se muestra el botón de calendario si la fecha aún no pasó
                if (s.fechaProximo > System.currentTimeMillis()) {
                    Spacer(Modifier.height(8.dp))
                    val inicio = s.fechaProximo.aInicioDeJornada()
                    BotonAgregarCalendario(
                        titulo = "Seguimiento: ${s.proximaAccion}",
                        descripcion = "Oportunidad: $nombreOportunidad. Gestión previa: ${s.tipoAccion}",
                        inicioMillis = inicio,
                        finMillis = inicio + UNA_HORA,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}