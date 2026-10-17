package duoc.example.gestionoportunidades.ui.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorScreen(
    onVerOportunidad: (Long) -> Unit,
    viewModel: SupervisorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var busqueda by remember { mutableStateOf("") }
    var mostrarDialogAsignar by remember { mutableStateOf(false) }
    var oportunidadSeleccionada by remember { mutableStateOf<Oportunidad?>(null) }

    LaunchedEffect(Unit) {
        viewModel.inicializarPanel()
    }

    LaunchedEffect(busqueda) {
        viewModel.inicializarPanel(busqueda)
    }

    // Snackbar para mensajes
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
        topBar = {
            TopAppBar(title = { Text("Panel de Supervisor") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                label = { Text("Buscar oportunidad...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.listaOportunidadesGlobal.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay oportunidades", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.listaOportunidadesGlobal) { oportunidad ->
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
    }

    // Dialog de asignación
    if (mostrarDialogAsignar && oportunidadSeleccionada != null) {
        val oportunidad = oportunidadSeleccionada!!
        Dialog(onDismissRequest = { mostrarDialogAsignar = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).width(300.dp)
                ) {
                    Text("Asignar responsable", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Oportunidad: ${oportunidad.nombre}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    uiState.ejecutivos.forEach { ejecutivo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                ejecutivo.nombreCompleto,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

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
                TextButton(onClick = onVerDetalle) {
                    Text("Ver")
                }
            }
        }
    }
}