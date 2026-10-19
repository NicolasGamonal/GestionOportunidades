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

// PanelFiltrosAnimado: panel de filtros (estado + prioridad) con animación de expansión.
//
// ¿Dónde se usa?
// - OportunidadesScreen: el usuario presiona un botón de "Filtros" y este panel
//   aparece/desaparece debajo de la barra de búsqueda.
// - También podría usarse en otras listas que necesiten filtrar por estado y prioridad.
//
// ¿Por qué AnimatedVisibility?
// El checklist pide "al menos 3 interacciones animadas con 2 mecanismos".
// Este panel es 1 de los 3 (los otros 2 son MensajeConfirmacion y el chip de estado
// con animateColorAsState). Aquí se usan 2 mecanismos simultáneos:
// 1. expandVertically / shrinkVertically → el panel crece/se encoge verticalmente
// 2. fadeIn / fadeOut → el panel aparece/desaparece con transparencia
//
// ¿Por qué FlowRow y no Row?
// Porque hay 6 estados + 3 prioridades = 9 chips. En una pantalla estrecha (teléfono),
// una Row los apilaría horizontalmente y se saldrían de la pantalla.
// FlowRow los "envuelve" a la siguiente línea automáticamente cuando no caben.
// (Por eso se necesita @OptIn(ExperimentalLayoutApi::class).)
//
// ¿Por qué FilterChip y no Checkbox o ToggleButton?
// Porque FilterChip es el componente de Material 3 diseñado específicamente para filtros:
// - Muestra un check cuando está seleccionado.
// - Permite selección múltiple visual (aunque aquí es single-select por diseño).
// - Es más compacto que un Checkbox y más claro que un ToggleButton.

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelFiltrosAnimado(
    // Si el panel está visible o no. Lo controla la Screen (un botón "Filtros" lo alterna).
    // Cuando cambia de false a true → animación de entrada (expand + fade in).
    // Cuando cambia de true a false → animación de salida (shrink + fade out).
    visible: Boolean,
    // Estado actualmente seleccionado ("" = ningún filtro activo).
    // Viene del UiState del ViewModel (FiltrosOportunidad.estado).
    estadoSeleccionado: String,
    // Prioridad actualmente seleccionada ("" = ningún filtro activo).
    // Viene del UiState del ViewModel (FiltrosOportunidad.prioridad).
    prioridadSeleccionada: String,
    // Callback: se llama cuando el usuario selecciona (o deselecciona) un estado.
    // El ViewModel actualiza el filtro y la consulta del DAO se re-ejecuta.
    onEstadoChange: (String) -> Unit,
    // Callback: se llama cuando el usuario selecciona (o deselecciona) una prioridad.
    onPrioridadChange: (String) -> Unit,
    // Callback: se llama cuando el usuario presiona "Limpiar filtros" (resetea ambos a "").
    onLimpiar: () -> Unit
) {
    // AnimatedVisibility: solo compone el contenido cuando visible = true.
    //
    // Animación de ENTRADA (visible: false → true):
    // - expandVertically(): el panel crece desde altura 0 hasta su altura natural.
    //   (Como si se "desplegara" hacia abajo.)
    // - fadeIn(): el panel aparece de transparencia 0 a 1.
    // - El "+" combina ambas en paralelo (se expande Y aparece a la vez).
    //
    // Animación de SALIDA (visible: true → false):
    // - shrinkVertically(): el panel se encoge de su altura natural a 0.
    // - fadeOut(): el panel va de transparencia 1 a 0.
    //
    // ¿Por qué expand/shrink y no slideIn/slideOut?
    // Porque expand/shrink ANIMA LA ALTURA del contenido (el panel crece literalmente).
    // slideIn/slideOut movería el panel sin cambiar su altura (dejaría un espacio vacío).
    // Para un panel que "aparece y empuja" el contenido de abajo, expand/shrink es lo correcto.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // Column con fondo surfaceVariant (un tono ligeramente distinto al background)
        // para que el panel se distinga visualmente del resto de la pantalla.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)  // Fondo diferenciado
                .padding(16.dp)
        ) {
            // --- SECCIÓN: ESTADO ---
            // Título de la sección
            Text("Estado", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))

            // FlowRow: los chips se disponen en filas que se "envuelven" si no caben.
            // horizontalArrangement: 8dp de espacio entre chips en la misma fila.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Itera los 6 estados del flujo (Estados.todos):
                // "Identificada", "Contacto realizado", "Propuesta enviada",
                // "En espera de respuesta", "Confirmada", "Descartada"
                Estados.todos.forEach { estado ->
                    // FilterChip: chip de filtro de Material 3.
                    // selected: true si este estado es el actualmente seleccionado.
                    //   El chip muestra un check (✓) cuando está seleccionado.
                    // onClick: lógica de "toggle" (seleccionar/deseleccionar):
                    //   - Si el chip ya está seleccionado (estado == estadoSeleccionado) →
                    //     se deselecciona (onEstadoChange("")).
                    //   - Si no está seleccionado → se selecciona (onEstadoChange(estado)).
                    //   Esto permite "quitar el filtro" tocando el mismo chip.
                    FilterChip(
                        selected = estado == estadoSeleccionado,
                        // tocar el chip activo lo desmarca
                        onClick = { onEstadoChange(if (estado == estadoSeleccionado) "" else estado) },
                        label = { Text(estado) }
                    )
                }
            }

            // Espacio entre las dos secciones de filtros
            Spacer(Modifier.height(12.dp))

            // --- SECCIÓN: PRIORIDAD ---
            Text("Prioridad", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))

            // FlowRow para los chips de prioridad (3 opciones: Alta, Media, Baja)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Itera las prioridades (Estados.prioridades): "Alta", "Media", "Baja"
                Estados.prioridades.forEach { prioridad ->
                    // Mismo patrón de toggle que los estados.
                    FilterChip(
                        selected = prioridad == prioridadSeleccionada,
                        onClick = { onPrioridadChange(if (prioridad == prioridadSeleccionada) "" else prioridad) },
                        label = { Text(prioridad) }
                    )
                }
            }

            // --- BOTÓN: LIMPIAR FILTROS ---
            // Solo se muestra si hay al menos un filtro activo (estado o prioridad no vacío).
            // Si ambos están vacíos, no tiene sentido mostrar "Limpiar filtros".
            if (estadoSeleccionado.isNotEmpty() || prioridadSeleccionada.isNotEmpty()) {
                TextButton(onClick = onLimpiar) { Text("Limpiar filtros") }
            }
        }
    }
}