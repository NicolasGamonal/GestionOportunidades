package duoc.example.gestionoportunidades.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import duoc.example.gestionoportunidades.GestionApp
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.ui.alertas.DashboardViewModel
import duoc.example.gestionoportunidades.ui.auth.AuthViewModel
import duoc.example.gestionoportunidades.ui.notas.NotasMarketingViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleOportunidadViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.FormularioOportunidadViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadesViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.DetalleOrganizacionViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioContactoViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.OrganizacionesViewModel
import duoc.example.gestionoportunidades.ui.resultados.FormularioResultadoViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.HistorialSeguimientoViewModel
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * Pruebas de "cableado" (Fase 4): comprueban que la fábrica AppViewModelProvider.Factory sabe crear
 * los 14 ViewModels de la app con el AppContainer real de GestionApp, y que los argumentos de la
 * ruta ("id", "idOportunidad", "idOrganizacion") llegan a los ViewModels por el SavedStateHandle.
 *
 * - Se usa una ComponentActivity de Robolectric como ViewModelStoreOwner: es lo mismo que hace
 *   Navigation Compose en la app. Los argumentos se pasan como extras del Intent de la Activity.
 * - MainDispatcherRule deja Dispatchers.Main en un dispatcher de prueba que nunca se avanza:
 *   las corrutinas del init de los ViewModels no corren, así que no se hace ninguna llamada a la red.
 */
