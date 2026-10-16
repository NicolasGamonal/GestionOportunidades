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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.theme.*

@Composable
fun StatusChip(estado: String) {
    // 1. Determinamos el color basado en el estado (sin usar hexadecimales directos aquí)
    val targetColor = when (estado) {
        "Identificada" -> EstadoIdentificada
        "Contacto realizado" -> EstadoContactoRealizado
        "Propuesta enviada" -> EstadoPropuestaEnviada
        "Confirmada" -> EstadoConfirmada
        "Descartada" -> EstadoDescartada
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    // 2. Animación de color solicitada en J1
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "ColorAnimation"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(animatedColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = estado,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}