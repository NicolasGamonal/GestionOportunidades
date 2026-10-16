package duoc.example.gestionoportunidades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.ui.auth.LoginScreen
import duoc.example.gestionoportunidades.ui.auth.AuthViewModel
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorViewModel
import duoc.example.gestionoportunidades.ui.alertas.DashboardViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadViewModel
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestionOportunidadesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val database = AppDatabase.getDatabase(applicationContext)

                    val authRepository = AuthRepository(database.usuarioDao())
                    val oportunidadRepository = OportunidadRepository(
                        database.oportunidadDao(),
                        database.organizacionDao(),
                        database.contactoDao(),
                        database.seguimientoDao()
                    )

                    // Usa la fábrica local definida abajo en el mismo archivo
                    val authViewModel: AuthViewModel = viewModel(
                        factory = LocalViewModelFactory(authRepository, oportunidadRepository)
                    )

                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginExitoso = { rol, idUsuario ->
                            println("--- LOGIN EXITOSO LOCAL ---")
                            println("ID: $idUsuario - Rol: $rol")
                        }
                    )
                }
            }
        }
    }
}

// Fábrica local unificada para evitar problemas de paquetes o imports cruzados
@Suppress("UNCHECKED_CAST")
class LocalViewModelFactory(
    private val authRepository: AuthRepository,
    private val oportunidadRepository: OportunidadRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(OportunidadViewModel::class.java) -> {
                OportunidadViewModel(oportunidadRepository) as T
            }
            modelClass.isAssignableFrom(SupervisorViewModel::class.java) -> {
                SupervisorViewModel(oportunidadRepository, authRepository) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(oportunidadRepository) as T
            }
            else -> throw IllegalArgumentException("ViewModel Desconocido: ${modelClass.name}")
        }
    }
}
