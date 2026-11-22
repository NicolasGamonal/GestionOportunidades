package duoc.example.gestionoportunidades.ui.supervisor

import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.iniciarSesionComo
import duoc.example.gestionoportunidades.testutil.oportunidadLocal
import duoc.example.gestionoportunidades.testutil.usuarioPrueba
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import duoc.example.gestionoportunidades.util.Catalogos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SupervisorViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()

    private val oportunidadRepo = mockk<OportunidadRepository>()
    private val authRepo = mockk<AuthRepository>()

    private val ejecutivos = listOf(
        usuarioPrueba("EJECUTIVO", id = 5L, nombre = "Ana Pérez"),
        usuarioPrueba("EJECUTIVO", id = 6L, nombre = "Luis Soto")
    )

    private fun crearVm(listaEjecutivos: List<Usuario> = ejecutivos): SupervisorViewModel {
        every { authRepo.obtenerEjecutivos() } returns flowOf(listaEjecutivos)
        return SupervisorViewModel(oportunidadRepo, authRepo)
    }

    // =========================================================================
    // Construcción: carga de ejecutivos
    // =========================================================================

    @Test
    fun `el estado inicial es cargando y sin mensajes`() {
        val vm = crearVm()

        assertEquals(SupervisorUiState(), vm.uiState.value)
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `al crearse carga la lista de ejecutivos`() = runTest {
        val vm = crearVm()

        advanceUntilIdle()

        assertEquals(ejecutivos, vm.uiState.value.ejecutivos)
        verify(exactly = 1) { authRepo.obtenerEjecutivos() }
    }

    @Test
    fun `si la lista de ejecutivos cambia el estado se actualiza`() = runTest {
        val flujo = MutableStateFlow(ejecutivos)
        every { authRepo.obtenerEjecutivos() } returns flujo
        val vm = SupervisorViewModel(oportunidadRepo, authRepo)
        advanceUntilIdle()

        flujo.value = listOf(usuarioPrueba("EJECUTIVO", id = 7L, nombre = "Eva Mora"))
        advanceUntilIdle()

        assertEquals(listOf(usuarioPrueba("EJECUTIVO", id = 7L, nombre = "Eva Mora")), vm.uiState.value.ejecutivos)
    }

    // =========================================================================
    // inicializarPanel
    // =========================================================================

    @Test
    fun `inicializarPanel carga todas las oportunidades con la busqueda vacia por defecto`() = runTest {
        val lista = listOf(oportunidadLocal(idOportunidad = 1L), oportunidadLocal(idOportunidad = 2L))
        every { oportunidadRepo.obtenerTodasLasOportunidades("") } returns flowOf(lista)
        val vm = crearVm()

        vm.inicializarPanel()
        advanceUntilIdle()

        assertEquals(lista, vm.uiState.value.listaOportunidadesGlobal)
        assertFalse(vm.uiState.value.isLoading)
        verify(exactly = 1) { oportunidadRepo.obtenerTodasLasOportunidades("") }
    }

    @Test
    fun `inicializarPanel con busqueda la pasa al repositorio`() = runTest {
        val lista = listOf(oportunidadLocal(idOportunidad = 1L, nombre = "Feria Sur"))
        every { oportunidadRepo.obtenerTodasLasOportunidades("feria") } returns flowOf(lista)
        val vm = crearVm()

        vm.inicializarPanel("feria")
        advanceUntilIdle()

        assertEquals(lista, vm.uiState.value.listaOportunidadesGlobal)
        verify(exactly = 1) { oportunidadRepo.obtenerTodasLasOportunidades("feria") }
    }

    @Test
    fun `inicializarPanel marca cargando mientras llegan los datos`() = runTest {
        val flujo = MutableStateFlow(listOf(oportunidadLocal(idOportunidad = 1L)))
        every { oportunidadRepo.obtenerTodasLasOportunidades(any()) } returns flujo
        val vm = crearVm()
        vm.inicializarPanel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)           // ya llegaron los datos

        vm.inicializarPanel()

        // Antes de que corra la corrutina, el estado vuelve a "cargando".
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `inicializarPanel cancela la busqueda anterior`() = runTest {
        val flujoViejo = MutableStateFlow(listOf(oportunidadLocal(idOportunidad = 1L, nombre = "Vieja")))
        val flujoNuevo = MutableStateFlow(listOf(oportunidadLocal(idOportunidad = 2L, nombre = "Nueva")))
        every { oportunidadRepo.obtenerTodasLasOportunidades("a") } returns flujoViejo
        every { oportunidadRepo.obtenerTodasLasOportunidades("b") } returns flujoNuevo
        val vm = crearVm()
        vm.inicializarPanel("a")
        advanceUntilIdle()

        vm.inicializarPanel("b")
        advanceUntilIdle()
        // La búsqueda "a" ya no debe seguir escribiendo en el estado.
        flujoViejo.value = listOf(oportunidadLocal(idOportunidad = 99L, nombre = "Intrusa"))
        advanceUntilIdle()

        assertEquals(listOf(oportunidadLocal(idOportunidad = 2L, nombre = "Nueva")), vm.uiState.value.listaOportunidadesGlobal)
    }

    @Test
    fun `la busqueda nueva sigue actualizando el estado cuando cambian sus datos`() = runTest {
        val flujo = MutableStateFlow(listOf(oportunidadLocal(idOportunidad = 1L)))
        every { oportunidadRepo.obtenerTodasLasOportunidades("") } returns flujo
        val vm = crearVm()
        vm.inicializarPanel()
        advanceUntilIdle()

        flujo.value = listOf(oportunidadLocal(idOportunidad = 1L), oportunidadLocal(idOportunidad = 2L))
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.listaOportunidadesGlobal.size)
    }

    // =========================================================================
    // reasignarResponsable
    // =========================================================================

    @Test
    fun `reasignar sin permiso muestra error y no llama al repositorio`() = runTest {
        iniciarSesionComo("EJECUTIVO")
        val vm = crearVm()

        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()

        assertEquals("Solo el supervisor puede asignar responsables", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.modificarOportunidad(any()) }
    }

    @Test
    fun `reasignar sin sesion tampoco tiene permiso`() = runTest {
        val vm = crearVm()

        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()

        assertEquals("Solo el supervisor puede asignar responsables", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.modificarOportunidad(any()) }
    }

    @Test
    fun `reasignar como marketing tampoco tiene permiso`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()

        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()

        assertEquals("Solo el supervisor puede asignar responsables", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.modificarOportunidad(any()) }
    }

    @Test
    fun `reasignar como supervisor modifica la oportunidad con el nuevo responsable`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.modificarOportunidad(any()) } returns true
        val vm = crearVm()
        val original = oportunidadLocal(idOportunidad = 10L, idResponsable = 5L)

        vm.reasignarResponsable(original, 6L)
        advanceUntilIdle()

        coVerify(exactly = 1) { oportunidadRepo.modificarOportunidad(original.copy(idResponsable = 6L)) }
        assertEquals("Responsable reasignado exitosamente", vm.uiState.value.mensajeExito)
        assertNull(vm.uiState.value.mensajeError)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `una excepcion al reasignar muestra el error`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.modificarOportunidad(any()) } throws RuntimeException("sin conexion")
        val vm = crearVm()

        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()

        assertEquals("Error al reasignar: sin conexion", vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `si la API rechaza la reasignacion no se muestra como exito`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.modificarOportunidad(any()) } returns false
        val vm = crearVm()

        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()

        assertEquals(Catalogos.MENSAJE_ERROR_REASIGNAR, vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `reasignar con exito reemplaza un error anterior`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.modificarOportunidad(any()) } throws RuntimeException("falla") andThen true
        val vm = crearVm()
        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()
        assertEquals("Error al reasignar: falla", vm.uiState.value.mensajeError)

        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()

        assertNull(vm.uiState.value.mensajeError)
        assertEquals("Responsable reasignado exitosamente", vm.uiState.value.mensajeExito)
    }

    // =========================================================================
    // confirmar y descartar
    // =========================================================================

    @Test
    fun `confirmar una oportunidad en espera cambia el estado a Confirmada`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) } returns Unit
        val vm = crearVm()
        val enEspera = oportunidadLocal(idOportunidad = 10L, estado = "En espera de respuesta")

        vm.confirmarOportunidad(enEspera)
        advanceUntilIdle()

        coVerify(exactly = 1) { oportunidadRepo.cambiarEstado(10L, "En espera de respuesta", "Confirmada", 1L) }
        assertEquals("Oportunidad confirmada", vm.uiState.value.mensajeExito)
        assertNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun `descartar una oportunidad identificada cambia el estado a Descartada`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) } returns Unit
        val vm = crearVm()
        val identificada = oportunidadLocal(idOportunidad = 11L, estado = "Identificada")

        vm.descartarOportunidad(identificada)
        advanceUntilIdle()

        coVerify(exactly = 1) { oportunidadRepo.cambiarEstado(11L, "Identificada", "Descartada", 1L) }
        assertEquals("Oportunidad descartada", vm.uiState.value.mensajeExito)
    }

    @Test
    fun `confirmar una oportunidad ya confirmada no tiene permiso`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        val vm = crearVm()

        vm.confirmarOportunidad(oportunidadLocal(estado = "Confirmada"))
        advanceUntilIdle()

        assertEquals("No tienes permiso para esta acción", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    @Test
    fun `descartar una oportunidad Confirmada es una transicion no permitida`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        val vm = crearVm()

        vm.descartarOportunidad(oportunidadLocal(estado = "Confirmada"))
        advanceUntilIdle()

        assertEquals("No tienes permiso para esta acción", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    @Test
    fun `confirmar una oportunidad Descartada no tiene permiso`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        val vm = crearVm()

        vm.confirmarOportunidad(oportunidadLocal(estado = "Descartada"))
        advanceUntilIdle()

        assertEquals("No tienes permiso para esta acción", vm.uiState.value.mensajeError)
    }

    @Test
    fun `un ejecutivo no puede confirmar`() = runTest {
        iniciarSesionComo("EJECUTIVO", id = 5L)
        val vm = crearVm()

        vm.confirmarOportunidad(oportunidadLocal(estado = "En espera de respuesta"))
        advanceUntilIdle()

        assertEquals("No tienes permiso para esta acción", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    @Test
    fun `un ejecutivo no puede descartar`() = runTest {
        iniciarSesionComo("EJECUTIVO", id = 5L)
        val vm = crearVm()

        vm.descartarOportunidad(oportunidadLocal(estado = "Identificada"))
        advanceUntilIdle()

        assertEquals("No tienes permiso para esta acción", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    @Test
    fun `confirmar sin sesion muestra el error de permiso`() = runTest {
        val vm = crearVm()

        vm.confirmarOportunidad(oportunidadLocal(estado = "En espera de respuesta"))
        advanceUntilIdle()

        assertEquals("No tienes permiso para esta acción", vm.uiState.value.mensajeError)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    @Test
    fun `una excepcion al confirmar muestra el error`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) } throws RuntimeException("fallo")
        val vm = crearVm()

        vm.confirmarOportunidad(oportunidadLocal(estado = "En espera de respuesta"))
        advanceUntilIdle()

        assertEquals("Error: fallo", vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
    }

    @Test
    fun `una excepcion al descartar muestra el error`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) } throws RuntimeException("fallo")
        val vm = crearVm()

        vm.descartarOportunidad(oportunidadLocal(estado = "Identificada"))
        advanceUntilIdle()

        assertEquals("Error: fallo", vm.uiState.value.mensajeError)
    }

    // =========================================================================
    // limpiarMensajes
    // =========================================================================

    @Test
    fun `limpiarMensajes borra el exito y el error`() = runTest {
        iniciarSesionComo("EJECUTIVO")
        val vm = crearVm()
        vm.reasignarResponsable(oportunidadLocal(), 6L)           // deja un mensaje de error
        advanceUntilIdle()
        assertEquals("Solo el supervisor puede asignar responsables", vm.uiState.value.mensajeError)

        vm.limpiarMensajes()

        assertNull(vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
    }

    @Test
    fun `limpiarMensajes borra el mensaje de exito`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        coEvery { oportunidadRepo.modificarOportunidad(any()) } returns true
        val vm = crearVm()
        vm.reasignarResponsable(oportunidadLocal(), 6L)
        advanceUntilIdle()
        assertEquals("Responsable reasignado exitosamente", vm.uiState.value.mensajeExito)

        vm.limpiarMensajes()

        assertNull(vm.uiState.value.mensajeExito)
    }

    @Test
    fun `limpiarMensajes no toca la lista ni los ejecutivos`() = runTest {
        every { oportunidadRepo.obtenerTodasLasOportunidades("") } returns flowOf(listOf(oportunidadLocal()))
        val vm = crearVm()
        vm.inicializarPanel()
        advanceUntilIdle()

        vm.limpiarMensajes()

        assertEquals(ejecutivos, vm.uiState.value.ejecutivos)
        assertEquals(1, vm.uiState.value.listaOportunidadesGlobal.size)
    }
}