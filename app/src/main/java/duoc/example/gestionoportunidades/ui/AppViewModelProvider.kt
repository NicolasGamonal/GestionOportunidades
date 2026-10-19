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
import duoc.example.gestionoportunidades.ui.oportunidades.FormularioOportunidadViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadesViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.DetalleOrganizacionViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioContactoViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.OrganizacionesViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.HistorialSeguimientoViewModel

// Factory de ViewModels: inyecta los repositorios (desde AppContainer) a cada ViewModel.
// Se usa en Navegacion.kt como: viewModel(factory = AppViewModelProvider.Factory)
// Sin este factory, los ViewModels no tendrían acceso a los repositorios (no se auto-inyectan).

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { AuthViewModel(app().container.authRepository) }
        initializer {
            DashboardViewModel(
                app().container.oportunidadRepository,
                app().container.seguimientoRepository
            )
        }
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
                app().container.authRepository,
                app().container.evidenciaRepository
            )
        }
        initializer {
            FormularioOportunidadViewModel(
                createSavedStateHandle(),
                app().container.oportunidadRepository,
                app().container.organizacionRepository
            )
        }
        initializer { OrganizacionesViewModel(app().container.organizacionRepository) }
        initializer {
            DetalleOrganizacionViewModel(
                createSavedStateHandle(),
                app().container.organizacionRepository,
                app().container.contactoRepository
            )
        }
        initializer {
            FormularioOrganizacionViewModel(createSavedStateHandle(), app().container.organizacionRepository)
        }
        initializer {
            FormularioContactoViewModel(
                createSavedStateHandle(),
                app().container.contactoRepository,
                app().container.organizacionRepository
            )
        }
        initializer {
            HistorialSeguimientoViewModel(
                createSavedStateHandle(),
                app().container.oportunidadRepository,
                app().container.seguimientoRepository,
                app().container.authRepository,
                app().container.contactoRepository
            )
        }
        initializer {
            FormularioSeguimientoViewModel(
                createSavedStateHandle(),
                app().container.seguimientoRepository,
                app().container.oportunidadRepository,
                app().container.contactoRepository
            )
        }
    }
}

// Extrae la aplicación (GestionApp) de los CreationExtras.
// GestionApp extiende Application y contiene el AppContainer (con todos los repositorios).
private fun CreationExtras.app(): GestionApp = this[APPLICATION_KEY] as GestionApp