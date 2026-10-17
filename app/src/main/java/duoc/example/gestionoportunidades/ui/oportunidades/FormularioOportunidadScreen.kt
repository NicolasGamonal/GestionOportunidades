package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.ui.components.CampoDesplegable
import duoc.example.gestionoportunidades.ui.components.CampoFecha
import duoc.example.gestionoportunidades.ui.components.CampoFormulario
import duoc.example.gestionoportunidades.ui.components.MensajeConfirmacion
import duoc.example.gestionoportunidades.util.Catalogos
import duoc.example.gestionoportunidades.util.Estados
import kotlinx.coroutines.delay

@Composable
fun FormularioOportunidadScreen(
    viewModel: FormularioOportunidadViewModel,
    onGuardado: () -> Unit,
    onCancelar: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.mensajeError) {
        ui.mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajeError()
        }
    }
    LaunchedEffect(ui.guardadoExitoso) {
        if (ui.guardadoExitoso) {
            delay(1200)          // deja ver la confirmación antes de volver
            onGuardado()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ui.sinAcceso -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No tienes permiso para editar esta oportunidad", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onCancelar) { Text("Volver") }
            }
            else -> ContenidoFormulario(ui, viewModel, onCancelar)
        }

        // Por encima del contenido
        MensajeConfirmacion(
            visible = ui.guardadoExitoso,
            mensaje = if (ui.esEdicion) "Oportunidad actualizada" else "Oportunidad creada"
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ContenidoFormulario(
    ui: FormularioUiState,
    viewModel: FormularioOportunidadViewModel,
    onCancelar: () -> Unit
) {
    val errores = ui.errores

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CampoFormulario(
            valor = ui.nombre,
            onValorChange = viewModel::onNombreChange,
            etiqueta = "Nombre de la oportunidad",
            error = errores[CampoOportunidad.NOMBRE]
        )

        CampoDesplegable(
            valor = ui.tipoEvento,
            etiqueta = "Tipo de evento",
            opciones = Catalogos.tiposEvento,
            textoDe = { it },
            error = errores[CampoOportunidad.TIPO],
            onSeleccion = viewModel::onTipoChange
        )

        CampoDesplegable(
            valor = ui.organizaciones.firstOrNull { it.idOrganizacion == ui.idOrganizacion }?.nombre ?: "",
            etiqueta = "Organización",
            opciones = ui.organizaciones,
            textoDe = { it.nombre },
            error = errores[CampoOportunidad.ORGANIZACION],
            onSeleccion = { viewModel.onOrganizacionChange(it.idOrganizacion) }
        )
        if (ui.organizaciones.isEmpty()) {
            Text(
                "No hay organizaciones registradas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        CampoFecha(
            valor = ui.fechaDeteccion,
            etiqueta = "Fecha de detección",
            error = errores[CampoOportunidad.DETECCION],
            onFechaElegida = viewModel::onDeteccionChange
        )
        CampoFecha(
            valor = ui.fechaInicio,
            etiqueta = "Fecha de inicio del evento",
            error = errores[CampoOportunidad.INICIO],
            onFechaElegida = viewModel::onInicioChange
        )
        CampoFecha(
            valor = ui.fechaTermino,
            etiqueta = "Fecha de término del evento",
            error = errores[CampoOportunidad.TERMINO],
            onFechaElegida = viewModel::onTerminoChange
        )

        CampoFormulario(
            valor = ui.comuna,
            onValorChange = viewModel::onComunaChange,
            etiqueta = "Comuna",
            error = errores[CampoOportunidad.COMUNA]
        )
        CampoFormulario(
            valor = ui.publicoEsperado,
            onValorChange = viewModel::onPublicoChange,
            etiqueta = "Público esperado",
            error = errores[CampoOportunidad.PUBLICO],
            keyboardType = KeyboardType.Number
        )

        Text("Prioridad", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Estados.prioridades.forEach { prioridad ->
                FilterChip(
                    selected = ui.prioridad == prioridad,
                    onClick = { viewModel.onPrioridadChange(prioridad) },
                    label = { Text(prioridad) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        CampoFormulario(
            valor = ui.costoReferencial,
            onValorChange = viewModel::onCostoChange,
            etiqueta = "Costo referencial (CLP)",
            error = errores[CampoOportunidad.COSTO],
            keyboardType = KeyboardType.Number
        )
        CampoFormulario(
            valor = ui.observaciones,
            onValorChange = viewModel::onObservacionesChange,
            etiqueta = "Observaciones",
            error = errores[CampoOportunidad.OBSERVACIONES],
            singleLine = false,
            minLines = 3
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::guardar,
            enabled = !ui.guardando && !ui.guardadoExitoso,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ui.guardando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (ui.esEdicion) "Guardar cambios" else "Crear oportunidad")
            }
        }
        OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }
    }
}