package duoc.example.gestionoportunidades.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.util.comoFechaHora

// TarjetaOportunidad: tarjeta reutilizable que muestra una oportunidad en la lista.
//
// ¿Dónde se usa?
// - OportunidadesScreen: en el LazyColumn, una tarjeta por cada oportunidad.
// - También podría usarse en el panel del supervisor (consulta global).
//
// ¿Por qué está en ui/components/ y no en ui/oportunidades/?
// Porque se usa en 2+ pantallas (lista de oportunidades + panel del supervisor).
//
// ¿Qué muestra?
// - Nombre de la oportunidad (título)
// - Chip de estado (StatusChip) a la derecha del nombre
// - Prioridad + comuna (línea 2)
// - Fecha de inicio formateada (línea 3)
//
// ¿Qué NO muestra?
// - Costo referencial, público esperado, tipo de evento, observaciones.
//   Esos datos se ven en la pantalla de DETALLE (DetalleOportunidadScreen).
//   La tarjeta es un resumen: solo lo suficiente para identificar y decidir si se abre.
//
// ¿Por qué es clicable (onClick)?
// Porque al tocar la tarjeta, el usuario navega al detalle de esa oportunidad.
// El callback lo pasa la Screen (que tiene el NavController).

@Composable
fun TarjetaOportunidad(
    // La oportunidad a mostrar (viene del UiState del ViewModel).
    oportunidad: Oportunidad,
    // Acción al tocar la tarjeta (navegar al detalle).
    // La Screen lo implementa como: { onVerDetalle(oportunidad.idOportunidad) }
    onClick: () -> Unit
) {
    // Card con onClick: toda la tarjeta es clicable (no solo un botón).
    // elevation = 2.dp: sombra sutil para que se distinga del fondo (Material 3).
    // CardDefaults.cardElevation(): usa la elevación por defecto de Material 3
    // (se adapta al tema claro/oscuro automáticamente).
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        // Column: apila los elementos verticalmente (nombre, prioridad, fecha).
        // padding(16.dp): espacio interno para que el texto no toque los bordes de la tarjeta.
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Fila 1: nombre + chip de estado (uno a la izquierda, otro a la derecha).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,  // Empuja a los extremos
                verticalAlignment = Alignment.CenterVertically     // Centra verticalmente
            ) {
                // Nombre de la oportunidad (título principal).
                // weight(1f): ocupa todo el espacio disponible, dejando solo lo necesario
                // para el StatusChip a la derecha. Si el nombre es muy largo, se trunca
                // (Compose lo maneja automáticamente con weight).
                Text(
                    text = oportunidad.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // Chip de estado: muestra el estado actual con su color semántico.
                // StatusChip es otro componente de ui/components/ que usa
                // animateColorAsState (1 de los 3 mecanismos de animación del checklist).
                StatusChip(estado = oportunidad.estado)
            }

            // Espacio entre la fila del nombre y la línea de prioridad
            Spacer(modifier = Modifier.height(8.dp))

            // Fila 2: prioridad + comuna (información de contexto rápida).
            // onSurfaceVariant: color secundario (gris) para jerarquía visual
            // (el nombre es más importante que la prioridad).
            Text(
                text = "Prioridad ${oportunidad.prioridad} · ${oportunidad.comuna}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Fila 3: fecha de inicio formateada.
            // comoFechaHora(): función de extensión de Long en util/ que convierte
            // el timestamp a formato legible (ej: "19 Oct 2026, 10:00").
            // bodySmall: tamaño más pequeño que bodyMedium (jerarquía: fecha es lo menos importante).
            Text(
                text = "Inicio: ${oportunidad.fechaInicio.comoFechaHora()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}