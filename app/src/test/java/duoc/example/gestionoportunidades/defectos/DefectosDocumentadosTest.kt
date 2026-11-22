package duoc.example.gestionoportunidades.defectos

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.contactoRemoto
import duoc.example.gestionoportunidades.testutil.oportunidadLocal
import duoc.example.gestionoportunidades.testutil.orgRemota
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.seguimientoRemoto
import duoc.example.gestionoportunidades.ui.alertas.DashboardUiState
import duoc.example.gestionoportunidades.ui.alertas.DashboardViewModel
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioContactoViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoViewModel
import duoc.example.gestionoportunidades.util.Recordatorios
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Defectos documentados del plan que atraviesan varias capas (Fase 4).
 *
 * Aquí se usan los repositorios REALES con un ApiService simulado y los ViewModels REALES, para
 * mostrar el defecto tal como lo vive el usuario: lo que responde la API llega hasta el estado de la pantalla.
 *
 * Los otros dos defectos documentados ya tienen su prueba en otro archivo:
 *  - Defecto 2 (se pierden horas y región al guardar): OportunidadRepositoryTest.
 *  - Defecto 4 (validarFecha acepta fechas inexistentes): ValidacionesTest.
 *
 * Cómo se lee este archivo: cada defecto tiene una prueba "control" (camino feliz, pasa hoy) y una
 * prueba del defecto, que describe el comportamiento CORRECTO, falla hoy y por eso queda en @Ignore.
 * La prueba de control demuestra que el montaje de la prueba es correcto: la única diferencia entre
 * ambas es que la API falla.
 *
 * El equipo decide: corregir el código y quitar el @Ignore de la prueba, o dejarla ignorada
 * documentando el defecto.
 *
 *  - Defecto 1: un guardado fallido se muestra como éxito. Los repositorios atrapan toda excepción y
 *    devuelven 0L (insertar) o Unit (actualizar), y los formularios solo miran si hubo excepción.
 *  - Defecto 3: un error de red se ve igual que "sin datos". Los Flow emiten lista vacía al fallar
 *    y DashboardUiState.error nunca se asigna.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefectosDocumentadosTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val api = mockk<ApiService>()
    private val contexto = mockk<Context>(relaxed = true)

    @Before
    fun prepararSimulaciones() {
        // Recordatorios real usa AlarmManager: aquí solo interesa saber si el formulario lo llama.
        mockkObject(Recordatorios)
        every { Recordatorios.programarSeguimiento(any(), any(), any(), any(), any()) } just Runs
    }

    @After
    fun limpiarSimulaciones() {
        unmockkObject(Recordatorios)
    }

    // =========================================================================
    // Ayudas
    // =========================================================================

    private fun iniciarSesion(rol: String, id: Long) {
        SesionManager.iniciarSesion(
            Usuario(idUsuario = id, username = "u$id", contrasena = "clave", nombreCompleto = "Usuario $id", rol = rol)
        )
    }

    private fun fechaFutura(): String =
        LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    // --- Seguimiento ---------------------------------------------------------

    private fun formularioSeguimiento(): FormularioSeguimientoViewModel {
        val oportunidadRepo = mockk<OportunidadRepository>()
        val contactoRepo = mockk<ContactoRepository>()
        coEvery { oportunidadRepo.obtenerPorId(10L) } returns
                oportunidadLocal(idOportunidad = 10L, idResponsable = 5L, idOrganizacion = 1L)
        every { contactoRepo.obtenerPorOrganizacion(1L) } returns flowOf(emptyList())
        return FormularioSeguimientoViewModel(
            SavedStateHandle(mapOf("idOportunidad" to 10L)),
            SeguimientoRepository(api),   // repositorio real
            oportunidadRepo,
            contactoRepo,
            contexto
        )
    }

    private fun TestScope.seguimientoListoParaGuardar(): FormularioSeguimientoViewModel {
        iniciarSesion("EJECUTIVO", 5L)
        val vm = formularioSeguimiento()
        advanceUntilIdle()
        vm.onTipoChange("Llamada")
        vm.onResultadoChange("Contacto interesado en la propuesta")
        vm.onProximaAccionChange("Enviar propuesta")
        vm.onFechaProximaChange(fechaFutura())
        return vm
    }

    // --- Organización --------------------------------------------------------

    private fun llenarOrganizacion(vm: FormularioOrganizacionViewModel) {
        vm.onNombreChange("Municipalidad de Puente Alto")
        vm.onTipoChange("Municipalidad")
        vm.onRubroChange("Gobierno local")
        vm.onComunaChange("Puente Alto")
    }

    /** Formulario de edición de la organización 3, ya cargado desde la API simulada. */
    private fun TestScope.organizacionEnEdicion(): FormularioOrganizacionViewModel {
        iniciarSesion("SUPERVISOR", 99L)
        coEvery { api.listarOrganizaciones() } returns listOf(orgRemota(id = 3L))
        val vm = FormularioOrganizacionViewModel(
            SavedStateHandle(mapOf("id" to 3L)),
            OrganizacionRepository(api)
        )
        advanceUntilIdle()
        return vm
    }

    // --- Contacto ------------------------------------------------------------

    private fun TestScope.contactoListoParaGuardar(): FormularioContactoViewModel {
        coEvery { api.listarOrganizaciones() } returns listOf(orgRemota(id = 1L))
        val vm = FormularioContactoViewModel(
            SavedStateHandle(mapOf("idOrganizacion" to 1L)),
            ContactoRepository(api),
            OrganizacionRepository(api)
        )
        advanceUntilIdle()
        vm.onNombreChange("Carla Rojas")
        vm.onCargoChange("Encargada de eventos")
        vm.onAreaChange("Extensión")
        vm.onCorreoChange("carla@muni.cl")
        vm.onTelefonoChange("+56911112222")
        return vm
    }

    // --- Dashboard -----------------------------------------------------------

    /** Crea el Dashboard, lo deja suscrito (el estado solo se calcula con un colector) y lee su estado. */
    private fun TestScope.estadoDelDashboard(): DashboardUiState {
        iniciarSesion("SUPERVISOR", 99L)
        val vm = DashboardViewModel(OportunidadRepository(api), SeguimientoRepository(api))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        advanceUntilIdle()
        return vm.uiState.value
    }

    // =========================================================================
    // DEFECTO 1 — Un guardado fallido se muestra como éxito
    // =========================================================================

    // --- Nueva gestión (SeguimientoRepository.insertar devuelve 0L) -----------

    @Test
    fun `control - nueva gestion guardada por la API queda como exitosa y programa el recordatorio`() = runTest {
        coEvery { api.crearSeguimiento(any()) } returns Response.success(seguimientoRemoto(id = 77L))
        val vm = seguimientoListoParaGuardar()

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        assertNull(vm.uiState.value.mensajeError)
        verify(exactly = 1) { Recordatorios.programarSeguimiento(any(), 77L, any(), any(), any()) }
    }

    @Test
    fun `DEFECTO 1 - nueva gestion rechazada por la API no debe mostrarse como guardada`() = runTest {
        coEvery { api.crearSeguimiento(any()) } returns respuestaError(500, """{"mensaje":"error interno"}""")
        val vm = seguimientoListoParaGuardar()

        vm.guardar()
        advanceUntilIdle()

        assertFalse("No debe marcarse como guardado si la API falló", vm.uiState.value.guardadoExitoso)
        assertNotNull("Debe avisar al usuario que no se pudo guardar", vm.uiState.value.mensajeError)
        verify(exactly = 0) { Recordatorios.programarSeguimiento(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `DEFECTO 1 - nueva gestion sin conexion no debe mostrarse como guardada`() = runTest {
        coEvery { api.crearSeguimiento(any()) } throws IOException("sin red")
        val vm = seguimientoListoParaGuardar()

        vm.guardar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.guardadoExitoso)
        assertNotNull(vm.uiState.value.mensajeError)
    }

    // --- Nueva organización (OrganizacionRepository.insertar devuelve 0L) -----

    @Test
    fun `control - organizacion creada por la API queda como guardada`() = runTest {
        coEvery { api.crearOrganizacion(any()) } returns Response.success(orgRemota(id = 3L))
        val vm = FormularioOrganizacionViewModel(SavedStateHandle(), OrganizacionRepository(api))
        llenarOrganizacion(vm)

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        assertNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun `DEFECTO 1 - organizacion rechazada por la API no debe mostrarse como guardada`() = runTest {
        coEvery { api.crearOrganizacion(any()) } returns respuestaError(400, """{"mensaje":"datos inválidos"}""")
        val vm = FormularioOrganizacionViewModel(SavedStateHandle(), OrganizacionRepository(api))
        llenarOrganizacion(vm)

        vm.guardar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.guardadoExitoso)
        assertNotNull(vm.uiState.value.mensajeError)
    }

    // --- Editar organización (OrganizacionRepository.actualizar devuelve Unit) -

    @Test
    fun `control - organizacion actualizada por la API queda como guardada`() = runTest {
        val vm = organizacionEnEdicion()
        coEvery { api.actualizarOrganizacion(any(), any()) } returns Response.success(orgRemota(id = 3L))

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        assertNull(vm.uiState.value.mensajeError)
    }

    @Ignore("DEFECTO 1: OrganizacionRepository.actualizar atrapa la excepción de red y devuelve Unit; el formulario muestra éxito")
    @Test
    fun `DEFECTO 1 - organizacion que no se pudo actualizar por falta de red no debe mostrarse como guardada`() = runTest {
        val vm = organizacionEnEdicion()
        coEvery { api.actualizarOrganizacion(any(), any()) } throws IOException("sin red")

        vm.guardar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.guardadoExitoso)
        assertNotNull(vm.uiState.value.mensajeError)
    }

    // --- Nuevo contacto (ContactoRepository.insertar devuelve 0L) -------------

    @Test
    fun `control - contacto creado por la API queda como guardado`() = runTest {
        coEvery { api.crearContacto(any()) } returns Response.success(contactoRemoto(id = 20L))
        val vm = contactoListoParaGuardar()

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        assertNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun `DEFECTO 1 - contacto rechazado por la API no debe mostrarse como guardado`() = runTest {
        coEvery { api.crearContacto(any()) } returns respuestaError(500, """{"mensaje":"error interno"}""")
        val vm = contactoListoParaGuardar()

        vm.guardar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.guardadoExitoso)
        assertNotNull(vm.uiState.value.mensajeError)
    }

    // =========================================================================
    // DEFECTO 3 — Un error de red se ve igual que "sin datos"
    // =========================================================================

    @Test
    fun `control - el dashboard sin oportunidades termina de cargar y no informa error`() = runTest {
        coEvery { api.listarOportunidades(any()) } returns emptyList()

        val estado = estadoDelDashboard()

        assertFalse(estado.isLoading)
        assertNull(estado.error)
        assertTrue(estado.eventosProximos.isEmpty())
    }

    @Ignore("DEFECTO 3: los Flow emiten lista vacía cuando falla la red y DashboardUiState.error nunca se asigna")
    @Test
    fun `DEFECTO 3 - el dashboard sin conexion debe informar el error y no mostrarse como sin datos`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        val estado = estadoDelDashboard()

        assertNotNull("Un error de red debe quedar en DashboardUiState.error", estado.error)
    }
}