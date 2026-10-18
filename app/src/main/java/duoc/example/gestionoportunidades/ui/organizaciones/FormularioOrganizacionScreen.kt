package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.ui.components.*
import duoc.example.gestionoportunidades.util.Catalogos

@Composable
fun FormularioOrganizacionScreen(
    viewModel: FormularioOrganizacionViewModel,
    onGuardado: () -> Unit,
    onEliminado: () -> Unit,
    onCancelar: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    FormularioContenedor(
        guardadoExitoso = ui.guardadoExitoso || ui.eliminado,
        mensajeExito = when {
            ui.eliminado -> "Organización eliminada"
            ui.esEdicion -> "Organización actualizada"
            else -> "Organización creada"
        },
        mensajeError = ui.mensajeError,
        onErrorMostrado = viewModel::limpiarMensajeError,
        onGuardado = if (ui.eliminado) onEliminado else onGuardado
    ) {
        when {
            ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ui.noEncontrada -> PantallaMensaje("No se encontró la organización", onCancelar)
            ui.sinPermiso -> PantallaMensaje("Solo el supervisor puede editar organizaciones", onCancelar)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CampoFormulario(
                    valor = ui.nombre,
                    onValorChange = viewModel::onNombreChange,
                    etiqueta = "Nombre de la organización",
                    error = ui.errores[CampoOrganizacion.NOMBRE]
                )
                CampoDesplegable(
                    valor = ui.tipo,
                    etiqueta = "Tipo de organización",
                    opciones = Catalogos.tiposOrganizacion,
                    textoDe = { it },
                    error = ui.errores[CampoOrganizacion.TIPO],
                    onSeleccion = viewModel::onTipoChange
                )
                CampoFormulario(
                    valor = ui.rubro,
                    onValorChange = viewModel::onRubroChange,
                    etiqueta = "Rubro",
                    error = ui.errores[CampoOrganizacion.RUBRO]
                )
                CampoFormulario(
                    valor = ui.comuna,
                    onValorChange = viewModel::onComunaChange,
                    etiqueta = "Comuna",
                    error = ui.errores[CampoOrganizacion.COMUNA]
                )
                Spacer(Modifier.height(8.dp))
                BotonesFormulario(
                    guardando = ui.guardando,
                    guardadoExitoso = ui.guardadoExitoso,
                    textoGuardar = if (ui.esEdicion) "Guardar cambios" else "Crear organización",
                    onGuardar = viewModel::guardar,
                    onCancelar = onCancelar
                )
                if (ui.esEdicion && ui.puedeEliminar) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    BotonEliminar(
                        texto = "Eliminar organización",
                        titulo = "¿Eliminar organización?",
                        mensaje = "Se eliminará ${ui.nombre} y todos sus contactos de forma permanente. " +
                                "Solo es posible si no tiene oportunidades asociadas.",
                        habilitado = !ui.guardando && !ui.guardadoExitoso && !ui.eliminado,
                        onConfirmar = viewModel::eliminar
                    )
                }
            }
        }
    }
}