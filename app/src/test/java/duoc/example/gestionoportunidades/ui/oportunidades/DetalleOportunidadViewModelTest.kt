package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.EvidenciaRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.data.repository.ResultadoActivacionRepository
import duoc.example.gestionoportunidades.model.Evidencia
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.ResultadoActivacion
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de DetalleOportunidadViewModel (Fase 3 del plan).
 * El uiState combina 4 flujos con stateIn(WhileSubscribed): para leerlo hay que tener un colector
 * activo, que se levanta en backgroundScope con un dispatcher sin cola (UnconfinedTestDispatcher).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleOportunidadViewModelTest {

    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()

    private val oportunidadRepo = mockk<OportunidadRepository>()
    private val organizacionRepo = mockk<OrganizacionRepository>()
    private val authRepo = mockk<AuthRepository>()
    private val evidenciaRepo = mockk<EvidenciaRepository>()
    private val resultadoRepo = mockk<ResultadoActivacionRepository>()

    // Fuentes que alimentan al ViewModel; cada prueba puede cambiar su valor.
    private val oportunidadFlow = MutableStateFlow<Oportunidad?>(null)
    private val evidenciasFlow = MutableStateFlow<List<Evidencia>>(emptyList())
    private val resultadoFlow = MutableStateFlow<ResultadoActivacion?>(null)

    // =========================================================================
    // Ayudas
    // =========================================================================

    private fun usuario(rol: String, id: Long, nombre: String = "Usuario $id") = Usuario(
        idUsuario = id, username = "usuario$id@agencia.cl", contrasena = "clave",
        nombreCompleto = nombre, rol = rol
    )

    private fun ejecutivo(id: Long = 7L) = usuario("EJECUTIVO", id, "Ana Pérez")
    private fun supervisor() = usuario("SUPERVISOR", 1L)
    private fun marketing() = usuario("MARKETING", 9L)

    private fun oportunidad(estado: String = "Identificada", idResponsable: Long? = 7L, idOrganizacion: Long = 1L) =
        Oportunidad(
            idOportunidad = 5L, nombre = "Feria de Ñuñoa", tipoEvento = "Feria municipal",
            fechaDeteccion = 1_000L, fechaInicio = 2_000L, fechaTermino = 3_000L,
            comuna = "Ñuñoa", publicoEsperado = 1500, prioridad = "Alta", estado = estado,
            costoReferencial = 850000.0, observaciones = "", idOrganizacion = idOrganizacion,
            idResponsable = idResponsable
        )

    private fun evidencia(id: Long, ruta: String) =
        Evidencia(idEvidencia = id, rutaArchivo = ruta, fecha = 1_700_000_000_000L + id, idOportunidad = 5L)

    private fun resultado() = ResultadoActivacion(
        idResultado = 12L, idOportunidad = 5L, asistentes = 100, contactosGenerados = 10,
        inscripciones = 5, evaluacion = 4, aprendizajes = "Funcionó bien",
        fechaRegistro = 1_700_000_000_000L, idUsuario = 1L
    )

    @Before
    fun preparar() {
        oportunidadFlow.value = oportunidad()
        every { oportunidadRepo.observarPorId(5L) } returns oportunidadFlow
        every { evidenciaRepo.observarPorOportunidad(5L) } returns evidenciasFlow
        every { resultadoRepo.observarPorOportunidad(5L) } returns resultadoFlow
        coEvery { organizacionRepo.obtenerPorId(1L) } returns Organizacion(
            idOrganizacion = 1L, nombre = "Municipalidad de Ñuñoa", tipo = "Municipalidad",
            rubro = "Gobierno local", comuna = "Ñuñoa"
        )
        coEvery { authRepo.obtenerUsuarioPorId(7L) } returns ejecutivo(7L)
    }

    private fun crearViewModel(argumentos: Map<String, Any?> = mapOf("id" to 5L)) = DetalleOportunidadViewModel(
        savedStateHandle = SavedStateHandle(argumentos),
        oportunidadRepository = oportunidadRepo,
        organizacionRepository = organizacionRepo,
        authRepository = authRepo,
        evidenciaRepository = evidenciaRepo,
        resultadoRepository = resultadoRepo
    )

    /** Crea el ViewModel y mantiene un colector activo para que el stateIn produzca valores. */
    private fun TestScope.vmActivo(): DetalleOportunidadViewModel {
        val vm = crearViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        advanceUntilIdle()
        return vm
    }

    /** Estados a los que el usuario puede pasar la oportunidad (campo estadosPermitidos). */
    private fun TestScope.estadosPermitidos(usuario: Usuario, estado: String): List<String> {
        SesionManager.iniciarSesion(usuario)
        oportunidadFlow.value = oportunidad(estado = estado)
        return vmActivo().uiState.value.estadosPermitidos
    }

    // =========================================================================
    // Carga del estado
    // =========================================================================

    @Test
    fun `sin el argumento id el ViewModel falla al crearse`() {
        assertThrows(IllegalStateException::class.java) { crearViewModel(emptyMap()) }
    }

    @Test
    fun `mientras la oportunidad no llega el estado es el inicial cargando`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        every { oportunidadRepo.observarPorId(5L) } returns MutableSharedFlow<Oportunidad?>()

        val s = vmActivo().uiState.value

        assertTrue(s.isLoading)
        assertNull(s.oportunidad)
        assertFalse(s.sinAcceso)
    }

    @Test
    fun `una oportunidad nula deja de cargar sin oportunidad y sin marcar sinAcceso`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        oportunidadFlow.value = null

        val s = vmActivo().uiState.value

        assertFalse(s.isLoading)
        assertNull(s.oportunidad)
        assertFalse(s.sinAcceso)
        assertFalse(s.puedeRegistrarResultado)
    }

    @Test
    fun `sin sesion el estado queda sinAcceso`() = runTest {
        val s = vmActivo().uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.isLoading)
        assertNull(s.oportunidad)
    }

    @Test
    fun `un ejecutivo ajeno queda sinAcceso`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 8L))

        val s = vmActivo().uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.isLoading)
        assertNull(s.oportunidad)
    }

    @Test
    fun `marketing sin acceso a una oportunidad que no esta Confirmada`() = runTest {
        SesionManager.iniciarSesion(marketing())
        oportunidadFlow.value = oportunidad(estado = "Contacto realizado")

        val s = vmActivo().uiState.value

        assertTrue(s.sinAcceso)
        assertNull(s.oportunidad)
    }

    @Test
    fun `marketing ve una oportunidad Confirmada pero no puede registrar resultado ni cambiar estado`() = runTest {
        SesionManager.iniciarSesion(marketing())
        oportunidadFlow.value = oportunidad(estado = "Confirmada")

        val s = vmActivo().uiState.value

        assertFalse(s.sinAcceso)
        assertEquals("Feria de Ñuñoa", s.oportunidad?.nombre)
        assertFalse(s.puedeRegistrarResultado)
        assertTrue(s.estadosPermitidos.isEmpty())
    }

    @Test
    fun `el supervisor ve la oportunidad con organizacion y responsable`() = runTest {
        SesionManager.iniciarSesion(supervisor())

        val s = vmActivo().uiState.value

        assertFalse(s.isLoading)
        assertFalse(s.sinAcceso)
        assertEquals(oportunidad(), s.oportunidad)
        assertEquals("Municipalidad de Ñuñoa", s.organizacion)
        assertEquals("Ana Pérez", s.responsable)
    }

    @Test
    fun `el ejecutivo responsable ve su oportunidad`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))

        val s = vmActivo().uiState.value

        assertFalse(s.sinAcceso)
        assertEquals(oportunidad(), s.oportunidad)
    }

    @Test
    fun `sin organizacion el texto es Sin organizacion`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { organizacionRepo.obtenerPorId(1L) } returns null

        assertEquals("Sin organización", vmActivo().uiState.value.organizacion)
    }

    @Test
    fun `sin responsable el texto es Sin asignar y no se consulta al usuario`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        oportunidadFlow.value = oportunidad(idResponsable = null)

        assertEquals("Sin asignar", vmActivo().uiState.value.responsable)
        coVerify(exactly = 0) { authRepo.obtenerUsuarioPorId(any()) }
    }

    @Test
    fun `si el responsable no se encuentra el texto es Sin asignar`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { authRepo.obtenerUsuarioPorId(7L) } returns null

        assertEquals("Sin asignar", vmActivo().uiState.value.responsable)
    }

    // =========================================================================
    // estadosPermitidos y puedeRegistrarResultado
    // =========================================================================

    @Test
    fun `el supervisor puede avanzar un paso o pasar a un estado final`() = runTest {
        assertEquals(
            listOf("Contacto realizado", "Confirmada", "Descartada"),
            estadosPermitidos(supervisor(), "Identificada")
        )
        assertEquals(
            listOf("Propuesta enviada", "Confirmada", "Descartada"),
            estadosPermitidos(supervisor(), "Contacto realizado")
        )
        assertEquals(
            listOf("Confirmada", "Descartada"),
            estadosPermitidos(supervisor(), "En espera de respuesta")
        )
    }

    @Test
    fun `desde un estado final nadie puede cambiar el estado`() = runTest {
        assertTrue(estadosPermitidos(supervisor(), "Confirmada").isEmpty())
        assertTrue(estadosPermitidos(supervisor(), "Descartada").isEmpty())
    }

    @Test
    fun `el ejecutivo solo avanza un paso y sin llegar a un estado final`() = runTest {
        assertEquals(listOf("Contacto realizado"), estadosPermitidos(ejecutivo(), "Identificada"))
        assertEquals(listOf("En espera de respuesta"), estadosPermitidos(ejecutivo(), "Propuesta enviada"))
        assertTrue(estadosPermitidos(ejecutivo(), "En espera de respuesta").isEmpty())
    }

    @Test
    fun `puedeRegistrarResultado solo con la oportunidad Confirmada y rol con acceso`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        oportunidadFlow.value = oportunidad(estado = "Confirmada")
        assertTrue(vmActivo().uiState.value.puedeRegistrarResultado)

        oportunidadFlow.value = oportunidad(estado = "Propuesta enviada")
        assertFalse(vmActivo().uiState.value.puedeRegistrarResultado)

        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        oportunidadFlow.value = oportunidad(estado = "Confirmada", idResponsable = 7L)
        assertTrue(vmActivo().uiState.value.puedeRegistrarResultado)
    }

    // =========================================================================
    // Evidencias y resultado (reactivos)
    // =========================================================================

    @Test
    fun `las evidencias y el resultado se combinan en el estado y se actualizan`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmActivo()
        assertTrue(vm.uiState.value.evidencias.isEmpty())
        assertNull(vm.uiState.value.resultado)

        val fotos = listOf(evidencia(1L, "evidencias/a.jpg"), evidencia(2L, "evidencias/b.jpg"))
        evidenciasFlow.value = fotos
        resultadoFlow.value = resultado()
        advanceUntilIdle()

        assertEquals(fotos, vm.uiState.value.evidencias)
        assertEquals(resultado(), vm.uiState.value.resultado)
    }

    @Test
    fun `si la oportunidad cambia de estado el estado de la pantalla se recalcula`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmActivo()
        assertEquals("Identificada", vm.uiState.value.oportunidad?.estado)

        oportunidadFlow.value = oportunidad(estado = "Contacto realizado")
        advanceUntilIdle()

        assertEquals("Contacto realizado", vm.uiState.value.oportunidad?.estado)
        assertEquals(
            listOf("Propuesta enviada", "Confirmada", "Descartada"),
            vm.uiState.value.estadosPermitidos
        )
    }

    // =========================================================================
    // cambiarEstado
    // =========================================================================

    @Test
    fun `cambiarEstado con exito llama al repositorio y muestra el mensaje`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        coEvery { oportunidadRepo.cambiarEstado(5L, "Identificada", "Contacto realizado", 7L) } just Runs
        val vm = vmActivo()

        vm.cambiarEstado("Contacto realizado")
        advanceUntilIdle()

        assertEquals("Estado actualizado a Contacto realizado", vm.uiState.value.mensajeExito)
        assertNull(vm.uiState.value.mensajeError)
        coVerify(exactly = 1) { oportunidadRepo.cambiarEstado(5L, "Identificada", "Contacto realizado", 7L) }
    }

    @Test
    fun `cambiarEstado sin permiso muestra el error y no llama al repositorio`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        val vm = vmActivo()

        vm.cambiarEstado("Confirmada")           // el ejecutivo no puede confirmar
        advanceUntilIdle()

        assertEquals("No tienes permiso para este cambio de estado", vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    @Test
    fun `cambiarEstado con una excepcion muestra No se pudo actualizar`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        coEvery { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) } throws RuntimeException("sin conexión")
        val vm = vmActivo()

        vm.cambiarEstado("Contacto realizado")
        advanceUntilIdle()

        assertEquals("No se pudo actualizar: sin conexión", vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
    }

    @Test
    fun `cambiarEstado sin sesion no hace nada`() = runTest {
        val vm = vmActivo()

        vm.cambiarEstado("Contacto realizado")
        advanceUntilIdle()

        assertNull(vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    @Test
    fun `cambiarEstado sin oportunidad cargada no hace nada`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 8L))      // ajeno: la oportunidad no se expone
        val vm = vmActivo()

        vm.cambiarEstado("Contacto realizado")
        advanceUntilIdle()

        assertNull(vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
        coVerify(exactly = 0) { oportunidadRepo.cambiarEstado(any(), any(), any(), any()) }
    }

    // =========================================================================
    // Evidencias
    // =========================================================================

    @Test
    fun `registrarEvidencia envia la ruta con la oportunidad actual y muestra el mensaje`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val enviada = slot<Evidencia>()
        coEvery { evidenciaRepo.registrar(capture(enviada)) } returns 3L
        val vm = vmActivo()
        val antes = System.currentTimeMillis()

        vm.registrarEvidencia("evidencias/foto1.jpg")
        advanceUntilIdle()

        val despues = System.currentTimeMillis()
        assertEquals("Foto guardada en la oportunidad", vm.uiState.value.mensajeExito)
        assertNull(vm.uiState.value.mensajeError)
        assertEquals(0L, enviada.captured.idEvidencia)
        assertEquals("evidencias/foto1.jpg", enviada.captured.rutaArchivo)
        assertEquals(5L, enviada.captured.idOportunidad)
        assertTrue(enviada.captured.fecha in antes..despues)
    }

    @Test
    fun `registrarEvidencia con una excepcion muestra No se pudo guardar la foto`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { evidenciaRepo.registrar(any()) } throws RuntimeException("disco lleno")
        val vm = vmActivo()

        vm.registrarEvidencia("evidencias/foto1.jpg")
        advanceUntilIdle()

        assertEquals("No se pudo guardar la foto: disco lleno", vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
    }

    @Test
    fun `eliminarEvidencia con exito muestra Foto eliminada`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val foto = evidencia(1L, "evidencias/a.jpg")
        coEvery { evidenciaRepo.eliminar(foto) } just Runs
        val vm = vmActivo()

        vm.eliminarEvidencia(foto)
        advanceUntilIdle()

        assertEquals("Foto eliminada", vm.uiState.value.mensajeExito)
        assertNull(vm.uiState.value.mensajeError)
        coVerify(exactly = 1) { evidenciaRepo.eliminar(foto) }
    }

    @Test
    fun `eliminarEvidencia con una excepcion muestra No se pudo eliminar la foto`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val foto = evidencia(1L, "evidencias/a.jpg")
        coEvery { evidenciaRepo.eliminar(foto) } throws RuntimeException("archivo en uso")
        val vm = vmActivo()

        vm.eliminarEvidencia(foto)
        advanceUntilIdle()

        assertEquals("No se pudo eliminar la foto: archivo en uso", vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
    }

    // =========================================================================
    // Mensajes
    // =========================================================================

    @Test
    fun `un mensaje nuevo reemplaza al anterior`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { evidenciaRepo.registrar(any()) } returns 3L
        coEvery { evidenciaRepo.eliminar(any()) } throws RuntimeException("falla")
        val vm = vmActivo()

        vm.registrarEvidencia("evidencias/a.jpg")
        advanceUntilIdle()
        assertEquals("Foto guardada en la oportunidad", vm.uiState.value.mensajeExito)

        vm.eliminarEvidencia(evidencia(1L, "evidencias/a.jpg"))
        advanceUntilIdle()

        assertNull(vm.uiState.value.mensajeExito)
        assertEquals("No se pudo eliminar la foto: falla", vm.uiState.value.mensajeError)
    }

    @Test
    fun `limpiarMensajes deja el exito y el error en null`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        val vm = vmActivo()
        vm.cambiarEstado("Confirmada")           // sin permiso: deja un mensaje de error
        advanceUntilIdle()
        assertEquals("No tienes permiso para este cambio de estado", vm.uiState.value.mensajeError)

        vm.limpiarMensajes()
        advanceUntilIdle()

        assertNull(vm.uiState.value.mensajeError)
        assertNull(vm.uiState.value.mensajeExito)
    }
}