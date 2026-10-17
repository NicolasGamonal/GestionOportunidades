package duoc.example.gestionoportunidades.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.theme.*

@Composable
fun StatusChip(estado: String) {
    // Par (fondo, texto) por estado; los valores salen de ui/theme/Color.kt
    val (fondo, texto) = when (estado) {
        "Identificada" -> EstadoIdentificada to TextoSobreEstado
        "Contacto realizado" -> EstadoContactoRealizado to TextoSobreEstado
        "Propuesta enviada" -> EstadoPropuestaEnviada to TextoSobrePropuestaEnviada
        "En espera de respuesta" -> EstadoEnEspera to TextoSobreEstado
        "Confirmada" -> EstadoConfirmada to TextoSobreEstado
        "Descartada" -> EstadoDescartada to TextoSobreEstado
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Animación de color (J1): se anima el fondo y el texto
    val fondoAnimado by animateColorAsState(
        targetValue = fondo,
        animationSpec = tween(durationMillis = 500),
        label = "ColorFondoEstado"
    )
    val textoAnimado by animateColorAsState(
        targetValue = texto,
        animationSpec = tween(durationMillis = 500),
        label = "ColorTextoEstado"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(fondoAnimado)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = estado,
            color = textoAnimado,
            style = MaterialTheme.typography.labelMedium
        )
    }
}