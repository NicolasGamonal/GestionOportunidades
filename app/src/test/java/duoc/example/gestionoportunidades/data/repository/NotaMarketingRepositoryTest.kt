package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.NotaMarketingRemote
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.notaLocal
import duoc.example.gestionoportunidades.testutil.notaRemota
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.respuestaSinCuerpo
import duoc.example.gestionoportunidades.testutil.unico
import duoc.example.gestionoportunidades.testutil.usuarioRemoto
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class NotaMarketingRepositoryTest {

    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val api = mockk<ApiService>()
    private lateinit var repo: NotaMarketingRepository

    @Before
    fun crearRepositorio() {
        // Se crea aquí para que lea la zona America/Santiago que fija la regla.
        repo = NotaMarketingRepository(api)
    }

    private fun stubUsuarios() {
        coEvery { api.listarUsuarios() } returns listOf(
            usuarioRemoto(id = 5L, nombre = "Ana Pérez"),
            usuarioRemoto(id = 6L, nombre = "Luis Soto")
        )
    }

    // =========================================================================
    // observarPorOportunidad
    // =========================================================================

    @Test
    fun `observarPorOportunidad convierte la nota y busca el nombre del autor`() = runTest {
        stubUsuarios()
        coEvery { api.listarNotas(10L) } returns listOf(notaRemota(autorId = 6L))

        val nota = repo.observarPorOportunidad(10L).unico().single()

        assertEquals(50L, nota.idNota)
        assertEquals("Material", nota.tipo)
        assertEquals("Llevar pendones", nota.texto)
        assertEquals(FechasPrueba.MARZO_10_1430, nota.fechaHora)
        assertEquals("Luis Soto", nota.autor)
    }

    @Test
    fun `una nota sin autor o con autor desconocido se atribuye al Equipo Marketing`() = runTest {
        stubUsuarios()
        coEvery { api.listarNotas(10L) } returns listOf(
            notaRemota(id = 1L, autorId = null),
            notaRemota(id = 2L, autorId = 999L)
        )

        val autores = repo.observarPorOportunidad(10L).unico().map { it.autor }

        assertEquals(listOf("Equipo Marketing", "Equipo Marketing"), autores)
    }

    @Test
    fun `una nota sin tipo se muestra como Observacion y sin id queda en 0`() = runTest {
        stubUsuarios()
        coEvery { api.listarNotas(10L) } returns listOf(notaRemota(id = null, materialRequerido = null))

        val nota = repo.observarPorOportunidad(10L).unico().single()

        assertEquals("Observación", nota.tipo)
        assertEquals(0L, nota.idNota)
    }

    @Test
    fun `una fecha de creacion nula o invalida se reemplaza por el momento actual`() = runTest {
        stubUsuarios()
        for (fecha in listOf(null, "", "  ", "hoy")) {
            coEvery { api.listarNotas(10L) } returns listOf(notaRemota(fechaCreacion = fecha))

            val antes = System.currentTimeMillis()
            val nota = repo.observarPorOportunidad(10L).unico().single()
            val despues = System.currentTimeMillis()

            assertTrue("Fecha '$fecha'", nota.fechaHora in antes..despues)
        }
    }

    @Test
    fun `observarPorOportunidad ordena las notas de la mas reciente a la mas antigua`() = runTest {
        stubUsuarios()
        coEvery { api.listarNotas(10L) } returns listOf(
            notaRemota(id = 1L, fechaCreacion = "2026-03-10T09:00:00"),
            notaRemota(id = 2L, fechaCreacion = "2026-03-12T09:00:00"),
            notaRemota(id = 3L, fechaCreacion = "2026-03-11T09:00:00")
        )

        val ids = repo.observarPorOportunidad(10L).unico().map { it.idNota }

        assertEquals(listOf(2L, 3L, 1L), ids)
    }

    @Test
    fun `observarPorOportunidad devuelve lista vacia si fallan las notas`() = runTest {
        stubUsuarios()
        coEvery { api.listarNotas(any()) } throws IOException("sin red")

        assertTrue(repo.observarPorOportunidad(10L).unico().isEmpty())
    }

    @Test
    fun `observarPorOportunidad devuelve lista vacia si falla la lista de usuarios`() = runTest {
        coEvery { api.listarUsuarios() } throws IOException("sin red")
        coEvery { api.listarNotas(10L) } returns listOf(notaRemota())

        assertTrue(repo.observarPorOportunidad(10L).unico().isEmpty())
    }

    // =========================================================================
    // insertar
    // =========================================================================

    @Test
    fun `insertar envia la nota y devuelve el id asignado`() = runTest {
        val enviada = slot<NotaMarketingRemote>()
        coEvery { api.crearNota(capture(enviada)) } returns Response.success(notaRemota(id = 71L))

        val id = repo.insertar(notaLocal(tipo = "Observación", texto = "Confirmar horario", idUsuario = 6L, idOportunidad = 12L))

        assertEquals(71L, id)
        val n = enviada.captured
        assertNull(n.id)
        assertEquals("Confirmar horario", n.contenido)
        assertEquals("Observación", n.materialRequerido)
        assertEquals(6L, n.autorId)
        assertEquals(12L, n.oportunidad.id)
        assertNull("el servidor asigna la fecha", n.fechaCreacion)
    }

    @Test
    fun `insertar devuelve 0 si la respuesta trae error sin cuerpo o sin id`() = runTest {
        coEvery { api.crearNota(any()) } returns respuestaError(400)
        assertEquals(0L, repo.insertar(notaLocal()))

        coEvery { api.crearNota(any()) } returns respuestaSinCuerpo()
        assertEquals(0L, repo.insertar(notaLocal()))

        coEvery { api.crearNota(any()) } returns Response.success(notaRemota(id = null))
        assertEquals(0L, repo.insertar(notaLocal()))
    }

    @Test
    fun `insertar devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearNota(any()) } throws IOException("sin red")

        assertEquals(0L, repo.insertar(notaLocal()))
    }
}
