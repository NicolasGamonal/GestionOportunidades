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
import duoc.example.gestionoportunidades.ui.components.BotonEliminar
import duoc.example.gestionoportunidades.ui.components.CampoDesplegable
import duoc.example.gestionoportunidades.ui.components.CampoFecha
import duoc.example.gestionoportunidades.ui.components.CampoFormulario
import duoc.example.gestionoportunidades.ui.components.FormularioContenedor
import duoc.example.gestionoportunidades.ui.components.MensajeConfirmacion
import duoc.example.gestionoportunidades.ui.components.PantallaMensaje
import duoc.example.gestionoportunidades.util.Catalogos
import duoc.example.gestionoportunidades.util.Estados
import kotlinx.coroutines.delay

@Composable
fun FormularioOportunidadScreen(
    viewModel: FormularioOportunidadViewModel,
    onGuardado: () -> Unit,
    onEliminado: () -> Unit,
    onCancelar: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    FormularioContenedor(
        guardadoExitoso = ui.guardadoExitoso || ui.eliminado,
        mensajeExito = when {
            ui.eliminado -> "Oportunidad eliminada"
            ui.esEdicion -> "Oportunidad actualizada"
            else -> "Oportunidad creada"
        },
        mensajeError = ui.mensajeError,
        onErrorMostrado = viewModel::limpiarMensajeError,
        onGuardado = if (ui.eliminado) onEliminado else onGuardado
    ) {
        when {
            ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ui.sinAcceso -> PantallaMensaje("No tienes permiso para editar esta oportunidad", onCancelar)
            else -> ContenidoFormulario(ui, viewModel, onCancelar)
        }
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
            enabled = !ui.guardando && !ui.guardadoExitoso && !ui.eliminado,
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
        if (ui.esEdicion && ui.puedeEliminar) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            BotonEliminar(
                texto = "Eliminar oportunidad",
                titulo = "¿Eliminar oportunidad?",
                mensaje = "Se eliminará «${ui.nombre}» de forma permanente. Esta acción no se puede deshacer.",
                habilitado = !ui.guardando && !ui.guardadoExitoso && !ui.eliminado,
                onConfirmar = viewModel::eliminar
            )
        }
    }
}