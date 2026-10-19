package duoc.example.gestionoportunidades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import duoc.example.gestionoportunidades.ui.navigation.NavegacionPrincipal
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme

/**
 * Única actividad de la app (arquitectura Single Activity): aplica el tema y
 * delega toda la navegación en NavegacionPrincipal.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestionOportunidadesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavegacionPrincipal()
                }
            }
        }
    }
}