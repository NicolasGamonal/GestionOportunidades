package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// Importamos la función viewModel() que provee la instancia del ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import duoc.example.gestionoportunidades.model.Oportunidad

// Importaciones de nuestras clases creadas en el proyecto
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme

/**
 * PANTALLA DE REGISTRO INTEGRADA CON VIEWMODEL (PASO 3)
 *
 * La pantalla ahora recibe una instancia de 'OportunidadViewModel'.
 * Esto nos permite delegar la responsabilidad del almacenamiento al ViewModel.
 */
@Composable
fun RegistroOportunidadScreen(
    modifier: Modifier = Modifier,
    // Inyectamos el ViewModel usando la función predeterminada de Compose
    viewModel: OportunidadViewModel = viewModel()
) {
    // --- 1. ESTADOS LOCALES DE LOS CAMPOS DE TEXTO ---
    var nombreEvento by remember { mutableStateOf("") }
    var organizacion by remember { mutableStateOf("") }
    var correoContacto by remember { mutableStateOf("") }
    var tipoEvento by remember { mutableStateOf("") }

    // --- 2. ESTADOS LOCALES BOOLEANOS (SWITCH Y CHECKBOX) ---
    var esAltaAfluencia by remember { mutableStateOf(false) }
    var requiereStand by remember { mutableStateOf(false) }

    // --- 3. DISEÑO DE LA PANTALLA (Column) ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Registro de Oportunidad Comercial",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo 1: Nombre del Evento
        OutlinedTextField(
            value = nombreEvento,
            onValueChange = { nombreEvento = it },
            label = { Text("Nombre del Evento / Oportunidad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo 2: Empresa u Organización
        OutlinedTextField(
            value = organizacion,
            onValueChange = { organizacion = it },
            label = { Text("Organización / Empresa Ficticia") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo 3: Correo del Contacto
        OutlinedTextField(
            value = correoContacto,
            onValueChange = { correoContacto = it },
            label = { Text("Correo Electrónico de Contacto") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo 4: Tipo de Evento
        OutlinedTextField(
            value = tipoEvento,
            onValueChange = { tipoEvento = it },
            label = { Text("Tipo de Evento (ej. Feria, Jornada, Municipal)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control 1: Switch (Alta Afluencia)
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

        // Control 2: Checkbox (Requiere Stand)
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

        // --- 4. ACCIÓN DEL BOTÓN CON INTEGRACIÓN AL VIEWMODEL ---
        Button(
            onClick = {
                // Validación básica: al menos Nombre y Organización deben tener texto
                if (nombreEvento.isNotBlank() && organizacion.isNotBlank()) {

                    // a) Instanciamos la Data Class con la información del formulario
                    // a) Instanciamos la Data Class con la información real del formulario en 3FN
                    val nuevaOportunidad = Oportunidad(
                        idOportunidad = 0, // Room lo autogenera automáticamente gracias a @PrimaryKey(autoGenerate = true)
                        nombre = nombreEvento, // Cambiado de 'nombreEvento' a 'nombre'
                        tipoEvento = if (tipoEvento.isNotBlank()) tipoEvento else "Feria empresarial",
                        fechaDeteccion = System.currentTimeMillis(), // Guarda la fecha/hora actual en milisegundos
                        fechaInicio = System.currentTimeMillis(),    // Reemplazar por la variable del DatePicker de Josue
                        fechaTermino = System.currentTimeMillis() + 86400000, // Próximo día por defecto
                        comuna = "Santiago", // Campo obligatorio de la checklist (Ubicación)
                        publicoEsperado = 100, // Reemplazar por la variable numérica del formulario
                        prioridad = "ALTA",  // Reemplazar por la variable del Dropdown o RadioButton
                        estado = "Identificada",
                        costoReferencial = 0.0, // Reemplazar por el Double de la caja de texto
                        observaciones = "Sin observaciones iniciales",
                        idOrganizacion = 1L, // Llave foránea obligatoria (Debe apuntar a una Organización existente)
                        idResponsable = null // Inicialmente sin asignar hasta que actúe el Supervisor Comercial
                    )


                    // b) Guardamos el objeto en la lista del ViewModel
                    viewModel.agregarOportunidad(nuevaOportunidad)

                    // c) REQUERIMIENTO UX: Limpiamos los campos del formulario tras registrar
                    nombreEvento = ""
                    organizacion = ""
                    correoContacto = ""
                    tipoEvento = ""
                    esAltaAfluencia = false
                    requiereStand = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Oportunidad")
        }
    }
}



// --- VISTA PREVIA (Preview) ---
@Preview(showBackground = true)
@Composable
fun RegistroOportunidadPreview() {
    GestionOportunidadesTheme {
        RegistroOportunidadScreen()
    }
}