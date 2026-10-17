package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.components.StatusChip
import duoc.example.gestionoportunidades.ui.nativo.BotonAgregarCalendario
import duoc.example.gestionoportunidades.ui.nativo.SeccionEvidencia
import duoc.example.gestionoportunidades.util.comoFechaHora
import duoc.example.gestionoportunidades.util.comoPesos
import duoc.example.gestionoportunidades.util.Estados

@Composable
fun DetalleOportunidadScreen(
    viewModel: DetalleOportunidadViewModel,
    onVolver: () -> Unit,
    onVerHistorial: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.mensajeExito, uiState.mensajeError) {
        val mensaje = uiState.mensajeExito ?: uiState.mensajeError
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensajes()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val oportunidad = uiState.oportunidad
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.sinAcceso ->
                MensajeCentrado("No tienes permiso para ver esta oportunidad", onVolver)
            oportunidad == null ->
                MensajeCentrado("No se encontró la oportunidad", onVolver)
            else -> DetalleContenido(
                oportunidad = oportunidad,
                organizacion = uiState.organizacion,
                responsable = uiState.responsable,
                estadosPermitidos = uiState.estadosPermitidos,
                onCambiarEstado = viewModel::cambiarEstado,
                onVolver = onVolver,
                onVerHistorial = onVerHistorial
            )
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MensajeCentrado(texto: String, onVolver: () -> Unit) {
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
@Composable
private fun DetalleContenido(
    oportunidad: Oportunidad,
    organizacion: String,
    responsable: String,
    estadosPermitidos: List<String>,
    onCambiarEstado: (String) -> Unit,
    onVolver: () -> Unit,
    onVerHistorial: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = oportunidad.nombre,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatusChip(estado = oportunidad.estado)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        DetalleItem("Organización", organizacion)
        DetalleItem("Responsable", responsable)
        DetalleItem("Tipo de evento", oportunidad.tipoEvento)
        DetalleItem("Comuna", oportunidad.comuna)
        DetalleItem("Público esperado", oportunidad.publicoEsperado.toString())
        DetalleItem("Prioridad", oportunidad.prioridad)
        DetalleItem("Costo referencial", oportunidad.costoReferencial.comoPesos())

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        DetalleItem("Detectada el", oportunidad.fechaDeteccion.comoFechaHora())
        DetalleItem("Inicio", oportunidad.fechaInicio.comoFechaHora())
        DetalleItem("Término", oportunidad.fechaTermino.comoFechaHora())

        BotonAgregarCalendario(
            titulo = "Evento: ${oportunidad.nombre}",
            descripcion = "Activación corporativa en ${oportunidad.comuna}. Tipo: ${oportunidad.tipoEvento}",
            inicioMillis = oportunidad.fechaInicio,
            finMillis = oportunidad.fechaTermino,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Observaciones:", fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SeccionCambioEstado(oportunidad.estado, estadosPermitidos, onCambiarEstado)
        Text(oportunidad.observaciones, style = MaterialTheme.typography.bodyLarge)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SeccionEvidencia()

        Button(
            onClick = onVerHistorial,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Ver historial de gestión")
        }

        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a la lista")
        }
    }
}

@Composable
fun DetalleItem(titulo: String, valor: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(text = titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SeccionCambioEstado(
    estadoActual: String,
    estadosPermitidos: List<String>,
    onCambiarEstado: (String) -> Unit
) {
    var pendiente by remember { mutableStateOf<String?>(null) }

    Text("Cambiar estado", style = MaterialTheme.typography.titleMedium)

    if (estadosPermitidos.isEmpty()) {
        Text(
            text = if (estadoActual in Estados.finales) {
                "Este estado es final y no puede modificarse."
            } else {
                "Tu rol no puede avanzar este estado; el supervisor debe confirmarlo o descartarlo."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        estadosPermitidos.forEach { estado ->
            val esFinal = estado in Estados.finales
            val etiqueta = when (estado) {
                "Confirmada" -> "Confirmar participación"
                "Descartada" -> "Descartar oportunidad"
                else -> "Avanzar a: $estado"
            }
            val accion = { if (esFinal) pendiente = estado else onCambiarEstado(estado) }

            if (estado == "Descartada") {
                OutlinedButton(
                    onClick = accion,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(etiqueta) }
            } else {
                Button(onClick = accion, modifier = Modifier.fillMaxWidth()) { Text(etiqueta) }
            }
        }
    }

    pendiente?.let { estado ->
        AlertDialog(
            onDismissRequest = { pendiente = null },
            title = { Text("¿Confirmar el cambio?") },
            text = { Text("La oportunidad pasará a \"$estado\" y ya no podrá modificarse.") },
            confirmButton = {
                TextButton(onClick = {
                    onCambiarEstado(estado)
                    pendiente = null
                }) { Text("Sí, continuar") }
            },
            dismissButton = {
                TextButton(onClick = { pendiente = null }) { Text("Cancelar") }
            }
        )
    }
}