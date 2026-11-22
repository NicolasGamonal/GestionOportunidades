package duoc.example.gestionoportunidades.ui.alertas

import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.model.ConteoEstado
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.iniciarSesionComo
import duoc.example.gestionoportunidades.testutil.oportunidadLocal
import duoc.example.gestionoportunidades.testutil.suscribir
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val oportunidadRepo = mockk<OportunidadRepository>()
    private val seguimientoRepo = mockk<SeguimientoRepository>()

    private val unDia = 24L * 60 * 60 * 1000

    private fun alerta(id: Long, fechaProximo: Long) = AlertaSeguimiento(
        idOportunidad = id, nombreOportunidad = "Oportunidad $id", nombreResponsable = "Ana Pérez",
        tipoAccion = "Llamada", proximaAccion = "Enviar propuesta", fechaProximo = fechaProximo
    )

    /** Deja todos los repositorios devolviendo lo indicado, para cualquier id de responsable. */
    private fun stubs(
        proximos: Flow<List<Oportunidad>> = flowOf(emptyList()),
        gestiones: Flow<List<AlertaSeguimiento>> = flowOf(emptyList()),
        sinGestiones: Flow<List<Oportunidad>> = flowOf(emptyList()),
        sinContacto: Flow<List<Oportunidad>> = flowOf(emptyList()),
        conteos: Flow<List<ConteoEstado>> = flowOf(emptyList())
    ) {
        every { oportunidadRepo.obtenerEventosProximos(any(), any()) } returns proximos
        every { seguimientoRepo.obtenerUltimasGestiones(any()) } returns gestiones
        every { oportunidadRepo.obtenerSinGestiones(any()) } returns sinGestiones
        every { oportunidadRepo.obtenerSinContacto(any()) } returns sinContacto
        every { oportunidadRepo.contarPorEstado(any()) } returns conteos
    }

    private fun crearVm() = DashboardViewModel(oportunidadRepo, seguimientoRepo)

    // =========================================================================
    // Estado y datos
    // =========================================================================

    @Test
    fun `el estado inicial es cargando y sin datos`() {
        stubs()
        val vm = crearVm()

        assertEquals(DashboardUiState(), vm.uiState.value)
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `sin datos deja las listas vacias, termina de cargar y no marca error`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        stubs()
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.isLoading)
        assertNull(estado.error)
        assertTrue(estado.eventosProximos.isEmpty())
        assertTrue(estado.seguimientosPendientes.isEmpty())
        assertTrue(estado.seguimientosAtrasados.isEmpty())
        assertTrue(estado.sinSeguimiento.isEmpty())
        assertTrue(estado.sinContacto.isEmpty())
        assertEquals(IndicadoresEquipo(), estado.indicadores)
    }

    @Test
    fun `expone eventos proximos y oportunidades sin gestiones ni contacto tal como llegan`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val proximos = listOf(oportunidadLocal(idOportunidad = 1L), oportunidadLocal(idOportunidad = 2L))
        val sinGestiones = listOf(oportunidadLocal(idOportunidad = 3L))
        val sinContacto = listOf(oportunidadLocal(idOportunidad = 4L))
        stubs(
            proximos = flowOf(proximos), sinGestiones = flowOf(sinGestiones), sinContacto = flowOf(sinContacto)
        )
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(proximos, vm.uiState.value.eventosProximos)
        assertEquals(sinGestiones, vm.uiState.value.sinSeguimiento)
        assertEquals(sinContacto, vm.uiState.value.sinContacto)
    }

    // =========================================================================
    // Responsable según la sesión
    // =========================================================================

    @Test
    fun `sin sesion consulta con id menos 1`() {
        stubs()
        crearVm()

        verify(exactly = 1) { oportunidadRepo.obtenerEventosProximos(any(), -1L) }
        verify(exactly = 1) { seguimientoRepo.obtenerUltimasGestiones(-1L) }
        verify(exactly = 1) { oportunidadRepo.obtenerSinGestiones(-1L) }
        verify(exactly = 1) { oportunidadRepo.obtenerSinContacto(-1L) }
        verify(exactly = 1) { oportunidadRepo.contarPorEstado(-1L) }
    }

    @Test
    fun `el supervisor consulta a todo el equipo con id 0`() {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        stubs()
        crearVm()

        verify(exactly = 1) { oportunidadRepo.obtenerEventosProximos(any(), 0L) }
        verify(exactly = 1) { seguimientoRepo.obtenerUltimasGestiones(0L) }
        verify(exactly = 1) { oportunidadRepo.obtenerSinGestiones(0L) }
        verify(exactly = 1) { oportunidadRepo.obtenerSinContacto(0L) }
        verify(exactly = 1) { oportunidadRepo.contarPorEstado(0L) }
    }

    @Test
    fun `el ejecutivo consulta solo lo suyo con su id`() {
        iniciarSesionComo("EJECUTIVO", id = 9L)
        stubs()
        crearVm()

        verify(exactly = 1) { oportunidadRepo.obtenerEventosProximos(any(), 9L) }
        verify(exactly = 1) { seguimientoRepo.obtenerUltimasGestiones(9L) }
        verify(exactly = 1) { oportunidadRepo.obtenerSinGestiones(9L) }
        verify(exactly = 1) { oportunidadRepo.obtenerSinContacto(9L) }
        verify(exactly = 1) { oportunidadRepo.contarPorEstado(9L) }
    }

    @Test
    fun `los eventos proximos se piden desde la medianoche de hoy en Santiago`() {
        iniciarSesionComo("SUPERVISOR")
        val desde = slot<Long>()
        stubs()
        every { oportunidadRepo.obtenerEventosProximos(capture(desde), any()) } returns flowOf(emptyList())

        crearVm()

        val zona = ZoneId.of("America/Santiago")
        val medianocheHoy = LocalDate.now(zona).atStartOfDay(zona).toInstant().toEpochMilli()
        assertEquals(medianocheHoy, desde.captured)
    }

    // =========================================================================
    // Pendientes y atrasados
    // =========================================================================

    @Test
    fun `separa los seguimientos en pendientes y atrasados segun la fecha proxima`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val ahora = System.currentTimeMillis()
        val manana = alerta(1L, ahora + unDia)
        val ayer = alerta(2L, ahora - unDia)
        val enUnaSemana = alerta(3L, ahora + 7 * unDia)
        val haceUnaSemana = alerta(4L, ahora - 7 * unDia)
        stubs(gestiones = flowOf(listOf(manana, ayer, enUnaSemana, haceUnaSemana)))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(listOf(manana, enUnaSemana), vm.uiState.value.seguimientosPendientes)
        assertEquals(listOf(ayer, haceUnaSemana), vm.uiState.value.seguimientosAtrasados)
    }

    @Test
    fun `sin gestiones no hay pendientes ni atrasados`() = runTest {
        iniciarSesionComo("EJECUTIVO")
        stubs(gestiones = flowOf(emptyList()))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.seguimientosPendientes.isEmpty())
        assertTrue(vm.uiState.value.seguimientosAtrasados.isEmpty())
    }

    @Test
    fun `si todas las gestiones estan vencidas no hay pendientes`() = runTest {
        iniciarSesionComo("EJECUTIVO")
        val ahora = System.currentTimeMillis()
        stubs(gestiones = flowOf(listOf(alerta(1L, ahora - unDia), alerta(2L, ahora - 2 * unDia))))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.seguimientosPendientes.isEmpty())
        assertEquals(2, vm.uiState.value.seguimientosAtrasados.size)
    }

    // =========================================================================
    // Indicadores del equipo
    // =========================================================================

    @Test
    fun `la conversion es confirmadas por 100 sobre el total`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val conteos = listOf(ConteoEstado("Identificada", 3), ConteoEstado("Confirmada", 1))
        stubs(conteos = flowOf(conteos))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        val indicadores = vm.uiState.value.indicadores
        assertEquals(4, indicadores.total)
        assertEquals(conteos, indicadores.porEstado)
        assertEquals(25, indicadores.conversionPorcentaje)
    }

    @Test
    fun `un tercio de confirmadas da 33 por ciento`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        // 1 de 3 = 33,33 % -> 33
        stubs(conteos = flowOf(listOf(ConteoEstado("Identificada", 2), ConteoEstado("Confirmada", 1))))
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()
        assertEquals(33, vm.uiState.value.indicadores.conversionPorcentaje)
    }

    @Test
    fun `dos de tres confirmadas dan 67 por ciento`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        stubs(conteos = flowOf(listOf(ConteoEstado("Identificada", 1), ConteoEstado("Confirmada", 2))))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(67, vm.uiState.value.indicadores.conversionPorcentaje)
    }

    @Test
    fun `el medio por ciento se redondea hacia arriba`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        // 1 de 8 = 12,5 % -> 13
        stubs(conteos = flowOf(listOf(ConteoEstado("Identificada", 7), ConteoEstado("Confirmada", 1))))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(13, vm.uiState.value.indicadores.conversionPorcentaje)
    }

    @Test
    fun `todas confirmadas dan 100 por ciento`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        stubs(conteos = flowOf(listOf(ConteoEstado("Confirmada", 5))))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(100, vm.uiState.value.indicadores.conversionPorcentaje)
    }

    @Test
    fun `sin ninguna confirmada la conversion es 0 y no null`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        stubs(conteos = flowOf(listOf(ConteoEstado("Identificada", 4), ConteoEstado("Descartada", 1))))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(5, vm.uiState.value.indicadores.total)
        assertEquals(0, vm.uiState.value.indicadores.conversionPorcentaje)
    }

    @Test
    fun `con total 0 la conversion es null`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        stubs(conteos = flowOf(emptyList()))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.indicadores.total)
        assertNull(vm.uiState.value.indicadores.conversionPorcentaje)
    }

    @Test
    fun `una fila Confirmada con cantidad 0 no da conversion si el total es 0`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        stubs(conteos = flowOf(listOf(ConteoEstado("Confirmada", 0))))
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertNull(vm.uiState.value.indicadores.conversionPorcentaje)
    }

    // =========================================================================
    // Defecto documentado 3: un error de red se ve igual que "sin datos"
    // =========================================================================

    @Test
    fun `caracterizacion sin datos el dashboard queda vacio y sin error`() = runTest {
        // Hoy los repositorios convierten una falla de red en lista vacía, así que el ViewModel
        // no puede distinguir "no hay datos" de "no hubo conexión". Esta prueba documenta que
        // `error` queda en null en ambos casos.
        iniciarSesionComo("EJECUTIVO", id = 9L)
        stubs()
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Ignore(
        "Defecto 3 del plan: DashboardUiState.error existe pero ningún código lo asigna. " +
                "Si el repositorio propaga la falla (o el ViewModel la captura), se espera error != null. " +
                "Quitar el @Ignore cuando se corrija."
    )
    @Test
    fun `defecto 3 una falla de lectura debe mostrarse como error`() = runTest {
        iniciarSesionComo("EJECUTIVO", id = 9L)
        stubs(proximos = flow { throw IOException("sin conexión") })
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
    }
}