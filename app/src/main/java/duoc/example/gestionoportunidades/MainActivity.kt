// 1. DECLARACIÓN DEL PAQUETE E IMPORTACIONES
// Ubicación del archivo dentro de la estructura de carpetas del proyecto.
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

// 2. ACTIVIDAD PRINCIPAL (Punto de entrada de la aplicación)
// Bajo la arquitectura de Jetpack Compose usamos "Single Activity": sólo tenemos esta MainActivity.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita que la app use toda la pantalla hasta los bordes del dispositivo
        enableEdgeToEdge()

        // setContent reemplaza al antiguo setContentView(R.layout.xml).
        // Aquí le decimos a Android qué pantalla Dibujar usando Compose.
        setContent {
            // Aplicamos el tema visual configurado para el proyecto
            GestionOportunidadesTheme {
                // Scaffold provee la estructura básica de pantalla (barras, márgenes seguros)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Llamamos a nuestra pantalla pasando el padding seguro para no chocar con la barra de estado
                    RegistroOportunidadScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// 3. FUNCIÓN COMPOSE (Interfaz Declarativa)
// La anotación @Composable indica a Kotlin que esta función genera elementos de UI.
// Nota: Por convención siempre debe empezar con Mayúscula (RegistroOportunidadScreen).
@Composable
fun RegistroOportunidadScreen(modifier: Modifier = Modifier) {

    // --- CONCEPTO CLAVE DE LA CLASE 1: MANEJO DE ESTADO ---
    // 'remember' mantiene el valor en memoria durante las recomposiciones de la pantalla.
    // 'mutableStateOf' hace que la variable sea reactiva: si el usuario escribe, Compose actualiza la UI automáticamente.
    var nombreEvento by remember { mutableStateOf("") }
    var organizacion by remember { mutableStateOf("") }
    var tipoEvento by remember { mutableStateOf("") }

    // --- MAQUETACIÓN EN CONTENEDORES (COLUMN) ---
    // Column organiza a sus hijos de forma vertical (uno debajo de otro).
    // Usamos Modifiers para ajustar el tamaño completo (fillMaxSize) y márgenes internos (padding 24.dp).
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Título principal con estilo tipográfico del tema de la app
        Text(
            text = "Registro de Oportunidad Comercial",
            style = MaterialTheme.typography.headlineSmall
        )

        // Spacer agrega espacio vertical invisible de 16dp entre componentes
        Spacer(modifier = Modifier.height(16.dp))

        // --- CAMPO DE ENTRADA 1 (Alineado al Caso: Nombre de la Oportunidad/Evento) ---
        OutlinedTextField(
            value = nombreEvento, // Muestra el estado actual
            onValueChange = { nombreEvento = it }, // Actualiza el estado cuando el usuario escribe
            label = { Text("Nombre del Evento / Oportunidad") },
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- CAMPO DE ENTRADA 2 (Alineado al Caso: Empresa u Organización Ficticia) ---
        OutlinedTextField(
            value = organizacion,
            onValueChange = { organizacion = it },
            label = { Text("Organización / Empresa Ficticia") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- CAMPO DE ENTRADA 3 (Alineado al Caso: Tipo de Evento) ---
        OutlinedTextField(
            value = tipoEvento,
            onValueChange = { tipoEvento = it },
            label = { Text("Tipo de Evento (ej. Feria, Jornada)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- BOTÓN DE ACCIÓN ---
        Button(
            onClick = {
                // Aquí en el futuro se conectará el ViewModel o la Base de Datos Local
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Oportunidad")
        }
    }
}

// 4. VISTA PREVIA (Preview)
// Permite visualizar el diseño en tiempo real en la pestaña Split/Design de Android Studio
// sin necesidad de ejecutar el emulador (ahorrando memoria RAM y procesador).
@Preview(showBackground = true)
@Composable
fun RegistroOportunidadPreview() {
    GestionOportunidadesTheme {
        RegistroOportunidadScreen()
    }
}