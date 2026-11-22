package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.ResultadoActivacionRemote
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.respuestaSinCuerpo
import duoc.example.gestionoportunidades.testutil.resultadoLocal
import duoc.example.gestionoportunidades.testutil.resultadoRemoto
import duoc.example.gestionoportunidades.testutil.unico
import io.mockk.coEvery
import io.mockk.coVerify
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

class ResultadoActivacionRepositoryTest {

    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val api = mockk<ApiService>()
    private lateinit var repo: ResultadoActivacionRepository

    @Before
    fun crearRepositorio() {
        // Se crea aquí para que lea la zona America/Santiago que fija la regla.
        repo = ResultadoActivacionRepository(api)
    }

    // =========================================================================
    // obtenerPorOportunidad
    // =========================================================================

    @Test
    fun `obtenerPorOportunidad convierte el resultado remoto al modelo local`() = runTest {
        coEvery { api.obtenerResultado(10L) } returns Response.success(resultadoRemoto())

        val r = repo.obtenerPorOportunidad(10L)!!

        assertEquals(60L, r.idResultado)
        assertEquals(10L, r.idOportunidad)
        assertEquals(420, r.asistentes)
        assertEquals(35, r.contactosGenerados)
        assertEquals(12, r.inscripciones)
        assertEquals(4, r.evaluacion)
        assertEquals("Funcionó bien el stand", r.aprendizajes)
        assertEquals(FechasPrueba.MARZO_16, r.fechaRegistro)
        assertEquals(5L, r.idUsuario)
    }

    @Test
    fun `obtenerPorOportunidad completa los campos nulos con sus valores por defecto`() = runTest {
        coEvery { api.obtenerResultado(10L) } returns Response.success(
            resultadoRemoto(
                id = null, asistentesReales = null, contactosGenerados = null,
                inscripcionesLogradas = null, evaluacionGeneral = null, observaciones = null,
                usuarioId = null
            )
        )

        val r = repo.obtenerPorOportunidad(10L)!!

        assertEquals(0L, r.idResultado)
        assertEquals(0, r.asistentes)
        assertEquals(0, r.contactosGenerados)
        assertEquals(0, r.inscripciones)
        assertEquals(5, r.evaluacion)
        assertEquals("", r.aprendizajes)
        assertEquals(1L, r.idUsuario)
    }

    @Test
    fun `una evaluacion que no es numero se reemplaza por 5`() = runTest {
        for (texto in listOf("excelente", "", "4,5", " 4")) {
            coEvery { api.obtenerResultado(10L) } returns
                    Response.success(resultadoRemoto(evaluacionGeneral = texto))

            assertEquals("Evaluación '$texto'", 5, repo.obtenerPorOportunidad(10L)!!.evaluacion)
        }
    }

    @Test
    fun `una fecha de realizacion nula o invalida se reemplaza por el momento actual`() = runTest {
        for (fecha in listOf(null, "", "  ", "ayer", "2026-02-31")) {
            coEvery { api.obtenerResultado(10L) } returns
                    Response.success(resultadoRemoto(fechaRealizacion = fecha))

            val antes = System.currentTimeMillis()
            val r = repo.obtenerPorOportunidad(10L)!!
            val despues = System.currentTimeMillis()

            assertTrue("Fecha '$fecha'", r.fechaRegistro in antes..despues)
        }
    }

    @Test
    fun `una fecha de realizacion con hora se lee por sus primeros 10 caracteres`() = runTest {
        coEvery { api.obtenerResultado(10L) } returns
                Response.success(resultadoRemoto(fechaRealizacion = "2026-03-16T18:45:00"))

        assertEquals(FechasPrueba.MARZO_16, repo.obtenerPorOportunidad(10L)!!.fechaRegistro)
    }

    @Test
    fun `obtenerPorOportunidad devuelve null si la oportunidad no tiene resultado`() = runTest {
        coEvery { api.obtenerResultado(10L) } returns respuestaSinCuerpo()

        assertNull(repo.obtenerPorOportunidad(10L))
    }

