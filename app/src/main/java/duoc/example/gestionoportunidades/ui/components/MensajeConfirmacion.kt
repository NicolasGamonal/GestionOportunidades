package duoc.example.gestionoportunidades.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.theme.Success // El color semántico que creamos

@Composable
fun MensajeConfirmacion(visible: Boolean, mensaje: String) {
    // Segundo AnimatedVisibility requerido por la tarea J1
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -40 }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Success)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mensaje,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}