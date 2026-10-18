package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.ui.components.*
import duoc.example.gestionoportunidades.util.Catalogos

@Composable
fun FormularioSeguimientoScreen(
    viewModel: FormularioSeguimientoViewModel,
    onGuardado: () -> Unit,
    onCancelar: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    FormularioContenedor(
        guardadoExitoso = ui.guardadoExitoso,
        mensajeExito = "Gestión registrada",
        mensajeError = ui.mensajeError,
        onErrorMostrado = viewModel::limpiarMensajeError,
        onGuardado = onGuardado
    ) {
        when {
            ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ui.sinAcceso -> PantallaMensaje("No tienes permiso para registrar gestiones aquí", onCancelar)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Oportunidad: ${ui.nombreOportunidad}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CampoDesplegable(
                    valor = ui.tipoAccion,
                    etiqueta = "Tipo de acción",
                    opciones = Catalogos.tiposAccion,
                    textoDe = { it },
                    error = ui.errores[CampoSeguimiento.TIPO],
                    onSeleccion = viewModel::onTipoChange
                )
                CampoFormulario(
                    valor = ui.resultado,
                    onValorChange = viewModel::onResultadoChange,
                    etiqueta = "Resultado de la gestión",
                    error = ui.errores[CampoSeguimiento.RESULTADO],
                    singleLine = false,
                    minLines = 3
                )
                CampoFormulario(
                    valor = ui.proximaAccion,
                    onValorChange = viewModel::onProximaAccionChange,
                    etiqueta = "Próxima acción (ej: enviar propuesta)",
                    error = ui.errores[CampoSeguimiento.PROXIMA_ACCION]
                )
                CampoFecha(
                    valor = ui.fechaProximo,
                    etiqueta = "Fecha del próximo seguimiento",
                    error = ui.errores[CampoSeguimiento.FECHA_PROXIMA],
                    onFechaElegida = viewModel::onFechaProximaChange
                )
                Spacer(Modifier.height(8.dp))
                BotonesFormulario(
                    guardando = ui.guardando,
                    guardadoExitoso = ui.guardadoExitoso,
                    textoGuardar = "Guardar gestión",
                    onGuardar = viewModel::guardar,
                    onCancelar = onCancelar
                )
            }
        }
    }
}