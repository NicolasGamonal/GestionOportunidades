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

// FormularioOportunidadScreen: pantalla para crear o editar una oportunidad.
//
// ¿Dónde se llega?
// - Desde OportunidadesScreen: botón "+" → Ruta.FormularioOportunidad.crear() (id = -1)
// - Desde DetalleOportunidadScreen: botón "Editar" → Ruta.FormularioOportunidad.editar(id)
//
// ¿Cómo sabe si es "Crear" o "Editar"?
// El ViewModel lo determina desde el SavedStateHandle:
// - id = -1 → modo CREAR (campos vacíos, botón dice "Crear oportunidad")
// - id > 0 → modo EDITAR (carga los datos de Room, botón dice "Guardar cambios")
//
// ¿Qué campos tiene?
// - Nombre (texto)
// - Tipo de evento (dropdown: Catalogos.tiposEvento)
// - Organización (dropdown: lista desde Room)
// - Fecha de detección (date picker)
// - Fecha de inicio (date picker)
// - Fecha de término (date picker)
// - Comuna (texto)
// - Público esperado (número)
// - Prioridad (chips: Alta, Media, Baja)
// - Costo referencial (número)
// - Observaciones (texto multilinea)
//
// Validaciones:
// - Cada campo se valida en tiempo real (en cada tecla/selección) vía el ViewModel.
// - Los errores se muestran con isError + supportingText + ícono (checklist).
// - Los valores NO se borran al fallar la validación (viene del UiState, no de remember).
// - Al presionar "Guardar", se validan TODOS los campos. Si hay errores, no se guarda.
//
// Acciones:
// - Guardar: valida → llama al ViewModel → Room → Snackbar de éxito → retorno automático.
// - Cancelar: vuelve sin guardar.
// - Eliminar (solo en modo edición y si tiene permiso): confirma con AlertDialog → borra de Room.
//
// Patrón MVVM:
// - La Screen NO tiene lógica de negocio: solo dibuja los campos y delega al ViewModel.
// - El ViewModel (FormularioOportunidadViewModel) maneja: validaciones, permisos,
//   carga de datos (edición), guardado en Room y eliminación.
// - FormularioContenedor maneja: Snackbar de error, banner de éxito y retorno automático.

@Composable
fun FormularioOportunidadScreen(
    // ViewModel inyectado desde Navegacion.kt (con AppViewModelProvider.Factory).
    // Contiene: todos los campos del formulario, errores por campo, organizaciones,
    // estado de guardado, permisos de eliminación.
    viewModel: FormularioOportunidadViewModel,
    // Navegar de vuelta al detalle (si se llegó desde el detalle) o a la lista.
    // Se llama después del guardado exitoso (retorno automático de FormularioContenedor).
    onGuardado: () -> Unit,
    // Navegar a la lista de oportunidades (pestaña). Se llama después de eliminar.
    onEliminado: () -> Unit,
    // Navegar de vuelta sin guardar (botón "Cancelar").
    onCancelar: () -> Unit
) {
    // Observa el estado del ViewModel. Se actualiza en cada tecla (onValorChange),
    // en cada selección (dropdown, fecha, chip) y en los eventos de guardado/eliminación.
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    // FormularioContenedor: envoltorio que aporta:
    // 1. Snackbar de error (cuando mensajeError no es null).
    // 2. Banner de éxito animado (cuando guardadoExitoso o eliminado = true).
    // 3. Retorno automático (delay 1.2s → onGuardado o onEliminado).
    //
    // guardadoExitoso: se activa en ambos casos (guardar Y eliminar) porque el banner
    // de éxito se muestra en ambos. El texto cambia según el contexto (mensajeExito).
    FormularioContenedor(
        guardadoExitoso = ui.guardadoExitoso || ui.eliminado,
        // Texto del banner: cambia según qué acción se completó.
        // - Eliminado → "Oportunidad eliminada"
        // - Edición → "Oportunidad actualizada"
        // - Creación → "Oportunidad creada"
        mensajeExito = when {
            ui.eliminado -> "Oportunidad eliminada"
            ui.esEdicion -> "Oportunidad actualizada"
            else -> "Oportunidad creada"
        },
        // Mensaje de error (viene del ViewModel: "No se pudo guardar", etc.)
        mensajeError = ui.mensajeError,
        // Callback: limpia el error en el ViewModel después de mostrar el Snackbar
        // (evita que se repita en la próxima composición).
        onErrorMostrado = viewModel::limpiarMensajeError,
        // Navegación al completar: si fue eliminación → ir a la lista; si fue guardado → volver.
        onGuardado = if (ui.eliminado) onEliminado else onGuardado
    ) {
        // Estados de la pantalla:
        when {
            // Cargando: el ViewModel está consultando Room (solo en modo edición,
            // para cargar los datos existentes). En modo creación no hay carga.
            ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            // Sin acceso: el ejecutivo intentó editar una oportunidad que no es suya
            // (o el supervisor intentó editar una que no puede).
            ui.sinAcceso -> PantallaMensaje("No tienes permiso para editar esta oportunidad", onCancelar)

            // Contenido normal: el formulario completo.
            else -> ContenidoFormulario(ui, viewModel, onCancelar)
        }
    }
}

