package duoc.example.gestionoportunidades.ui.organizaciones

import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrganizacionesViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<OrganizacionRepository>()

    private val muni = organizacionLocal(idOrganizacion = 1L, nombre = "Municipalidad de Puente Alto")
    private val banco = organizacionLocal(
        idOrganizacion = 2L, nombre = "Banco del Sur", tipo = "Empresa", rubro = "Finanzas", comuna = "Santiago"
    )

    @Test
    fun `el estado inicial es cargando y sin organizaciones`() {
        val vm = OrganizacionesViewModel(repo)

        assertEquals(OrganizacionesUiState(), vm.uiState.value)
        assertTrue(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.organizaciones.isEmpty())
        assertEquals("", vm.busqueda.value)
    }

    @Test
    fun `sin suscriptores no consulta al repositorio`() = runTest {
        OrganizacionesViewModel(repo)

        advanceUntilIdle()

        verify(exactly = 0) { repo.buscarPorNombre(any()) }
    }

    @Test
    fun `al suscribirse consulta con busqueda vacia y muestra las organizaciones`() = runTest {
        every { repo.buscarPorNombre("") } returns flowOf(listOf(muni, banco))
        val vm = OrganizacionesViewModel(repo)

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(listOf(muni, banco), vm.uiState.value.organizaciones)
        assertFalse(vm.uiState.value.isLoading)
        verify(exactly = 1) { repo.buscarPorNombre("") }
    }

    @Test
    fun `cambiar la busqueda vuelve a consultar al repositorio con el nuevo texto`() = runTest {
        every { repo.buscarPorNombre("") } returns flowOf(listOf(muni, banco))
        every { repo.buscarPorNombre("muni") } returns flowOf(listOf(muni))
        val vm = OrganizacionesViewModel(repo)
        suscribir(vm.uiState)
        advanceUntilIdle()

        vm.onBusquedaChange("muni")
        advanceUntilIdle()

        assertEquals("muni", vm.busqueda.value)
        assertEquals(listOf(muni), vm.uiState.value.organizaciones)
        assertFalse(vm.uiState.value.isLoading)
        verify(exactly = 1) { repo.buscarPorNombre("") }
        verify(exactly = 1) { repo.buscarPorNombre("muni") }
    }

    @Test
    fun `una busqueda sin resultados deja la lista vacia y termina de cargar`() = runTest {
        every { repo.buscarPorNombre("") } returns flowOf(listOf(muni))
        every { repo.buscarPorNombre("zzz") } returns flowOf(emptyList())
        val vm = OrganizacionesViewModel(repo)
        suscribir(vm.uiState)
        advanceUntilIdle()

        vm.onBusquedaChange("zzz")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.organizaciones.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `volver a la busqueda vacia recupera todas las organizaciones`() = runTest {
        every { repo.buscarPorNombre("") } returns flowOf(listOf(muni, banco))
        every { repo.buscarPorNombre("banco") } returns flowOf(listOf(banco))
        val vm = OrganizacionesViewModel(repo)
        suscribir(vm.uiState)
        advanceUntilIdle()
        vm.onBusquedaChange("banco")
        advanceUntilIdle()
        assertEquals(listOf(banco), vm.uiState.value.organizaciones)

        vm.onBusquedaChange("")
        advanceUntilIdle()

        assertEquals(listOf(muni, banco), vm.uiState.value.organizaciones)
    }

    @Test
    fun `repetir la misma busqueda no consulta otra vez`() = runTest {
        every { repo.buscarPorNombre("") } returns flowOf(listOf(muni))
        every { repo.buscarPorNombre("muni") } returns flowOf(listOf(muni))
        val vm = OrganizacionesViewModel(repo)
        suscribir(vm.uiState)
        advanceUntilIdle()

        vm.onBusquedaChange("muni")
        advanceUntilIdle()
        vm.onBusquedaChange("muni")
        advanceUntilIdle()

        verify(exactly = 1) { repo.buscarPorNombre("muni") }
    }
}