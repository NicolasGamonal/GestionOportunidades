package duoc.example.gestionoportunidades.ui.notas

import duoc.example.gestionoportunidades.data.repository.NotaMarketingRepository
import duoc.example.gestionoportunidades.model.NotaMarketing
import duoc.example.gestionoportunidades.model.NotaMarketingConAutor
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.argumentos
import duoc.example.gestionoportunidades.testutil.iniciarSesionComo
import duoc.example.gestionoportunidades.testutil.suscribir
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotasMarketingViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()

    private val repo = mockk<NotaMarketingRepository>()
    private val notasDelRepo = MutableStateFlow<List<NotaMarketingConAutor>>(emptyList())

    private fun crearVm(id: Long = 10L): NotasMarketingViewModel {
        every { repo.observarPorOportunidad(id) } returns notasDelRepo
        coEvery { repo.insertar(any()) } returns 1L
        return NotasMarketingViewModel(argumentos("id" to id), repo)
    }

    private fun nota(id: Long, texto: String) =
        NotaMarketingConAutor(idNota = id, tipo = "Observación", texto = texto, fechaHora = 1_000L, autor = "Luis")

    // =========================================================================
    // Construcción y estado
    // =========================================================================

    @Test
    fun `sin el argumento id lanza IllegalStateException`() {
        assertThrows(IllegalStateException::class.java) {
            NotasMarketingViewModel(argumentos(), repo)
        }
    }

    @Test
    fun `el estado inicial tiene tipo Observacion y texto vacio`() {
        val vm = crearVm()

        assertEquals(NotasUiState(), vm.uiState.value)
        assertEquals("Observación", vm.uiState.value.tipo)
        assertEquals("", vm.uiState.value.texto)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `observa las notas de la oportunidad recibida en la ruta`() = runTest {
        val vm = crearVm(id = 10L)
        notasDelRepo.value = listOf(nota(1L, "Llevar pendones"), nota(2L, "Confirmar stand"))

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(listOf(nota(1L, "Llevar pendones"), nota(2L, "Confirmar stand")), vm.uiState.value.notas)
    }

    @Test
    fun `una nota nueva del repositorio aparece sin perder lo escrito en el formulario`() = runTest {
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("borrador")
        vm.onTipoChange("Material")
        advanceUntilIdle()

        notasDelRepo.value = listOf(nota(1L, "Nueva"))
        advanceUntilIdle()

        assertEquals(listOf(nota(1L, "Nueva")), vm.uiState.value.notas)
        assertEquals("borrador", vm.uiState.value.texto)
        assertEquals("Material", vm.uiState.value.tipo)
    }

    @Test
    fun `una lista vacia del repositorio deja las notas vacias`() = runTest {
        every { repo.observarPorOportunidad(10L) } returns flowOf(emptyList())
        val vm = NotasMarketingViewModel(argumentos("id" to 10L), repo)

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.notas.isEmpty())
    }

    // =========================================================================
    // Edición del formulario
    // =========================================================================

    @Test
    fun `onTipoChange cambia el tipo`() = runTest {
        val vm = crearVm()
        suscribir(vm.uiState)

        vm.onTipoChange("Material")
        advanceUntilIdle()

        assertEquals("Material", vm.uiState.value.tipo)
    }

    @Test
    fun `onTextoChange cambia el texto`() = runTest {
        val vm = crearVm()
        suscribir(vm.uiState)

        vm.onTextoChange("Llevar pendones")
        advanceUntilIdle()

        assertEquals("Llevar pendones", vm.uiState.value.texto)
    }

    @Test
    fun `onTextoChange limpia el error`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.agregar()                                    // texto vacío: deja un error
        advanceUntilIdle()
        assertEquals("La nota es obligatorio", vm.uiState.value.error)

        vm.onTextoChange("a")
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
    }

    // =========================================================================
    // agregar: quién puede y qué se valida
    // =========================================================================

    @Test
    fun `sin sesion no inserta`() = runTest {
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("Llevar pendones")

        vm.agregar()
        advanceUntilIdle()

        coVerify(exactly = 0) { repo.insertar(any()) }
        assertEquals("Llevar pendones", vm.uiState.value.texto)
    }

    @Test
    fun `un ejecutivo no tiene permiso y no inserta`() = runTest {
        iniciarSesionComo("EJECUTIVO")
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("Llevar pendones")

        vm.agregar()
        advanceUntilIdle()

        coVerify(exactly = 0) { repo.insertar(any()) }
        assertNull(vm.uiState.value.error)
        assertEquals("Llevar pendones", vm.uiState.value.texto)
    }

    @Test
    fun `texto vacio muestra error y no inserta`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)

        vm.agregar()
        advanceUntilIdle()

        assertEquals("La nota es obligatorio", vm.uiState.value.error)
        coVerify(exactly = 0) { repo.insertar(any()) }
    }

    @Test
    fun `texto solo con espacios se trata como vacio`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("    ")

        vm.agregar()
        advanceUntilIdle()

        assertEquals("La nota es obligatorio", vm.uiState.value.error)
        coVerify(exactly = 0) { repo.insertar(any()) }
    }

    @Test
    fun `mas de 500 caracteres muestra error y no inserta`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("a".repeat(501))

        vm.agregar()
        advanceUntilIdle()

        assertEquals("La nota no puede superar 500 caracteres", vm.uiState.value.error)
        coVerify(exactly = 0) { repo.insertar(any()) }
        assertEquals(501, vm.uiState.value.texto.length)
    }

    @Test
    fun `exactamente 500 caracteres es valido`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("a".repeat(500))

        vm.agregar()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        coVerify(exactly = 1) { repo.insertar(any()) }
    }

    // =========================================================================
    // agregar: éxito
    // =========================================================================

    @Test
    fun `marketing inserta la nota con los datos del formulario y de la sesion`() = runTest {
        iniciarSesionComo("MARKETING", id = 8L)
        val capturada = slot<NotaMarketing>()
        val vm = crearVm(id = 10L)
        coEvery { repo.insertar(capture(capturada)) } returns 1L
        suscribir(vm.uiState)
        vm.onTipoChange("Material")
        vm.onTextoChange("  Llevar pendones  ")
        val antes = System.currentTimeMillis()

        vm.agregar()
        advanceUntilIdle()

        val despues = System.currentTimeMillis()
        val nota = capturada.captured
        assertEquals(0L, nota.idNota)
        assertEquals(10L, nota.idOportunidad)
        assertEquals("Material", nota.tipo)
        assertEquals("Llevar pendones", nota.texto)          // se guarda sin los espacios de los bordes
        assertEquals(8L, nota.idUsuario)
        assertTrue(nota.fechaHora in antes..despues)
    }

    @Test
    fun `el supervisor tambien puede agregar notas`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("Revisar costos")

        vm.agregar()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.insertar(match { it.idUsuario == 1L && it.texto == "Revisar costos" }) }
    }

    @Test
    fun `el tipo por defecto de la nota es Observacion`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.onTextoChange("Algo")

        vm.agregar()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.insertar(match { it.tipo == "Observación" }) }
    }

    @Test
    fun `agregar con exito limpia el texto y el error pero conserva el tipo`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)
        vm.agregar()                                    // primero un error por texto vacío
        vm.onTipoChange("Material")
        vm.onTextoChange("Llevar pendones")

        vm.agregar()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.texto)
        assertNull(vm.uiState.value.error)
        assertEquals("Material", vm.uiState.value.tipo)
    }

    @Test
    fun `agregar dos veces inserta dos notas`() = runTest {
        iniciarSesionComo("MARKETING")
        val vm = crearVm()
        suscribir(vm.uiState)

        vm.onTextoChange("Primera")
        vm.agregar()
        advanceUntilIdle()
        vm.onTextoChange("Segunda")
        vm.agregar()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.insertar(match { it.texto == "Primera" }) }
        coVerify(exactly = 1) { repo.insertar(match { it.texto == "Segunda" }) }
    }
}