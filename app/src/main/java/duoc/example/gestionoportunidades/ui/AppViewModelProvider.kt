package duoc.example.gestionoportunidades.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import duoc.example.gestionoportunidades.GestionApp
import duoc.example.gestionoportunidades.ui.alertas.DashboardViewModel
import duoc.example.gestionoportunidades.ui.auth.AuthViewModel
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { AuthViewModel(app().container.authRepository) }
        initializer { DashboardViewModel(app().container.oportunidadRepository) }
        initializer {
            SupervisorViewModel(
                app().container.oportunidadRepository,
                app().container.authRepository
            )
        }
        // aquí agregarás los tuyos (Oportunidades, OrgContactos, Seguimiento) en J2 a J4
    }
}

private fun CreationExtras.app(): GestionApp = this[APPLICATION_KEY] as GestionApp