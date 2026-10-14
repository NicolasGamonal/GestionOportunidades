package duoc.example.gestionoportunidades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestionOportunidadesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegistroOportunidadScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RegistroOportunidadScreen(modifier: Modifier = Modifier) {
    // 1. Manejo de Estado (remember + mutableStateOf)
    var nombreEvento by remember { mutableStateOf("") }
    var organizacion by remember { mutableStateOf("") }
    var tipoEvento by remember { mutableStateOf("") }

    // 2. Layout Principal en Column con Modifiers de tamaño y padding
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

        // Campo 1: Nombre del Evento / Oportunidad
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

        // Campo 3: Tipo de Actividad
        OutlinedTextField(
            value = tipoEvento,
            onValueChange = { tipoEvento = it },
            label = { Text("Tipo de Evento (ej. Feria, Jornada)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Botón de Registrar Acción
        Button(
            onClick = {
                // Aquí conectaremos la lógica de guardado más adelante
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Oportunidad")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistroOportunidadPreview() {
    GestionOportunidadesTheme {
        RegistroOportunidadScreen()
    }
}