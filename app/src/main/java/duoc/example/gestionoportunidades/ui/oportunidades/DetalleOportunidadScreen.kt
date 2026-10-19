package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.components.StatusChip
import duoc.example.gestionoportunidades.ui.nativo.BotonAgregarCalendario
import duoc.example.gestionoportunidades.ui.nativo.SeccionEvidencia
import duoc.example.gestionoportunidades.util.Estados
import duoc.example.gestionoportunidades.util.comoFechaCorta
import duoc.example.gestionoportunidades.util.comoFechaHora
import duoc.example.gestionoportunidades.util.comoPesos
import duoc.example.gestionoportunidades.model.Evidencia

// DetalleOportunidadScreen: pantalla de detalle completo de una oportunidad.
//
// ¿Dónde se llega?
// - Desde OportunidadesScreen (tocar una tarjeta en la lista).
// - Desde DashboardScreen (tocar una tarjeta de alerta).
// - Desde SupervisorScreen (tocar una oportunidad en la consulta global).
//
// ¿Qué muestra?
// - Nombre + chip de estado (cabecera)
// - Botón "Editar oportunidad"
// - Datos de la organización y responsable
// - Datos del evento (tipo, comuna, público, prioridad, costo)
// - Fechas (detección, inicio, término)
// - Botón "Agregar al calendario" (integración nativa)
// - Observaciones
// - Sección de cambio de estado (con permisos por rol)
// - Sección de evidencia fotográfica (cámara + galería)
// - Botón "Ver historial de gestión"
// - Botón "Volver a la lista"
//
// ¿Por qué es la pantalla más grande del proyecto?
// Porque es el "centro de operaciones" de una oportunidad: desde aquí se puede
// editar, cambiar estado, tomar fotos, agregar al calendario y ver el historial.
// Es la pantalla que el docente va a probar más en la demo.
//
// Estados que maneja (con when):
// 1. isLoading → CircularProgressIndicator (mientras consulta Room)
// 2. sinAcceso → mensaje de permiso (ejecutivo intentando ver oportunidad ajena)
// 3. oportunidad == null → "No se encontró" (id inválido o ya eliminada)
// 4. else → DetalleContenido (la vista completa)
//
// Patrón MVVM:
// - Observa el uiState del DetalleOportunidadViewModel con collectAsStateWithLifecycle().
// - No tiene lógica de negocio: los permisos de cambio de estado los calcula el ViewModel
//   (que usa Permisos.kt) y los expone como estadosPermitidos en el UiState.
// - Las acciones (cambiar estado, registrar foto, eliminar foto) se delegan al ViewModel.

