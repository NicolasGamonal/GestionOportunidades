package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// NUEVO: Importaciones para el scroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
// NUEVO: Importaciones para corrutinas (animaciones)
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Importaciones de nuestras clases
import duoc.example.gestionoportunidades.data.Oportunidad
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme
import duoc.example.gestionoportunidades.ui.viewmodel.OportunidadViewModel
import duoc.example.gestionoportunidades.ui.components.MensajeConfirmacion


@Composable
fun RegistroOportunidadScreen(
    modifier: Modifier = Modifier,
    viewModel: OportunidadViewModel = viewModel(),
    // NUEVO: Agregamos la función onVolver para poder navegar hacia atrás
    onVolver: () -> Unit
) {
    // --- 1. ESTADOS LOCALES DE LOS CAMPOS DE TEXTO ---
    var nombreEvento by remember { mutableStateOf("") }
    var organizacion by remember { mutableStateOf("") }
    var correoContacto by remember { mutableStateOf("") }
    var tipoEvento by remember { mutableStateOf("") }

    // --- 2. ESTADOS LOCALES BOOLEANOS ---
    var esAltaAfluencia by remember { mutableStateOf(false) }
    var requiereStand by remember { mutableStateOf(false) }

    // NUEVO: --- 3. ESTADOS PARA LA ANIMACIÓN ---
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            // NUEVO: Agregamos el scroll para que el teclado no tape el botón de guardar
            .verticalScroll(rememberScrollState())
    ) {
        // NUEVO: Mensaje animado de éxito
        MensajeConfirmacion(
            visible = mostrarConfirmacion,
            mensaje = "¡Oportunidad guardada con éxito!"
        )

        Text(
            text = "Registro de Oportunidad Comercial",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campos del formulario
        OutlinedTextField(
            value = nombreEvento,
            onValueChange = { nombreEvento = it },
            label = { Text("Nombre del Evento / Oportunidad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = organizacion,
            onValueChange = { organizacion = it },
            label = { Text("Organización / Empresa Ficticia") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = correoContacto,
            onValueChange = { correoContacto = it },
            label = { Text("Correo Electrónico de Contacto") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = tipoEvento,
            onValueChange = { tipoEvento = it },
            label = { Text("Tipo de Evento (ej. Feria, Jornada, Municipal)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Controles de Switch y Checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = esAltaAfluencia,
                onCheckedChange = { esAltaAfluencia = it }
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text("¿Es evento de alta afluencia público?")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = requiereStand,
                onCheckedChange = { requiereStand = it }
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text("Requiere espacio / stand de activación")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // NUEVO: --- 4. BOTONES DE CANCELAR Y GUARDAR EN FILA ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Botón Cancelar
            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            // Botón Guardar
            Button(
                onClick = {
                    if (nombreEvento.isNotBlank() && organizacion.isNotBlank()) {
                        // a) Creamos la Oportunidad
                        val nuevaOportunidad = Oportunidad(
                            id = (1..1000).random(),
                            nombre = nombreEvento,
                            tipoEvento = if (tipoEvento.isNotBlank()) tipoEvento else "Feria empresarial",
                            fechaDeteccion = "2026-10-16",
                            fechaInicio = "2026-11-01 09:00",
                            fechaTermino = "2026-11-01 18:00",
                            comuna = "Santiago",
                            publicoEsperado = 100,
                            prioridad = "Alta",
                            estado = "Identificada",
                            costoReferencial = 0,
                            observaciones = "Registro con alta afluencia: $esAltaAfluencia, Stand: $requiereStand",
                            idOrganizacion = 1
                        )

                        // b) Guardamos en el ViewModel
                        viewModel.agregarOportunidad(nuevaOportunidad)

                        // NUEVO: c) Mostramos la animación y volvemos a la lista
                        coroutineScope.launch {
                            mostrarConfirmacion = true
                            delay(2000) // Pausa de 2 segundos para que el usuario lea el mensaje
                            mostrarConfirmacion = false
                            onVolver() // Navegamos de vuelta
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                // El botón solo se habilita si hay texto en Nombre y Organización
                enabled = nombreEvento.isNotBlank() && organizacion.isNotBlank()
            ) {
                Text("Guardar")
            }
        }
    }
}

// --- VISTA PREVIA (Preview) ---
@Preview(showBackground = true)
@Composable
fun RegistroOportunidadPreview() {
    GestionOportunidadesTheme {
        // Le pasamos una función vacía {} al onVolver solo para que el Preview funcione
        RegistroOportunidadScreen(onVolver = {})
    }
}