@RunWith(RobolectricTestRunner::class)
class CableadoTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()

    private var controlador: ActivityController<ComponentActivity>? = null

    @After
    fun cerrarActividad() {
        // Al destruir la Activity se vacía su ViewModelStore y se cancelan los viewModelScope.
        controlador?.destroy()
    }

    // =========================================================================
    // Ayudas
    // =========================================================================

    private fun abrirActividad(argumentos: Intent.() -> Unit = {
        putExtra("id", 5L)
        putExtra("idOportunidad", 10L)
        putExtra("idOrganizacion", 1L)
    }): ComponentActivity {
        val intent = Intent().apply(argumentos)
        val nuevo = Robolectric.buildActivity(ComponentActivity::class.java, intent).create()
        controlador = nuevo
        return nuevo.get()
    }

    private inline fun <reified T : ViewModel> crear(actividad: ComponentActivity = abrirActividad()): T =
        ViewModelProvider(actividad, AppViewModelProvider.Factory)[T::class.java]

    private inline fun <reified T : ViewModel> assertLaFabricaCrea() {
        val vm = crear<T>()
        assertNotNull(vm)
        assertEquals(T::class.java, vm.javaClass)
    }

    // =========================================================================
    // La aplicación y el contenedor
    // =========================================================================

    @Test
    fun `la aplicacion de Robolectric es GestionApp`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()

        assertTrue(app is GestionApp)
    }

    @Test
    fun `el contenedor construye los 8 repositorios`() {
        val container = ApplicationProvider.getApplicationContext<GestionApp>().container

        assertNotNull(container.authRepository)
        assertNotNull(container.oportunidadRepository)
        assertNotNull(container.organizacionRepository)
        assertNotNull(container.contactoRepository)
        assertNotNull(container.seguimientoRepository)
        assertNotNull(container.evidenciaRepository)
        assertNotNull(container.resultadoRepository)
        assertNotNull(container.notaRepository)
    }

    @Test
    fun `el contenedor es unico y entrega siempre los mismos repositorios`() {
        val app = ApplicationProvider.getApplicationContext<GestionApp>()

        assertSame(app.container, app.container)
        assertSame(app.container.oportunidadRepository, app.container.oportunidadRepository)
    }

    // =========================================================================
    // Los 14 ViewModels
    // =========================================================================

    @Test fun `la fabrica crea AuthViewModel`() = assertLaFabricaCrea<AuthViewModel>()
    @Test fun `la fabrica crea DashboardViewModel`() = assertLaFabricaCrea<DashboardViewModel>()
    @Test fun `la fabrica crea SupervisorViewModel`() = assertLaFabricaCrea<SupervisorViewModel>()
    @Test fun `la fabrica crea OportunidadesViewModel`() = assertLaFabricaCrea<OportunidadesViewModel>()
    @Test fun `la fabrica crea DetalleOportunidadViewModel`() = assertLaFabricaCrea<DetalleOportunidadViewModel>()
    @Test fun `la fabrica crea FormularioOportunidadViewModel`() = assertLaFabricaCrea<FormularioOportunidadViewModel>()
    @Test fun `la fabrica crea OrganizacionesViewModel`() = assertLaFabricaCrea<OrganizacionesViewModel>()
    @Test fun `la fabrica crea DetalleOrganizacionViewModel`() = assertLaFabricaCrea<DetalleOrganizacionViewModel>()
    @Test fun `la fabrica crea FormularioOrganizacionViewModel`() = assertLaFabricaCrea<FormularioOrganizacionViewModel>()
    @Test fun `la fabrica crea FormularioContactoViewModel`() = assertLaFabricaCrea<FormularioContactoViewModel>()
    @Test fun `la fabrica crea HistorialSeguimientoViewModel`() = assertLaFabricaCrea<HistorialSeguimientoViewModel>()
    @Test fun `la fabrica crea FormularioResultadoViewModel`() = assertLaFabricaCrea<FormularioResultadoViewModel>()
    @Test fun `la fabrica crea FormularioSeguimientoViewModel`() = assertLaFabricaCrea<FormularioSeguimientoViewModel>()
    @Test fun `la fabrica crea NotasMarketingViewModel`() = assertLaFabricaCrea<NotasMarketingViewModel>()

    @Test
    fun `pedir dos veces el mismo ViewModel a la misma Activity devuelve la misma instancia`() {
        val actividad = abrirActividad()

        val primero = crear<AuthViewModel>(actividad)
        val segundo = crear<AuthViewModel>(actividad)

        assertSame(primero, segundo)
    }

    @Test
    fun `la fabrica no crea un ViewModel que no esta registrado`() {
        class ViewModelAjeno : ViewModel()
        val actividad = abrirActividad()

        assertThrows(IllegalArgumentException::class.java) {
            ViewModelProvider(actividad, AppViewModelProvider.Factory)[ViewModelAjeno::class.java]
        }
    }

    // =========================================================================
    // Argumentos de la ruta (SavedStateHandle)
    // =========================================================================

    @Test
    fun `el id de la ruta llega al formulario de organizacion y lo deja en modo edicion`() {
        val vm = crear<FormularioOrganizacionViewModel>(abrirActividad { putExtra("id", 5L) })

        assertTrue(vm.uiState.value.esEdicion)
    }

    @Test
    fun `sin id en la ruta el formulario de organizacion queda en modo creacion`() {
        val vm = crear<FormularioOrganizacionViewModel>(abrirActividad { })

        assertFalse(vm.uiState.value.esEdicion)
    }

    @Test
    fun `un ViewModel que exige idOportunidad falla si la ruta no lo trae`() {
        val actividad = abrirActividad { }

        assertThrows(IllegalStateException::class.java) {
            ViewModelProvider(actividad, AppViewModelProvider.Factory)[FormularioSeguimientoViewModel::class.java]
        }
    }

    @Test
    fun `un ViewModel que exige id falla si la ruta no lo trae`() {
        val actividad = abrirActividad { }

        assertThrows(IllegalStateException::class.java) {
            ViewModelProvider(actividad, AppViewModelProvider.Factory)[DetalleOportunidadViewModel::class.java]
        }
    }

    @Test
    fun `el formulario de contacto exige idOrganizacion`() {
        val actividad = abrirActividad { }

        assertThrows(IllegalStateException::class.java) {
            ViewModelProvider(actividad, AppViewModelProvider.Factory)[FormularioContactoViewModel::class.java]
        }
    }
}