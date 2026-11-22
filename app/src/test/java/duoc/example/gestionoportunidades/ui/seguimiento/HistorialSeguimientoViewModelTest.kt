package duoc.example.gestionoportunidades.ui.seguimiento

import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Seguimiento
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.argumentos
import duoc.example.gestionoportunidades.testutil.contactoLocal
import duoc.example.gestionoportunidades.testutil.iniciarSesionComo
import duoc.example.gestionoportunidades.testutil.oportunidadLocal
import duoc.example.gestionoportunidades.testutil.seguimientoLocal
import duoc.example.gestionoportunidades.testutil.suscribir
import duoc.example.gestionoportunidades.testutil.usuarioPrueba
import io.mockk.coEvery
import io.mockk.coVerify
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

class HistorialSeguimientoViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()

    private val oportunidadRepo = mockk<OportunidadRepository>()
    private val seguimientoRepo = mockk<SeguimientoRepository>()
    private val authRepo = mockk<AuthRepository>()
    private val contactoRepo = mockk<ContactoRepository>()

    /** Deja los repositorios con la oportunidad y el historial indicados y crea el ViewModel. */
    private fun crearVm(
        oportunidad: Oportunidad? = oportunidadLocal(idOportunidad = 10L, nombre = "Feria del Emprendedor", idResponsable = 5L),
        historial: List<Seguimiento> = emptyList(),
        id: Long = 10L
    ): HistorialSeguimientoViewModel {
        every { oportunidadRepo.observarPorId(id) } returns flowOf(oportunidad)
        every { seguimientoRepo.obtenerHistorial(id) } returns flowOf(historial)
        return HistorialSeguimientoViewModel(
            argumentos("idOportunidad" to id), oportunidadRepo, seguimientoRepo, authRepo, contactoRepo
        )
    }

    // =========================================================================
    // Construcción
    // =========================================================================

    @Test
    fun `sin el argumento idOportunidad lanza IllegalStateException`() {
        assertThrows(IllegalStateException::class.java) {
            HistorialSeguimientoViewModel(argumentos(), oportunidadRepo, seguimientoRepo, authRepo, contactoRepo)
        }
    }

    @Test
    fun `expone el id de la oportunidad recibido en la ruta`() {
        val vm = crearVm(id = 42L, oportunidad = oportunidadLocal(idOportunidad = 42L))

        assertEquals(42L, vm.idOportunidad)
        verify(exactly = 1) { oportunidadRepo.observarPorId(42L) }
        verify(exactly = 1) { seguimientoRepo.obtenerHistorial(42L) }
    }

    @Test
    fun `el estado inicial es cargando y sin items`() {
        val vm = crearVm()

        assertEquals(HistorialUiState(), vm.uiState.value)
        assertTrue(vm.uiState.value.isLoading)
    }

    // =========================================================================
    // Acceso
    // =========================================================================

    @Test
    fun `una oportunidad inexistente marca noEncontrada`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(oportunidad = null)

        suscribir(vm.uiState)
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.noEncontrada)
        assertFalse(estado.sinAcceso)
        assertFalse(estado.isLoading)
        assertTrue(estado.items.isEmpty())
    }

    @Test
    fun `sin sesion marca sinAcceso`() = runTest {
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.sinAcceso)
        assertFalse(estado.noEncontrada)
        assertFalse(estado.isLoading)
        assertEquals("", estado.nombreOportunidad)
    }

    @Test
    fun `un ejecutivo ajeno a la oportunidad marca sinAcceso`() = runTest {
        iniciarSesionComo("EJECUTIVO", id = 9L)
        val vm = crearVm(oportunidad = oportunidadLocal(idOportunidad = 10L, idResponsable = 5L))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sinAcceso)
        assertEquals("", vm.uiState.value.nombreOportunidad)
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun `el ejecutivo responsable puede ver el historial`() = runTest {
        iniciarSesionComo("EJECUTIVO", id = 5L)
        val vm = crearVm(oportunidad = oportunidadLocal(idOportunidad = 10L, nombre = "Mi feria", idResponsable = 5L))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.sinAcceso)
        assertEquals("Mi feria", vm.uiState.value.nombreOportunidad)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `el supervisor ve el historial de cualquier oportunidad`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        val vm = crearVm(oportunidad = oportunidadLocal(idOportunidad = 10L, nombre = "De otro", idResponsable = 5L))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.sinAcceso)
        assertEquals("De otro", vm.uiState.value.nombreOportunidad)
    }

    @Test
    fun `caracterizacion marketing no accede al historial porque el ViewModel no envia el estado`() = runTest {
        // Permisos.puedeVerOportunidad solo deja pasar a MARKETING si recibe estado "Confirmada",
        // y este ViewModel no lo envía: hoy marketing siempre queda con sinAcceso.
        iniciarSesionComo("MARKETING", id = 12L)
        val vm = crearVm(oportunidad = oportunidadLocal(idOportunidad = 10L, estado = "Confirmada"))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sinAcceso)
    }

    // =========================================================================
    // Items del historial
    // =========================================================================

    @Test
    fun `un historial vacio deja la lista de items vacia`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(historial = emptyList())

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.items.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `cada item trae el nombre del autor`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val gestion = seguimientoLocal(idSeguimiento = 30L, idUsuario = 5L)
        coEvery { authRepo.obtenerUsuarioPorId(5L) } returns usuarioPrueba("EJECUTIVO", id = 5L, nombre = "Ana Pérez")
        val vm = crearVm(historial = listOf(gestion))

        suscribir(vm.uiState)
        advanceUntilIdle()

        val item = vm.uiState.value.items.single()
        assertEquals(gestion, item.seguimiento)
        assertEquals("Ana Pérez", item.autor)
    }

    @Test
    fun `un autor desconocido se muestra como Usuario`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        coEvery { authRepo.obtenerUsuarioPorId(99L) } returns null
        val vm = crearVm(historial = listOf(seguimientoLocal(idUsuario = 99L)))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals("Usuario", vm.uiState.value.items.single().autor)
    }

    @Test
    fun `un contacto se muestra como Nombre punto Cargo`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        coEvery { authRepo.obtenerUsuarioPorId(any()) } returns usuarioPrueba()
        coEvery { contactoRepo.obtenerPorId(20L) } returns
                contactoLocal(idContacto = 20L, nombre = "Carla Rojas", cargo = "Encargada de eventos")
        val vm = crearVm(historial = listOf(seguimientoLocal(idContacto = 20L)))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals("Carla Rojas · Encargada de eventos", vm.uiState.value.items.single().contacto)
    }

    @Test
    fun `una gestion sin contacto deja contacto en null y no consulta contactos`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        coEvery { authRepo.obtenerUsuarioPorId(any()) } returns usuarioPrueba()
        val vm = crearVm(historial = listOf(seguimientoLocal(idContacto = null)))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertNull(vm.uiState.value.items.single().contacto)
        coVerify(exactly = 0) { contactoRepo.obtenerPorId(any()) }
    }

    @Test
    fun `un contacto que ya no existe deja contacto en null`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        coEvery { authRepo.obtenerUsuarioPorId(any()) } returns usuarioPrueba()
        coEvery { contactoRepo.obtenerPorId(20L) } returns null
        val vm = crearVm(historial = listOf(seguimientoLocal(idContacto = 20L)))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertNull(vm.uiState.value.items.single().contacto)
    }

    @Test
    fun `conserva el orden del historial y resuelve varios autores y contactos`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        coEvery { authRepo.obtenerUsuarioPorId(5L) } returns usuarioPrueba("EJECUTIVO", id = 5L, nombre = "Ana Pérez")
        coEvery { authRepo.obtenerUsuarioPorId(6L) } returns usuarioPrueba("EJECUTIVO", id = 6L, nombre = "Luis Soto")
        coEvery { contactoRepo.obtenerPorId(20L) } returns contactoLocal(idContacto = 20L, nombre = "Carla", cargo = "Jefa")
        val historial = listOf(
            seguimientoLocal(idSeguimiento = 3L, idUsuario = 6L, idContacto = null),
            seguimientoLocal(idSeguimiento = 2L, idUsuario = 5L, idContacto = 20L),
            seguimientoLocal(idSeguimiento = 1L, idUsuario = 5L, idContacto = null)
        )
        val vm = crearVm(historial = historial)

        suscribir(vm.uiState)
        advanceUntilIdle()

        val items = vm.uiState.value.items
        assertEquals(listOf(3L, 2L, 1L), items.map { it.seguimiento.idSeguimiento })
        assertEquals(listOf("Luis Soto", "Ana Pérez", "Ana Pérez"), items.map { it.autor })
        assertEquals(listOf(null, "Carla · Jefa", null), items.map { it.contacto })
    }

    @Test
    fun `consulta una sola vez cada autor y cada contacto repetido`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        coEvery { authRepo.obtenerUsuarioPorId(5L) } returns usuarioPrueba(id = 5L)
        coEvery { contactoRepo.obtenerPorId(20L) } returns contactoLocal(idContacto = 20L)
        val historial = listOf(
            seguimientoLocal(idSeguimiento = 1L, idUsuario = 5L, idContacto = 20L),
            seguimientoLocal(idSeguimiento = 2L, idUsuario = 5L, idContacto = 20L),
            seguimientoLocal(idSeguimiento = 3L, idUsuario = 5L, idContacto = 20L)
        )
        val vm = crearVm(historial = historial)

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.items.size)
        coVerify(exactly = 1) { authRepo.obtenerUsuarioPorId(5L) }
        coVerify(exactly = 1) { contactoRepo.obtenerPorId(20L) }
    }

    @Test
    fun `sin acceso no consulta autores ni contactos`() = runTest {
        iniciarSesionComo("EJECUTIVO", id = 9L)
        val vm = crearVm(
            oportunidad = oportunidadLocal(idOportunidad = 10L, idResponsable = 5L),
            historial = listOf(seguimientoLocal(idUsuario = 5L, idContacto = 20L))
        )

        suscribir(vm.uiState)
        advanceUntilIdle()

        coVerify(exactly = 0) { authRepo.obtenerUsuarioPorId(any()) }
        coVerify(exactly = 0) { contactoRepo.obtenerPorId(any()) }
    }
}