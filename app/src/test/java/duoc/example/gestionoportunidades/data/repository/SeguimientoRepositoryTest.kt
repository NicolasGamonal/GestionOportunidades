package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.SeguimientoRemote
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.oportunidadRemota
import duoc.example.gestionoportunidades.testutil.orgRemota
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.respuestaSinCuerpo
import duoc.example.gestionoportunidades.testutil.seguimientoLocal
import duoc.example.gestionoportunidades.testutil.seguimientoRemoto
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SeguimientoRepositoryTest {

    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val api = mockk<ApiService>()
    private lateinit var repo: SeguimientoRepository

    @Before
    fun crearRepositorio() {
        // Se crea aquí para que lea la zona America/Santiago que fija la regla.
        repo = SeguimientoRepository(api)
    }

    private fun stubTodos(vararg segs: SeguimientoRemote) {
        coEvery { api.listarTodosSeguimientos() } returns segs.toList()
    }

    // =========================================================================
    // obtenerHistorial y conversión remoto → local
    // =========================================================================

    @Test
    fun `obtenerHistorial convierte un seguimiento remoto al modelo local`() = runTest {
        coEvery { api.listarSeguimientos(10L) } returns listOf(seguimientoRemoto())

        val seg = repo.obtenerHistorial(10L).unico().single()

        assertEquals(30L, seg.idSeguimiento)
        assertEquals(10L, seg.idOportunidad)
        assertEquals(5L, seg.idUsuario)
        assertEquals("Llamada", seg.tipoAccion)
        // 14:30 de Santiago (UTC-3) = 17:30 UTC
        assertEquals(FechasPrueba.MARZO_10_1430, seg.fechaHora)
        assertEquals("Contacto interesado", seg.resultado)
        assertEquals("Enviar propuesta", seg.proximaAccion)
        assertEquals(FechasPrueba.dia("2026-03-20"), seg.fechaProximo)
        assertNull(seg.idContacto)
    }

    @Test
    fun `obtenerHistorial completa id y proxima accion nulos`() = runTest {
        coEvery { api.listarSeguimientos(10L) } returns
                listOf(seguimientoRemoto(id = null, proximaAccion = null))

        val seg = repo.obtenerHistorial(10L).unico().single()

        assertEquals(0L, seg.idSeguimiento)
        assertEquals("", seg.proximaAccion)
    }

    @Test
    fun `obtenerHistorial ordena del mas reciente al mas antiguo`() = runTest {
        coEvery { api.listarSeguimientos(10L) } returns listOf(
            seguimientoRemoto(id = 1L, fechaHora = "2026-03-10T09:00:00"),
            seguimientoRemoto(id = 2L, fechaHora = "2026-03-12T09:00:00"),
            seguimientoRemoto(id = 3L, fechaHora = "2026-03-11T09:00:00")
        )

        val ids = repo.obtenerHistorial(10L).unico().map { it.idSeguimiento }

        assertEquals(listOf(2L, 3L, 1L), ids)
    }

    @Test
    fun `la fecha y hora se lee por sus primeros 19 caracteres`() = runTest {
        coEvery { api.listarSeguimientos(10L) } returns
                listOf(seguimientoRemoto(fechaHora = "2026-03-10T14:30:00.123456"))

        val seg = repo.obtenerHistorial(10L).unico().single()

        assertEquals(FechasPrueba.MARZO_10_1430, seg.fechaHora)
    }

    @Test
    fun `una fecha y hora sin hora se interpreta como el inicio del dia`() = runTest {
        coEvery { api.listarSeguimientos(10L) } returns
                listOf(seguimientoRemoto(fechaHora = "2026-03-10"))

        val seg = repo.obtenerHistorial(10L).unico().single()

        assertEquals(FechasPrueba.dia("2026-03-10"), seg.fechaHora)
    }

    @Test
    fun `una fecha y hora nula o invalida se reemplaza por el momento actual`() = runTest {
        for (fecha in listOf(null, "", "  ", "sin fecha")) {
            coEvery { api.listarSeguimientos(10L) } returns
                    listOf(seguimientoRemoto(fechaHora = fecha))

            val antes = System.currentTimeMillis()
            val seg = repo.obtenerHistorial(10L).unico().single()
            val despues = System.currentTimeMillis()

            assertTrue("Fecha '$fecha'", seg.fechaHora in antes..despues)
        }
    }

    @Test
    fun `una fecha de proximo seguimiento nula o invalida se reemplaza por el momento actual`() = runTest {
        for (fecha in listOf(null, "", "no es fecha", "2026-02-31")) {
            coEvery { api.listarSeguimientos(10L) } returns
                    listOf(seguimientoRemoto(fechaProximoSeguimiento = fecha))

            val antes = System.currentTimeMillis()
            val seg = repo.obtenerHistorial(10L).unico().single()
            val despues = System.currentTimeMillis()

            assertTrue("Fecha '$fecha'", seg.fechaProximo in antes..despues)
        }
    }

    @Test
    fun `obtenerHistorial devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarSeguimientos(any()) } throws IOException("sin red")

        assertTrue(repo.obtenerHistorial(10L).unico().isEmpty())
    }

    // =========================================================================
    // obtenerAtrasados
    // =========================================================================

    @Test
    fun `obtenerAtrasados devuelve los vencidos antes de la fecha actual ordenados por vencimiento`() = runTest {
        stubTodos(
            seguimientoRemoto(id = 1L, fechaProximoSeguimiento = "2026-03-14"),
            seguimientoRemoto(id = 2L, fechaProximoSeguimiento = "2026-03-10"),
            seguimientoRemoto(id = 3L, fechaProximoSeguimiento = "2026-03-20")
        )

        val lista = repo.obtenerAtrasados(FechasPrueba.dia("2026-03-15")).unico()

        assertEquals(listOf(2L, 1L), lista.map { it.idSeguimiento })
    }

    @Test
    fun `obtenerAtrasados no incluye el que vence justo en la fecha actual`() = runTest {
        stubTodos(seguimientoRemoto(id = 1L, fechaProximoSeguimiento = "2026-03-15"))

        assertTrue(repo.obtenerAtrasados(FechasPrueba.dia("2026-03-15")).unico().isEmpty())
    }

    @Test
    fun `obtenerAtrasados ignora los cambios de estado en cualquier capitalizacion`() = runTest {
        stubTodos(
            seguimientoRemoto(id = 1L, tipoAccion = "Cambio de estado", fechaProximoSeguimiento = "2026-03-01"),
            seguimientoRemoto(id = 2L, tipoAccion = "CAMBIO DE ESTADO", fechaProximoSeguimiento = "2026-03-01"),
            seguimientoRemoto(id = 3L, tipoAccion = "Reunión", fechaProximoSeguimiento = "2026-03-01")
        )

        val lista = repo.obtenerAtrasados(FechasPrueba.dia("2026-03-15")).unico()

        assertEquals(listOf(3L), lista.map { it.idSeguimiento })
    }

    @Test
    fun `obtenerAtrasados devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarTodosSeguimientos() } throws IOException("sin red")

        assertTrue(repo.obtenerAtrasados(0L).unico().isEmpty())
    }

    // =========================================================================
    // obtenerPendientesPorUsuario
    // =========================================================================

    @Test
    fun `obtenerPendientesPorUsuario filtra por usuario fecha y ordena por vencimiento`() = runTest {
        stubTodos(
            seguimientoRemoto(id = 1L, idUsuario = 5L, fechaProximoSeguimiento = "2026-03-25"),
            seguimientoRemoto(id = 2L, idUsuario = 5L, fechaProximoSeguimiento = "2026-03-16"),
            seguimientoRemoto(id = 3L, idUsuario = 6L, fechaProximoSeguimiento = "2026-03-20"),
            seguimientoRemoto(id = 4L, idUsuario = 5L, fechaProximoSeguimiento = "2026-03-10")
        )

        val lista = repo.obtenerPendientesPorUsuario(5L, FechasPrueba.dia("2026-03-15")).unico()

        assertEquals(listOf(2L, 1L), lista.map { it.idSeguimiento })
    }

    @Test
    fun `obtenerPendientesPorUsuario incluye el que vence justo en la fecha actual`() = runTest {
        stubTodos(seguimientoRemoto(id = 1L, idUsuario = 5L, fechaProximoSeguimiento = "2026-03-15"))

        val lista = repo.obtenerPendientesPorUsuario(5L, FechasPrueba.dia("2026-03-15")).unico()

        assertEquals(listOf(1L), lista.map { it.idSeguimiento })
    }

    @Test
    fun `obtenerPendientesPorUsuario con usuario 0 incluye a todos`() = runTest {
        stubTodos(
            seguimientoRemoto(id = 1L, idUsuario = 5L, fechaProximoSeguimiento = "2026-03-20"),
            seguimientoRemoto(id = 2L, idUsuario = 6L, fechaProximoSeguimiento = "2026-03-21")
        )

        val lista = repo.obtenerPendientesPorUsuario(0L, FechasPrueba.dia("2026-03-15")).unico()

        assertEquals(listOf(1L, 2L), lista.map { it.idSeguimiento })
    }

    @Test
    fun `obtenerPendientesPorUsuario ignora los cambios de estado`() = runTest {
        stubTodos(
            seguimientoRemoto(id = 1L, tipoAccion = "Cambio de estado", fechaProximoSeguimiento = "2026-03-20"),
            seguimientoRemoto(id = 2L, tipoAccion = "Llamada", fechaProximoSeguimiento = "2026-03-20")
        )

        val lista = repo.obtenerPendientesPorUsuario(0L, FechasPrueba.dia("2026-03-15")).unico()

        assertEquals(listOf(2L), lista.map { it.idSeguimiento })
    }

    @Test
    fun `obtenerPendientesPorUsuario devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarTodosSeguimientos() } throws IOException("sin red")

        assertTrue(repo.obtenerPendientesPorUsuario(5L, 0L).unico().isEmpty())
    }

    // =========================================================================
    // obtenerPorUsuario
    // =========================================================================

    @Test
    fun `obtenerPorUsuario devuelve solo los de ese usuario del mas reciente al mas antiguo`() = runTest {
        stubTodos(
            seguimientoRemoto(id = 1L, idUsuario = 5L, fechaHora = "2026-03-10T09:00:00"),
            seguimientoRemoto(id = 2L, idUsuario = 6L, fechaHora = "2026-03-11T09:00:00"),
            seguimientoRemoto(id = 3L, idUsuario = 5L, fechaHora = "2026-03-12T09:00:00")
        )

        val lista = repo.obtenerPorUsuario(5L).unico()

        assertEquals(listOf(3L, 1L), lista.map { it.idSeguimiento })
    }

    @Test
    fun `obtenerPorUsuario devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarTodosSeguimientos() } throws IOException("sin red")

        assertTrue(repo.obtenerPorUsuario(5L).unico().isEmpty())
    }

    // =========================================================================
    // insertar y actualizar
    // =========================================================================

    @Test
    fun `insertar envia el seguimiento sin id y devuelve el id asignado`() = runTest {
        val enviado = slot<SeguimientoRemote>()
        coEvery { api.crearSeguimiento(capture(enviado)) } returns Response.success(seguimientoRemoto(id = 99L))

        val id = repo.insertar(seguimientoLocal(idSeguimiento = 30L))

        assertEquals(99L, id)
        val s = enviado.captured
        assertNull("al crear no se envía el id", s.id)
        assertNull("el servidor asigna la fecha y hora", s.fechaHora)
        assertEquals("Llamada", s.tipoAccion)
        assertEquals("Contacto interesado", s.resultado)
        assertEquals("Enviar propuesta", s.proximaAccion)
        assertEquals("2026-03-20", s.fechaProximoSeguimiento)
        assertEquals("", s.observaciones)
        assertEquals(10L, s.oportunidad.id)
        assertEquals(5L, s.usuario.id)
    }

    @Test
    fun `insertar devuelve 0 si la respuesta trae error sin cuerpo o sin id`() = runTest {
        coEvery { api.crearSeguimiento(any()) } returns respuestaError(400)
        assertEquals(0L, repo.insertar(seguimientoLocal()))

        coEvery { api.crearSeguimiento(any()) } returns respuestaSinCuerpo()
        assertEquals(0L, repo.insertar(seguimientoLocal()))

        coEvery { api.crearSeguimiento(any()) } returns Response.success(seguimientoRemoto(id = null))
        assertEquals(0L, repo.insertar(seguimientoLocal()))
    }

    @Test
    fun `insertar devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearSeguimiento(any()) } throws IOException("sin red")

        assertEquals(0L, repo.insertar(seguimientoLocal()))
    }

    @Test
    fun `actualizar envia el id del seguimiento cuando es mayor que 0`() = runTest {
        val enviado = slot<SeguimientoRemote>()
        coEvery { api.actualizarSeguimiento(30L, capture(enviado)) } returns Response.success(seguimientoRemoto())

        repo.actualizar(seguimientoLocal(idSeguimiento = 30L, resultado = "Reunión realizada"))

        coVerify(exactly = 1) { api.actualizarSeguimiento(30L, any()) }
        assertEquals(30L, enviado.captured.id)
        assertEquals("Reunión realizada", enviado.captured.resultado)
    }

    @Test
    fun `actualizar no envia id cuando el seguimiento aun no tiene id`() = runTest {
        val enviado = slot<SeguimientoRemote>()
        coEvery { api.actualizarSeguimiento(any(), capture(enviado)) } returns Response.success(seguimientoRemoto())

        repo.actualizar(seguimientoLocal(idSeguimiento = 0L))

        assertNull(enviado.captured.id)
    }

    @Test
    fun `actualizar no propaga el error de red`() = runTest {
        coEvery { api.actualizarSeguimiento(any(), any()) } throws IOException("sin red")

        repo.actualizar(seguimientoLocal())

        coVerify(exactly = 1) { api.actualizarSeguimiento(any(), any()) }
    }

    // =========================================================================
    // obtenerUltimasGestiones
    // =========================================================================

    @Test
    fun `obtenerUltimasGestiones toma la gestion mas reciente ignorando los cambios de estado`() = runTest {
        coEvery { api.listarOportunidades(any()) } returns listOf(
            oportunidadRemota(id = 10L, nombre = "Feria A", responsable = usuarioRemoto(nombre = "Ana Pérez"))
        )
        coEvery { api.listarSeguimientos(10L) } returns listOf(
            seguimientoRemoto(
                id = 1L, tipoAccion = "Llamada", fechaHora = "2026-03-05T10:00:00",
                proximaAccion = "Enviar", fechaProximoSeguimiento = "2026-03-12"
            ),
            seguimientoRemoto(
                id = 2L, tipoAccion = "Reunión", fechaHora = "2026-03-08T10:00:00",
                proximaAccion = null, fechaProximoSeguimiento = "2026-03-20"
            ),
            // Es el más reciente, pero un cambio de estado no cuenta como gestión.
            seguimientoRemoto(id = 3L, tipoAccion = "Cambio de estado", fechaHora = "2026-03-09T10:00:00")
        )

        val alertas = repo.obtenerUltimasGestiones(0L).unico()

        assertEquals(1, alertas.size)
        val a = alertas[0]
        assertEquals(10L, a.idOportunidad)
        assertEquals("Feria A", a.nombreOportunidad)
        assertEquals("Ana Pérez", a.nombreResponsable)
        assertEquals("Reunión", a.tipoAccion)
        assertEquals("", a.proximaAccion)
        assertEquals(FechasPrueba.dia("2026-03-20"), a.fechaProximo)
    }

    @Test
    fun `obtenerUltimasGestiones omite descartadas y las que no tienen gestiones reales`() = runTest {
        coEvery { api.listarOportunidades(any()) } returns listOf(
            oportunidadRemota(id = 10L, estado = "Identificada"),
            oportunidadRemota(id = 11L, estado = "Descartada"),
            oportunidadRemota(id = 12L, estado = "Identificada"),
            oportunidadRemota(id = 13L, estado = "Identificada"),
            oportunidadRemota(id = null, estado = "Identificada")
        )
        coEvery { api.listarSeguimientos(10L) } returns listOf(seguimientoRemoto(id = 1L, idOportunidad = 10L))
        coEvery { api.listarSeguimientos(12L) } returns
                listOf(seguimientoRemoto(id = 2L, tipoAccion = "Cambio de estado", idOportunidad = 12L))
        coEvery { api.listarSeguimientos(13L) } returns emptyList()

        val alertas = repo.obtenerUltimasGestiones(0L).unico()

        assertEquals(listOf(10L), alertas.map { it.idOportunidad })
        coVerify(exactly = 0) { api.listarSeguimientos(11L) }
    }

    @Test
    fun `obtenerUltimasGestiones deja el responsable en null si la oportunidad no esta asignada`() = runTest {
        coEvery { api.listarOportunidades(any()) } returns listOf(oportunidadRemota(id = 10L, responsable = null))
        coEvery { api.listarSeguimientos(10L) } returns listOf(seguimientoRemoto())

        val alerta = repo.obtenerUltimasGestiones(0L).unico().single()

        assertNull(alerta.nombreResponsable)
    }

    @Test
    fun `obtenerUltimasGestiones consulta por responsable solo cuando se indica`() = runTest {
        coEvery { api.listarOportunidades(any()) } returns emptyList()

        repo.obtenerUltimasGestiones(0L).unico()
        repo.obtenerUltimasGestiones(8L).unico()

        coVerify(exactly = 1) { api.listarOportunidades(null) }
        coVerify(exactly = 1) { api.listarOportunidades(8L) }
    }

    @Test
    fun `obtenerUltimasGestiones devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        assertTrue(repo.obtenerUltimasGestiones(0L).unico().isEmpty())
    }

    // =========================================================================
    // obtenerHistorialPorOrganizacion
    // =========================================================================

    @Test
    fun `obtenerHistorialPorOrganizacion reune las gestiones de las oportunidades de esa organizacion`() = runTest {
        coEvery { api.listarUsuarios() } returns listOf(usuarioRemoto(id = 5L, nombre = "Ana Pérez"))
        coEvery { api.listarOportunidades(any()) } returns listOf(
            oportunidadRemota(id = 10L, nombre = "Feria A", organizacion = orgRemota(id = 1L)),
            oportunidadRemota(id = 11L, nombre = "Feria B", organizacion = orgRemota(id = 1L)),
            oportunidadRemota(id = 12L, nombre = "Otra org", organizacion = orgRemota(id = 2L)),
            oportunidadRemota(id = null, nombre = "Sin id", organizacion = orgRemota(id = 1L))
        )
        coEvery { api.listarSeguimientos(10L) } returns listOf(
            seguimientoRemoto(
                id = 1L, tipoAccion = "Llamada", fechaHora = "2026-03-05T10:00:00",
                resultado = "Interesados", idOportunidad = 10L, idUsuario = 5L
            )
        )
        coEvery { api.listarSeguimientos(11L) } returns listOf(
            seguimientoRemoto(
                id = 2L, tipoAccion = "Reunión", fechaHora = "2026-03-07T10:00:00",
                resultado = "Se acordó demo", idOportunidad = 11L, idUsuario = 99L
            ),
            seguimientoRemoto(id = null, fechaHora = "2026-03-06T10:00:00", idOportunidad = 11L, idUsuario = 5L)
        )

        val historial = repo.obtenerHistorialPorOrganizacion(1L, 0L).unico()

        // Más reciente primero
        assertEquals(listOf(2L, 0L, 1L), historial.map { it.idSeguimiento })
        val masReciente = historial[0]
        assertEquals(FechasPrueba.hora("2026-03-07T10:00:00"), masReciente.fechaHora)
        assertEquals("Reunión", masReciente.tipoAccion)
        assertEquals("Se acordó demo", masReciente.resultado)
        assertEquals(11L, masReciente.idOportunidad)
        assertEquals("Feria B", masReciente.nombreOportunidad)
        assertEquals("Usuario #99", masReciente.autor)
        assertNull(masReciente.nombreContacto)
        assertEquals("Ana Pérez", historial[1].autor)
        assertEquals("Feria A", historial[2].nombreOportunidad)
        coVerify(exactly = 0) { api.listarSeguimientos(12L) }
    }

    @Test
    fun `obtenerHistorialPorOrganizacion consulta por responsable solo cuando se indica`() = runTest {
        coEvery { api.listarUsuarios() } returns emptyList()
        coEvery { api.listarOportunidades(any()) } returns emptyList()

        repo.obtenerHistorialPorOrganizacion(1L, 0L).unico()
        repo.obtenerHistorialPorOrganizacion(1L, 8L).unico()

        coVerify(exactly = 1) { api.listarOportunidades(null) }
        coVerify(exactly = 1) { api.listarOportunidades(8L) }
    }

    @Test
    fun `obtenerHistorialPorOrganizacion devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarUsuarios() } throws IOException("sin red")

        assertTrue(repo.obtenerHistorialPorOrganizacion(1L, 0L).unico().isEmpty())
    }
}