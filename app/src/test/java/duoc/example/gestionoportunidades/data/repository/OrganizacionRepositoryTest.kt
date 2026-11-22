package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.OrganizacionRemote
import duoc.example.gestionoportunidades.testutil.orgRemota
import duoc.example.gestionoportunidades.testutil.organizacionLocal
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

class OrganizacionRepositoryTest {

    private val api = mockk<ApiService>()
    private val repo = OrganizacionRepository(api)

    // =========================================================================
    // Consultas
    // =========================================================================

    @Test
    fun `obtenerTodas convierte las organizaciones al modelo local`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(orgRemota(id = 1L))

        assertEquals(listOf(organizacionLocal(idOrganizacion = 1L)), repo.obtenerTodas().unico())
    }

    @Test
    fun `obtenerTodas completa los campos nulos con vacio`() = runTest {
        coEvery { api.listarOrganizaciones() } returns
                listOf(orgRemota(id = null, rubro = null, comuna = null))

        val org = repo.obtenerTodas().unico().single()

        assertEquals(0L, org.idOrganizacion)
        assertEquals("", org.rubro)
        assertEquals("", org.comuna)
    }

    @Test
    fun `obtenerTodas devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOrganizaciones() } throws IOException("sin red")

        assertTrue(repo.obtenerTodas().unico().isEmpty())
    }

    @Test
    fun `buscarPorNombre filtra sin distinguir mayusculas ni espacios y ordena alfabeticamente`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(
            orgRemota(id = 1L, nombre = "Universidad Norte"),
            orgRemota(id = 2L, nombre = "Colegio San José"),
            orgRemota(id = 3L, nombre = "universidad Austral"),
            orgRemota(id = 4L, nombre = "Banco del Sur")
        )

        val lista = repo.buscarPorNombre("  UNIVERSIDAD ").unico()

        assertEquals(listOf("universidad Austral", "Universidad Norte"), lista.map { it.nombre })
    }

    @Test
    fun `buscarPorNombre con texto vacio devuelve todas ordenadas`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(
            orgRemota(id = 1L, nombre = "Zeta"),
            orgRemota(id = 2L, nombre = "Alfa")
        )

        assertEquals(listOf("Alfa", "Zeta"), repo.buscarPorNombre("").unico().map { it.nombre })
    }

    @Test
    fun `buscarPorNombre sin coincidencias devuelve lista vacia`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(orgRemota(nombre = "Alfa"))

        assertTrue(repo.buscarPorNombre("zzz").unico().isEmpty())
    }

    @Test
    fun `buscarPorNombre devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOrganizaciones() } throws IOException("sin red")

        assertTrue(repo.buscarPorNombre("a").unico().isEmpty())
    }

    @Test
    fun `obtenerPorId encuentra la organizacion entre todas`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(
            orgRemota(id = 1L, nombre = "Alfa"),
            orgRemota(id = 2L, nombre = "Beta")
        )

        assertEquals("Beta", repo.obtenerPorId(2L)?.nombre)
    }

    @Test
    fun `obtenerPorId devuelve null si no existe`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(orgRemota(id = 1L))

        assertNull(repo.obtenerPorId(99L))
    }

    @Test
    fun `obtenerPorId devuelve null si la red falla`() = runTest {
        coEvery { api.listarOrganizaciones() } throws IOException("sin red")

        assertNull(repo.obtenerPorId(1L))
    }

    @Test
    fun `observarPorId emite la organizacion una sola vez`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(orgRemota(id = 1L, nombre = "Alfa"))

        assertEquals("Alfa", repo.observarPorId(1L).unico()?.nombre)
    }

    @Test
    fun `observarPorId emite null si la organizacion no existe o la red falla`() = runTest {
        coEvery { api.listarOrganizaciones() } returns emptyList()
        assertNull(repo.observarPorId(1L).unico())

        coEvery { api.listarOrganizaciones() } throws IOException("sin red")
        assertNull(repo.observarPorId(1L).unico())
    }

    // =========================================================================
    // insertar y actualizar
    // =========================================================================

    @Test
    fun `insertar envia la organizacion sin id y devuelve el id asignado`() = runTest {
        val enviada = slot<OrganizacionRemote>()
        coEvery { api.crearOrganizacion(capture(enviada)) } returns Response.success(orgRemota(id = 44L))

        // Aunque el modelo local traiga un id, al crear no se envía.
        val id = repo.insertar(organizacionLocal(idOrganizacion = 9L))

        assertEquals(44L, id)
        val o = enviada.captured
        assertNull(o.id)
        assertEquals("Municipalidad de Puente Alto", o.nombre)
        assertEquals("Municipalidad", o.tipo)
        assertEquals("Gobierno local", o.rubro)
        assertEquals("Puente Alto", o.comuna)
        assertEquals("", o.sitioWeb)
        assertEquals("", o.publicoAsociado)
        assertEquals("", o.observaciones)
    }

    @Test
    fun `insertar devuelve 0 si la respuesta trae error sin cuerpo o sin id`() = runTest {
        coEvery { api.crearOrganizacion(any()) } returns respuestaError(400)
        assertEquals(0L, repo.insertar(organizacionLocal()))

        coEvery { api.crearOrganizacion(any()) } returns respuestaSinCuerpo()
        assertEquals(0L, repo.insertar(organizacionLocal()))

        coEvery { api.crearOrganizacion(any()) } returns Response.success(orgRemota(id = null))
        assertEquals(0L, repo.insertar(organizacionLocal()))
    }

    @Test
    fun `insertar devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearOrganizacion(any()) } throws IOException("sin red")

        assertEquals(0L, repo.insertar(organizacionLocal()))
    }

    @Test
    fun `actualizar envia el id de la organizacion cuando es mayor que 0`() = runTest {
        val enviada = slot<OrganizacionRemote>()
        coEvery { api.actualizarOrganizacion(9L, capture(enviada)) } returns Response.success(orgRemota())

        repo.actualizar(organizacionLocal(idOrganizacion = 9L, nombre = "Nuevo nombre"))

        coVerify(exactly = 1) { api.actualizarOrganizacion(9L, any()) }
        assertEquals(9L, enviada.captured.id)
        assertEquals("Nuevo nombre", enviada.captured.nombre)
    }

    @Test
    fun `actualizar no envia id cuando la organizacion aun no lo tiene`() = runTest {
        val enviada = slot<OrganizacionRemote>()
        coEvery { api.actualizarOrganizacion(any(), capture(enviada)) } returns Response.success(orgRemota())

        repo.actualizar(organizacionLocal(idOrganizacion = 0L))

        assertNull(enviada.captured.id)
    }

    @Test
    fun `actualizar no propaga el error de red y devuelve false`() = runTest {
        coEvery { api.actualizarOrganizacion(any(), any()) } throws IOException("sin red")

        assertEquals(false, repo.actualizar(organizacionLocal()))

        coVerify(exactly = 1) { api.actualizarOrganizacion(any(), any()) }
    }

    @Test
    fun `actualizar devuelve true si la API acepta y false si responde error`() = runTest {
        coEvery { api.actualizarOrganizacion(any(), any()) } returns Response.success(orgRemota())
        assertEquals(true, repo.actualizar(organizacionLocal()))

        coEvery { api.actualizarOrganizacion(any(), any()) } returns respuestaError(500)
        assertEquals(false, repo.actualizar(organizacionLocal()))
    }

    // =========================================================================
    // eliminar
    // =========================================================================

    @Test
    fun `eliminar devuelve null cuando el servidor la borra`() = runTest {
        coEvery { api.eliminarOrganizacion(9L) } returns Response.success(Unit)

        assertNull(repo.eliminar(9L))
        coVerify(exactly = 1) { api.eliminarOrganizacion(9L) }
    }

    @Test
    fun `eliminar devuelve el mensaje del servidor cuando la organizacion tiene oportunidades`() = runTest {
        coEvery { api.eliminarOrganizacion(9L) } returns
                respuestaError(409, """{"mensaje":"La organización tiene oportunidades asociadas"}""")

        assertEquals("La organización tiene oportunidades asociadas", repo.eliminar(9L))
    }

    @Test
    fun `eliminar devuelve el cuerpo completo si el error no es JSON`() = runTest {
        coEvery { api.eliminarOrganizacion(9L) } returns respuestaError(500, "Error interno")

        assertEquals("Error interno", repo.eliminar(9L))
    }

    @Test
    fun `eliminar devuelve el cuerpo completo si el JSON no trae mensaje`() = runTest {
        coEvery { api.eliminarOrganizacion(9L) } returns respuestaError(400, """{"error":"x"}""")

        assertEquals("""{"error":"x"}""", repo.eliminar(9L))
    }

    @Test
    fun `eliminar usa un mensaje generico si el error no trae cuerpo`() = runTest {
        coEvery { api.eliminarOrganizacion(9L) } returns respuestaUnitSinCuerpoDeError()

        assertEquals("No se puede eliminar la organización.", repo.eliminar(9L))
    }

    @Test
    fun `eliminar informa el error de conexion con el detalle`() = runTest {
        coEvery { api.eliminarOrganizacion(9L) } throws IOException("sin red")

        assertEquals("Error de conexión al eliminar: sin red", repo.eliminar(9L))
    }
}
