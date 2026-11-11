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
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.ui.oportunidades.FiltrosOportunidad
import duoc.example.gestionoportunidades.util.Catalogos
import duoc.example.gestionoportunidades.util.Estados
import duoc.example.gestionoportunidades.util.Regiones

// Panel de filtros de la lista de oportunidades (HU-22), con animación de despliegue.
//
// Filtros disponibles: estado, prioridad, tipo de evento, región, comuna, responsable (solo
// supervisor) y rango de fechas del evento. Es un componente "tonto": recibe los filtros actuales y
// avisa cada cambio con callbacks; el ViewModel decide cómo aplicarlos.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelFiltrosAnimado(
    visible: Boolean,
    filtros: FiltrosOportunidad,
    ejecutivos: List<Usuario>,
    mostrarResponsable: Boolean,          // true solo para el supervisor
    onEstadoChange: (String) -> Unit,
    onPrioridadChange: (String) -> Unit,
    onTipoEventoChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onComunaChange: (String) -> Unit,
    onResponsableChange: (Long) -> Unit,
    onFechaDesdeChange: (String) -> Unit,
    onFechaHastaChange: (String) -> Unit,
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
                .background(MaterialTheme.colorScheme.surfaceVariant)  // Fondo diferenciado
                .padding(16.dp)
        ) {
            Text("Estado", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Estados.todos.forEach { estado ->
                    FilterChip(
                        selected = estado == filtros.estado,
                        onClick = { onEstadoChange(if (estado == filtros.estado) "" else estado) },
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
                        selected = prioridad == filtros.prioridad,
                        onClick = { onPrioridadChange(if (prioridad == filtros.prioridad) "" else prioridad) },
                        label = { Text(prioridad) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // En los desplegables, la opción "" representa "Todos" (sin filtrar).
            CampoDesplegable(
                valor = filtros.tipoEvento.ifEmpty { "Todos" },
                etiqueta = "Tipo de evento",
                opciones = listOf("") + Catalogos.tiposEvento,
                textoDe = { it.ifEmpty { "Todos" } },
                error = null,
                onSeleccion = onTipoEventoChange
            )

            CampoDesplegable(
                valor = filtros.region.ifEmpty { "Todas" },
                etiqueta = "Región",
                opciones = listOf("") + Regiones.nombres,
                textoDe = { it.ifEmpty { "Todas" } },
                error = null,
                onSeleccion = onRegionChange
            )

            // La comuna solo se ofrece una vez elegida la región (evita una lista de 346 comunas).
            if (filtros.region.isNotEmpty()) {
                CampoDesplegable(
                    valor = filtros.comuna.ifEmpty { "Todas" },
                    etiqueta = "Comuna",
                    opciones = listOf("") + Regiones.comunasDe(filtros.region),
                    textoDe = { it.ifEmpty { "Todas" } },
                    error = null,
                    onSeleccion = onComunaChange
                )
            }

            // Solo el supervisor puede filtrar por ejecutivo responsable.
            if (mostrarResponsable) {
                CampoDesplegable(
                    valor = ejecutivos.firstOrNull { it.idUsuario == filtros.idResponsable }?.nombreCompleto
                        ?: "Todos",
                    etiqueta = "Responsable",
                    opciones = listOf<Usuario?>(null) + ejecutivos,
                    textoDe = { it?.nombreCompleto ?: "Todos" },
                    error = null,
                    onSeleccion = { onResponsableChange(it?.idUsuario ?: 0L) }
                )
            }

            CampoFecha(
                valor = filtros.fechaDesde,
                etiqueta = "Evento desde",
                error = null,
                onFechaElegida = onFechaDesdeChange
            )

            CampoFecha(
                valor = filtros.fechaHasta,
                etiqueta = "Evento hasta",
                error = null,
                onFechaElegida = onFechaHastaChange
            )

            if (filtros.hayFiltros()) {
                TextButton(onClick = onLimpiar) { Text("Limpiar filtros") }
            }
        }
    }
}