@Composable
fun DetalleOportunidadScreen(
    // ViewModel inyectado desde Navegacion.kt (con AppViewModelProvider.Factory).
    // Contiene: oportunidad, organizacion, responsable, estadosPermitidos, evidencias,
    // mensajes de éxito/error, isLoading, sinAcceso.
    viewModel: DetalleOportunidadViewModel,
    // Navegar de vuelta a la lista (o al dashboard, según de dónde se llegó).
    onVolver: () -> Unit,
    // Navegar al historial de gestiones de esta oportunidad.
    onVerHistorial: (Long) -> Unit,
    // Navegar al formulario de edición de esta oportunidad.
    onEditar: (Long) -> Unit
) {
    // Observa el estado del ViewModel. Se actualiza cuando:
    // - Termina la carga inicial (isLoading → false)
    // - Se cambia el estado (oportunidad se actualiza vía Flow)
    // - Se registra/elimina una evidencia (evidencias se actualiza vía Flow)
    // - Hay un mensaje de éxito o error
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Estado del Snackbar: se usa para mostrar mensajes de éxito/error
    // (ej: "Estado cambiado a: Contacto realizado", "Evidencia eliminada").
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar el Snackbar cuando el ViewModel emite un mensaje.
    // LaunchedEffect se re-ejecuta cuando CUALQUIERA de los 2 mensajes cambia.
    // Si hay mensajeExito → lo muestra y lo limpia.
    // Si hay mensajeError → lo muestra y lo limpia.
    // Si ambos son null → no se ejecuta (no hay nada que mostrar).
    LaunchedEffect(uiState.mensajeExito, uiState.mensajeError) {
        val mensaje = uiState.mensajeExito ?: uiState.mensajeError
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensajes()  // Limpia para que no se repita en la próxima composición
        }
    }

    // Box: contenedor relativo que permite superponer el Snackbar sobre el contenido.
    Box(modifier = Modifier.fillMaxSize()) {
        val oportunidad = uiState.oportunidad

        // Estados posibles de la pantalla (en orden de prioridad):
        when {
            // 1. Cargando: se muestra un spinner centrado.
            //    El ViewModel consulta Room al abrir la pantalla (obtenerPorId + organizacion + responsable + evidencias).
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            // 2. Sin acceso: el ejecutivo intentó ver una oportunidad que no es suya.
            //    El ViewModel lo detecta con Permisos.puedeVerOportunidad() y pone sinAcceso = true.
            //    Se muestra un mensaje claro y un botón de volver.
            uiState.sinAcceso ->
                MensajeCentrado("No tienes permiso para ver esta oportunidad", onVolver)

            // 3. No encontrada: el id no existe en Room (puede que se haya eliminado).
            oportunidad == null ->
                MensajeCentrado("No se encontró la oportunidad", onVolver)

            // 4. Contenido normal: se muestra el detalle completo.
            else -> DetalleContenido(
                oportunidad = oportunidad,
                organizacion = uiState.organizacion,
                responsable = uiState.responsable,
                estadosPermitidos = uiState.estadosPermitidos,
                onCambiarEstado = viewModel::cambiarEstado,
                onEditar = { onEditar(oportunidad.idOportunidad) },
                onVolver = onVolver,
                onVerHistorial = { onVerHistorial(oportunidad.idOportunidad) },
                evidencias = uiState.evidencias,
                onFotoCapturada = viewModel::registrarEvidencia,
                onEliminarFoto = viewModel::eliminarEvidencia
            )
        }
        // Snackbar en la parte inferior: se muestra cuando hay mensaje de éxito/error.
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

// Mensaje centrado con botón de volver.
// Se usa en los 2 estados de error (sinAcceso y no encontrada).
// Es private: solo se usa dentro de este archivo.
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

// Contenido principal del detalle: todos los datos de la oportunidad organizados en secciones.
// Es private: solo se llama desde DetalleOportunidadScreen cuando hay datos que mostrar.
//
// Estructura visual (de arriba a abajo):
// 1. Cabecera: nombre + StatusChip
// 2. Botón "Editar"
// 3. Divider
// 4. Datos de organización/responsable/evento (DetalleItem × 7)
// 5. Divider
// 6. Fechas (DetalleItem × 3)
// 7. Botón "Agregar al calendario"
// 8. Divider
// 9. Observaciones
// 10. Divider
// 11. Sección de cambio de estado (SeccionCambioEstado)
// 12. Divider
// 13. Sección de evidencia fotográfica (SeccionEvidencia)
// 14. Botón "Ver historial de gestión"
// 15. Botón "Volver a la lista"
@Composable
private fun DetalleContenido(
    // La oportunidad a mostrar (viene del UiState, se actualiza en tiempo real vía Flow)
    oportunidad: Oportunidad,
    // Nombre de la organización (resuelto por el ViewModel desde el idOrganizacion FK)
    organizacion: String,
    // Nombre del responsable (resuelto por el ViewModel desde el idResponsable FK)
    responsable: String,
    // Estados a los que el usuario PUEDE avanzar (calculado por el ViewModel con Permisos.kt).
    // Ej: ejecutivo en "Identificada" → ["Contacto realizado"]
    //     supervisor en "En espera" → ["Confirmada", "Descartada"]
    //     ejecutivo en "Confirmada" → [] (estado final, no se puede avanzar)
    estadosPermitidos: List<String>,
    // Acción al cambiar de estado (llama al ViewModel que verifica permisos y actualiza Room)
    onCambiarEstado: (String) -> Unit,
    // Acción al presionar "Editar" (navega al formulario con el id)
    onEditar: () -> Unit,
    // Acción al presionar "Volver" (regresa a la lista)
    onVolver: () -> Unit,
    // Acción al presionar "Ver historial" (navega al historial de gestiones)
    onVerHistorial: () -> Unit,
    // Fotos asociadas a esta oportunidad (viene del UiState, se actualiza vía Flow)
    evidencias: List<Evidencia>,
    // Callback: se llama con la ruta cuando se captura una foto válida
    onFotoCapturada: (String) -> Unit,
    // Callback: se llama con la Evidencia a borrar
    onEliminarFoto: (Evidencia) -> Unit

) {
    // Column scrollable: el detalle puede ser más largo que la pantalla (especialmente
    // con muchas fotos o observaciones largas).
    // spacedBy(12.dp): espacio uniforme entre secciones.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- CABECERA: nombre + chip de estado ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Nombre de la oportunidad (título principal, color primario)
            Text(
                text = oportunidad.nombre,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)  // Ocupa el espacio disponible (empuja el chip a la derecha)
            )
            // Chip de estado con color semántico (y animación de color con animateColorAsState)
            StatusChip(estado = oportunidad.estado)
        }

        // --- BOTÓN: EDITAR ---
        // El ViewModel del formulario re-valida el permiso al abrir (doble seguridad).
        // Aquí solo se muestra el botón; la restricción real está en el ViewModel.
        OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Editar oportunidad")
        }

        // Divider: separa visualmente la cabecera de los datos
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- SECCIÓN: DATOS DE LA OPORTUNIDAD ---
        // Cada DetalleItem muestra un label (gris, pequeño) + valor (negro, normal).
        // La organización y el responsable son Strings resueltos por el ViewModel
        // (hace el JOIN con las tablas usuarios y organizaciones).
        DetalleItem("Organización", organizacion)
        DetalleItem("Responsable", responsable)
        DetalleItem("Tipo de evento", oportunidad.tipoEvento)
        DetalleItem("Comuna", oportunidad.comuna)
        DetalleItem("Público esperado", oportunidad.publicoEsperado.toString())
        DetalleItem("Prioridad", oportunidad.prioridad)
        // comoPesos(): convierte el Double a formato monetario chileno (ej: "$850.000")
        DetalleItem("Costo referencial", oportunidad.costoReferencial.comoPesos())

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- SECCIÓN: FECHAS ---
        // comoFechaCorta(): "19 Oct 2026" (solo fecha, para la detección)
        // comoFechaHora(): "19 Oct 2026, 10:00" (fecha + hora, para inicio y término)
        DetalleItem("Detectada el", oportunidad.fechaDeteccion.comoFechaCorta())
        DetalleItem("Inicio", oportunidad.fechaInicio.comoFechaHora())
        DetalleItem("Término", oportunidad.fechaTermino.comoFechaHora())

        // --- BOTÓN: AGREGAR AL CALENDARIO ---
        // Abre la app de calendario del sistema con las fechas del evento.
        // Si finMillis <= inicioMillis (evento de un día), el botón agrega 1 hora por defecto.
        // Si no hay app de calendario → Toast de error (manejado dentro del componente).
        BotonAgregarCalendario(
            titulo = "Evento: ${oportunidad.nombre}",
            descripcion = "Activación corporativa en ${oportunidad.comuna}. Tipo: ${oportunidad.tipoEvento}",
            inicioMillis = oportunidad.fechaInicio,
            finMillis = oportunidad.fechaTermino,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- SECCIÓN: OBSERVACIONES ---
        // Texto libre del ejecutivo (puede ser "" si no hay observaciones).
        Text("Observaciones", style = MaterialTheme.typography.titleSmall)
        Text(oportunidad.observaciones, style = MaterialTheme.typography.bodyLarge)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- SECCIÓN: CAMBIO DE ESTADO ---
        // Muestra los botones para avanzar el estado (según permisos del rol).
        // Si no hay estados permitidos, muestra un mensaje explicativo.
        SeccionCambioEstado(oportunidad.estado, estadosPermitidos, onCambiarEstado)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- SECCIÓN: EVIDENCIA FOTOGRAFÍCA ---
        // Cámara + galería de fotos asociadas a esta oportunidad.
        // Componente de ui/nativo/ que maneja: permiso, captura, cancelación, galería, eliminación.
        SeccionEvidencia(
            evidencias = evidencias,
            onFotoCapturada = onFotoCapturada,
            onEliminarFoto = onEliminarFoto
        )

        // --- BOTÓN: VER HISTORIAL ---
        // Navega a la pantalla de historial de gestiones (timeline de seguimientos).
        // Color secondary: lo distingue del botón primario (Editar) y del de error (Descartar).
        Button(
            onClick = onVerHistorial,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Ver historial de gestión")
        }

        // --- BOTÓN: VOLVER ---
        // Regresa a la lista de oportunidades (o al dashboard, según de dónde se llegó).
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a la lista")
        }
    }
}

