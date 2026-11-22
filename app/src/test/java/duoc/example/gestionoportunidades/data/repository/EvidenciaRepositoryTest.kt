package duoc.example.gestionoportunidades.data.repository

import android.util.Base64
import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.EvidenciaRemote
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.evidenciaLocal
import duoc.example.gestionoportunidades.testutil.evidenciaRemota
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.respuestaSinCuerpo
import duoc.example.gestionoportunidades.testutil.unico
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.io.IOException

class EvidenciaRepositoryTest {

    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()
    @Rule @JvmField val carpeta = TemporaryFolder()

    private val api = mockk<ApiService>()
    private lateinit var repo: EvidenciaRepository

    @Before
    fun crearRepositorio() {
        // Se crea aquí: necesita la carpeta temporal ya creada y la zona America/Santiago de la regla.
        repo = EvidenciaRepository(carpeta.root, api)
    }

    // =========================================================================
    // observarPorOportunidad
    // =========================================================================

    @Test
    fun `observarPorOportunidad convierte la evidencia remota al modelo local`() = runTest {
        coEvery { api.listarEvidencias(10L) } returns listOf(evidenciaRemota())

        val evidencia = repo.observarPorOportunidad(10L).unico().single()

        assertEquals(40L, evidencia.idEvidencia)
        assertEquals("evidencias/foto1.jpg", evidencia.rutaArchivo)
        assertEquals(FechasPrueba.MARZO_10_1430, evidencia.fecha)
        assertEquals(10L, evidencia.idOportunidad)
    }

    @Test
    fun `observarPorOportunidad completa id y descripcion nulos`() = runTest {
        coEvery { api.listarEvidencias(10L) } returns listOf(evidenciaRemota(id = null, descripcion = null))

        val evidencia = repo.observarPorOportunidad(10L).unico().single()

        assertEquals(0L, evidencia.idEvidencia)
        assertEquals("", evidencia.rutaArchivo)
    }

    @Test
    fun `observarPorOportunidad ordena las fotos de la mas reciente a la mas antigua`() = runTest {
        coEvery { api.listarEvidencias(10L) } returns listOf(
            evidenciaRemota(id = 1L, fechaCaptura = "2026-03-10T09:00:00"),
            evidenciaRemota(id = 2L, fechaCaptura = "2026-03-12T09:00:00"),
            evidenciaRemota(id = 3L, fechaCaptura = "2026-03-11T09:00:00")
        )

        val ids = repo.observarPorOportunidad(10L).unico().map { it.idEvidencia }

        assertEquals(listOf(2L, 3L, 1L), ids)
    }

    @Test
    fun `una fecha de captura nula o invalida se reemplaza por el momento actual`() = runTest {
        for (fecha in listOf(null, "", "  ", "ayer")) {
            coEvery { api.listarEvidencias(10L) } returns listOf(evidenciaRemota(fechaCaptura = fecha))

            val antes = System.currentTimeMillis()
            val evidencia = repo.observarPorOportunidad(10L).unico().single()
            val despues = System.currentTimeMillis()

            assertTrue("Fecha '$fecha'", evidencia.fecha in antes..despues)
        }
    }

    @Test
    fun `observarPorOportunidad devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarEvidencias(any()) } throws IOException("sin red")

        assertTrue(repo.observarPorOportunidad(10L).unico().isEmpty())
    }

    // =========================================================================
    // registrar
    // =========================================================================

    @Test
    fun `registrar sin archivo en disco envia la imagen vacia y devuelve el id asignado`() = runTest {
        val enviada = slot<EvidenciaRemote>()
        coEvery { api.crearEvidencia(capture(enviada)) } returns Response.success(evidenciaRemota(id = 91L))

        val id = repo.registrar(evidenciaLocal(rutaArchivo = "no_existe.jpg", idOportunidad = 12L))

        assertEquals(91L, id)
        val e = enviada.captured
        assertEquals("no_existe.jpg", e.descripcion)
        assertEquals("", e.imagenBase64)
        assertEquals(12L, e.oportunidad.id)
    }

    @Test
    fun `registrar con archivo en disco envia su contenido codificado en Base64`() = runTest {
        File(carpeta.root, "foto1.jpg").writeBytes(byteArrayOf(1, 2, 3))
        val bytesLeidos = slot<ByteArray>()
        val enviada = slot<EvidenciaRemote>()
        // android.util.Base64 no existe en la JVM de las pruebas: se simula.
        mockkStatic(Base64::class)
        try {
            every { Base64.encodeToString(capture(bytesLeidos), any<Int>()) } returns "AQID"
            coEvery { api.crearEvidencia(capture(enviada)) } returns Response.success(evidenciaRemota(id = 92L))

            val id = repo.registrar(evidenciaLocal(rutaArchivo = "foto1.jpg"))

            assertEquals(92L, id)
            assertArrayEquals(byteArrayOf(1, 2, 3), bytesLeidos.captured)
            assertEquals("AQID", enviada.captured.imagenBase64)
            assertEquals("foto1.jpg", enviada.captured.descripcion)
        } finally {
            unmockkStatic(Base64::class)
        }
    }

    @Test
    fun `registrar devuelve 0 si la respuesta trae error sin cuerpo o sin id`() = runTest {
        coEvery { api.crearEvidencia(any()) } returns respuestaError(400)
        assertEquals(0L, repo.registrar(evidenciaLocal(rutaArchivo = "no_existe.jpg")))

        coEvery { api.crearEvidencia(any()) } returns respuestaSinCuerpo()
        assertEquals(0L, repo.registrar(evidenciaLocal(rutaArchivo = "no_existe.jpg")))

        coEvery { api.crearEvidencia(any()) } returns Response.success(evidenciaRemota(id = null))
        assertEquals(0L, repo.registrar(evidenciaLocal(rutaArchivo = "no_existe.jpg")))
    }

    @Test
    fun `registrar devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearEvidencia(any()) } throws IOException("sin red")

        assertEquals(0L, repo.registrar(evidenciaLocal(rutaArchivo = "no_existe.jpg")))
    }

    // =========================================================================
    // eliminar
    // =========================================================================

    @Test
    fun `eliminar borra la evidencia en el servidor y el archivo en disco`() = runTest {
        val archivo = File(carpeta.newFolder("evidencias"), "foto1.jpg")
        archivo.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { api.eliminarEvidencia(40L) } returns Response.success(Unit)

        repo.eliminar(evidenciaLocal(idEvidencia = 40L, rutaArchivo = "evidencias/foto1.jpg"))

        coVerify(exactly = 1) { api.eliminarEvidencia(40L) }
        assertFalse(archivo.exists())
    }

    @Test
    fun `eliminar borra el archivo aunque falle la llamada al servidor`() = runTest {
        val archivo = File(carpeta.root, "foto1.jpg")
        archivo.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { api.eliminarEvidencia(any()) } throws IOException("sin red")

        repo.eliminar(evidenciaLocal(rutaArchivo = "foto1.jpg"))

        assertFalse(archivo.exists())
    }

    @Test
    fun `eliminar no falla si el archivo ya no existe`() = runTest {
        coEvery { api.eliminarEvidencia(any()) } returns Response.success(Unit)

        repo.eliminar(evidenciaLocal(rutaArchivo = "ya_borrada.jpg"))

        coVerify(exactly = 1) { api.eliminarEvidencia(any()) }
    }
}