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
import duoc.example.gestionoportunidades.model.Contacto

// Formulario de registrar una nueva gestión (llamada, email, reunión).
// Se llega desde HistorialSeguimientoScreen (botón "Nueva gestión").
// Muestra el nombre de la oportunidad como contexto.
// El contacto es opcional (puede ser "Sin contacto").
@Composable
fun FormularioSeguimientoScreen(
    viewModel: FormularioSeguimientoViewModel,
    onGuardado: () -> Unit,    // Volver al historial
    onCancelar: () -> Unit     // Volver sin guardar
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
            // El ejecutivo solo registra gestiones en oportunidades que le pertenecen
            ui.sinAcceso -> PantallaMensaje("No tienes permiso para registrar gestiones aquí", onCancelar)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Contexto: a qué oportunidad se asocia la gestión
                Text(
                    text = "Oportunidad: ${ui.nombreOportunidad}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Tipo de acción (dropdown: Llamada, Email, Reunión, etc.)
                CampoDesplegable(
                    valor = ui.tipoAccion,
                    etiqueta = "Tipo de acción",
                    opciones = Catalogos.tiposAccion,
                    textoDe = { it },
                    error = ui.errores[CampoSeguimiento.TIPO],
                    onSeleccion = viewModel::onTipoChange
                )

                // Contacto (opcional): dropdown con "Sin contacto" + los contactos de la org
                // Se usa listOf<Contacto?>(null) + ui.contactos para incluir la opción null
                CampoDesplegable(
                    valor = ui.contactos.firstOrNull { it.idContacto == ui.idContacto }
                        ?.let { "${it.nombre} · ${it.cargo}" } ?: "",
                    etiqueta = "Contacto (opcional)",
                    opciones = listOf<Contacto?>(null) + ui.contactos,
                    textoDe = { contacto -> contacto?.let { "${it.nombre} · ${it.cargo}" } ?: "Sin contacto" },
                    error = null,   // No tiene validación (es opcional)
                    onSeleccion = { contacto -> viewModel.onContactoChange(contacto?.idContacto) }
                )
                // Si la org no tiene contactos, se muestra un aviso
                if (ui.contactos.isEmpty()) {
                    Text(
                        "Esta organización aún no tiene contactos. Puedes agregarlos desde Organizaciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Resultado de la gestión (multilinea, obligatorio)
                CampoFormulario(
                    valor = ui.resultado,
                    onValorChange = viewModel::onResultadoChange,
                    etiqueta = "Resultado de la gestión",
                    error = ui.errores[CampoSeguimiento.RESULTADO],
                    singleLine = false,
                    minLines = 3
                )

                // Próxima acción (obligatorio)
                CampoFormulario(
                    valor = ui.proximaAccion,
                    onValorChange = viewModel::onProximaAccionChange,
                    etiqueta = "Próxima acción (ej: enviar propuesta)",
                    error = ui.errores[CampoSeguimiento.PROXIMA_ACCION]
                )

                // Fecha del próximo seguimiento (obligatorio, debe ser >= fecha actual)
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