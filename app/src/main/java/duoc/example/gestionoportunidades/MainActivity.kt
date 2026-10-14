package duoc.example.gestionoportunidades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import duoc.example.gestionoportunidades.ui.RegistroOportunidadScreen
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme

/**
 * ACTIVIDAD PRINCIPAL (MainActivity)
 *
 * En el desarrollo moderno con Jetpack Compose seguimos la arquitectura "Single Activity".
 * Esta clase es el ÚNICO punto de entrada de la app y cumple con:
 * 1. Inicializar el contenedor gráfico de Compose con setContent.
 * 2. Aplicar el tema visual general de la aplicación (GestionOportunidadesTheme).
 * 3. Envolver la vista en un Scaffold para respetar los bordes seguros de la pantalla.
 * 4. Invocar a la pantalla RegistroOportunidadScreen() ubicada en el paquete 'ui'.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita que la aplicación se expanda de borde a borde en la pantalla
        enableEdgeToEdge()

        // Dibuja el contenido gráfico con Compose
        setContent {
            GestionOportunidadesTheme {
                // Scaffold provee la estructura de diseño base y calcula los márgenes de ventana (innerPadding)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Invocamos la pantalla principal pasándole el margen seguro
                    RegistroOportunidadScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}