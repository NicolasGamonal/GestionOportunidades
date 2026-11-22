package duoc.example.gestionoportunidades.ui.oportunidades

import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.iniciarSesionComo
import duoc.example.gestionoportunidades.testutil.oportunidadLocal
import duoc.example.gestionoportunidades.testutil.suscribir
import duoc.example.gestionoportunidades.testutil.usuarioPrueba
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OportunidadesViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val repo = mockk<OportunidadRepository>()
    private val authRepo = mockk<AuthRepository>()

    private val regionMetropolitana = "Región Metropolitana de Santiago"
    private val regionValparaiso = "Valparaíso"

    /** Crea el ViewModel con `buscar` devolviendo siempre la lista indicada. */
    private fun crearVm(lista: List<Oportunidad> = emptyList()): OportunidadesViewModel {
        every { repo.buscar(any(), any(), any(), any()) } returns flowOf(lista)
        every { authRepo.obtenerEjecutivos() } returns flowOf(emptyList())
        return OportunidadesViewModel(repo, authRepo)
    }

    private fun nombres(vm: OportunidadesViewModel) = vm.uiState.value.oportunidades.map { it.nombre }

    // =========================================================================
    // FiltrosOportunidad.hayFiltros
    // =========================================================================

    @Test
    fun `hayFiltros es false con los valores por defecto`() {
        assertFalse(FiltrosOportunidad().hayFiltros())
    }

    @Test
    fun `la busqueda sola no cuenta como filtro`() {
        assertFalse(FiltrosOportunidad(busqueda = "feria").hayFiltros())
    }

    @Test
    fun `hayFiltros es true si cualquiera de los otros campos tiene valor`() {
        assertTrue(FiltrosOportunidad(estado = "Identificada").hayFiltros())
        assertTrue(FiltrosOportunidad(prioridad = "Alta").hayFiltros())
        assertTrue(FiltrosOportunidad(tipoEvento = "Feria empresarial").hayFiltros())
        assertTrue(FiltrosOportunidad(region = regionValparaiso).hayFiltros())
        assertTrue(FiltrosOportunidad(comuna = "Puente Alto").hayFiltros())
        assertTrue(FiltrosOportunidad(idResponsable = 6L).hayFiltros())
        assertTrue(FiltrosOportunidad(fechaDesde = "01/03/2026").hayFiltros())
        assertTrue(FiltrosOportunidad(fechaHasta = "31/03/2026").hayFiltros())
    }

    // =========================================================================
    // Estado inicial y datos
    // =========================================================================

    @Test
    fun `el estado inicial es cargando, sin oportunidades y sin filtros`() {
        val vm = crearVm()

        assertEquals(OportunidadesUiState(), vm.uiState.value)
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(FiltrosOportunidad(), vm.filtros.value)
    }

    @Test
    fun `sin suscriptores no consulta al repositorio`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        crearVm()

        advanceUntilIdle()

        verify(exactly = 0) { repo.buscar(any(), any(), any(), any()) }
    }

    @Test
    fun `muestra las oportunidades que devuelve el repositorio y termina de cargar`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val lista = listOf(oportunidadLocal(idOportunidad = 1L, nombre = "Uno"), oportunidadLocal(idOportunidad = 2L, nombre = "Dos"))
        val vm = crearVm(lista)

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(lista, vm.uiState.value.oportunidades)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `una lista vacia termina de cargar y queda vacia`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(emptyList())

        suscribir(vm.uiState)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.oportunidades.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `expone la lista de ejecutivos del repositorio de autenticacion`() = runTest {
        val ejecutivos = listOf(usuarioPrueba("EJECUTIVO", id = 5L), usuarioPrueba("EJECUTIVO", id = 6L))
        every { repo.buscar(any(), any(), any(), any()) } returns flowOf(emptyList())
        every { authRepo.obtenerEjecutivos() } returns flowOf(ejecutivos)
        val vm = OportunidadesViewModel(repo, authRepo)

        suscribir(vm.ejecutivos)
        advanceUntilIdle()

        assertEquals(ejecutivos, vm.ejecutivos.value)
    }

    // =========================================================================
    // Qué se le pide al repositorio según el rol
    // =========================================================================

    @Test
    fun `el supervisor consulta a todo el equipo con id 0 y sin filtros`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        verify(exactly = 1) { repo.buscar("", "", "", 0L) }
    }

    @Test
    fun `el supervisor puede filtrar por un responsable`() = runTest {
        iniciarSesionComo("SUPERVISOR", id = 1L)
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()

        vm.onResponsableChange(6L)
        advanceUntilIdle()

        verify(exactly = 1) { repo.buscar("", "", "", 6L) }
    }

    @Test
    fun `el ejecutivo siempre consulta solo con su id aunque pida otro responsable`() = runTest {
        iniciarSesionComo("EJECUTIVO", id = 9L)
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()
        verify(exactly = 1) { repo.buscar("", "", "", 9L) }

        vm.onResponsableChange(6L)
        advanceUntilIdle()

        verify(exactly = 0) { repo.buscar(any(), any(), any(), 6L) }
        verify(exactly = 2) { repo.buscar("", "", "", 9L) }
    }

    @Test
    fun `marketing fuerza el estado Confirmada y consulta con responsable 0`() = runTest {
        iniciarSesionComo("MARKETING", id = 12L)
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        verify(exactly = 1) { repo.buscar("Confirmada", "", "", 0L) }
    }

    @Test
    fun `marketing no puede cambiar el estado que se consulta`() = runTest {
        iniciarSesionComo("MARKETING", id = 12L)
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()

        vm.onEstadoChange("Identificada")
        advanceUntilIdle()

        verify(exactly = 0) { repo.buscar("Identificada", any(), any(), any()) }
        verify(atLeast = 2) { repo.buscar("Confirmada", "", "", 0L) }
    }

    @Test
    fun `sin sesion consulta con id menos 1`() = runTest {
        val vm = crearVm()

        suscribir(vm.uiState)
        advanceUntilIdle()

        verify(exactly = 1) { repo.buscar("", "", "", -1L) }
    }

    // =========================================================================
    // Filtros que se envían al repositorio
    // =========================================================================

    @Test
    fun `estado prioridad y busqueda se envian al repositorio`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()

        vm.onEstadoChange("Identificada")
        vm.onPrioridadChange("Alta")
        vm.onBusquedaChange("feria")
        advanceUntilIdle()

        verify(exactly = 1) { repo.buscar("Identificada", "Alta", "feria", 0L) }
    }

    @Test
    fun `cada onXChange actualiza solo su propio filtro`() {
        val vm = crearVm()

        vm.onBusquedaChange("feria")
        assertEquals(FiltrosOportunidad(busqueda = "feria"), vm.filtros.value)
        vm.onEstadoChange("Identificada")
        assertEquals(FiltrosOportunidad(busqueda = "feria", estado = "Identificada"), vm.filtros.value)
        vm.onPrioridadChange("Alta")
        vm.onTipoEventoChange("Feria empresarial")
        vm.onComunaChange("Puente Alto")
        vm.onResponsableChange(6L)
        vm.onFechaDesdeChange("01/03/2026")
        vm.onFechaHastaChange("31/03/2026")

        assertEquals(
            FiltrosOportunidad(
                busqueda = "feria", estado = "Identificada", prioridad = "Alta",
                tipoEvento = "Feria empresarial", region = "", comuna = "Puente Alto",
                idResponsable = 6L, fechaDesde = "01/03/2026", fechaHasta = "31/03/2026"
            ),
            vm.filtros.value
        )
    }

    @Test
    fun `onRegionChange guarda la region y limpia la comuna`() {
        val vm = crearVm()
        vm.onComunaChange("Puente Alto")

        vm.onRegionChange(regionValparaiso)

        assertEquals(regionValparaiso, vm.filtros.value.region)
        assertEquals("", vm.filtros.value.comuna)
    }

    @Test
    fun `onComunaChange despues de la region conserva la region`() {
        val vm = crearVm()
        vm.onRegionChange(regionMetropolitana)

        vm.onComunaChange("Puente Alto")

        assertEquals(regionMetropolitana, vm.filtros.value.region)
        assertEquals("Puente Alto", vm.filtros.value.comuna)
    }

    @Test
    fun `limpiarFiltros deja todo en blanco pero conserva la busqueda`() {
        val vm = crearVm()
        vm.onBusquedaChange("feria")
        vm.onEstadoChange("Identificada")
        vm.onPrioridadChange("Alta")
        vm.onTipoEventoChange("Feria empresarial")
        vm.onRegionChange(regionMetropolitana)
        vm.onComunaChange("Puente Alto")
        vm.onResponsableChange(6L)
        vm.onFechaDesdeChange("01/03/2026")
        vm.onFechaHastaChange("31/03/2026")
        assertTrue(vm.filtros.value.hayFiltros())

        vm.limpiarFiltros()

        assertEquals(FiltrosOportunidad(busqueda = "feria"), vm.filtros.value)
        assertFalse(vm.filtros.value.hayFiltros())
    }

    @Test
    fun `limpiarFiltros vuelve a consultar al repositorio sin los filtros`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm()
        suscribir(vm.uiState)
        advanceUntilIdle()
        vm.onEstadoChange("Identificada")
        advanceUntilIdle()
        verify(exactly = 1) { repo.buscar("Identificada", "", "", 0L) }

        vm.limpiarFiltros()
        advanceUntilIdle()

        verify(exactly = 2) { repo.buscar("", "", "", 0L) }   // la consulta inicial y la de después de limpiar
    }

    // =========================================================================
    // Filtros locales (se aplican sobre lo que devuelve el repositorio)
    // =========================================================================

    @Test
    fun `filtra por tipo de evento`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "Feria", tipoEvento = "Feria empresarial"),
                oportunidadLocal(idOportunidad = 2L, nombre = "Corporativo", tipoEvento = "Evento corporativo")
            )
        )
        suscribir(vm.uiState)
        vm.onTipoEventoChange("Feria empresarial")
        advanceUntilIdle()

        assertEquals(listOf("Feria"), nombres(vm))
    }

    @Test
    fun `filtra por region derivada de la comuna`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "En Puente Alto", comuna = "Puente Alto"),
                oportunidadLocal(idOportunidad = 2L, nombre = "En Viña", comuna = "Viña del Mar"),
                oportunidadLocal(idOportunidad = 3L, nombre = "Comuna desconocida", comuna = "Atlantis")
            )
        )
        suscribir(vm.uiState)
        vm.onRegionChange(regionMetropolitana)
        advanceUntilIdle()
        assertEquals(listOf("En Puente Alto"), nombres(vm))

        vm.onRegionChange(regionValparaiso)
        advanceUntilIdle()
        assertEquals(listOf("En Viña"), nombres(vm))
    }

    @Test
    fun `una comuna fuera del catalogo no aparece cuando se filtra por region`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(listOf(oportunidadLocal(idOportunidad = 3L, nombre = "Rara", comuna = "Atlantis")))
        suscribir(vm.uiState)
        vm.onRegionChange(regionMetropolitana)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.oportunidades.isEmpty())
    }

    @Test
    fun `filtra por comuna exacta`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "Puente Alto", comuna = "Puente Alto"),
                oportunidadLocal(idOportunidad = 2L, nombre = "Providencia", comuna = "Providencia")
            )
        )
        suscribir(vm.uiState)
        vm.onComunaChange("Providencia")
        advanceUntilIdle()

        assertEquals(listOf("Providencia"), nombres(vm))
    }

    @Test
    fun `cambiar la region limpia la comuna y deja ver otra vez toda la region`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "Puente Alto", comuna = "Puente Alto"),
                oportunidadLocal(idOportunidad = 2L, nombre = "Providencia", comuna = "Providencia")
            )
        )
        suscribir(vm.uiState)
        vm.onRegionChange(regionMetropolitana)
        vm.onComunaChange("Providencia")
        advanceUntilIdle()
        assertEquals(listOf("Providencia"), nombres(vm))

        vm.onRegionChange(regionMetropolitana)
        advanceUntilIdle()

        assertEquals(listOf("Puente Alto", "Providencia"), nombres(vm))
    }

    @Test
    fun `el rango de fechas incluye los dos extremos`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "14 a medianoche", fechaInicio = FechasPrueba.hora("2026-03-14T23:59:59")),
                oportunidadLocal(idOportunidad = 2L, nombre = "15 a medianoche", fechaInicio = FechasPrueba.hora("2026-03-15T00:00:00")),
                oportunidadLocal(idOportunidad = 3L, nombre = "20 al mediodia", fechaInicio = FechasPrueba.hora("2026-03-20T12:00:00")),
                oportunidadLocal(idOportunidad = 4L, nombre = "25 casi medianoche", fechaInicio = FechasPrueba.hora("2026-03-25T23:59:59")),
                oportunidadLocal(idOportunidad = 5L, nombre = "26 a medianoche", fechaInicio = FechasPrueba.hora("2026-03-26T00:00:00"))
            )
        )
        suscribir(vm.uiState)
        vm.onFechaDesdeChange("15/03/2026")
        vm.onFechaHastaChange("25/03/2026")
        advanceUntilIdle()

        assertEquals(listOf("15 a medianoche", "20 al mediodia", "25 casi medianoche"), nombres(vm))
    }

    @Test
    fun `solo fecha desde excluye lo anterior`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "Antes", fechaInicio = FechasPrueba.dia("2026-03-01")),
                oportunidadLocal(idOportunidad = 2L, nombre = "Despues", fechaInicio = FechasPrueba.dia("2026-03-20"))
            )
        )
        suscribir(vm.uiState)
        vm.onFechaDesdeChange("10/03/2026")
        advanceUntilIdle()

        assertEquals(listOf("Despues"), nombres(vm))
    }

    @Test
    fun `solo fecha hasta excluye lo posterior`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "Antes", fechaInicio = FechasPrueba.dia("2026-03-01")),
                oportunidadLocal(idOportunidad = 2L, nombre = "Despues", fechaInicio = FechasPrueba.dia("2026-03-20"))
            )
        )
        suscribir(vm.uiState)
        vm.onFechaHastaChange("10/03/2026")
        advanceUntilIdle()

        assertEquals(listOf("Antes"), nombres(vm))
    }

    @Test
    fun `una fecha invalida se ignora y no filtra`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(idOportunidad = 1L, nombre = "Uno", fechaInicio = FechasPrueba.dia("2026-03-01")),
                oportunidadLocal(idOportunidad = 2L, nombre = "Dos", fechaInicio = FechasPrueba.dia("2026-03-20"))
            )
        )
        suscribir(vm.uiState)
        vm.onFechaDesdeChange("abc")
        vm.onFechaHastaChange("99/99/9999")
        advanceUntilIdle()

        assertEquals(listOf("Uno", "Dos"), nombres(vm))
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `una fecha en blanco no filtra`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(listOf(oportunidadLocal(idOportunidad = 1L, nombre = "Uno")))
        suscribir(vm.uiState)
        vm.onFechaDesdeChange("   ")
        vm.onFechaHastaChange("")
        advanceUntilIdle()

        assertEquals(listOf("Uno"), nombres(vm))
    }

    @Test
    fun `los filtros locales se combinan`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val vm = crearVm(
            listOf(
                oportunidadLocal(
                    idOportunidad = 1L, nombre = "Cumple todo", tipoEvento = "Feria empresarial",
                    comuna = "Puente Alto", fechaInicio = FechasPrueba.dia("2026-03-20")
                ),
                oportunidadLocal(
                    idOportunidad = 2L, nombre = "Otro tipo", tipoEvento = "Evento corporativo",
                    comuna = "Puente Alto", fechaInicio = FechasPrueba.dia("2026-03-20")
                ),
                oportunidadLocal(
                    idOportunidad = 3L, nombre = "Otra comuna", tipoEvento = "Feria empresarial",
                    comuna = "Providencia", fechaInicio = FechasPrueba.dia("2026-03-20")
                ),
                oportunidadLocal(
                    idOportunidad = 4L, nombre = "Fuera de fecha", tipoEvento = "Feria empresarial",
                    comuna = "Puente Alto", fechaInicio = FechasPrueba.dia("2026-05-01")
                )
            )
        )
        suscribir(vm.uiState)
        vm.onTipoEventoChange("Feria empresarial")
        vm.onRegionChange(regionMetropolitana)
        vm.onComunaChange("Puente Alto")
        vm.onFechaDesdeChange("01/03/2026")
        vm.onFechaHastaChange("31/03/2026")
        advanceUntilIdle()

        assertEquals(listOf("Cumple todo"), nombres(vm))
    }

    @Test
    fun `sin filtros locales devuelve la lista completa en el mismo orden`() = runTest {
        iniciarSesionComo("SUPERVISOR")
        val lista = listOf(
            oportunidadLocal(idOportunidad = 3L, nombre = "C"),
            oportunidadLocal(idOportunidad = 1L, nombre = "A"),
            oportunidadLocal(idOportunidad = 2L, nombre = "B")
        )
        val vm = crearVm(lista)
        suscribir(vm.uiState)
        advanceUntilIdle()

        assertEquals(lista, vm.uiState.value.oportunidades)
    }
}