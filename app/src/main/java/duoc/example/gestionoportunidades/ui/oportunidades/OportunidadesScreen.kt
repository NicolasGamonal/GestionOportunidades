package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.components.PanelFiltrosAnimado
import duoc.example.gestionoportunidades.ui.components.StatusChip
import duoc.example.gestionoportunidades.ui.components.TarjetaOportunidad
import duoc.example.gestionoportunidades.util.comoFechaHora

// OportunidadesScreen: lista principal de oportunidades (pestaña "Oportunidades" del menú inferior).
//
// ¿Dónde se llega?
// - Desde el menú inferior (bottom bar): pestaña "Oportunidades".
// - Es una de las 3-4 pantallas "raíz" (no muestra botón "Volver" en el TopBar).
//
// ¿Qué muestra?
// 1. Barra de búsqueda (campo de texto con ícono de lupa) + botón de filtros (ícono de embudo).
// 2. Panel de filtros animado (estado + prioridad) que aparece/desaparece con AnimatedVisibility.
// 3. Lista de tarjetas (LazyColumn) con una TarjetaOportunidad por cada oportunidad.
// 4. Botón flotante "Nueva" (ExtendedFloatingActionButton) en la esquina inferior derecha.
//
// Estados de la lista (when):
// - isLoading → spinner centrado (mientras el ViewModel consulta Room por primera vez)
// - oportunidades.isEmpty() → mensaje "No hay oportunidades que mostrar" (estado vacío, checklist)
// - else → LazyColumn con las tarjetas
//
// ¿Por qué LazyColumn y no Column?
// Porque la lista puede tener muchas oportunidades (decenas o cientos). LazyColumn solo
// crea los items visibles en pantalla (y unos pocos de buffer), lo cual es eficiente.
// Una Column crearía todos los Composables de una vez (lento con muchas tarjetas).
//
// ¿Por qué un Box como contenedor raíz?
// Porque el ExtendedFloatingActionButton necesita superponerse al LazyColumn
// (flotar sobre la lista). Box permite posicionar el FAB con .align(BottomEnd)
// sin que ocupe espacio en el flujo de la lista.
//
// Patrón MVVM:
// - Observa 2 StateFlows del ViewModel:
//   1. uiState → las oportunidades + isLoading (viene del DAO vía Flow reactivo)
//   2. filtros → el estado de búsqueda y filtros (se actualiza en cada tecla/selección)
// - No tiene lógica de negocio: la filtración la hace el ViewModel (que consulta al DAO
//   con los filtros activos). La Screen solo dibuja lo que el ViewModel le pasa.
// - Las acciones (navegar al detalle, crear nueva) se delegan al padre (Navegacion.kt) vía callbacks.
//
// Cumple el checklist:
// - "Mostrar listado de oportunidades con búsqueda y filtros por estado o prioridad"
// - "Mostrar estados vacíos, indicadores de carga y confirmaciones cuando corresponda"
// - "Adaptar el contenido a diferentes tamaños de pantalla y permitir desplazamiento"
// - "Implementar al menos 3 interacciones animadas" → PanelFiltrosAnimado es 1 de las 3

