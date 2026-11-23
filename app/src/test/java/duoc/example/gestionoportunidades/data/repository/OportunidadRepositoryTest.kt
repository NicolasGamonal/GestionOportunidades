package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.ContactoRemote
import duoc.example.gestionoportunidades.data.remote.OportunidadRemote
import duoc.example.gestionoportunidades.data.remote.OrganizacionRemote
import duoc.example.gestionoportunidades.data.remote.SeguimientoRemote
import duoc.example.gestionoportunidades.model.ConteoEstado
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.contactoLocal
import duoc.example.gestionoportunidades.testutil.contactoRemoto
import duoc.example.gestionoportunidades.testutil.evidenciaRemota
import duoc.example.gestionoportunidades.testutil.fallaCon
import duoc.example.gestionoportunidades.testutil.oportunidadLocal
import duoc.example.gestionoportunidades.testutil.oportunidadRemota
import duoc.example.gestionoportunidades.testutil.orgRemota
import duoc.example.gestionoportunidades.testutil.organizacionLocal
import duoc.example.gestionoportunidades.testutil.respuestaError
import duoc.example.gestionoportunidades.testutil.respuestaOportunidadSinCuerpoDeError
import duoc.example.gestionoportunidades.testutil.respuestaSinCuerpo
import duoc.example.gestionoportunidades.testutil.seguimientoRemoto
import duoc.example.gestionoportunidades.testutil.unico
import duoc.example.gestionoportunidades.util.Regiones
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class OportunidadRepositoryTest {

    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val api = mockk<ApiService>()
    private lateinit var repo: OportunidadRepository

    @Before
    fun crearRepositorio() {
        // Se crea aquí (y no como propiedad) porque el repositorio lee ZoneId.systemDefault()
        // al construirse y la regla ya fijó America/Santiago.
        repo = OportunidadRepository(api)
    }

    private fun stubLista(vararg ops: OportunidadRemote) {
        coEvery { api.listarOportunidades(any()) } returns ops.toList()
    }

    // =========================================================================
    // Conversión remoto → local (se prueba a través de obtenerPorId)
    // =========================================================================

    @Test
    fun `obtenerPorId convierte una oportunidad remota completa al modelo local`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()

        val op = repo.obtenerPorId(10L)!!

        assertEquals(10L, op.idOportunidad)
        assertEquals("Feria del Emprendedor", op.nombre)
        assertEquals("Feria municipal", op.tipoEvento)
        // 00:00 de Santiago = 03:00 UTC (horario de verano, UTC-3)
        assertEquals(FechasPrueba.MARZO_01, op.fechaDeteccion)
        // El horario por defecto "09:00 - 18:00" se aplica al inicio y al término (UTC-3 en marzo).
        assertEquals(FechasPrueba.instante("2026-03-15T12:00:00Z"), op.fechaInicio)    // 15/03 09:00
        assertEquals(FechasPrueba.instante("2026-03-16T21:00:00Z"), op.fechaTermino)   // 16/03 18:00
        assertEquals("Puente Alto", op.comuna)
        assertEquals(500, op.publicoEsperado)
        assertEquals("Alta", op.prioridad)
        assertEquals("Identificada", op.estado)
        assertEquals(150000.0, op.costoReferencial, 0.0)
        assertEquals("Observación", op.observaciones)
        assertEquals(1L, op.idOrganizacion)
        assertEquals(5L, op.idResponsable)
        assertEquals("Descripción del evento", op.descripcion)
        assertEquals("Sitio web", op.fuenteInformacion)
        assertEquals("Alto", op.potencialComercial)
        assertEquals("Alta", op.afinidad)
        assertEquals("Familias", op.publicoObjetivo)
        assertEquals("Arriendo de stand", op.condicionesParticipacion)
        assertEquals("Permiso municipal", op.requisitos)
        assertEquals(3, op.espaciosDisponibles)
    }

    @Test
    fun `obtenerPorId reemplaza los campos nulos por sus valores por defecto`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(
            id = null,
            descripcion = null,
            fuenteInformacion = null,
            comuna = null,
            publicoObjetivo = null,
            publicoEsperado = null,
            costoReferencial = null,
            potencialComercial = null,
            afinidad = null,
            condicionesParticipacion = null,
            requisitos = null,
            espaciosDisponibles = null,
            observaciones = null,
            organizacion = orgRemota(id = null),
            responsable = null
        )

        val op = repo.obtenerPorId(10L)!!

        assertEquals(0L, op.idOportunidad)
        assertEquals("", op.descripcion)
        assertEquals("", op.fuenteInformacion)
        assertEquals("", op.comuna)
        assertEquals("", op.publicoObjetivo)
        assertEquals(0, op.publicoEsperado)
        assertEquals(0.0, op.costoReferencial, 0.0)
        assertEquals("Medio", op.potencialComercial)
        assertEquals("Media", op.afinidad)
        assertEquals("", op.condicionesParticipacion)
        assertEquals("", op.requisitos)
        assertEquals(0, op.espaciosDisponibles)
        assertEquals("", op.observaciones)
        assertEquals(0L, op.idOrganizacion)
        assertNull(op.idResponsable)
    }

    @Test
    fun `obtenerPorId usa la fecha del evento cuando no hay fecha de termino`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns
                oportunidadRemota(fechaEvento = "2026-03-15", fechaTermino = null)

        val op = repo.obtenerPorId(10L)!!

        // Sin fecha de término, el término es el mismo día pero a la hora de término del horario.
        assertEquals(FechasPrueba.instante("2026-03-15T12:00:00Z"), op.fechaInicio)    // 15/03 09:00
        assertEquals(FechasPrueba.instante("2026-03-15T21:00:00Z"), op.fechaTermino)   // 15/03 18:00
    }

    @Test
    fun `las fechas con hora o zona se leen solo por sus primeros 10 caracteres`() = runTest {
        val formatos = listOf("2026-03-15T10:30:00", "2026-03-15T23:59:59.999Z", "2026-03-15")
        for (formato in formatos) {
            coEvery { api.obtenerOportunidad(10L) } returns
                    oportunidadRemota(fechaEvento = formato, fechaTermino = formato)

            val op = repo.obtenerPorId(10L)!!

            assertEquals("Formato $formato", FechasPrueba.instante("2026-03-15T12:00:00Z"), op.fechaInicio)
        }
    }

    @Test
    fun `una fecha en invierno se convierte con el desfase UTC-4 de Santiago`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns
                oportunidadRemota(fechaEvento = "2026-07-10", fechaTermino = "2026-07-10")

        val op = repo.obtenerPorId(10L)!!

        // 09:00 de Santiago en julio (UTC-4) = 13:00 UTC
        assertEquals(FechasPrueba.instante("2026-07-10T13:00:00Z"), op.fechaInicio)
    }

    @Test
    fun `una fecha nula vacia o invalida se reemplaza por el momento actual`() = runTest {
        val invalidas = listOf(null, "", "   ", "mañana", "15/03/2026", "2026-02-31")
        for (fecha in invalidas) {
            coEvery { api.obtenerOportunidad(10L) } returns
                    oportunidadRemota(fechaDeteccion = fecha, fechaEvento = fecha, fechaTermino = null)

            val antes = System.currentTimeMillis()
            val op = repo.obtenerPorId(10L)!!
            val despues = System.currentTimeMillis()

            assertTrue("Fecha '$fecha'", op.fechaDeteccion in antes..despues)
            assertTrue("Fecha '$fecha'", op.fechaInicio in antes..despues)
            assertTrue("Fecha '$fecha'", op.fechaTermino in antes..despues)
        }
    }

    @Test
    fun `obtenerPorId devuelve null cuando la API falla`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } throws IOException("sin red")

        assertNull(repo.obtenerPorId(10L))
    }

    @Test
    fun `observarPorId emite la oportunidad una sola vez`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()

        val op = repo.observarPorId(10L).unico()

        assertEquals("Feria del Emprendedor", op?.nombre)
    }

    @Test
    fun `observarPorId emite null cuando la API falla`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } throws IOException("sin red")

        assertNull(repo.observarPorId(10L).unico())
    }

    // =========================================================================
    // guardarOportunidad y payload
    // =========================================================================

    @Test
    fun `guardarOportunidad devuelve el id que asigna el servidor`() = runTest {
        coEvery { api.crearOportunidad(any()) } returns Response.success(oportunidadRemota(id = 77L))

        assertEquals(77L, repo.guardarOportunidad(oportunidadLocal(idOportunidad = 0L)))
    }

    @Test
    fun `guardarOportunidad devuelve 0 si el servidor responde sin id`() = runTest {
        coEvery { api.crearOportunidad(any()) } returns Response.success(oportunidadRemota(id = null))

        assertEquals(0L, repo.guardarOportunidad(oportunidadLocal()))
    }

    @Test
    fun `guardarOportunidad devuelve 0 si la respuesta no trae cuerpo`() = runTest {
        coEvery { api.crearOportunidad(any()) } returns respuestaSinCuerpo()

        assertEquals(0L, repo.guardarOportunidad(oportunidadLocal()))
    }

    @Test
    fun `guardarOportunidad devuelve 0 si el servidor responde con error`() = runTest {
        coEvery { api.crearOportunidad(any()) } returns respuestaError(400, """{"mensaje":"datos inválidos"}""")

        assertEquals(0L, repo.guardarOportunidad(oportunidadLocal()))
    }

    @Test
    fun `guardarOportunidad devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearOportunidad(any()) } throws IOException("sin red")

        assertEquals(0L, repo.guardarOportunidad(oportunidadLocal()))
    }

    @Test
    fun `el payload tiene exactamente los 24 campos que espera el servidor`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(oportunidadLocal())

        val esperadas = setOf(
            "nombre", "tipo", "descripcion", "fuenteInformacion", "fechaDeteccion", "fechaEvento",
            "fechaTermino", "horarioEvento", "direccion", "comuna", "region", "publicoObjetivo",
            "publicoEsperado", "costoReferencial", "prioridad", "potencialComercial", "afinidad",
            "estado", "condicionesParticipacion", "requisitos", "espaciosDisponibles",
            "observaciones", "organizacion", "responsable"
        )
        assertEquals(esperadas, payload.captured.keys)
    }

    @Test
    fun `el payload lleva los datos de la oportunidad`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(oportunidadLocal())

        val p = payload.captured
        assertEquals("Feria del Emprendedor", p["nombre"])
        assertEquals("Feria municipal", p["tipo"])
        assertEquals("Descripción del evento", p["descripcion"])
        assertEquals("Sitio web", p["fuenteInformacion"])
        assertEquals("2026-03-01", p["fechaDeteccion"])
        assertEquals("2026-03-15", p["fechaEvento"])
        assertEquals("2026-03-16", p["fechaTermino"])
        assertEquals("Puente Alto", p["direccion"])
        assertEquals("Puente Alto", p["comuna"])
        assertEquals("Familias", p["publicoObjetivo"])
        assertEquals(500, p["publicoEsperado"])
        assertEquals(150000.0, p["costoReferencial"])
        assertEquals("Alta", p["prioridad"])
        assertEquals("Alto", p["potencialComercial"])
        assertEquals("Alta", p["afinidad"])
        assertEquals("Identificada", p["estado"])
        assertEquals("Arriendo de stand", p["condicionesParticipacion"])
        assertEquals("Permiso municipal", p["requisitos"])
        assertEquals(3, p["espaciosDisponibles"])
        assertEquals("Observación", p["observaciones"])
        assertEquals(mapOf("id" to 1L), p["organizacion"])
        assertEquals(mapOf("id" to 5L), p["responsable"])
    }

    @Test
    fun `el payload envia responsable nulo cuando la oportunidad no esta asignada`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(oportunidadLocal(idResponsable = null))

        assertTrue(payload.captured.containsKey("responsable"))
        assertNull(payload.captured["responsable"])
    }

    @Test
    fun `las fechas del payload usan el dia de Santiago y no el dia UTC`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())
        // 23:30 del 15/03 en Santiago = 02:30 UTC del 16/03
        val tarde = FechasPrueba.instante("2026-03-16T02:30:00Z")

        repo.guardarOportunidad(oportunidadLocal(fechaInicio = tarde, fechaTermino = tarde))

        assertEquals("2026-03-15", payload.captured["fechaEvento"])
        assertEquals("2026-03-15", payload.captured["fechaTermino"])
    }

    // --- Defecto 2 (corregido): la hora y la región del evento se conservan al guardar y reabrir ---

    @Test
    fun `guardar una oportunidad conserva la hora del evento`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())
        // Evento que parte a las 15:30 de Santiago (18:30 UTC) el 15/03/2026
        val inicio = FechasPrueba.instante("2026-03-15T18:30:00Z")

        repo.guardarOportunidad(oportunidadLocal(fechaInicio = inicio, fechaTermino = inicio))

        val horario = payload.captured["horarioEvento"] as String
        assertTrue("El horario enviado debe incluir 15:30 pero fue '$horario'", horario.contains("15:30"))
    }

    @Test
    fun `el horario del payload lleva la hora de inicio y la de termino`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(
            oportunidadLocal(
                fechaInicio = FechasPrueba.instante("2026-03-15T12:30:00Z"),    // 15/03 09:30
                fechaTermino = FechasPrueba.instante("2026-03-16T21:45:00Z")    // 16/03 18:45
            )
        )

        assertEquals("09:30 - 18:45", payload.captured["horarioEvento"])
    }

    @Test
    fun `el horario del payload usa el desfase de invierno de Santiago`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(
            oportunidadLocal(
                fechaInicio = FechasPrueba.instante("2026-07-10T13:00:00Z"),    // 09:00 (UTC-4)
                fechaTermino = FechasPrueba.instante("2026-07-10T22:00:00Z")    // 18:00 (UTC-4)
            )
        )

        assertEquals("09:00 - 18:00", payload.captured["horarioEvento"])
    }

    @Test
    fun `una oportunidad sin hora a medianoche envia 00 00 en el horario`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(oportunidadLocal())   // fechas a medianoche

        assertEquals("00:00 - 00:00", payload.captured["horarioEvento"])
    }

    @Test
    fun `obtenerPorId aplica el horario del servidor al inicio y al termino`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(
            fechaEvento = "2026-03-15", fechaTermino = "2026-03-15", horarioEvento = "15:30 - 17:45"
        )

        val op = repo.obtenerPorId(10L)!!

        assertEquals(FechasPrueba.instante("2026-03-15T18:30:00Z"), op.fechaInicio)    // 15:30
        assertEquals(FechasPrueba.instante("2026-03-15T20:45:00Z"), op.fechaTermino)   // 17:45
    }

    @Test
    fun `el horario del servidor se lee con o sin espacios alrededor del guion`() = runTest {
        for (horario in listOf("15:30 - 17:45", "15:30-17:45", " 15:30 -17:45 ")) {
            coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(
                fechaEvento = "2026-03-15", fechaTermino = "2026-03-15", horarioEvento = horario
            )

            val op = repo.obtenerPorId(10L)!!

            assertEquals("Horario '$horario'", FechasPrueba.instante("2026-03-15T18:30:00Z"), op.fechaInicio)
            assertEquals("Horario '$horario'", FechasPrueba.instante("2026-03-15T20:45:00Z"), op.fechaTermino)
        }
    }

    @Test
    fun `un horario nulo vacio o ilegible deja las fechas a medianoche`() = runTest {
        val ilegibles = listOf(null, "", "   ", "mañana", "09:00", "25:00 - 18:00", "09:00 - 61:00", "9:00 - 18:00", "09:00 - 12:00 - 18:00")
        for (horario in ilegibles) {
            coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(horarioEvento = horario)

            val op = repo.obtenerPorId(10L)!!

            assertEquals("Horario '$horario'", FechasPrueba.MARZO_15, op.fechaInicio)
            assertEquals("Horario '$horario'", FechasPrueba.MARZO_16, op.fechaTermino)
        }
    }

    @Test
    fun `un evento de varios dias puede terminar a una hora anterior a la de inicio`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(
            fechaEvento = "2026-03-15", fechaTermino = "2026-03-17", horarioEvento = "18:00 - 09:00"
        )

        val op = repo.obtenerPorId(10L)!!

        assertEquals(FechasPrueba.instante("2026-03-15T21:00:00Z"), op.fechaInicio)    // 15/03 18:00
        assertEquals(FechasPrueba.instante("2026-03-17T12:00:00Z"), op.fechaTermino)   // 17/03 09:00
    }

    @Test
    fun `guardar y volver a abrir conserva la fecha y la hora del evento`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())
        val inicio = FechasPrueba.instante("2026-03-15T18:30:00Z")    // 15/03 15:30
        val termino = FechasPrueba.instante("2026-03-16T20:45:00Z")   // 16/03 17:45

        repo.guardarOportunidad(oportunidadLocal(fechaInicio = inicio, fechaTermino = termino))

        // El servidor devuelve lo mismo que recibió.
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(
            fechaEvento = payload.captured["fechaEvento"] as String,
            fechaTermino = payload.captured["fechaTermino"] as String,
            horarioEvento = payload.captured["horarioEvento"] as String
        )
        val reabierta = repo.obtenerPorId(10L)!!

        assertEquals(inicio, reabierta.fechaInicio)
        assertEquals(termino, reabierta.fechaTermino)
    }

    @Test
    fun `cambiarEstado no pisa el horario del evento con el horario por defecto`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(
            fechaEvento = "2026-03-15", fechaTermino = "2026-03-15", horarioEvento = "15:30 - 17:45"
        )
        val payload = slot<Map<String, Any?>>()
        coEvery { api.actualizarOportunidad(10L, 3L, capture(payload)) } returns Response.success(oportunidadRemota())
        coEvery { api.crearSeguimiento(any()) } returns Response.success(seguimientoRemoto())

        repo.cambiarEstado(10L, "Identificada", "Contacto realizado", 3L)

        assertEquals("15:30 - 17:45", payload.captured["horarioEvento"])
    }

    // --- Defecto 2: la región se deduce de la comuna ---

    @Test
    fun `guardar una oportunidad conserva la region de su comuna`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(oportunidadLocal(comuna = "Valparaíso"))

        assertEquals(Regiones.regionDe("Valparaíso"), payload.captured["region"])
        assertEquals("Valparaíso", payload.captured["region"])
    }

    @Test
    fun `la region del payload cambia segun la comuna y ya no es siempre Metropolitana`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(oportunidadLocal(comuna = "Puente Alto"))
        assertEquals("Región Metropolitana de Santiago", payload.captured["region"])

        repo.guardarOportunidad(oportunidadLocal(comuna = "Arica"))
        assertEquals("Arica y Parinacota", payload.captured["region"])
    }

    @Test
    fun `una comuna fuera del catalogo conserva la region historica Metropolitana`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.crearOportunidad(capture(payload)) } returns Response.success(oportunidadRemota())

        repo.guardarOportunidad(oportunidadLocal(comuna = "Narnia"))

        assertEquals("Metropolitana", payload.captured["region"])
    }

    @Test
    fun `modificar una oportunidad tambien envia la region de su comuna`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.actualizarOportunidad(any(), any(), capture(payload)) } returns Response.success(oportunidadRemota())

        repo.modificarOportunidad(oportunidadLocal(idOportunidad = 10L, comuna = "Valparaíso"))

        assertEquals("Valparaíso", payload.captured["region"])
    }

    // =========================================================================
    // modificarOportunidad
    // =========================================================================

    @Test
    fun `modificarOportunidad actualiza con el responsable como usuario actor`() = runTest {
        val payload = slot<Map<String, Any?>>()
        coEvery { api.actualizarOportunidad(10L, 5L, capture(payload)) } returns
                Response.success(oportunidadRemota())

        repo.modificarOportunidad(oportunidadLocal(idOportunidad = 10L, idResponsable = 5L, nombre = "Nuevo nombre"))

        coVerify(exactly = 1) { api.actualizarOportunidad(10L, 5L, any()) }
        assertEquals("Nuevo nombre", payload.captured["nombre"])
    }

    @Test
    fun `modificarOportunidad usa el usuario 1 si la oportunidad no tiene responsable`() = runTest {
        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns Response.success(oportunidadRemota())

        repo.modificarOportunidad(oportunidadLocal(idOportunidad = 10L, idResponsable = null))

        coVerify(exactly = 1) { api.actualizarOportunidad(10L, 1L, any()) }
    }

    @Test
    fun `modificarOportunidad no propaga el error de red y devuelve false`() = runTest {
        coEvery { api.actualizarOportunidad(any(), any(), any()) } throws IOException("sin red")

        assertEquals(false, repo.modificarOportunidad(oportunidadLocal()))

        coVerify(exactly = 1) { api.actualizarOportunidad(any(), any(), any()) }
    }

    @Test
    fun `modificarOportunidad devuelve true si la API acepta y false si responde error`() = runTest {
        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns Response.success(oportunidadRemota())
        assertEquals(true, repo.modificarOportunidad(oportunidadLocal()))

        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns respuestaError(500)
        assertEquals(false, repo.modificarOportunidad(oportunidadLocal()))
    }

    // =========================================================================
    // Consultas con filtros
    // =========================================================================

    @Test
    fun `obtenerTodasLasOportunidades sin busqueda devuelve todas`() = runTest {
        stubLista(oportunidadRemota(id = 1L, nombre = "Feria A"), oportunidadRemota(id = 2L, nombre = "Jornada B"))

        val lista = repo.obtenerTodasLasOportunidades("").unico()

        assertEquals(listOf("Feria A", "Jornada B"), lista.map { it.nombre })
        coVerify(exactly = 1) { api.listarOportunidades(null) }
    }

    @Test
    fun `obtenerTodasLasOportunidades filtra por nombre sin distinguir mayusculas ni espacios`() = runTest {
        stubLista(oportunidadRemota(id = 1L, nombre = "Feria del Libro"), oportunidadRemota(id = 2L, nombre = "Jornada Deportiva"))

        val lista = repo.obtenerTodasLasOportunidades("  FERIA ").unico()

        assertEquals(listOf("Feria del Libro"), lista.map { it.nombre })
    }

    @Test
    fun `obtenerTodasLasOportunidades con busqueda en blanco no filtra`() = runTest {
        stubLista(oportunidadRemota(id = 1L, nombre = "Feria A"), oportunidadRemota(id = 2L, nombre = "Jornada B"))

        assertEquals(2, repo.obtenerTodasLasOportunidades("   ").unico().size)
    }

    @Test
    fun `obtenerTodasLasOportunidades sin coincidencias devuelve lista vacia`() = runTest {
        stubLista(oportunidadRemota(nombre = "Feria A"))

        assertTrue(repo.obtenerTodasLasOportunidades("zzz").unico().isEmpty())
    }

    @Test
    fun `obtenerTodasLasOportunidades devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        assertTrue(repo.obtenerTodasLasOportunidades("").unico().isEmpty())
    }

    private fun variasParaFiltrar() = stubLista(
        oportunidadRemota(id = 1L, nombre = "Feria Verde", estado = "Identificada", prioridad = "Alta"),
        oportunidadRemota(id = 2L, nombre = "Feria Azul", estado = "Confirmada", prioridad = "Alta"),
        oportunidadRemota(id = 3L, nombre = "Jornada Verde", estado = "Identificada", prioridad = "Baja"),
        oportunidadRemota(id = 4L, nombre = "Expo Roja", estado = "Descartada", prioridad = "Media")
    )

    @Test
    fun `filtrarOportunidades sin filtros devuelve todas`() = runTest {
        variasParaFiltrar()

        assertEquals(4, repo.filtrarOportunidades("", "", "").unico().size)
    }

    @Test
    fun `filtrarOportunidades filtra por estado ignorando mayusculas`() = runTest {
        variasParaFiltrar()

        val lista = repo.filtrarOportunidades("IDENTIFICADA", "", "").unico()

        assertEquals(listOf(1L, 3L), lista.map { it.idOportunidad })
    }

    @Test
    fun `filtrarOportunidades filtra por prioridad`() = runTest {
        variasParaFiltrar()

        val lista = repo.filtrarOportunidades("", "alta", "").unico()

        assertEquals(listOf(1L, 2L), lista.map { it.idOportunidad })
    }

    @Test
    fun `filtrarOportunidades filtra por texto en el nombre`() = runTest {
        variasParaFiltrar()

        val lista = repo.filtrarOportunidades("", "", " verde ").unico()

        assertEquals(listOf(1L, 3L), lista.map { it.idOportunidad })
    }

    @Test
    fun `filtrarOportunidades combina estado prioridad y texto`() = runTest {
        variasParaFiltrar()

        val lista = repo.filtrarOportunidades("Identificada", "Alta", "verde").unico()

        assertEquals(listOf(1L), lista.map { it.idOportunidad })
    }

    @Test
    fun `filtrarOportunidades devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        assertTrue(repo.filtrarOportunidades("", "", "").unico().isEmpty())
    }

    @Test
    fun `obtenerPorResponsable consulta con el id del usuario y convierte los datos`() = runTest {
        stubLista(oportunidadRemota(id = 1L), oportunidadRemota(id = 2L))

        val lista = repo.obtenerPorResponsable(7L).unico()

        assertEquals(listOf(1L, 2L), lista.map { it.idOportunidad })
        coVerify(exactly = 1) { api.listarOportunidades(7L) }
    }

    @Test
    fun `obtenerPorResponsable devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        assertTrue(repo.obtenerPorResponsable(7L).unico().isEmpty())
    }

    @Test
    fun `obtenerEventosProximos filtra por fecha excluye descartadas y ordena por inicio`() = runTest {
        stubLista(
            oportunidadRemota(id = 1L, nombre = "Pasado", fechaEvento = "2026-03-10"),
            oportunidadRemota(id = 2L, nombre = "Tardío", fechaEvento = "2026-03-25"),
            oportunidadRemota(id = 3L, nombre = "Justo hoy", fechaEvento = "2026-03-15"),
            oportunidadRemota(id = 4L, nombre = "Descartado", fechaEvento = "2026-03-20", estado = "Descartada"),
            oportunidadRemota(id = 5L, nombre = "Descartado en minúscula", fechaEvento = "2026-03-21", estado = "descartada"),
            oportunidadRemota(id = 6L, nombre = "Medio", fechaEvento = "2026-03-20")
        )

        val lista = repo.obtenerEventosProximos(FechasPrueba.dia("2026-03-15"), 0L).unico()

        // El del mismo día de "desde" se incluye; los descartados y el pasado no.
        assertEquals(listOf(3L, 6L, 2L), lista.map { it.idOportunidad })
    }

    @Test
    fun `obtenerEventosProximos con responsable 0 consulta todas y con id consulta solo esa persona`() = runTest {
        stubLista()

        repo.obtenerEventosProximos(0L, 0L).unico()
        repo.obtenerEventosProximos(0L, 9L).unico()

        coVerify(exactly = 1) { api.listarOportunidades(null) }
        coVerify(exactly = 1) { api.listarOportunidades(9L) }
    }

    @Test
    fun `obtenerEventosProximos propaga el error de red en vez de devolver lista vacia`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        val error = fallaCon { repo.obtenerEventosProximos(0L, 0L).unico() }

        assertTrue(error is IOException)
    }

    @Test
    fun `buscar aplica los tres filtros y consulta por responsable`() = runTest {
        variasParaFiltrar()

        val lista = repo.buscar("Identificada", "Alta", "verde", 9L).unico()

        assertEquals(listOf(1L), lista.map { it.idOportunidad })
        coVerify(exactly = 1) { api.listarOportunidades(9L) }
    }

    @Test
    fun `buscar con responsable 0 consulta todas las oportunidades`() = runTest {
        variasParaFiltrar()

        val lista = repo.buscar("", "", "", 0L).unico()

        assertEquals(4, lista.size)
        coVerify(exactly = 1) { api.listarOportunidades(null) }
    }

    @Test
    fun `buscar devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        assertTrue(repo.buscar("", "", "", 0L).unico().isEmpty())
    }

    // =========================================================================
    // Organizaciones y contactos (compatibilidad con los ViewModels)
    // =========================================================================

    @Test
    fun `guardarOrganizacion envia los datos y devuelve el id asignado`() = runTest {
        val enviada = slot<OrganizacionRemote>()
        coEvery { api.crearOrganizacion(capture(enviada)) } returns Response.success(orgRemota(id = 31L))

        val id = repo.guardarOrganizacion(organizacionLocal(idOrganizacion = 0L))

        assertEquals(31L, id)
        assertNull(enviada.captured.id)
        assertEquals("Municipalidad de Puente Alto", enviada.captured.nombre)
        assertEquals("Municipalidad", enviada.captured.tipo)
        assertEquals("Gobierno local", enviada.captured.rubro)
        assertEquals("Puente Alto", enviada.captured.comuna)
        assertEquals("Región Metropolitana de Santiago", enviada.captured.region)
        assertEquals("", enviada.captured.sitioWeb)
        assertEquals("", enviada.captured.publicoAsociado)
        assertEquals("", enviada.captured.observaciones)
    }

    @Test
    fun `guardarOrganizacion devuelve 0 si la respuesta trae error o no trae id`() = runTest {
        coEvery { api.crearOrganizacion(any()) } returns respuestaError(500)
        assertEquals(0L, repo.guardarOrganizacion(organizacionLocal()))

        coEvery { api.crearOrganizacion(any()) } returns Response.success(orgRemota(id = null))
        assertEquals(0L, repo.guardarOrganizacion(organizacionLocal()))
    }

    @Test
    fun `guardarOrganizacion devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearOrganizacion(any()) } throws IOException("sin red")

        assertEquals(0L, repo.guardarOrganizacion(organizacionLocal()))
    }

    @Test
    fun `obtenerOrganizaciones convierte la lista y reemplaza nulos por vacio`() = runTest {
        coEvery { api.listarOrganizaciones() } returns listOf(
            orgRemota(id = 1L, nombre = "Muni", tipo = "Municipalidad", rubro = "Gobierno", comuna = "Puente Alto"),
            orgRemota(id = null, nombre = "Sin datos", tipo = "Empresa", rubro = null, comuna = null)
        )

        val lista = repo.obtenerOrganizaciones().unico()

        assertEquals(2, lista.size)
        assertEquals(organizacionLocal(1L, "Muni", "Municipalidad", "Gobierno", "Puente Alto"), lista[0])
        assertEquals(organizacionLocal(0L, "Sin datos", "Empresa", "", ""), lista[1])
    }

    @Test
    fun `obtenerOrganizaciones devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarOrganizaciones() } throws IOException("sin red")

        assertTrue(repo.obtenerOrganizaciones().unico().isEmpty())
    }

    @Test
    fun `guardarContacto envia el contacto como activo y devuelve el id asignado`() = runTest {
        val enviado = slot<ContactoRemote>()
        coEvery { api.crearContacto(capture(enviado)) } returns Response.success(contactoRemoto(id = 55L))

        val id = repo.guardarContacto(contactoLocal(idContacto = 0L, idOrganizacion = 3L))

        assertEquals(55L, id)
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
    fun `guardarContacto devuelve 0 si la respuesta trae error o no trae id`() = runTest {
        coEvery { api.crearContacto(any()) } returns respuestaError(400)
        assertEquals(0L, repo.guardarContacto(contactoLocal()))

        coEvery { api.crearContacto(any()) } returns Response.success(contactoRemoto(id = null))
        assertEquals(0L, repo.guardarContacto(contactoLocal()))
    }

    @Test
    fun `guardarContacto devuelve 0 si la red falla`() = runTest {
        coEvery { api.crearContacto(any()) } throws IOException("sin red")

        assertEquals(0L, repo.guardarContacto(contactoLocal()))
    }

    @Test
    fun `obtenerContactosPorOrganizacion convierte los contactos y completa los nulos`() = runTest {
        coEvery { api.listarContactos(3L) } returns listOf(
            contactoRemoto(id = 1L, idOrganizacion = 3L),
            contactoRemoto(id = null, cargo = null, area = null, medioPreferente = null, idOrganizacion = 3L)
        )

        val lista = repo.obtenerContactosPorOrganizacion(3L).unico()

        assertEquals(contactoLocal(idContacto = 1L, idOrganizacion = 3L), lista[0])
        assertEquals(
            contactoLocal(idContacto = 0L, cargo = "", area = "", medioPreferente = "Correo", idOrganizacion = 3L),
            lista[1]
        )
    }

    @Test
    fun `obtenerContactosPorOrganizacion devuelve lista vacia si la red falla`() = runTest {
        coEvery { api.listarContactos(any()) } throws IOException("sin red")

        assertTrue(repo.obtenerContactosPorOrganizacion(3L).unico().isEmpty())
    }

    // =========================================================================
    // Alertas e indicadores
    // =========================================================================

    @Test
    fun `obtenerSinGestiones devuelve solo las oportunidades sin seguimientos`() = runTest {
        stubLista(oportunidadRemota(id = 10L), oportunidadRemota(id = 11L))
        coEvery { api.listarSeguimientos(10L) } returns emptyList()
        coEvery { api.listarSeguimientos(11L) } returns listOf(seguimientoRemoto(idOportunidad = 11L))

        val lista = repo.obtenerSinGestiones(0L).unico()

        assertEquals(listOf(10L), lista.map { it.idOportunidad })
        coVerify(exactly = 1) { api.listarOportunidades(null) }
    }

    @Test
    fun `obtenerSinGestiones consulta por responsable cuando se indica`() = runTest {
        stubLista()

        repo.obtenerSinGestiones(4L).unico()

        coVerify(exactly = 1) { api.listarOportunidades(4L) }
    }

    @Test
    fun `obtenerSinGestiones propaga el error de red en vez de devolver lista vacia`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        val error = fallaCon { repo.obtenerSinGestiones(0L).unico() }

        assertTrue(error is IOException)
    }

    @Test
    fun `obtenerSinContacto devuelve solo las oportunidades cuya organizacion no tiene contactos`() = runTest {
        stubLista(
            oportunidadRemota(id = 10L, organizacion = orgRemota(id = 1L)),
            oportunidadRemota(id = 11L, organizacion = orgRemota(id = 2L))
        )
        coEvery { api.listarContactos(1L) } returns emptyList()
        coEvery { api.listarContactos(2L) } returns listOf(contactoRemoto(idOrganizacion = 2L))

        val lista = repo.obtenerSinContacto(0L).unico()

        assertEquals(listOf(10L), lista.map { it.idOportunidad })
    }

    @Test
    fun `obtenerSinContacto consulta por responsable cuando se indica`() = runTest {
        stubLista()

        repo.obtenerSinContacto(4L).unico()

        coVerify(exactly = 1) { api.listarOportunidades(4L) }
    }

    @Test
    fun `obtenerSinContacto propaga el error de red en vez de devolver lista vacia`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        val error = fallaCon { repo.obtenerSinContacto(0L).unico() }

        assertTrue(error is IOException)
    }

    @Test
    fun `contarPorEstado agrupa las oportunidades por estado`() = runTest {
        stubLista(
            oportunidadRemota(id = 1L, estado = "Identificada"),
            oportunidadRemota(id = 2L, estado = "Confirmada"),
            oportunidadRemota(id = 3L, estado = "Identificada")
        )

        val conteos = repo.contarPorEstado(0L).unico()

        assertEquals(listOf(ConteoEstado("Identificada", 2), ConteoEstado("Confirmada", 1)), conteos)
    }

    @Test
    fun `contarPorEstado sin oportunidades devuelve lista vacia y respeta el responsable`() = runTest {
        stubLista()

        assertTrue(repo.contarPorEstado(6L).unico().isEmpty())
        coVerify(exactly = 1) { api.listarOportunidades(6L) }
    }

    @Test
    fun `contarPorEstado propaga el error de red en vez de devolver lista vacia`() = runTest {
        coEvery { api.listarOportunidades(any()) } throws IOException("sin red")

        val error = fallaCon { repo.contarPorEstado(0L).unico() }

        assertTrue(error is IOException)
    }

    // =========================================================================
    // actualizarEstado
    // =========================================================================

    @Test
    fun `actualizarEstado envia la oportunidad con el nuevo estado y el responsable como actor`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = "Identificada")
        val payload = slot<Map<String, Any?>>()
        coEvery { api.actualizarOportunidad(10L, 5L, capture(payload)) } returns Response.success(oportunidadRemota())

        repo.actualizarEstado(10L, "Contacto realizado")

        assertEquals("Contacto realizado", payload.captured["estado"])
        assertEquals("Feria del Emprendedor", payload.captured["nombre"])
        coVerify(exactly = 1) { api.actualizarOportunidad(10L, 5L, any()) }
    }

    @Test
    fun `actualizarEstado usa el usuario 1 si la oportunidad no tiene responsable`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(responsable = null)
        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns Response.success(oportunidadRemota())

        repo.actualizarEstado(10L, "Confirmada")

        coVerify(exactly = 1) { api.actualizarOportunidad(10L, 1L, any()) }
    }

    @Test
    fun `actualizarEstado no actualiza si no puede leer la oportunidad`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } throws IOException("sin red")

        repo.actualizarEstado(10L, "Confirmada")

        coVerify(exactly = 0) { api.actualizarOportunidad(any(), any(), any()) }
    }

    @Test
    fun `actualizarEstado no propaga el error al actualizar`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()
        coEvery { api.actualizarOportunidad(any(), any(), any()) } throws IOException("sin red")

        repo.actualizarEstado(10L, "Confirmada")

        coVerify(exactly = 1) { api.actualizarOportunidad(any(), any(), any()) }
    }

    // =========================================================================
    // cambiarEstado
    // =========================================================================

    @Test
    fun `cambiarEstado actualiza la oportunidad y registra el cambio en el historial`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = "Identificada")
        val payload = slot<Map<String, Any?>>()
        coEvery { api.actualizarOportunidad(10L, 3L, capture(payload)) } returns Response.success(oportunidadRemota())
        val seguimiento = slot<SeguimientoRemote>()
        coEvery { api.crearSeguimiento(capture(seguimiento)) } returns Response.success(seguimientoRemoto())

        val antes = LocalDate.now().toString()
        repo.cambiarEstado(10L, "Identificada", "Contacto realizado", 3L)
        val despues = LocalDate.now().toString()

        assertEquals("Contacto realizado", payload.captured["estado"])

        val s = seguimiento.captured
        assertEquals("Cambio de estado", s.tipoAccion)
        assertEquals("Identificada → Contacto realizado", s.resultado)
        assertEquals("", s.proximaAccion)
        assertEquals("", s.observaciones)
        assertTrue(s.fechaProximoSeguimiento == antes || s.fechaProximoSeguimiento == despues)
        assertEquals(10L, s.oportunidad.id)
        assertEquals(3L, s.usuario.id)
        assertNull(s.id)
    }

    @Test
    fun `cambiarEstado lanza excepcion si la oportunidad no existe`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } throws IOException("404")

        val error = fallaCon { repo.cambiarEstado(10L, "Identificada", "Confirmada", 3L) }

        assertEquals("Oportunidad no encontrada", error.message)
        coVerify(exactly = 0) { api.actualizarOportunidad(any(), any(), any()) }
        coVerify(exactly = 0) { api.crearSeguimiento(any()) }
    }

    @Test
    fun `cambiarEstado muestra el mensaje del servidor cuando rechaza el cambio`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()
        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns
                respuestaError(400, """{"mensaje":"Solo el supervisor puede confirmar"}""")

        val error = fallaCon { repo.cambiarEstado(10L, "Identificada", "Confirmada", 3L) }

        assertEquals("Solo el supervisor puede confirmar", error.message)
        coVerify(exactly = 0) { api.crearSeguimiento(any()) }
    }

    @Test
    fun `cambiarEstado usa el cuerpo completo si el error no es JSON`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()
        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns respuestaError(500, "Error interno")

        val error = fallaCon { repo.cambiarEstado(10L, "Identificada", "Confirmada", 3L) }

        assertEquals("Error interno", error.message)
    }

    @Test
    fun `cambiarEstado usa el cuerpo completo si el JSON no trae mensaje`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()
        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns respuestaError(400, """{"error":"x"}""")

        val error = fallaCon { repo.cambiarEstado(10L, "Identificada", "Confirmada", 3L) }

        assertEquals("""{"error":"x"}""", error.message)
    }

    @Test
    fun `cambiarEstado usa un mensaje generico si el error no trae cuerpo`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()
        coEvery { api.actualizarOportunidad(any(), any(), any()) } returns respuestaOportunidadSinCuerpoDeError()

        val error = fallaCon { repo.cambiarEstado(10L, "Identificada", "Confirmada", 3L) }

        assertEquals("No se pudo cambiar el estado", error.message)
    }

    @Test
    fun `cambiarEstado propaga el error de red al actualizar`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota()
        coEvery { api.actualizarOportunidad(any(), any(), any()) } throws IOException("sin red")

        val error = fallaCon { repo.cambiarEstado(10L, "Identificada", "Confirmada", 3L) }

        assertTrue(error is IOException)
        assertNotNull(error.message)
    }

    // =========================================================================
    // eliminar
    // =========================================================================

    @Test
    fun `eliminar una oportunidad Identificada sin gestiones ni fotos devuelve null`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = "Identificada")
        coEvery { api.listarSeguimientos(10L) } returns emptyList()
        coEvery { api.listarEvidencias(10L) } returns emptyList()
        coEvery { api.eliminarOportunidad(10L) } returns Response.success(Unit)

        assertNull(repo.eliminar(10L))

        coVerify(exactly = 1) { api.eliminarOportunidad(10L) }
    }

    @Test
    fun `eliminar informa que la oportunidad ya no existe`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } throws IOException("404")

        assertEquals("La oportunidad ya no existe.", repo.eliminar(10L))
        coVerify(exactly = 0) { api.eliminarOportunidad(any()) }
    }

    @Test
    fun `eliminar rechaza toda oportunidad que no este en estado Identificada`() = runTest {
        val otrosEstados = listOf(
            "Contacto realizado", "Propuesta enviada", "En espera de respuesta", "Confirmada", "Descartada"
        )
        for (estado in otrosEstados) {
            coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = estado)

            assertEquals(
                "Estado $estado",
                "Solo se pueden eliminar oportunidades en estado Identificada. " +
                        "Las demás se descartan para conservar su historial.",
                repo.eliminar(10L)
            )
        }
        coVerify(exactly = 0) { api.eliminarOportunidad(any()) }
    }

    @Test
    fun `eliminar rechaza una oportunidad que tiene gestiones registradas`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = "Identificada")
        coEvery { api.listarSeguimientos(10L) } returns listOf(seguimientoRemoto())

        assertEquals(
            "Tiene gestiones registradas; no se puede eliminar para conservar el historial.",
            repo.eliminar(10L)
        )
        coVerify(exactly = 0) { api.eliminarOportunidad(any()) }
    }

    @Test
    fun `eliminar rechaza una oportunidad que tiene fotos de evidencia`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = "Identificada")
        coEvery { api.listarSeguimientos(10L) } returns emptyList()
        coEvery { api.listarEvidencias(10L) } returns listOf(evidenciaRemota())

        assertEquals("Tiene fotos de evidencia; elimínalas primero desde el detalle.", repo.eliminar(10L))
        coVerify(exactly = 0) { api.eliminarOportunidad(any()) }
    }

    @Test
    fun `eliminar informa cuando el servidor no pudo borrarla`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = "Identificada")
        coEvery { api.listarSeguimientos(10L) } returns emptyList()
        coEvery { api.listarEvidencias(10L) } returns emptyList()
        coEvery { api.eliminarOportunidad(10L) } returns respuestaError(409)

        assertEquals("No se pudo eliminar la oportunidad.", repo.eliminar(10L))
    }

    @Test
    fun `eliminar informa el error de conexion con el detalle`() = runTest {
        coEvery { api.obtenerOportunidad(10L) } returns oportunidadRemota(estado = "Identificada")
        coEvery { api.listarSeguimientos(10L) } throws IOException("sin red")

        assertEquals("Error de conexión: sin red", repo.eliminar(10L))
    }
}