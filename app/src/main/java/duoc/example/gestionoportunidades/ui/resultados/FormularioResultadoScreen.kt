package duoc.example.gestionoportunidades.ui.resultados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.ui.components.BotonesFormulario
import duoc.example.gestionoportunidades.ui.components.CampoFormulario
import duoc.example.gestionoportunidades.ui.components.FormularioContenedor
import duoc.example.gestionoportunidades.ui.components.PantallaMensaje
import duoc.example.gestionoportunidades.ui.components.SelectorChips
import duoc.example.gestionoportunidades.util.CampoResultado
import duoc.example.gestionoportunidades.util.Catalogos

// Formulario para registrar (o editar) el resultado real de una activación (HU-24).
@Composable
fun FormularioResultadoScreen(
    viewModel: FormularioResultadoViewModel,
    onGuardado: () -> Unit,
    onCancelar: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    FormularioContenedor(
        guardadoExitoso = ui.guardadoExitoso,
        mensajeExito = if (ui.esEdicion) "Resultado actualizado" else "Resultado registrado",
        mensajeError = ui.mensajeError,
        onErrorMostrado = viewModel::limpiarMensajeError,
        onGuardado = onGuardado
    ) {
        when {
            ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            ui.sinAcceso -> PantallaMensaje(
                "Solo se puede registrar el resultado de oportunidades confirmadas que tengas permiso de ver",
                onCancelar
            )

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

                CampoFormulario(
                    valor = ui.asistentes,
                    onValorChange = viewModel::onAsistentesChange,
                    etiqueta = "Asistentes reales",
                    error = ui.errores[CampoResultado.ASISTENTES],
                    keyboardType = KeyboardType.Number
                )

                CampoFormulario(
                    valor = ui.contactosGenerados,
                    onValorChange = viewModel::onContactosChange,
                    etiqueta = "Contactos generados",
                    error = ui.errores[CampoResultado.CONTACTOS],
                    keyboardType = KeyboardType.Number
                )

                CampoFormulario(
                    valor = ui.inscripciones,
                    onValorChange = viewModel::onInscripcionesChange,
                    etiqueta = "Inscripciones o convenios captados",
                    error = ui.errores[CampoResultado.INSCRIPCIONES],
                    keyboardType = KeyboardType.Number
                )

                // La evaluación se elige con chips del 1 al 5. SelectorChips trabaja con Strings,
                // así que se convierte de Int a String para mostrar y de vuelta a Int al elegir.
                SelectorChips(
                    titulo = "Evaluación de la activación (1 = mala, 5 = excelente)",
                    opciones = Catalogos.evaluaciones.map { it.toString() },
                    seleccionada = ui.evaluacion.toString(),
                    onSeleccion = { viewModel.onEvaluacionChange(it.toInt()) }
                )

                Spacer(Modifier.height(4.dp))

                CampoFormulario(
                    valor = ui.aprendizajes,
                    onValorChange = viewModel::onAprendizajesChange,
                    etiqueta = "Aprendizajes y observaciones",
                    error = ui.errores[CampoResultado.APRENDIZAJES],
                    singleLine = false,
                    minLines = 4
                )

                Spacer(Modifier.height(8.dp))
                BotonesFormulario(
                    guardando = ui.guardando,
                    guardadoExitoso = ui.guardadoExitoso,
                    textoGuardar = if (ui.esEdicion) "Guardar cambios" else "Registrar resultado",
                    onGuardar = viewModel::guardar,
                    onCancelar = onCancelar
                )
            }
        }
    }
}
