package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.ui.components.*
import duoc.example.gestionoportunidades.util.Catalogos

// Formulario de crear/editar un contacto.
// Se llega desde DetalleOrganizacionScreen (botón "Agregar contacto" o editar un existente).
// Muestra el nombre de la organización como contexto (no es editable desde aquí).
@Composable
fun FormularioContactoScreen(
    viewModel: FormularioContactoViewModel,
    onGuardado: () -> Unit,    // Volver al detalle de la organización
    onEliminado: () -> Unit,   // Volver al detalle (después de eliminar)
    onCancelar: () -> Unit     // Volver sin guardar
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    // FormularioContenedor: snackbar de error + banner de éxito + retorno automático
    FormularioContenedor(
        guardadoExitoso = ui.guardadoExitoso || ui.eliminado,
        mensajeExito = when {
            ui.eliminado -> "Contacto eliminado"
            ui.esEdicion -> "Contacto actualizado"
            else -> "Contacto registrado"
        },
        mensajeError = ui.mensajeError,
        onErrorMostrado = viewModel::limpiarMensajeError,
        onGuardado = if (ui.eliminado) onEliminado else onGuardado
    ) {
        when {
            // Cargando datos (solo en modo edición)
            ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            // Contacto no encontrado (se eliminó mientras se tenía abierto)
            ui.noEncontrado -> PantallaMensaje("No se encontró el contacto", onCancelar)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Contexto: muestra a qué organización pertenece el contacto
                if (ui.nombreOrganizacion.isNotEmpty()) {
                    Text(
                        text = "Organización: ${ui.nombreOrganizacion}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Nombre del contacto (obligatorio)
                CampoFormulario(
                    valor = ui.nombre,
                    onValorChange = viewModel::onNombreChange,
                    etiqueta = "Nombre del contacto",
                    error = ui.errores[CampoContacto.NOMBRE]
                )

                // Cargo o función (obligatorio)
                CampoFormulario(
                    valor = ui.cargo,
                    onValorChange = viewModel::onCargoChange,
                    etiqueta = "Cargo o función",
                    error = ui.errores[CampoContacto.CARGO]
                )

                // Área (dropdown con catálogo fijo: RRHH, Logística, Comunicaciones, etc.)
                CampoDesplegable(
                    valor = ui.area,
                    etiqueta = "Área",
                    opciones = Catalogos.areasContacto,
                    textoDe = { it },
                    error = ui.errores[CampoContacto.AREA],
                    onSeleccion = viewModel::onAreaChange
                )

                // Correo (valida formato de email con Validaciones.kt)
                CampoFormulario(
                    valor = ui.correo,
                    onValorChange = viewModel::onCorreoChange,
                    etiqueta = "Correo (ficticio)",
                    error = ui.errores[CampoContacto.CORREO],
                    keyboardType = KeyboardType.Email
                )

                // Teléfono (valida formato con Validaciones.kt)
                CampoFormulario(
                    valor = ui.telefono,
                    onValorChange = viewModel::onTelefonoChange,
                    etiqueta = "Teléfono (ficticio)",
                    error = ui.errores[CampoContacto.TELEFONO],
                    keyboardType = KeyboardType.Phone
                )

                // Botones: guardar (con spinner) + cancelar
                Spacer(Modifier.height(8.dp))
                BotonesFormulario(
                    guardando = ui.guardando,
                    guardadoExitoso = ui.guardadoExitoso || ui.eliminado,
                    textoGuardar = if (ui.esEdicion) "Guardar cambios" else "Registrar contacto",
                    onGuardar = viewModel::guardar,
                    onCancelar = onCancelar
                )

                // Eliminar: solo en modo edición
                if (ui.esEdicion) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    BotonEliminar(
                        texto = "Eliminar contacto",
                        titulo = "¿Eliminar contacto?",
                        mensaje = "Se eliminará a ${ui.nombre} de forma permanente. Esta acción no se puede deshacer.",
                        habilitado = !ui.guardando && !ui.guardadoExitoso && !ui.eliminado,
                        onConfirmar = viewModel::eliminar
                    )
                }
            }
        }
    }
}