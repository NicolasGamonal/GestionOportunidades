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
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.theme.colores

// MensajeConfirmacion: banner animado de éxito que se muestra al guardar un formulario.
//
// ¿Dónde se usa?
// - FormularioContenedor (ui/components/): lo superpone en el centro de la pantalla
//   cuando guardadoExitoso = true. El usuario lo ve 1.2 segundos antes del retorno automático.
// - También podría usarse en cualquier pantalla que necesite una confirmación visual.
//
// ¿Por qué AnimatedVisibility y no un if?
// Porque AnimatedVisibility maneja la animación de entrada Y salida automáticamente.
// Con un if simple, el banner aparecería y desaparecería de golpe (sin transición).
//
// Animaciones (checklist: "al menos 3 interacciones animadas con 2 mecanismos"):
// Este componente usa 2 mecanismos simultáneos:
// 1. slideInVertically / slideOutVertically → el banner desliza desde arriba (offset -40dp)
// 2. fadeIn / fadeOut → el banner aparece/desaparece con transparencia
//
// ¿Por qué desde arriba (-40)?
// Porque el banner se ubica en la parte superior de la pantalla (debajo del TopAppBar).
// Deslizar desde arriba da la sensación de "caer" sobre el contenido, como un toast.
// Si se deslizará desde abajo (+40), parecería que sube desde el Snackbar (confuso).
//
// Colores:
// - MaterialTheme.colores.exito → color de fondo (verde semántico, definido en ui/theme/Color.kt)
// - MaterialTheme.colores.sobreExito → color del texto (blanco o negro, según contraste)
// No se usan hexadecimales sueltos (checklist: "Evitar colores hexadecimales repetidos").

@Composable
fun MensajeConfirmacion(visible: Boolean, mensaje: String) {
    // AnimatedVisibility: solo compone el contenido cuando visible = true.
    // - enter: animación de APARICIÓN (cuando visible pasa de false a true).
    //   slideInVertically(initialOffsetY = { -40 }): empieza 40dp por encima de su posición final.
    //   fadeIn(): empieza con transparencia 0 y va a 1.
    //   El "+" combina ambas animaciones en paralelo (desliza Y aparece a la vez).
    // - exit: animación de DESAPARICIÓN (cuando visible pasa de true a false).
    //   slideOutVertically(targetOffsetY = { -40 }): se va 40dp hacia arriba.
    //   fadeOut(): va de transparencia 1 a 0.
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -40 }) + fadeOut()
    ) {
        // Box: contenedor del banner.
        // fillMaxWidth(): ocupa todo el ancho de la pantalla.
        // padding(16.dp) externo: margen entre el banner y los bordes de la pantalla.
        // clip(RoundedCornerShape(8.dp)): esquinas redondeadas (8dp).
        //   Sin clip, el background rectangular se vería "fuera" de las esquinas.
        // background(MaterialTheme.colores.exito): fondo verde semántico del tema.
        // padding(16.dp) interno: espacio entre el borde del banner y el texto.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colores.exito)
                .padding(16.dp),
            contentAlignment = Alignment.Center  // Centra el texto horizontal y verticalmente
        ) {
            // El mensaje de confirmación (ej: "Oportunidad creada", "Contacto actualizado").
            // Color del texto: sobreExito (contraste sobre el fondo verde).
            // Tipografía: bodyLarge (legible sin ser excesivamente grande).
            Text(
                text = mensaje,
                color = MaterialTheme.colores.sobreExito,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}