// Contenido del formulario: todos los campos organizados en una Column scrollable.
// Es private: solo se usa dentro de FormularioOportunidadScreen.
//
// ¿Por qué una Column y no un LazyColumn?
// Porque un formulario tiene un número fijo de campos (10-12). LazyColumn es para
// listas infinitas o muy largas. Una Column con verticalScroll es más simple y suficiente.
//
// spacedBy(4.dp): espacio mínimo entre campos (los campos ya tienen padding interno).
@Composable
private fun ContenidoFormulario(
    // El estado completo del formulario (viene del UiState del ViewModel)
    ui: FormularioUiState,
    // El ViewModel: se pasa para llamar a las funciones de cambio de campo y guardar
    viewModel: FormularioOportunidadViewModel,
    // Acción al presionar "Cancelar" (navega de vuelta sin guardar)
    onCancelar: () -> Unit
) {
    // Mapa de errores por campo: CampoOportunidad.NOMBRE → "El nombre es obligatorio", etc.
    // Si el campo es válido, el valor es null (no se muestra error).
    // Viene del UiState (el ViewModel lo calcula en cada onXxxChange).
    val errores = ui.errores

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())  // Scroll si el formulario no cabe en la pantalla
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- NOMBRE ---
        // Campo de texto libre. Validación: obligatorio, longitud mínima.
        CampoFormulario(
            valor = ui.nombre,
            onValorChange = viewModel::onNombreChange,  // Cada tecla → ViewModel valida y actualiza
            etiqueta = "Nombre de la oportunidad",
            error = errores[CampoOportunidad.NOMBRE]
        )

        // --- TIPO DE EVENTO ---
        // Dropdown con las opciones de Catalogos.tiposEvento
        // (ej: "Feria municipal", "Jornada de beneficios", "Evento corporativo", etc.)
        CampoDesplegable(
            valor = ui.tipoEvento,
            etiqueta = "Tipo de evento",
            opciones = Catalogos.tiposEvento,  // Lista de Strings (catálogo fijo)
            textoDe = { it },                   // Cada opción es un String: se muestra directamente
            error = errores[CampoOportunidad.TIPO],
            onSeleccion = viewModel::onTipoChange
        )

        // --- ORGANIZACIÓN ---
        // Dropdown con las organizaciones que existen en Room.
        // El valor mostrado es el NOMBRE de la organización seleccionada (resuelto del id).
        // Si no hay organizaciones registradas, se muestra un mensaje informativo.
        CampoDesplegable(
            // Resuelve el nombre desde el id: busca la org con el id seleccionado y toma su nombre.
            // Si no hay selección (id = 0 o -1) → "" (vacío).
            valor = ui.organizaciones.firstOrNull { it.idOrganizacion == ui.idOrganizacion }?.nombre ?: "",
            etiqueta = "Organización",
            opciones = ui.organizaciones,       // Lista de Organizacion (viene de Room)
            textoDe = { it.nombre },            // Muestra el nombre de cada organización
            error = errores[CampoOportunidad.ORGANIZACION],
            // Al seleccionar: pasa el idOrganizacion al ViewModel (no el objeto completo)
            onSeleccion = { viewModel.onOrganizacionChange(it.idOrganizacion) }
        )
        // Si no hay organizaciones en Room, se muestra un aviso (no se puede crear oportunidad sin org)
        if (ui.organizaciones.isEmpty()) {
            Text(
                "No hay organizaciones registradas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- FECHA DE DETECCIÓN ---
        // Date picker: cuándo se detectó la oportunidad.
        // Validación: debe ser <= fechaInicio (no se puede detectar después de que inicia).
        CampoFecha(
            valor = ui.fechaDeteccion,
            etiqueta = "Fecha de detección",
            error = errores[CampoOportunidad.DETECCION],
            onFechaElegida = viewModel::onDeteccionChange
        )

        // --- FECHA DE INICIO ---
        // Date picker: cuándo inicia el evento.
        // Validación: debe ser >= fechaDeteccion y <= fechaTermino.
        CampoFecha(
            valor = ui.fechaInicio,
            etiqueta = "Fecha de inicio del evento",
            error = errores[CampoOportunidad.INICIO],
            onFechaElegida = viewModel::onInicioChange
        )

        // --- FECHA DE TÉRMINO ---
        // Date picker: cuándo termina el evento.
        // Validación: debe ser >= fechaInicio (puede ser el mismo día).
        CampoFecha(
            valor = ui.fechaTermino,
            etiqueta = "Fecha de término del evento",
            error = errores[CampoOportunidad.TERMINO],
            onFechaElegida = viewModel::onTerminoChange
        )

        // --- COMUNA ---
        // Campo de texto libre (solo comuna, no región: decisión de 3FN).
        CampoFormulario(
            valor = ui.comuna,
            onValorChange = viewModel::onComunaChange,
            etiqueta = "Comuna",
            error = errores[CampoOportunidad.COMUNA]
        )

        // --- PÚBLICO ESPERADO ---
        // Campo numérico: cantidad estimada de asistentes.
        // Validación: debe ser > 0 (número entero positivo).
        // KeyboardType.Number: muestra el teclado numérico (más rápido para ingresar cifras).
        CampoFormulario(
            valor = ui.publicoEsperado,
            onValorChange = viewModel::onPublicoChange,
            etiqueta = "Público esperado",
            error = errores[CampoOportunidad.PUBLICO],
            keyboardType = KeyboardType.Number
        )

        // --- PRIORIDAD ---
        // Chips de selección (no es un dropdown porque son solo 3 opciones y se ven mejor como chips).
        // Estados.prioridades = ["Alta", "Media", "Baja"]
        // Es single-select: al tocar un chip, se deseleccionan los otros.
        Text("Prioridad", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Estados.prioridades.forEach { prioridad ->
                FilterChip(
                    selected = ui.prioridad == prioridad,  // true si este chip está seleccionado
                    onClick = { viewModel.onPrioridadChange(prioridad) },
                    label = { Text(prioridad) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // --- COSTO REFERENCIAL ---
        // Campo numérico: costo estimado de la activación en pesos chilenos.
        // Validación: debe ser >= 0 (no negativo).
        // Se guarda como String en el UiState (para el campo de texto) y se convierte
        // a Double en el ViewModel al momento de guardar en Room.
        CampoFormulario(
            valor = ui.costoReferencial,
            onValorChange = viewModel::onCostoChange,
            etiqueta = "Costo referencial (CLP)",
            error = errores[CampoOportunidad.COSTO],
            keyboardType = KeyboardType.Number
        )

        // --- OBSERVACIONES ---
        // Campo de texto multilinea (minLines = 3): notas libres del ejecutivo.
        // Validación: opcional (puede ser vacío).
        CampoFormulario(
            valor = ui.observaciones,
            onValorChange = viewModel::onObservacionesChange,
            etiqueta = "Observaciones",
            error = errores[CampoOportunidad.OBSERVACIONES],
            singleLine = false,   // Permite saltos de línea
            minLines = 3          // Muestra 3 líneas mínimas (caja más alta)
        )

        // --- BOTONES: GUARDAR / CANCELAR ---
        Spacer(Modifier.height(8.dp))

        // Botón de guardar:
        // - Texto cambia según el modo: "Crear oportunidad" vs "Guardar cambios"
        // - Se deshabilita si está guardando, ya se guardó, o ya se eliminó
        // - Al presionar: el ViewModel valida TODOS los campos. Si hay errores, no guarda.
        //   Si no hay errores, guarda en Room y emite guardadoExitoso = true.
        Button(
            onClick = viewModel::guardar,
            enabled = !ui.guardando && !ui.guardadoExitoso && !ui.eliminado,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ui.guardando) {
                // Spinner mientras se escribe en Room
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (ui.esEdicion) "Guardar cambios" else "Crear oportunidad")
            }
        }

        // Botón de cancelar: vuelve sin guardar nada.
        OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }

        // --- BOTÓN: ELIMINAR (solo en modo edición y si tiene permiso) ---
        // Solo se muestra si:
        // 1. ui.esEdicion = true (no se puede eliminar al crear)
        // 2. ui.puedeEliminar = true (el ViewModel lo calcula con Permisos.kt:
        //    el ejecutivo solo elimina las suyas; el supervisor cualquiera)
        if (ui.esEdicion && ui.puedeEliminar) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()  // Separador visual entre los botones de acción y el de eliminar
            Spacer(Modifier.height(8.dp))

            // BotonEliminar (ui/components/): botón rojo con AlertDialog de confirmación.
            // Al confirmar: llama a viewModel::eliminar que:
            // 1. Llama a oportunidadRepository.eliminar(id)
            // 2. Si el repo devuelve un error (tiene historial, no está en "Identificada", etc.)
            //    → mensajeError se muestra en el Snackbar.
            // 3. Si es exitoso → eliminado = true → banner "Oportunidad eliminada" → navegación.
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