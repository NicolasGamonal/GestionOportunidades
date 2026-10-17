package duoc.example.gestionoportunidades.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import duoc.example.gestionoportunidades.GestionApp
import duoc.example.gestionoportunidades.ui.alertas.DashboardViewModel
import duoc.example.gestionoportunidades.ui.auth.AuthViewModel
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorViewModel
import androidx.lifecycle.createSavedStateHandle
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleOportunidadViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadesViewModel

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
        initializer { OportunidadesViewModel(app().container.oportunidadRepository) }
        initializer {
            DetalleOportunidadViewModel(
                createSavedStateHandle(),
                app().container.oportunidadRepository,
                app().container.organizacionRepository,
                app().container.authRepository
            )
        }
    }
}

private fun CreationExtras.app(): GestionApp = this[APPLICATION_KEY] as GestionApp