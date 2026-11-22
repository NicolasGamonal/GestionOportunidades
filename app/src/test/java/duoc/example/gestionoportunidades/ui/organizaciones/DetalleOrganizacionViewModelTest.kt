package duoc.example.gestionoportunidades.ui.organizaciones

import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.GestionOrganizacion
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.argumentos
import duoc.example.gestionoportunidades.testutil.contactoLocal
import duoc.example.gestionoportunidades.testutil.iniciarSesionComo
import duoc.example.gestionoportunidades.testutil.organizacionLocal
import duoc.example.gestionoportunidades.testutil.suscribir
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetalleOrganizacionViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()

    private val organizacionRepo = mockk<OrganizacionRepository>()
    private val contactoRepo = mockk<ContactoRepository>()
    private val seguimientoRepo = mockk<SeguimientoRepository>()

    private val org = organizacionLocal(idOrganizacion = 5L)
    private val contactos = listOf(contactoLocal(idContacto = 20L, idOrganizacion = 5L))
    private val gestiones = listOf(
        GestionOrganizacion(
            idSeguimiento = 30L, fechaHora = 1_000L, tipoAccion = "Llamada", resultado = "Interesado",
            idOportunidad = 10L, nombreOportunidad = "Feria del Emprendedor", autor = "Ana Pérez",
            nombreContacto = "Carla Rojas"
        )
    )

    private fun crearVm(id: Long = 5L): DetalleOrganizacionViewModel {
        every { organizacionRepo.observarPorId(id) } returns flowOf(org)
        every { contactoRepo.obtenerPorOrganizacion(id) } returns flowOf(contactos)
        every { seguimientoRepo.obtenerHistorialPorOrganizacion(id, any()) } returns flowOf(gestiones)
        return DetalleOrganizacionViewModel(argumentos("id" to id), organizacionRepo, contactoRepo, seguimientoRepo)
    }

    // =========================================================================
    // Datos
    // =========================================================================

    @Test
    fun `el estado inicial es cargando y sin datos`() {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm()

        assertEquals(DetalleOrganizacionUiState(), vm.uiState.value)
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `combina organizacion contactos y gestiones`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals(org, estado.organizacion)
        assertEquals(contactos, estado.contactos)
        assertEquals(gestiones, estado.gestiones)
        assertFalse(estado.isLoading)
    }

    @Test
    fun `una organizacion inexistente deja organizacion en null`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        every { organizacionRepo.observarPorId(5L) } returns flowOf(null)
        every { contactoRepo.obtenerPorOrganizacion(5L) } returns flowOf(emptyList())
        every { seguimientoRepo.obtenerHistorialPorOrganizacion(5L, any()) } returns flowOf(emptyList())
        val vm = DetalleOrganizacionViewModel(argumentos("id" to 5L), organizacionRepo, contactoRepo, seguimientoRepo)

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertNull(vm.uiState.value.organizacion)
        assertTrue(vm.uiState.value.contactos.isEmpty())
        assertTrue(vm.uiState.value.gestiones.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `consulta los repositorios con el id recibido en la ruta`() {
        iniciarSesionComo("SUPERVISOR")
        crearVm(id = 77L)

        verify(exactly = 1) { organizacionRepo.observarPorId(77L) }
        verify(exactly = 1) { contactoRepo.obtenerPorOrganizacion(77L) }
        verify(exactly = 1) { seguimientoRepo.obtenerHistorialPorOrganizacion(77L, any()) }
    }

    @Test
    fun `sin el argumento id lanza IllegalStateException`() {
        iniciarSesionComo("SUPERVISOR")

        assertThrows(IllegalStateException::class.java) {
            DetalleOrganizacionViewModel(argumentos(), organizacionRepo, contactoRepo, seguimientoRepo)
        }
    }

    // =========================================================================
    // Filtro del historial según el rol
    // =========================================================================

    @Test
    fun `el supervisor pide el historial de todo el equipo con id 0`() {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        crearVm()

        verify(exactly = 1) { seguimientoRepo.obtenerHistorialPorOrganizacion(5L, 0L) }
    }

    @Test
    fun `el ejecutivo pide solo su propio historial`() {
        iniciarSesionComo("EJECUTIVO", id = 9L)
        crearVm()

        verify(exactly = 1) { seguimientoRepo.obtenerHistorialPorOrganizacion(5L, 9L) }
    }

    @Test
    fun `sin sesion pide el historial con id menos 1`() {
        crearVm()

        verify(exactly = 1) { seguimientoRepo.obtenerHistorialPorOrganizacion(5L, -1L) }
    }

    @Test
    fun `marketing se trata como un usuario normal con su propio id`() {
        iniciarSesionComo("MARKETING", id = 12L)
        crearVm()

        verify(exactly = 1) { seguimientoRepo.obtenerHistorialPorOrganizacion(5L, 12L) }
    }

    // =========================================================================
    // puedeEditar
    // =========================================================================

    @Test
    fun `el supervisor puede editar la organizacion`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.puedeEditar)
    }

    @Test
    fun `el ejecutivo no puede editar la organizacion`() = runTest {
        iniciarSesionComo("EJECUTIVO")
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.puedeEditar)
    }

    @Test
    fun `marketing no puede editar la organizacion`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.puedeEditar)
    }

    @Test
    fun `sin sesion no puede editar la organizacion`() = runTest {
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.puedeEditar)
    }
}