// Sección de cambio de estado: muestra los botones para avanzar el estado de la oportunidad.
//
// ¿Cómo funciona?
// 1. El ViewModel calcula estadosPermitidos usando Permisos.puedeCambiarEstado().
//    - Ejecutivo en "Identificada" → ["Contacto realizado"] (1 paso adelante, no finales)
//    - Ejecutivo en "En espera" → [] (el siguiente sería "Confirmada" que es final → no puede)
//    - Supervisor en "En espera" → ["Confirmada", "Descartada"] (puede llegar a finales)
//    - Cualquiera en "Confirmada" o "Descartada" → [] (estado final, no se modifica)
//
// 2. Si estadosPermitidos está vacío → muestra un mensaje explicativo:
//    - Si es estado final: "Este estado es final y no puede modificarse."
//    - Si no es final: "Tu rol no puede avanzar este estado; el supervisor debe confirmarlo o descartarlo."
//
// 3. Si hay estados permitidos → muestra un botón por cada uno:
//    - Estados intermedios: Button normal (ej: "Avanzar a: Contacto realizado")
//    - "Confirmada": Button normal con texto "Confirmar participación"
//    - "Descartada": OutlinedButton en ROJO (color de error) con texto "Descartar oportunidad"
//
// 4. Los estados FINALES (Confirmada, Descartada) piden CONFIRMACIÓN con AlertDialog
//    antes de ejecutarse (son irreversibles). Los intermedios no piden confirmación.
//
// Es private: solo se usa dentro de DetalleContenido.
@Composable
private fun SeccionCambioEstado(
    // Estado actual de la oportunidad (para el mensaje cuando no hay transiciones permitidas)
    estadoActual: String,
    // Estados a los que se PUEDE avanzar (calculado por el ViewModel con Permisos.kt)
    estadosPermitidos: List<String>,
    // Callback: se llama con el nuevo estado (el ViewModel lo procesa: actualiza Room + registra historial)
    onCambiarEstado: (String) -> Unit
) {
    // Estado local: si hay un estado final pendiente de confirmar (AlertDialog abierto).
    // null = no hay confirmación pendiente.
    var pendiente by remember { mutableStateOf<String?>(null) }

    // Título de la sección
    Text("Cambiar estado", style = MaterialTheme.typography.titleMedium)

    if (estadosPermitidos.isEmpty()) {
        // No hay transiciones permitidas: se muestra un mensaje explicativo.
        // El mensaje depende de por qué está vacío:
        // - Si el estado actual es final (Confirmada/Descartada) → "no puede modificarse"
        // - Si no es final → el rol no tiene permiso (ejecutivo no puede confirmar/descartar)
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
        // Hay transiciones permitidas: se muestra un botón por cada estado destino.
        estadosPermitidos.forEach { estado ->
            // ¿Es un estado final? (Confirmada o Descartada)
            // Los finales piden confirmación (AlertDialog) porque son irreversibles.
            val esFinal = estado in Estados.finales

            // Texto del botón: se adapta al tipo de estado.
            // - "Confirmada" → "Confirmar participación" (más claro que "Avanzar a: Confirmada")
            // - "Descartada" → "Descartar oportunidad" (más claro que "Avanzar a: Descartada")
            // - Otros → "Avanzar a: [estado]" (ej: "Avanzar a: Contacto realizado")
            val etiqueta = when (estado) {
                "Confirmada" -> "Confirmar participación"
                "Descartada" -> "Descartar oportunidad"
                else -> "Avanzar a: $estado"
            }

            // Acción al presionar:
            // - Si es final → abre el AlertDialog de confirmación (pendiente = estado)
            // - Si no es final → cambia el estado directamente (sin confirmación)
            val accion = { if (esFinal) pendiente = estado else onCambiarEstado(estado) }

            if (estado == "Descartada") {
                // "Descartada" se muestra como OutlinedButton en ROJO (color de error).
                // Visualmente se distingue de los demás: es una acción negativa/destructiva.
                OutlinedButton(
                    onClick = accion,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(etiqueta) }
            } else {
                // Estados intermedios y "Confirmada": Button normal (color primario).
                Button(onClick = accion, modifier = Modifier.fillMaxWidth()) { Text(etiqueta) }
            }
        }
    }

    // --- DIALOGO DE CONFIRMACIÓN (solo para estados finales) ---
    // Se muestra cuando el usuario presiona "Confirmar participación" o "Descartar oportunidad".
    // Obliga a confirmar porque estos estados son IRREVERSIBLES:
    // - Una vez "Confirmada", no se puede volver a "En espera".
    // - Una vez "Descartada", no se puede reabrir.
    pendiente?.let { estado ->
        AlertDialog(
            onDismissRequest = { pendiente = null },  // Tocar fuera → cancelar
            title = { Text("¿Confirmar el cambio?") },
            text = { Text("La oportunidad pasará a \"$estado\" y ya no podrá modificarse.") },
            confirmButton = {
                // "Sí, continuar": ejecuta el cambio de estado y cierra el diálogo.
                TextButton(onClick = {
                    onCambiarEstado(estado)  // ViewModel actualiza Room + registra historial
                    pendiente = null         // Cierra el diálogo
                }) { Text("Sí, continuar") }
            },
            dismissButton = {
                // "Cancelar": solo cierra el diálogo, no cambia nada.
                TextButton(onClick = { pendiente = null }) { Text("Cancelar") }
            }
        )
    }
}

// DetalleItem: par de label + valor para mostrar un dato del detalle.
// Es público (no private) porque podría reutilizarse en otras pantallas de detalle
// (ej: DetalleOrganizacionScreen).
//
// Visual:
// - Label: texto pequeño en gris (onSurfaceVariant, labelLarge) → "Organización"
// - Valor: texto normal en negro (bodyLarge) → "Municipalidad de Valle Verde"
//
// Se usa 10 veces en DetalleContenido (org, responsable, tipo, comuna, público,
// prioridad, costo, detección, inicio, término).
@Composable
fun DetalleItem(titulo: String, valor: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        // Label (nombre del campo): gris, tamaño pequeño
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Valor (dato del campo): negro, tamaño normal
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}