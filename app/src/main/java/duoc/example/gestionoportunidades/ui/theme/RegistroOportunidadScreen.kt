package duoc.example.gestionoportunidades.ui


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
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme

/**
 * PANTALLA DE REGISTRO DE OPORTUNIDADES COMERCIALES
 *
 * Esta función @Composable representa la interfaz gráfica completa del formulario.
 * Incorpora los conceptos clave de la Clase 2 de Jetpack Compose:
 * 1. Manejo de estados (remember + mutableStateOf) para texto y valores booleanos.
 * 2. Maquetación estructurada con Column (vertical) y Row (horizontal).
 * 3. Controles interactivos: OutlinedTextField, Switch, Checkbox y Button.
 * 4. Impresión por consola (println) dentro del evento onClick.
 */
@Composable
fun RegistroOportunidadScreen(modifier: Modifier = Modifier) {

    // --- 1. ESTADOS REACTIVOS DE LA INTERFAZ ---
    // 'remember' conserva el estado en memoria durante las recomposiciones.
    // 'mutableStateOf' notifica a Compose cuando un valor cambia para redibujar la UI automáticamente.

    // Campos de texto del formulario (Clase 1 y 2)
    var nombreEvento by remember { mutableStateOf("") }
    var organizacion by remember { mutableStateOf("") }
    var correoContacto by remember { mutableStateOf("") }

    // Estados booleanos para los controles de selección de la Clase 2 (Switch y Checkbox)
    var esAltaAfluencia by remember { mutableStateOf(false) }
    var requiereStand by remember { mutableStateOf(false) }

    // --- 2. ESTRUCTURA CONTENEDORA PRINCIPAL (Column) ---
    // Organiza todos los elementos gráficos de forma vertical (de arriba a abajo).
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Título estilizado con la tipografía del tema de la app
        Text(
            text = "Registro de Oportunidad Comercial",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CAMPO 1: Nombre del Evento u Oportunidad
        OutlinedTextField(
            value = nombreEvento,
            onValueChange = { nombreEvento = it },
            label = { Text("Nombre del Evento / Oportunidad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // CAMPO 2: Empresa u Organización Ficticia
        OutlinedTextField(
            value = organizacion,
            onValueChange = { organizacion = it },
            label = { Text("Organización / Empresa Ficticia") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // CAMPO 3: Correo de Contacto Corporativo
        OutlinedTextField(
            value = correoContacto,
            onValueChange = { correoContacto = it },
            label = { Text("Correo Electrónico de Contacto") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- CONTROL 1: SWITCH + ETIQUETA (Disposición Horizontal con Row) ---
        // Row alinea el Switch y el Texto uno al lado del otro
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

        // --- CONTROL 2: CHECKBOX + ETIQUETA (Disposición Horizontal con Row) ---
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

        // --- BOTÓN DE ACCIÓN CON LOGCAT / CONSOLA ---
        Button(
            onClick = {
                // Requerimiento de Clase 2: Impresión por consola de todos los estados actuales
                println("=== NUEVA OPORTUNIDAD REGISTRADA ===")
                println("Nombre Evento: $nombreEvento")
                println("Organización: $organizacion")
                println("Correo Contacto: $correoContacto")
                println("Alta Afluencia (Switch): $esAltaAfluencia")
                println("Requiere Stand (Checkbox): $requiereStand")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Oportunidad")
        }
    }
}

// --- VISTA PREVIA PARA ANDROID STUDIO ---
// Permite previsualizar la pantalla en el panel Split/Design sin gastar recursos cargando el emulador.
@Preview(showBackground = true)
@Composable
fun RegistroOportunidadPreview() {
    GestionOportunidadesTheme {
        RegistroOportunidadScreen()
    }
}