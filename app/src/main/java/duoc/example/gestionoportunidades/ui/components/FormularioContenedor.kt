package duoc.example.gestionoportunidades.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Envoltorio común: Snackbar de error, banner animado de éxito y retorno automático. */
@Composable
fun FormularioContenedor(
    guardadoExitoso: Boolean,
    mensajeExito: String,
    mensajeError: String?,
    onErrorMostrado: () -> Unit,
    onGuardado: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            onErrorMostrado()
        }
    }
    LaunchedEffect(guardadoExitoso) {
        if (guardadoExitoso) {
            delay(1200)          // deja ver la confirmación antes de volver
            onGuardado()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        MensajeConfirmacion(visible = guardadoExitoso, mensaje = mensajeExito)
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun BotonesFormulario(
    guardando: Boolean,
    guardadoExitoso: Boolean,
    textoGuardar: String,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    Button(
        onClick = onGuardar,
        enabled = !guardando && !guardadoExitoso,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (guardando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(textoGuardar)
        }
    }
    OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
        Text("Cancelar")
    }
}

@Composable
fun PantallaMensaje(texto: String, onVolver: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onVolver) { Text("Volver") }
    }
}