package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.ContactoRemote
import duoc.example.gestionoportunidades.testutil.contactoLocal
import duoc.example.gestionoportunidades.testutil.contactoRemoto
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.respuestaSinCuerpo
import duoc.example.gestionoportunidades.testutil.respuestaUnitSinCuerpoDeError
import duoc.example.gestionoportunidades.testutil.unico
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

class ContactoRepositoryTest {

    private val api = mockk<ApiService>()
    private val repo = ContactoRepository(api)

    // =========================================================================
    // Consultas
    // =========================================================================

    @Test
    fun `obtenerTodos convierte los contactos al modelo local`() = runTest {
        coEvery { api.listarTodosContactos() } returns listOf(contactoRemoto(id = 20L, idOrganizacion = 3L))

        val lista = repo.obtenerTodos().unico()

        assertEquals(listOf(contactoLocal(idContacto = 20L, idOrganizacion = 3L)), lista)
    }

    @Test
    fun `obtenerTodos completa los campos nulos`() = runTest {
        coEvery { api.listarTodosContactos() } returns listOf(
            contactoRemoto(id = null, cargo = null, area = null, medioPreferente = null, idOrganizacion = 3L)
        )

        val contacto = repo.obtenerTodos().unico().single()

        assertEquals(0L, contacto.idContacto)
        assertEquals("", contacto.cargo)
        assertEquals("", contacto.area)
        assertEquals("Correo", contacto.medioPreferente)
        assertEquals(3L, contacto.idOrganizacion)
    }