@Composable
fun OportunidadesScreen(
    // ViewModel inyectado desde Navegacion.kt (con AppViewModelProvider.Factory).
    // Contiene: la lista de oportunidades (reactiva), el estado de carga,
    // y el estado de los filtros (búsqueda, estado, prioridad).
    viewModel: OportunidadesViewModel,
    // Callback: se llama con el id de la oportunidad cuando el usuario toca una tarjeta.
    // El padre (Navegacion.kt) navega al detalle: navController.navigate(Ruta.DetalleOportunidad.crear(id))
    onVerDetalle: (Long) -> Unit,
    // Callback: se llama cuando el usuario presiona el botón "Nueva".
    // El padre navega al formulario de creación: navController.navigate(Ruta.FormularioOportunidad.crear())
    onNueva: () -> Unit
) {
    // Observa el estado principal del ViewModel (oportunidades + isLoading).
    // Se actualiza solo cuando Room emite un cambio (se crea, edita o elimina una oportunidad).
    // collectAsStateWithLifecycle(): pausa la colección si la pantalla no es visible (ahorra recursos).
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observa el estado de los filtros (búsqueda, estado, prioridad).
    // Se actualiza en cada tecla del campo de búsqueda y en cada selección de chip.
    // El ViewModel usa estos filtros para re-ejecutar la query del DAO (flatMapLatest con debounce).
    val filtros by viewModel.filtros.collectAsStateWithLifecycle()

    // Estado LOCAL: si el panel de filtros está visible o no.
    // No va en el UiState del ViewModel porque es un detalle visual efímero
    // (no afecta la lógica de negocio ni persiste entre composiciones).
    // Se alterna al presionar el ícono de embudo (FilterList).
    var mostrarFiltros by remember { mutableStateOf(false) }

    // Box: contenedor relativo que permite superponer el FAB sobre la lista.
    // Sin Box, el FAB ocuparía espacio en el flujo (empujando la lista hacia arriba).
    Box(modifier = Modifier.fillMaxSize()) {

        // LazyColumn: lista scrollable eficiente (solo crea items visibles).
        //
        // contentPadding: márgenes internos de la lista.
        // - start/top/end = 16.dp: espacio entre las tarjetas y los bordes de la pantalla.
        // - bottom = 88.dp: espacio extra en la parte inferior para que el botón flotante
        //   (que mide ~56dp + 16dp de padding) NO tape la última tarjeta.
        //   Sin este padding, la última tarjeta quedaría oculta detrás del FAB.
        //
        // verticalArrangement: 12dp de espacio entre cada item de la lista
        // (tarjetas, barra de búsqueda, panel de filtros).
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- ITEM 1: BARRA DE BÚSQUEDA + BOTÓN DE FILTROS ---
            // Siempre visible (no depende del estado de carga).
            // Row: campo de búsqueda a la izquierda (ocupa el espacio disponible con weight)
            // + botón de filtros a la derecha.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Campo de búsqueda: texto libre, una línea, con ícono de lupa.
                    // - value = filtros.busqueda: el texto actual (viene del ViewModel).
                    // - onValueChange: cada tecla actualiza el filtro en el ViewModel,
                    //   que re-ejecuta la query del DAO (con debounce para no consultar en cada tecla).
                    // - placeholder: texto gris que se muestra cuando el campo está vacío.
                    // - weight(1f): ocupa todo el ancho disponible, dejando solo lo necesario
                    //   para el IconButton de filtros.
                    OutlinedTextField(
                        value = filtros.busqueda,
                        onValueChange = viewModel::onBusquedaChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Buscar por nombre...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        singleLine = true
                    )

                    // Espacio entre el campo y el botón
                    Spacer(modifier = Modifier.width(8.dp))

                    // Botón de filtros: alterna la visibilidad del PanelFiltrosAnimado.
                    // Al presionar: mostrarFiltros = !mostrarFiltros (toggle).
                    // El ícono de embudo (FilterList) indica "hay filtros disponibles".
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                    }
                }
            }

            // --- ITEM 2: PANEL DE FILTROS ANIMADO ---
            // Siempre se compone (está en la LazyColumn), pero AnimatedVisibility
            // controla si se ve o no (con animación de expand/shrink + fade).
            // - visible = mostrarFiltros: true si el usuario presionó el botón de filtros.
            // - estadoSeleccionado / prioridadSeleccionada: los filtros activos (viene del ViewModel).
            // - onEstadoChange / onPrioridadChange: actualizan el filtro en el ViewModel.
            // - onLimpiar: resetea ambos filtros a "" (el ViewModel re-ejecuta la query sin filtros).
            item {
                PanelFiltrosAnimado(
                    visible = mostrarFiltros,
                    estadoSeleccionado = filtros.estado,
                    prioridadSeleccionada = filtros.prioridad,
                    onEstadoChange = viewModel::onEstadoChange,
                    onPrioridadChange = viewModel::onPrioridadChange,
                    onLimpiar = viewModel::limpiarFiltros
                )
            }

            // --- ITEM 3: CONTENIDO DE LA LISTA (3 estados posibles) ---
            when {
                // 1. CARGANDO: el ViewModel aún no tiene datos (primera consulta a Room).
                //    Se muestra un spinner centrado.
                //    (En la práctica, con Room local esto dura < 100ms, pero es necesario
                //     para el checklist: "indicadores de carga mientras se consulta Room".)
                uiState.isLoading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                // 2. ESTADO VACÍO: no hay oportunidades (o los filtros no devuelven resultados).
                //    Se muestra un mensaje informativo (checklist: "Mostrar estados vacíos").
                //    El usuario puede: limpiar filtros, cambiar la búsqueda, o crear una nueva.
                uiState.oportunidades.isEmpty() -> item {
                    Text(
                        "No hay oportunidades que mostrar",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // 3. LISTA NORMAL: se muestra una TarjetaOportunidad por cada oportunidad.
                //    items() con key = { it.idOportunidad }: Compose usa el id para identificar
                //    cada item (si se borra una del medio, las demás no se desfasan).
                //    Al tocar una tarjeta → onVerDetalle(id) → navega al detalle.
                else -> items(uiState.oportunidades, key = { it.idOportunidad }) { oportunidad ->
                    TarjetaOportunidad(
                        oportunidad = oportunidad,
                        onClick = { onVerDetalle(oportunidad.idOportunidad) }
                    )
                }
            }
        }

        // --- BOTÓN FLOTANTE: "NUEVA" ---
        // ExtendedFloatingActionButton: FAB con ícono + texto (más grande que un FAB normal).
        // - icon: ícono de "+" (Agregar).
        // - text: "Nueva" (indicador claro de la acción).
        // - align(BottomEnd): se posiciona en la esquina inferior derecha (convención Material).
        // - padding(16.dp): margen desde los bordes de la pantalla.
        // - Flota SOBRE la lista (no ocupa espacio en el LazyColumn, por eso el Box).
        // - Al presionar: onNueva() → navega al formulario de creación.
        ExtendedFloatingActionButton(
            onClick = onNueva,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nueva") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}