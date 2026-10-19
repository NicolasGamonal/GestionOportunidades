package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import kotlinx.coroutines.flow.*

// DetalleOrganizacionViewModel: lógica detrás de la pantalla de detalle de organización.
//
// Responsabilidad:
// - Cargar la organización en tiempo real (Flow reactivo desde Room).
// - Cargar los contactos de esa organización en tiempo real (Flow reactivo desde Room).
// - Calcular si el usuario puede editar la organización (permiso por rol).
// - Exponer todo como un único StateFlow que la Screen observa.
//
// Patrón MVVM:
// - Recibe 2 repositorios por inyección (desde AppViewModelProvider.Factory → AppContainer).
// - Expone un único StateFlow<DetalleOrganizacionUiState> que la Screen observa.
// - No conoce Composables ni navegación.
//
// ¿Por qué SavedStateHandle?
// Porque la ruta de navegación incluye un argumento "id" (ej: "detalle_organizacion/3").
// Navigation Compose lo pasa al ViewModel vía SavedStateHandle.
// El ViewModel lo extrae para saber QUÉ organización cargar.
// checkNotNull: si el argumento no existe por un bug, es mejor crashear que mostrar vacío.

// Estado que la Screen observa.
data class DetalleOrganizacionUiState(
    // La organización a mostrar (null = no encontrada o aún cargando)
    val organizacion: Organizacion? = null,
    // Contactos que pertenecen a esta organización (se actualiza en tiempo real vía Flow)
    val contactos: List<Contacto> = emptyList(),
    // true si el usuario puede editar la organización (solo SUPERVISOR, vía Permisos.kt).
    // La Screen usa este valor para mostrar/ocultar el botón "Editar organización".
    val puedeEditar: Boolean = false,
    // true mientras se carga la información inicial (muestra CircularProgressIndicator)
    val isLoading: Boolean = true
)

class DetalleOrganizacionViewModel(
    // SavedStateHandle: contiene el argumento "id" de la ruta de navegación
    // (el id de la organización a mostrar).
    savedStateHandle: SavedStateHandle,

    // Repositorio de organizaciones: para observar la organización en tiempo real.
    // No es private porque se usa dentro del combine{} (fuera del alcance de funciones del VM).
    organizacionRepository: OrganizacionRepository,

    // Repositorio de contactos: para observar los contactos de la organización en tiempo real.
    contactoRepository: ContactoRepository
) : ViewModel() {

    // El id de la organización a mostrar. Se extrae de la ruta de navegación.
    // checkNotNull: si por algún bug el argumento "id" no existe, lanza excepción
    // (es mejor crashear aquí que mostrar una pantalla vacía sin saber por qué).
    private val idOrganizacion: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    // El estado que la Screen observa.
    //
    // combine() de 2 Flows reactivos:
    // 1. organizacionRepository.observarPorId(idOrganizacion) → Flow<Organizacion?>
    //    Se actualiza solo cuando la organización cambia en Room
    //    (ej: el supervisor edita el nombre → la UI se actualiza sola).
    //
    // 2. contactoRepository.obtenerPorOrganizacion(idOrganizacion) → Flow<List<Contacto>>
    //    Se actualiza solo cuando se agrega, edita o elimina un contacto de esta org
    //    (ej: se crea un contacto nuevo → la lista se actualiza sola sin recargar).
    //
    // ¿Por qué combine y no 2 StateFlows separados?
    // Porque la Screen necesita ambos datos JUNTOS para dibujar la pantalla.
    // Al combinarlos, se evita que la Screen observe 2 fuentes por separado
    // y se reduzcan a 1 único estado coherente.
    //
    // Dentro del combine:
    // - puedeEditar: se calcula en CADA emisión (no se cachea) porque el rol
    //   podría cambiar si el usuario cierra sesión y abre otra (aunque en la práctica
    //   no pasa durante la vida de la pantalla).
    // - isLoading = false: una vez que el combine emite por primera vez, ya hay datos.
    //   (El valor inicial del stateIn es DetalleOrganizacionUiState() con isLoading = true,
    //   que se muestra como spinner hasta la primera emisión.)
    //
    // stateIn(WhileSubscribed(5_000)): se cancela 5 segundos después de que la Screen se va.
    // Ahorra recursos si el usuario navega a otra pantalla y no vuelve.
    val uiState: StateFlow<DetalleOrganizacionUiState> = combine(
        // Flow 1: la organización (se actualiza si se edita)
        organizacionRepository.observarPorId(idOrganizacion),
        // Flow 2: los contactos de la organización (se actualiza si se agrega/elimina uno)
        contactoRepository.obtenerPorOrganizacion(idOrganizacion)
    ) { organizacion, contactos ->
        // Construir el estado completo con ambos datos + permiso
        DetalleOrganizacionUiState(
            organizacion = organizacion,
            contactos = contactos,
            // Calcular el permiso de edición:
            // Permisos.puedeEditarOrganizacion(rol) → true solo si rol == "SUPERVISOR"
            // SesionManager.getRol(): obtiene el rol del usuario logueado (estado global)
            puedeEditar = Permisos.puedeEditarOrganizacion(SesionManager.getRol()),
            // Ya hay datos: se desactiva el spinner
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleOrganizacionUiState())
}