    @Test
    fun `obtenerTodos devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarTodosContactos() } throws IOException("sin red")

        assertTrue(repo.obtenerTodos().unico().isEmpty())
    }

    @Test
    fun `obtenerPorOrganizacion consulta los contactos de esa organizacion`() = runTest {
        coEvery { api.listarContactos(3L) } returns listOf(
            contactoRemoto(id = 1L, nombre = "Ana", idOrganizacion = 3L),
            contactoRemoto(id = 2L, nombre = "Luis", idOrganizacion = 3L)
        )

        val lista = repo.obtenerPorOrganizacion(3L).unico()

        assertEquals(listOf("Ana", "Luis"), lista.map { it.nombre })
        coVerify(exactly = 1) { api.listarContactos(3L) }
    }

    @Test
    fun `obtenerPorOrganizacion devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarContactos(any()) } throws IOException("sin red")

        assertTrue(repo.obtenerPorOrganizacion(3L).unico().isEmpty())
    }

    @Test
    fun `buscarPorNombre filtra sin distinguir mayusculas ni espacios y ordena alfabeticamente`() = runTest {
        coEvery { api.listarTodosContactos() } returns listOf(
            contactoRemoto(id = 1L, nombre = "María Soto"),
            contactoRemoto(id = 2L, nombre = "Luis Pérez"),
            contactoRemoto(id = 3L, nombre = "ana maría Rojas"),
            contactoRemoto(id = 4L, nombre = "Pedro Díaz")
        )

        val lista = repo.buscarPorNombre("  MARÍA ").unico()

        assertEquals(listOf("ana maría Rojas", "María Soto"), lista.map { it.nombre })
    }

    @Test
    fun `buscarPorNombre con texto vacio devuelve todos ordenados`() = runTest {
        coEvery { api.listarTodosContactos() } returns listOf(
            contactoRemoto(id = 1L, nombre = "Zoe"),
            contactoRemoto(id = 2L, nombre = "Ana")
        )

        val lista = repo.buscarPorNombre("").unico()

        assertEquals(listOf("Ana", "Zoe"), lista.map { it.nombre })
    }

    @Test
    fun `buscarPorNombre sin coincidencias devuelve lista vacia`() = runTest {
        coEvery { api.listarTodosContactos() } returns listOf(contactoRemoto(nombre = "Ana"))

        assertTrue(repo.buscarPorNombre("zzz").unico().isEmpty())
    }

    @Test
    fun `buscarPorNombre devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarTodosContactos() } throws IOException("sin red")

        assertTrue(repo.buscarPorNombre("ana").unico().isEmpty())
    }

    @Test
    fun `obtenerPorId encuentra el contacto entre todos`() = runTest {
        coEvery { api.listarTodosContactos() } returns listOf(
            contactoRemoto(id = 1L, nombre = "Ana"),
            contactoRemoto(id = 2L, nombre = "Luis")
        )

        assertEquals("Luis", repo.obtenerPorId(2L)?.nombre)
    }

    @Test
    fun `obtenerPorId devuelve null si el contacto no existe`() = runTest {
        coEvery { api.listarTodosContactos() } returns listOf(contactoRemoto(id = 1L))

        assertNull(repo.obtenerPorId(99L))
    }

    @Test
    fun `obtenerPorId devuelve null si la red falla`() = runTest {
        coEvery { api.listarTodosContactos() } throws IOException("sin red")

        assertNull(repo.obtenerPorId(1L))
    }

    // =========================================================================
    // insertar y actualizar
    // =========================================================================

    @Test
    fun `insertar envia el contacto sin id y como activo y devuelve el id asignado`() = runTest {
        val enviado = slot<ContactoRemote>()
        coEvery { api.crearContacto(capture(enviado)) } returns Response.success(contactoRemoto(id = 88L))

        // Aunque el modelo local traiga un id, al crear no se envía.
        val id = repo.insertar(contactoLocal(idContacto = 20L, idOrganizacion = 3L))

        assertEquals(88L, id)
        val c = enviado.captured
        assertNull(c.id)
        assertEquals("Carla Rojas", c.nombre)
        assertEquals("Encargada de eventos", c.cargo)
        assertEquals("Extensión", c.area)
        assertEquals("carla@muni.cl", c.correo)
        assertEquals("+56911112222", c.telefono)
        assertEquals("WhatsApp", c.medioPreferente)
        assertEquals("Activo", c.estado)
        assertEquals("", c.observaciones)
        assertEquals(3L, c.organizacion.id)
    }

    @Test
    fun `insertar devuelve 0 si la respuesta trae error sin cuerpo o sin id`() = runTest {
        coEvery { api.crearContacto(any()) } returns respuestaError(400)
        assertEquals(0L, repo.insertar(contactoLocal()))

        coEvery { api.crearContacto(any()) } returns respuestaSinCuerpo()
        assertEquals(0L, repo.insertar(contactoLocal()))

        coEvery { api.crearContacto(any()) } returns Response.success(contactoRemoto(id = null))
        assertEquals(0L, repo.insertar(contactoLocal()))
    }

    @Test
    fun `insertar devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearContacto(any()) } throws IOException("sin red")

        assertEquals(0L, repo.insertar(contactoLocal()))
    }

    @Test
    fun `actualizar envia el id del contacto cuando es mayor que 0`() = runTest {
        val enviado = slot<ContactoRemote>()
        coEvery { api.actualizarContacto(20L, capture(enviado)) } returns Response.success(contactoRemoto())

        repo.actualizar(contactoLocal(idContacto = 20L, nombre = "Carla Rojas Soto"))

        coVerify(exactly = 1) { api.actualizarContacto(20L, any()) }
        assertEquals(20L, enviado.captured.id)
        assertEquals("Carla Rojas Soto", enviado.captured.nombre)
        assertEquals("Activo", enviado.captured.estado)
    }

    @Test
    fun `actualizar no envia id cuando el contacto aun no lo tiene`() = runTest {
        val enviado = slot<ContactoRemote>()
        coEvery { api.actualizarContacto(any(), capture(enviado)) } returns Response.success(contactoRemoto())

        repo.actualizar(contactoLocal(idContacto = 0L))

        assertNull(enviado.captured.id)
    }

    @Test
    fun `actualizar no propaga el error de red`() = runTest {
        coEvery { api.actualizarContacto(any(), any()) } throws IOException("sin red")

        repo.actualizar(contactoLocal())

        coVerify(exactly = 1) { api.actualizarContacto(any(), any()) }
    }

    // =========================================================================
    // eliminar
    // =========================================================================

    @Test
    fun `eliminar devuelve null cuando el servidor lo borra`() = runTest {
        coEvery { api.eliminarContacto(20L) } returns Response.success(Unit)

        assertNull(repo.eliminar(20L))
        coVerify(exactly = 1) { api.eliminarContacto(20L) }
    }

    @Test
    fun `eliminar devuelve el mensaje del servidor cuando el contacto tiene historial`() = runTest {
        coEvery { api.eliminarContacto(20L) } returns
                respuestaError(409, """{"mensaje":"El contacto tiene gestiones registradas"}""")

        assertEquals("El contacto tiene gestiones registradas", repo.eliminar(20L))
    }

    @Test
    fun `eliminar devuelve el cuerpo completo si el error no es JSON`() = runTest {
        coEvery { api.eliminarContacto(20L) } returns respuestaError(500, "Error interno")

        assertEquals("Error interno", repo.eliminar(20L))
    }

    @Test
    fun `eliminar devuelve el cuerpo completo si el JSON no trae mensaje`() = runTest {
        coEvery { api.eliminarContacto(20L) } returns respuestaError(400, """{"error":"x"}""")

        assertEquals("""{"error":"x"}""", repo.eliminar(20L))
    }

    @Test
    fun `eliminar usa un mensaje generico si el error no trae cuerpo`() = runTest {
        coEvery { api.eliminarContacto(20L) } returns respuestaUnitSinCuerpoDeError()

        assertEquals("No se puede eliminar el contacto.", repo.eliminar(20L))
    }

    @Test
    fun `eliminar informa el error de conexion con el detalle`() = runTest {
        coEvery { api.eliminarContacto(20L) } throws IOException("sin red")

        assertEquals("Error de conexión al eliminar: sin red", repo.eliminar(20L))
    }
}
