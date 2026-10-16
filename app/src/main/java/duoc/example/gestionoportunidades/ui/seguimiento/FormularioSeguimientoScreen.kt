package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import duoc.example.gestionoportunidades.data.Seguimiento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioSeguimientoScreen(
    viewModel: SeguimientoViewModel,
    onNavigateBack: () -> Unit
) {
    // Para mostrar los mensajes flotantes (Snackbar)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Estado del formulario: se usa rememberSaveable para conservar datos si hay error
    var tipoAccion by rememberSaveable { mutableStateOf("") }
    var resultado by rememberSaveable { mutableStateOf("") }
    var proximaAccion by rememberSaveable { mutableStateOf("") }
    var fechaProximo by rememberSaveable { mutableStateOf("") }

    // Estado de errores individuales[cite: 4]
    var errorTipo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorResultado by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nueva Gestión") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo: Tipo de Acción
            OutlinedTextField(
                value = tipoAccion,
                onValueChange = {
                    tipoAccion = it
                    errorTipo = null // Limpia el error al escribir
                },
                label = { Text("Tipo de Acción (Ej: Llamada)") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorTipo != null,
                supportingText = { if (errorTipo != null) Text(errorTipo!!) },
                trailingIcon = { if (errorTipo != null) Icon(Icons.Default.Error, "Error") }
            )

            // Campo: Resultado
            OutlinedTextField(
                value = resultado,
                onValueChange = {
                    resultado = it
                    errorResultado = null
                },
                label = { Text("Resultado de la gestión") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorResultado != null,
                supportingText = { if (errorResultado != null) Text(errorResultado!!) },
                trailingIcon = { if (errorResultado != null) Icon(Icons.Default.Error, "Error") }
            )

            // Campo: Próxima Acción (Opcional en este paso)
            OutlinedTextField(
                value = proximaAccion,
                onValueChange = { proximaAccion = it },
                label = { Text("Próxima Acción (Ej: Enviar correo)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Campo: Fecha Próximo Seguimiento
            OutlinedTextField(
                value = fechaProximo,
                onValueChange = { fechaProximo = it },
                label = { Text("Fecha (Ej: 2026-10-20)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Botón Guardar
            Button(
                onClick = {
                    var esValido = true
                    // Validaciones básicas que activan los isError y supportingText[cite: 4]
                    if (tipoAccion.isBlank()) {
                        errorTipo = "El tipo de acción es obligatorio"
                        esValido = false
                    }
                    if (resultado.isBlank()) {
                        errorResultado = "El resultado es obligatorio"
                        esValido = false
                    }

                    if (esValido) {
                        val nuevoSeguimiento = Seguimiento(
                            tipoAccion = tipoAccion,
                            fechaHora = "2026-10-16",
                            resultado = resultado,
                            proximaAccion = proximaAccion,
                            fechaProximo = fechaProximo,
                            idOportunidad = 1,
                            idUsuario = 1
                        )

                        viewModel.guardarSeguimiento(nuevoSeguimiento)

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Gestión guardada con éxito") // Snackbar de éxito[cite: 4]
                            onNavigateBack()
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Por favor, corrige los errores") // Snackbar de error[cite: 4]
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Gestión")
            }
        }
    }
}