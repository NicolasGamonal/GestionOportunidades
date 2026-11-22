package duoc.example.gestionoportunidades.ui.auth

import app.cash.turbine.test
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.usuarioPrueba
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AuthViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()

    private val repo = mockk<AuthRepository>()
    private val vm = AuthViewModel(repo)

    // =========================================================================
    // Estado inicial y validación de campos
    // =========================================================================

    @Test
    fun `el estado inicial no carga ni tiene error ni sesion`() {
        assertEquals(AuthUiState(), vm.uiState.value)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMensaje)
        assertFalse(vm.uiState.value.loginExitoso)
        assertNull(vm.uiState.value.usuarioLogueado)
    }

    @Test
    fun `usuario vacio muestra error y no llama al repositorio`() = runTest {
        vm.intentarLogin("", "1234")
        advanceUntilIdle()

        assertEquals("Por favor, completa todos los campos", vm.uiState.value.errorMensaje)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.loginExitoso)
        coVerify(exactly = 0) { repo.iniciarSesion(any(), any()) }
    }

    @Test
    fun `contrasena vacia muestra error y no llama al repositorio`() = runTest {
        vm.intentarLogin("ana", "")
        advanceUntilIdle()

        assertEquals("Por favor, completa todos los campos", vm.uiState.value.errorMensaje)
        coVerify(exactly = 0) { repo.iniciarSesion(any(), any()) }
    }

    @Test
    fun `campos solo con espacios se tratan como vacios`() = runTest {
        vm.intentarLogin("   ", "   ")
        advanceUntilIdle()

        assertEquals("Por favor, completa todos los campos", vm.uiState.value.errorMensaje)
        coVerify(exactly = 0) { repo.iniciarSesion(any(), any()) }
    }

    @Test
    fun `con campos vacios no se inicia la sesion global`() = runTest {
        vm.intentarLogin("", "")
        advanceUntilIdle()

        assertFalse(SesionManager.estaLogueado())
    }

    // =========================================================================
    // Resultado del login
    // =========================================================================

    @Test
    fun `login exitoso deja al usuario en SesionManager y marca loginExitoso`() = runTest {
        val ana = usuarioPrueba("EJECUTIVO", id = 5L)
        coEvery { repo.iniciarSesion("ana", "1234") } returns ana

        vm.intentarLogin("ana", "1234")
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.loginExitoso)
        assertSame(ana, estado.usuarioLogueado)
        assertNull(estado.errorMensaje)
        assertFalse(estado.isLoading)
        assertTrue(SesionManager.estaLogueado())
        assertSame(ana, SesionManager.usuarioActual)
        assertEquals("EJECUTIVO", SesionManager.getRol())
    }

    @Test
    fun `login exitoso envia al repositorio exactamente lo escrito`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } returns usuarioPrueba("SUPERVISOR")

        vm.intentarLogin("jefa@agencia.cl", "Clave 123")
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.iniciarSesion("jefa@agencia.cl", "Clave 123") }
    }

    @Test
    fun `login con rol supervisor deja ese rol en la sesion`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } returns usuarioPrueba("SUPERVISOR", id = 1L)

        vm.intentarLogin("jefa", "1234")
        advanceUntilIdle()

        assertEquals("SUPERVISOR", SesionManager.getRol())
    }

    @Test
    fun `credenciales incorrectas muestran el mensaje y no inician sesion`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } returns null

        vm.intentarLogin("ana", "mala")
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals("Credenciales incorrectas", estado.errorMensaje)
        assertFalse(estado.loginExitoso)
        assertNull(estado.usuarioLogueado)
        assertFalse(estado.isLoading)
        assertFalse(SesionManager.estaLogueado())
    }

    @Test
    fun `una excepcion del repositorio muestra Error con su mensaje`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } throws IOException("sin conexion")

        vm.intentarLogin("ana", "1234")
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertEquals("Error: sin conexion", estado.errorMensaje)
        assertFalse(estado.loginExitoso)
        assertFalse(estado.isLoading)
        assertFalse(SesionManager.estaLogueado())
    }

    @Test
    fun `una excepcion sin mensaje muestra Error null`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } throws RuntimeException()

        vm.intentarLogin("ana", "1234")
        advanceUntilIdle()

        assertEquals("Error: null", vm.uiState.value.errorMensaje)
    }

    @Test
    fun `reintentar despues de un error limpia el mensaje`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } returns null
        vm.intentarLogin("ana", "mala")
        advanceUntilIdle()
        assertEquals("Credenciales incorrectas", vm.uiState.value.errorMensaje)

        coEvery { repo.iniciarSesion(any(), any()) } returns usuarioPrueba()
        vm.intentarLogin("ana", "1234")
        advanceUntilIdle()

        assertNull(vm.uiState.value.errorMensaje)
        assertTrue(vm.uiState.value.loginExitoso)
    }

    @Test
    fun `un error de validacion no borra el usuario ya logueado`() = runTest {
        val ana = usuarioPrueba()
        coEvery { repo.iniciarSesion(any(), any()) } returns ana
        vm.intentarLogin("ana", "1234")
        advanceUntilIdle()

        vm.intentarLogin("", "")

        assertEquals("Por favor, completa todos los campos", vm.uiState.value.errorMensaje)
        assertSame(ana, vm.uiState.value.usuarioLogueado)
    }

    // =========================================================================
    // isLoading (secuencia con Turbine)
    // =========================================================================

    @Test
    fun `isLoading pasa por true mientras consulta y vuelve a false al exito`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } coAnswers {
            delay(100)
            usuarioPrueba()
        }

        vm.uiState.test {
            assertFalse(awaitItem().isLoading)          // estado inicial

            vm.intentarLogin("ana", "1234")

            val cargando = awaitItem()
            assertTrue(cargando.isLoading)
            assertNull(cargando.errorMensaje)
            assertFalse(cargando.loginExitoso)

            val terminado = awaitItem()
            assertFalse(terminado.isLoading)
            assertTrue(terminado.loginExitoso)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading vuelve a false cuando las credenciales son incorrectas`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } coAnswers {
            delay(100)
            null
        }

        vm.uiState.test {
            awaitItem()                                  // estado inicial

            vm.intentarLogin("ana", "mala")

            assertTrue(awaitItem().isLoading)
            val terminado = awaitItem()
            assertFalse(terminado.isLoading)
            assertEquals("Credenciales incorrectas", terminado.errorMensaje)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // cerrarSesion
    // =========================================================================

    @Test
    fun `cerrarSesion limpia el estado y la sesion global`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } returns usuarioPrueba()
        vm.intentarLogin("ana", "1234")
        advanceUntilIdle()
        assertTrue(SesionManager.estaLogueado())
        assertTrue(vm.uiState.value.loginExitoso)

        vm.cerrarSesion()

        assertEquals(AuthUiState(), vm.uiState.value)
        assertFalse(SesionManager.estaLogueado())
        assertNull(SesionManager.usuarioActual)
        assertEquals("", SesionManager.getRol())
    }

    @Test
    fun `cerrarSesion sin haber iniciado no falla`() {
        vm.cerrarSesion()

        assertEquals(AuthUiState(), vm.uiState.value)
        assertFalse(SesionManager.estaLogueado())
    }

    @Test
    fun `se puede volver a iniciar sesion despues de cerrarla`() = runTest {
        coEvery { repo.iniciarSesion(any(), any()) } returns usuarioPrueba("EJECUTIVO", id = 5L)
        vm.intentarLogin("ana", "1234")
        advanceUntilIdle()
        vm.cerrarSesion()

        coEvery { repo.iniciarSesion(any(), any()) } returns usuarioPrueba("MARKETING", id = 9L)
        vm.intentarLogin("luis", "1234")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.loginExitoso)
        assertEquals("MARKETING", SesionManager.getRol())
        assertEquals(9L, SesionManager.usuarioActual?.idUsuario)
    }
}