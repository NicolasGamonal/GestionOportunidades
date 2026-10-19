package duoc.example.gestionoportunidades.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Archivo de componentes reutilizables para formularios (acciones y contenedores).
// Vive en ui/components/ porque se usa en 2+ pantallas:
// - FormularioOportunidadScreen (crear/editar oportunidad)
// - FormularioOrganizacionScreen (crear/editar organización)
// - FormularioContactoScreen (crear/editar contacto)
// - FormularioSeguimientoScreen (nueva gestión)
// - DetalleOportunidadScreen (botón eliminar)
//
// Diferencia con FormularioCampos.kt (el otro archivo en components/):
// - FormularioCampos.kt → los CAMPOS (text fields, dropdowns, date pickers)
// - FormularioContenedor.kt → la ESTRUCTURA del formulario (botones, snackbars, confirmaciones)

// Envoltorio común para todos los formularios de la app.
// Aporta 3 cosas que cada formulario necesitaría implementar por separado:
//
// 1. SNACKBAR DE ERROR: cuando el ViewModel emite un mensajeError (ej: "No se pudo guardar"),
//    se muestra un Snackbar en la parte inferior de la pantalla.
//    Después de mostrarlo, llama a onErrorMostrado() para que el ViewModel lo limpie
//    (evita que se muestre 2 veces si la composición se re-ejecuta).
//
// 2. BANNER DE ÉXITO ANIMADO: cuando guardadoExitoso = true, se muestra
//    MensajeConfirmacion (con AnimatedVisibility) en el centro de la pantalla.
//    El usuario lo ve 1.2 segundos antes de ser redirigido.
//
// 3. RETORNO AUTOMÁTICO: después de mostrar la confirmación 1.2 segundos,
//    llama a onGuardado() que navega de vuelta a la lista (o al detalle).
//
// ¿Por qué un contenedor y no hacerlo en cada formulario?
// Porque el patrón es idéntico en los 4 formularios. Si cada uno lo implementara,
// habría 4 copias del mismo LaunchedEffect + SnackbarHost + MensajeConfirmacion.
//
// Parámetros:
// - guardadoExitoso: true cuando el ViewModel confirma que el guardado fue exitoso.
//   Dispara el banner de éxito y el retorno automático.
// - mensajeExito: texto a mostrar en el banner (ej: "Oportunidad creada", "Contacto actualizado").
// - mensajeError: texto del error o null. Si no es null, se muestra en un Snackbar.
// - onErrorMostrado: callback para que el ViewModel limpie mensajeError (lo pone a null).
//   Sin esto, el LaunchedEffect se re-ejecutaría en cada composición y el Snackbar se repetiría.
// - onGuardado: callback de navegación (volver a la lista o al detalle).
// - content: el contenido del formulario (los campos, los botones). Se dibuja dentro del Box.
@Composable
fun FormularioContenedor(
    guardadoExitoso: Boolean,
    mensajeExito: String,
    mensajeError: String?,
    onErrorMostrado: () -> Unit,
    onGuardado: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    // Estado del Snackbar: controla cuándo se muestra y qué texto tiene.
    // remember: se crea una sola vez y sobrevive a recomposiciones.
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar el Snackbar cuando hay un error.
    // LaunchedEffect(mensajeError): se re-ejecuta solo cuando mensajeError CAMBIA.
    // Si es null → no se ejecuta (no hay error).
    // Si pasa de null a "No se pudo guardar" → muestra el Snackbar y limpia el error.
    //
    // ¿Por qué llamar a onErrorMostrado() después de showSnackbar?
    // Porque si no se limpia, el siguiente frame de composición vería el mismo valor
    // de mensajeError y el LaunchedEffect no se re-ejecutaría (el key no cambió).
    // Pero si el ViewModel lo limpia, el key cambia a null y está listo para el próximo error.
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            snackbarHostState.showSnackbar(it)   // Muestra el Snackbar (bloqueante: espera a que termine)
            onErrorMostrado()                     // Limpia el error en el ViewModel
        }
    }

    // Retorno automático después del guardado exitoso.
    // LaunchedEffect(guardadoExitoso): se re-ejecuta cuando guardadoExitoso cambia.
    // Si pasa de false a true → espera 1.2s (para que el usuario vea la confirmación)
    // y luego navega de vuelta.
    //
    // ¿Por qué delay(1200) y no inmediato?
    // Porque el usuario necesita VER el mensaje "Oportunidad creada" antes de ser
    // redirigido. 1.2 segundos es el tiempo mínimo perceptible para leer un mensaje corto.
    LaunchedEffect(guardadoExitoso) {
        if (guardadoExitoso) {
            delay(1200)          // deja ver la confirmación antes de volver
            onGuardado()         // Navega de vuelta (la llama el ViewModel o la Screen)
        }
    }

    // Box: contenedor relativo que permite superponer elementos.
    // El formulario (content) ocupa todo el espacio.
    // El MensajeConfirmacion y el SnackbarHost se superponen encima.
    Box(modifier = Modifier.fillMaxSize()) {
        // El contenido del formulario (campos + botones)
        content()
        // Banner de éxito animado (AnimatedVisibility interno en MensajeConfirmacion).
        // Solo se ve cuando guardadoExitoso = true.
        MensajeConfirmacion(visible = guardadoExitoso, mensaje = mensajeExito)
        // Snackbar en la parte inferior centro de la pantalla.
        // Solo se ve cuando showSnackbar() se llama (cuando hay error).
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

// Botones de guardar y cancelar para los formularios.
// Se usa en todos los formularios (oportunidad, organización, contacto, seguimiento).
//
// Parámetros:
// - guardando: true mientras el ViewModel está escribiendo en Room (spinner en el botón).
// - guardadoExitoso: true después del guardado (se deshabilita para evitar dobles submits).
// - textoGuardar: texto del botón ("Guardar oportunidad", "Crear contacto", etc.)
//   Cambia según el contexto: "Crear" vs "Actualizar".
// - onGuardar: acción al presionar (llama al ViewModel).
// - onCancelar: acción al presionar cancelar (navega de vuelta sin guardar).
@Composable
fun BotonesFormulario(
    guardando: Boolean,
    guardadoExitoso: Boolean,
    textoGuardar: String,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    // Botón principal de guardar.
    // enabled = !guardando && !guardadoExitoso:
    // - Si está guardando → deshabilitado (evita dobles submits).
    // - Si ya se guardó → deshabilitado (el retorno automático ya está en marcha).
    Button(
        onClick = onGuardar,
        enabled = !guardando && !guardadoExitoso,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Si está guardando: spinner + (sin texto, el spinner es suficiente)
        // Si no: texto del botón ("Guardar oportunidad")
        if (guardando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(textoGuardar)
        }
    }
    // Botón secundario de cancelar (Outline: menos prominente que el de guardar).
    // Navega de vuelta sin guardar nada.
    OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
        Text("Cancelar")
    }
}

