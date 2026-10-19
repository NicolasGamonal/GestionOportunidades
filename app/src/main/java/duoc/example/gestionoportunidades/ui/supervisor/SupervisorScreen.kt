package duoc.example.gestionoportunidades.ui.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Usuario

// Panel del supervisor (pestaña "Equipo"). Solo visible para rol SUPERVISOR.
// Consulta global de oportunidades + asignar/reasignar + confirmar/descartar.
// El ejecutivo NO puede acceder (Navegacion.kt redirige al Dashboard).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorScreen(
    onVerOportunidad: (Long) -> Unit,   // Navegar al detalle de una oportunidad
    viewModel: SupervisorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Búsqueda local (se pasa al ViewModel vía LaunchedEffect)
    var busqueda by remember { mutableStateOf("") }
    // Estado del diálogo de asignación
    var mostrarDialogAsignar by remember { mutableStateOf(false) }
    var oportunidadSeleccionada by remember { mutableStateOf<Oportunidad?>(null) }

    // Al cambiar la búsqueda, notifica al ViewModel para que re-filtre
    LaunchedEffect(busqueda) {
        viewModel.inicializarPanel(busqueda)
    }

    // Snackbar para mensajes de éxito/error
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.mensajeExito, uiState.mensajeError) {
        val msg = uiState.mensajeExito ?: uiState.mensajeError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.limpiarMensajes()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Panel de Supervisor") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Campo de búsqueda
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar oportunidad...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when {
                uiState.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.listaOportunidadesGlobal.isEmpty() -> item {
                    Text("No hay oportunidades", style = MaterialTheme.typography.bodyLarge)
                }
                else -> items(uiState.listaOportunidadesGlobal, key = { it.idOportunidad }) { oportunidad ->
                    SupervisorCard(
                        oportunidad = oportunidad,
                        ejecutivos = uiState.ejecutivos,
                        onVerDetalle = { onVerOportunidad(oportunidad.idOportunidad) },
                        onAsignar = {
                            oportunidadSeleccionada = oportunidad
                            mostrarDialogAsignar = true
                        },
                        onConfirmar = { viewModel.confirmarOportunidad(oportunidad) },
                        onDescartar = { viewModel.descartarOportunidad(oportunidad) }
                    )
                }
            }
        }
    }

    // --- DIALOGO: ASIGNAR / REASIGNAR RESPONSABLE ---
    // Muestra la lista de ejecutivos. Al presionar uno, reasigna la oportunidad.
    if (mostrarDialogAsignar && oportunidadSeleccionada != null) {
        val oportunidad = oportunidadSeleccionada!!
        Dialog(onDismissRequest = { mostrarDialogAsignar = false }) {
            Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Asignar responsable", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Oportunidad: ${oportunidad.nombre}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Un botón por cada ejecutivo
                    uiState.ejecutivos.forEach { ejecutivo ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ejecutivo.nombreCompleto, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.reasignarResponsable(oportunidad, ejecutivo.idUsuario)
                                mostrarDialogAsignar = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Asignar a ${ejecutivo.nombreCompleto}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { mostrarDialogAsignar = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Cancelar") }
                }
            }
        }
    }
}

// Tarjeta de oportunidad en el panel del supervisor.
// Muestra: nombre, comuna · prioridad · estado + botones de acción.
// Los botones "Asignar", "Confirmar" y "Descartar" solo se muestran
// si la oportunidad NO está en estado final (Confirmada/Descartada).
// "Ver" siempre está disponible (navega al detalle).
@Composable
fun SupervisorCard(
    oportunidad: Oportunidad,
    ejecutivos: List<Usuario>,
    onVerDetalle: () -> Unit,
    onAsignar: () -> Unit,
    onConfirmar: () -> Unit,
    onDescartar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(oportunidad.nombre, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${oportunidad.comuna} · ${oportunidad.prioridad} · ${oportunidad.estado}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Acciones solo si no está en estado final
                if (oportunidad.estado !in listOf("Confirmada", "Descartada")) {
                    TextButton(onClick = onAsignar) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Asignar")
                    }
                    OutlinedButton(onClick = onConfirmar) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar")
                    }
                    OutlinedButton(onClick = onDescartar) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Descartar")
                    }
                }
                // "Ver" siempre disponible
                TextButton(onClick = onVerDetalle) {
                    Text("Ver")
                }
            }
        }
    }
}