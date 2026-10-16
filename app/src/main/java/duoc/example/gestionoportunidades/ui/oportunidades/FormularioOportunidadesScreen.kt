package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.components.MensajeConfirmacion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioOportunidadScreen(
    onVolver: () -> Unit
) {
    // Estados para los campos del formulario
    var nombre by remember { mutableStateOf("") }
    var tipoEvento by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }
    var prioridad by remember { mutableStateOf("Media") }

    // Estado para la animación de guardado (Tarea J1)
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            // verticalScroll permite que la pantalla sea scrolleable si el teclado tapa los campos
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mensaje animado de éxito (Se mostrará solo cuando mostrarConfirmacion sea true)
        MensajeConfirmacion(
            visible = mostrarConfirmacion,
            mensaje = "¡Oportunidad guardada con éxito!"
        )

        Text(
            text = "Nueva Oportunidad",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre de la Oportunidad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = tipoEvento,
            onValueChange = { tipoEvento = it },
            label = { Text("Tipo de Evento") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = comuna,
            onValueChange = { comuna = it },
            label = { Text("Comuna") },
            modifier = Modifier.fillMaxWidth()
        )

        // Por ahora usamos un TextField para prioridad, luego en PC EXTRA se puede cambiar a un Dropdown[cite: 1]
        OutlinedTextField(
            value = prioridad,
            onValueChange = { prioridad = it },
            label = { Text("Prioridad (Alta, Media, Baja)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    // Simulamos el guardado y mostramos la animación
                    coroutineScope.launch {
                        mostrarConfirmacion = true
                        delay(2000) // Mostramos el mensaje por 2 segundos
                        mostrarConfirmacion = false
                        onVolver() // Volvemos a la lista
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = nombre.isNotBlank() // El botón se habilita solo si hay nombre
            ) {
                Text("Guardar")
            }
        }
    }
}