// Pantalla simple con un mensaje y un botón de volver.
// Se usa cuando una acción no se puede completar y hay que informar al usuario
// (ej: "No se puede eliminar esta organización porque tiene oportunidades asociadas").
//
// ¿Por qué no usar un Snackbar para esto?
// Porque el mensaje es largo (2-3 líneas) y el Snackbar se auto-cierra.
// Una pantalla dedicada da tiempo al usuario de leer y entender el motivo.
//
// Parámetros:
// - texto: el mensaje a mostrar (ej: "Tiene 3 oportunidades asociadas; no se puede eliminar.").
// - onVolver: acción al presionar "Volver" (regresa a la pantalla anterior).
@Composable
fun PantallaMensaje(texto: String, onVolver: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,      // Centrar verticalmente
        horizontalAlignment = Alignment.CenterHorizontally  // Centrar horizontalmente
    ) {
        // El mensaje (puede ser largo: 2-3 líneas)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        // Botón para volver a la pantalla anterior
        OutlinedButton(onClick = onVolver) { Text("Volver") }
    }
}

// Botón de acción destructiva (eliminar) con confirmación obligatoria.
// Se usa en: detalle de oportunidad, detalle de organización, detalle de contacto.
//
// ¿Por qué exigir confirmación?
// Porque eliminar es irreversible (o casi: el repositorio impide borrar si hay historial,
// pero si se puede borrar, no hay "deshacer"). El AlertDialog obliga al usuario
// a confirmar que realmente quiere eliminar.
//
// Cumple el checklist: "Mostrar confirmaciones de éxito y mensajes claros ante errores de operación."
//
// Parámetros:
// - texto: texto del botón ("Eliminar oportunidad", "Eliminar contacto").
// - titulo: título del AlertDialog ("¿Eliminar?").
// - mensaje: explicación del AlertDialog ("Esta acción no se puede deshacer.").
// - habilitado: si es false, el botón aparece gris y no se puede presionar.
//   Se usa cuando la oportunidad no se puede eliminar (ej: tiene historial).
// - onConfirmar: acción al confirmar (llama al ViewModel que intenta eliminar).
//   Si el repositorio devuelve un error (String?), el ViewModel lo muestra
//   en un PantallaMensaje o Snackbar.
@Composable
fun BotonEliminar(
    texto: String,
    titulo: String,
    mensaje: String,
    habilitado: Boolean,
    onConfirmar: () -> Unit
) {
    // Estado local: si el AlertDialog de confirmación está abierto.
    // No va en el UiState del ViewModel porque es un detalle visual efímero.
    var confirmando by remember { mutableStateOf(false) }

    // Botón de eliminar: estilo Outline con color de error (rojo).
    // Icono de basura + texto. Si habilitado = false, aparece gris y no responde.
    OutlinedButton(
        onClick = { confirmando = true },  // Al presionar: abre el diálogo de confirmación
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Default.Delete, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(texto)
    }

    // Diálogo de confirmación: solo se compone cuando confirmando = true.
    // Si el usuario cierra el diálogo (toca fuera o presiona "Cancelar"), confirmando = false.
    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },  // Tocar fuera → cerrar sin eliminar
            title = { Text(titulo) },                     // "¿Eliminar?"
            text = { Text(mensaje) },                     // "Esta acción no se puede deshacer."
            confirmButton = {
                // Botón "Eliminar" en rojo (color de error del tema).
                // Al presionar: cierra el diálogo Y ejecuta la acción de eliminar.
                TextButton(
                    onClick = {
                        confirmando = false   // Cierra el diálogo
                        onConfirmar()         // Llama al ViewModel para eliminar
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                // Botón "Cancelar": solo cierra el diálogo, no elimina nada.
                TextButton(onClick = { confirmando = false }) { Text("Cancelar") }
            }
        )
    }
}   