    @Test
    fun `obtenerPorOportunidad devuelve null si el servidor responde con error`() = runTest {
        coEvery { api.obtenerResultado(10L) } returns respuestaError(404)

        assertNull(repo.obtenerPorOportunidad(10L))
    }

    @Test
    fun `obtenerPorOportunidad devuelve null si la red falla`() = runTest {
        coEvery { api.obtenerResultado(10L) } throws IOException("sin red")

        assertNull(repo.obtenerPorOportunidad(10L))
    }

    @Test
    fun `observarPorOportunidad emite el resultado una sola vez`() = runTest {
        coEvery { api.obtenerResultado(10L) } returns Response.success(resultadoRemoto())

        assertEquals(60L, repo.observarPorOportunidad(10L).unico()?.idResultado)
    }

    @Test
    fun `observarPorOportunidad emite null si no hay resultado`() = runTest {
        coEvery { api.obtenerResultado(10L) } returns respuestaSinCuerpo()

        assertNull(repo.observarPorOportunidad(10L).unico())
    }

    // =========================================================================
    // guardar
    // =========================================================================

    @Test
    fun `guardar envia el resultado nuevo sin id y devuelve el id asignado`() = runTest {
        val enviado = slot<ResultadoActivacionRemote>()
        coEvery { api.guardarResultado(capture(enviado)) } returns Response.success(resultadoRemoto(id = 61L))

        val id = repo.guardar(resultadoLocal(idResultado = 0L))

        assertEquals(61L, id)
        val r = enviado.captured
        assertNull(r.id)
        assertEquals("2026-03-16", r.fechaRealizacion)
        assertEquals("Realizada", r.estadoFinal)
        assertEquals(420, r.asistentesReales)
        assertEquals(35, r.contactosGenerados)
        assertEquals(35, r.personasInteresadas)
        assertEquals(12, r.inscripcionesLogradas)
        assertEquals("4", r.evaluacionGeneral)
        assertEquals("Funcionó bien el stand", r.observaciones)
        assertEquals(5L, r.usuarioId)
        assertEquals(10L, r.oportunidad.id)
    }

    @Test
    fun `guardar envia el id cuando el resultado ya existe`() = runTest {
        val enviado = slot<ResultadoActivacionRemote>()
        coEvery { api.guardarResultado(capture(enviado)) } returns Response.success(resultadoRemoto(id = 60L))

        repo.guardar(resultadoLocal(idResultado = 60L))

        assertEquals(60L, enviado.captured.id)
    }

    @Test
    fun `guardar usa el dia de Santiago y no el dia UTC en la fecha de realizacion`() = runTest {
        val enviado = slot<ResultadoActivacionRemote>()
        coEvery { api.guardarResultado(capture(enviado)) } returns Response.success(resultadoRemoto())
        // 23:30 del 15/03 en Santiago = 02:30 UTC del 16/03
        val tarde = FechasPrueba.instante("2026-03-16T02:30:00Z")

        repo.guardar(resultadoLocal(fechaRegistro = tarde))

        assertEquals("2026-03-15", enviado.captured.fechaRealizacion)
    }

    @Test
    fun `guardar devuelve 0 si la respuesta trae error sin cuerpo o sin id`() = runTest {
        coEvery { api.guardarResultado(any()) } returns respuestaError(400)
        assertEquals(0L, repo.guardar(resultadoLocal()))

        coEvery { api.guardarResultado(any()) } returns respuestaSinCuerpo()
        assertEquals(0L, repo.guardar(resultadoLocal()))

        coEvery { api.guardarResultado(any()) } returns Response.success(resultadoRemoto(id = null))
        assertEquals(0L, repo.guardar(resultadoLocal()))
    }

    @Test
    fun `guardar devuelve 0 si la red falla`() = runTest {
        coEvery { api.guardarResultado(any()) } throws IOException("sin red")

        assertEquals(0L, repo.guardar(resultadoLocal()))
        coVerify(exactly = 1) { api.guardarResultado(any()) }
    }
}
