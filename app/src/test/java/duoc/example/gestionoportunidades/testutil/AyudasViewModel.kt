package duoc.example.gestionoportunidades.testutil

import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

// =============================================================================
// Ayudas compartidas por las pruebas de ViewModels (Fase 3).
// =============================================================================

/** Usuario de prueba con el rol y el id indicados. */
fun usuarioPrueba(
    rol: String = "EJECUTIVO",
    id: Long = 5L,
    nombre: String = "Ana Pérez"
) = Usuario(
    idUsuario = id,
    username = "usuario$id",
    contrasena = "1234",
    nombreCompleto = nombre,
    rol = rol
)

/** Inicia la sesión global (SesionManager) con un usuario del rol indicado y lo devuelve. */
fun iniciarSesionComo(rol: String, id: Long = 5L, nombre: String = "Ana Pérez"): Usuario {
    val usuario = usuarioPrueba(rol, id, nombre)
    SesionManager.iniciarSesion(usuario)
    return usuario
}

/** SavedStateHandle con los argumentos de navegación indicados, por ejemplo `argumentos("id" to 5L)`. */
fun argumentos(vararg pares: Pair<String, Any?>): SavedStateHandle = SavedStateHandle(mapOf(*pares))

/**
 * Mantiene un suscriptor activo durante toda la prueba.
 *
 * Los StateFlow creados con `stateIn(..., WhileSubscribed(5_000), ...)` solo empiezan a trabajar cuando
 * alguien los recoge; sin suscriptor, `uiState.value` se queda con el valor inicial. El suscriptor vive en
 * `backgroundScope`, así que runTest lo cancela solo al terminar la prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.suscribir(flujo: Flow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect { } }
}