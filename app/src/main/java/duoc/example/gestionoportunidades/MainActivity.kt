package duoc.example.gestionoportunidades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import duoc.example.gestionoportunidades.ui.RegistroOportunidadScreen
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme
import duoc.example.gestionoportunidades.ui.navigation.NavegacionPrincipal
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
                // 2. Usamos un Surface para que el fondo tome el color "background" de tu tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. ¡Aquí llamamos a la navegación que acabas de crear!
                    NavegacionPrincipal()
                }
            }
        }
    }
}