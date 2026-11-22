package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.LoginRequestRemote
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.respuestaSinCuerpo
import duoc.example.gestionoportunidades.testutil.unico
import duoc.example.gestionoportunidades.testutil.usuarioRemoto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AuthRepositoryTest {

    private val api = mockk<ApiService>()
    private val repo = AuthRepository(api)

    // =========================================================================
    // iniciarSesion
    // =========================================================================

    @Test
    fun `iniciarSesion devuelve el usuario convertido al modelo local`() = runTest {
        coEvery { api.login(any()) } returns Response.success(
            usuarioRemoto(id = 5L, nombre = "Ana Pérez", correo = "ana@agencia.cl", password = "clave123",
                rol = "EJECUTIVO", area = "Ventas", sucursal = "Santiago Centro")
        )

        val usuario = repo.iniciarSesion("ana@agencia.cl", "clave123")

        assertEquals(
            Usuario(
                idUsuario = 5L, username = "ana@agencia.cl", contrasena = "clave123",
                nombreCompleto = "Ana Pérez", rol = "EJECUTIVO", area = "Ventas", zona = "Santiago Centro"
            ),
            usuario
        )
    }

    @Test
    fun `iniciarSesion completa con vacio la contrasena el area y la zona nulas`() = runTest {
        coEvery { api.login(any()) } returns
                Response.success(usuarioRemoto(password = null, area = null, sucursal = null))

        val usuario = repo.iniciarSesion("ana@agencia.cl", "clave123")!!

        assertEquals("", usuario.contrasena)
        assertEquals("", usuario.area)
        assertEquals("", usuario.zona)
    }

    @Test
    fun `iniciarSesion recorta el correo pero no la contrasena`() = runTest {
        val enviado = slot<LoginRequestRemote>()
        coEvery { api.login(capture(enviado)) } returns Response.success(usuarioRemoto())

        repo.iniciarSesion("  ana@agencia.cl  ", " clave 123 ")

        assertEquals("ana@agencia.cl", enviado.captured.correo)
        assertEquals(" clave 123 ", enviado.captured.password)
    }

    @Test
    fun `iniciarSesion con correo vacio o en blanco devuelve null sin llamar a la API`() = runTest {
        assertNull(repo.iniciarSesion("", "clave"))
        assertNull(repo.iniciarSesion("   ", "clave"))

        coVerify(exactly = 0) { api.login(any()) }
    }

    @Test
    fun `iniciarSesion con contrasena vacia o en blanco devuelve null sin llamar a la API`() = runTest {
        assertNull(repo.iniciarSesion("ana@agencia.cl", ""))
        assertNull(repo.iniciarSesion("ana@agencia.cl", "   "))

        coVerify(exactly = 0) { api.login(any()) }
    }

    @Test
    fun `iniciarSesion devuelve null si las credenciales son incorrectas`() = runTest {
        coEvery { api.login(any()) } returns respuestaError(401, """{"mensaje":"Credenciales inválidas"}""")

        assertNull(repo.iniciarSesion("ana@agencia.cl", "mala"))
    }

    @Test
    fun `iniciarSesion devuelve null si la respuesta exitosa no trae usuario`() = runTest {
        coEvery { api.login(any()) } returns respuestaSinCuerpo()

        assertNull(repo.iniciarSesion("ana@agencia.cl", "clave"))
    }

    @Test
    fun `iniciarSesion devuelve null si la red falla`() = runTest {
        coEvery { api.login(any()) } throws IOException("sin red")

        assertNull(repo.iniciarSesion("ana@agencia.cl", "clave"))
    }

    // =========================================================================
    // obtenerUsuarioPorId
    // =========================================================================

    @Test
    fun `obtenerUsuarioPorId encuentra al usuario en la lista`() = runTest {
        coEvery { api.listarUsuarios() } returns listOf(
            usuarioRemoto(id = 5L, nombre = "Ana Pérez"),
            usuarioRemoto(id = 6L, nombre = "Luis Soto", rol = "SUPERVISOR")
        )

        val usuario = repo.obtenerUsuarioPorId(6L)

        assertEquals("Luis Soto", usuario?.nombreCompleto)
        assertEquals("SUPERVISOR", usuario?.rol)
        assertEquals(6L, usuario?.idUsuario)
    }

    @Test
    fun `obtenerUsuarioPorId devuelve null si no existe`() = runTest {
        coEvery { api.listarUsuarios() } returns listOf(usuarioRemoto(id = 5L))

        assertNull(repo.obtenerUsuarioPorId(99L))
    }

    @Test
    fun `obtenerUsuarioPorId devuelve null si la red falla`() = runTest {
        coEvery { api.listarUsuarios() } throws IOException("sin red")

        assertNull(repo.obtenerUsuarioPorId(5L))
    }

    // =========================================================================
    // obtenerEjecutivos
    // =========================================================================

    @Test
    fun `obtenerEjecutivos devuelve solo ejecutivos activos`() = runTest {
        coEvery { api.listarUsuarios() } returns listOf(
            usuarioRemoto(id = 1L, nombre = "Ejecutivo activo", rol = "EJECUTIVO", activo = true),
            usuarioRemoto(id = 2L, nombre = "Supervisora", rol = "SUPERVISOR", activo = true),
            usuarioRemoto(id = 3L, nombre = "Ejecutivo inactivo", rol = "EJECUTIVO", activo = false),
            usuarioRemoto(id = 4L, nombre = "Ejecutivo en minúscula", rol = "ejecutivo", activo = true)
        )

        val ejecutivos = repo.obtenerEjecutivos().unico()

        assertEquals(listOf("Ejecutivo activo", "Ejecutivo en minúscula"), ejecutivos.map { it.nombreCompleto })
        assertTrue(ejecutivos.all { it.idUsuario == 1L || it.idUsuario == 4L })
    }

    @Test
    fun `obtenerEjecutivos sin ejecutivos devuelve lista vacia`() = runTest {
        coEvery { api.listarUsuarios() } returns listOf(usuarioRemoto(rol = "SUPERVISOR"))

        assertTrue(repo.obtenerEjecutivos().unico().isEmpty())
    }

    @Test
    fun `obtenerEjecutivos devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarUsuarios() } throws IOException("sin red")

        assertTrue(repo.obtenerEjecutivos().unico().isEmpty())
    }
}
