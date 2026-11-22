package duoc.example.gestionoportunidades.ui.resultados

import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.ResultadoActivacionRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.ResultadoActivacion
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.CampoResultado
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de FormularioResultadoViewModel (Fase 3 del plan).
 * El argumento de la ruta se llama "idOportunidad" (no "id").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioResultadoViewModelTest {

    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()

    private val oportunidadRepo = mockk<OportunidadRepository>()
    private val resultadoRepo = mockk<ResultadoActivacionRepository>()

    // =========================================================================
    // Ayudas
    // =========================================================================

    private fun usuario(rol: String, id: Long) = Usuario(
        idUsuario = id, username = "usuario$id@agencia.cl", contrasena = "clave",
        nombreCompleto = "Usuario $id", rol = rol
    )

    private fun ejecutivo(id: Long = 7L) = usuario("EJECUTIVO", id)
    private fun supervisor(id: Long = 1L) = usuario("SUPERVISOR", id)
    private fun marketing(id: Long = 9L) = usuario("MARKETING", id)

    private fun oportunidad(estado: String = "Confirmada", idResponsable: Long? = 7L) = Oportunidad(
        idOportunidad = 5L, nombre = "Feria de Ñuñoa", tipoEvento = "Feria municipal",
        fechaDeteccion = 1_000L, fechaInicio = 2_000L, fechaTermino = 3_000L,
        comuna = "Ñuñoa", publicoEsperado = 1500, prioridad = "Alta", estado = estado,
        costoReferencial = 850000.0, observaciones = "", idOrganizacion = 1L, idResponsable = idResponsable
    )

    private fun resultadoPrevio() = ResultadoActivacion(
        idResultado = 12L, idOportunidad = 5L, asistentes = 100, contactosGenerados = 10,
        inscripciones = 5, evaluacion = 4, aprendizajes = "Funcionó bien", fechaRegistro = 1_700_000_000_000L,
        idUsuario = 3L
    )

    @Before
    fun preparar() {
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        coEvery { resultadoRepo.obtenerPorOportunidad(5L) } returns null
    }

    private fun crearViewModel(argumentos: Map<String, Any?> = mapOf("idOportunidad" to 5L)) =
        FormularioResultadoViewModel(SavedStateHandle(argumentos), oportunidadRepo, resultadoRepo)

    private fun TestScope.vmListo(): FormularioResultadoViewModel =
        crearViewModel().also { advanceUntilIdle() }

    private fun llenarValido(vm: FormularioResultadoViewModel) {
        vm.onAsistentesChange("300")
        vm.onContactosChange("45")
        vm.onInscripcionesChange("20")
        vm.onAprendizajesChange("  Buena convocatoria  ")
    }

    // =========================================================================
    // Carga y acceso
    // =========================================================================

    @Test
    fun `sin el argumento idOportunidad el ViewModel falla al crearse`() {
        assertThrows(IllegalStateException::class.java) { crearViewModel(emptyMap()) }
    }

    @Test
    fun `parte cargando y con evaluacion 3 por defecto`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = crearViewModel()

        assertTrue(vm.uiState.value.cargando)
        assertEquals(3, vm.uiState.value.evaluacion)
    }

    @Test
    fun `una oportunidad Confirmada sin resultado previo abre el formulario vacio`() = runTest {
        SesionManager.iniciarSesion(supervisor())

        val s = vmListo().uiState.value

        assertFalse(s.cargando)
        assertFalse(s.sinAcceso)
        assertFalse(s.esEdicion)
        assertEquals("Feria de Ñuñoa", s.nombreOportunidad)
        assertEquals("", s.asistentes)
        assertEquals("", s.contactosGenerados)
        assertEquals("", s.inscripciones)
        assertEquals("", s.aprendizajes)
        assertEquals(3, s.evaluacion)
    }

    @Test
    fun `con un resultado previo precarga los datos y queda en modo edicion`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { resultadoRepo.obtenerPorOportunidad(5L) } returns resultadoPrevio()

        val s = vmListo().uiState.value

        assertTrue(s.esEdicion)
        assertFalse(s.cargando)
        assertEquals("Feria de Ñuñoa", s.nombreOportunidad)
        assertEquals("100", s.asistentes)
        assertEquals("10", s.contactosGenerados)
        assertEquals("5", s.inscripciones)
        assertEquals(4, s.evaluacion)
        assertEquals("Funcionó bien", s.aprendizajes)
    }

    @Test
    fun `el ejecutivo responsable puede registrar el resultado`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))

        assertFalse(vmListo().uiState.value.sinAcceso)
    }

    @Test
    fun `sin acceso si la oportunidad no esta Confirmada`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(estado = "Propuesta enviada")

        val s = vmListo().uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.cargando)
        assertEquals("", s.nombreOportunidad)
        coVerify(exactly = 0) { resultadoRepo.obtenerPorOportunidad(any()) }
    }

    @Test
    fun `sin acceso si la oportunidad no existe`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns null

        val s = vmListo().uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.cargando)
    }

    @Test
    fun `sin acceso si no hay sesion`() = runTest {
        val s = vmListo().uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.cargando)
    }

    @Test
    fun `sin acceso para un ejecutivo que no es el responsable`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 8L))

        assertTrue(vmListo().uiState.value.sinAcceso)
    }

    @Test
    fun `sin acceso para marketing aunque la oportunidad este Confirmada`() = runTest {
        SesionManager.iniciarSesion(marketing())

        assertTrue(vmListo().uiState.value.sinAcceso)
    }

    // =========================================================================
    // Callbacks de campos
    // =========================================================================

    @Test
    fun `los filtros dejan solo digitos y respetan el largo maximo de 7 6 y 6`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()

        vm.onAsistentesChange("12a345.6789")
        vm.onContactosChange("9876543210")
        vm.onInscripcionesChange("-12 34 5678")

        val s = vm.uiState.value
        assertEquals("1234567", s.asistentes)
        assertEquals("987654", s.contactosGenerados)
        assertEquals("123456", s.inscripciones)
    }

    @Test
    fun `onEvaluacionChange y onAprendizajesChange actualizan su valor`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()

        vm.onEvaluacionChange(5)
        vm.onAprendizajesChange("Mucho aprendizaje")

        assertEquals(5, vm.uiState.value.evaluacion)
        assertEquals("Mucho aprendizaje", vm.uiState.value.aprendizajes)
    }

    @Test
    fun `guardar con el formulario vacio marca los 4 campos con error`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()

        vm.guardar()

        assertEquals(CampoResultado.entries.toSet(), vm.uiState.value.errores.keys)
    }

    @Test
    fun `cada onChange limpia solo el error de su propio campo`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val todos = CampoResultado.entries.toSet()
        val casos: List<Pair<CampoResultado, (FormularioResultadoViewModel) -> Unit>> = listOf(
            CampoResultado.ASISTENTES to { vm: FormularioResultadoViewModel -> vm.onAsistentesChange("10") },
            CampoResultado.CONTACTOS to { vm: FormularioResultadoViewModel -> vm.onContactosChange("10") },
            CampoResultado.INSCRIPCIONES to { vm: FormularioResultadoViewModel -> vm.onInscripcionesChange("10") },
            CampoResultado.APRENDIZAJES to { vm: FormularioResultadoViewModel -> vm.onAprendizajesChange("Algo") }
        )

        casos.forEach { (campo, accion) ->
            val vm = vmListo()
            vm.guardar()
            assertEquals(todos, vm.uiState.value.errores.keys)

            accion(vm)

            assertEquals("tras editar $campo", todos - campo, vm.uiState.value.errores.keys)
        }
    }

    @Test
    fun `cambiar la evaluacion no limpia ningun error`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()
        vm.guardar()

        vm.onEvaluacionChange(1)

        assertEquals(CampoResultado.entries.toSet(), vm.uiState.value.errores.keys)
    }

    @Test
    fun `limpiarMensajeError deja el mensaje en null`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()
        llenarValido(vm)
        SesionManager.cerrarSesion()
        vm.guardar()
        assertNotNull(vm.uiState.value.mensajeError)

        vm.limpiarMensajeError()

        assertNull(vm.uiState.value.mensajeError)
    }

    // =========================================================================
    // Guardar
    // =========================================================================

    @Test
    fun `guardar con errores no llama al repositorio y conserva lo escrito`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()
        vm.onAsistentesChange("300")

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("300", s.asistentes)
        assertNotNull(s.errores[CampoResultado.CONTACTOS])
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
        coVerify(exactly = 0) { resultadoRepo.guardar(any()) }
    }

    @Test
    fun `guardar sin sesion muestra Tu sesion expiro y no llama al repositorio`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()
        llenarValido(vm)
        SesionManager.cerrarSesion()

        vm.guardar()
        advanceUntilIdle()

        assertEquals("Tu sesión expiró. Vuelve a iniciar sesión.", vm.uiState.value.mensajeError)
        assertFalse(vm.uiState.value.guardadoExitoso)
        coVerify(exactly = 0) { resultadoRepo.guardar(any()) }
    }

    @Test
    fun `guardar un resultado nuevo envia id 0 los numeros recortados y el usuario actual`() = runTest {
        SesionManager.iniciarSesion(supervisor(id = 1L))
        val enviado = slot<ResultadoActivacion>()
        coEvery { resultadoRepo.guardar(capture(enviado)) } returns 10L
        val vm = vmListo()
        llenarValido(vm)
        vm.onEvaluacionChange(5)
        val antes = System.currentTimeMillis()

        vm.guardar()
        advanceUntilIdle()

        val despues = System.currentTimeMillis()
        val s = vm.uiState.value
        assertTrue(s.guardadoExitoso)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        val r = enviado.captured
        assertEquals(0L, r.idResultado)
        assertEquals(5L, r.idOportunidad)
        assertEquals(300, r.asistentes)
        assertEquals(45, r.contactosGenerados)
        assertEquals(20, r.inscripciones)
        assertEquals(5, r.evaluacion)
        assertEquals("Buena convocatoria", r.aprendizajes)
        assertEquals(1L, r.idUsuario)
        assertTrue(r.fechaRegistro in antes..despues)
    }

    @Test
    fun `sin tocar la evaluacion se guarda el valor por defecto 3`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val enviado = slot<ResultadoActivacion>()
        coEvery { resultadoRepo.guardar(capture(enviado)) } returns 10L
        val vm = vmListo()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()

        assertEquals(3, enviado.captured.evaluacion)
    }

    @Test
    fun `guardar en edicion conserva el id del resultado existente`() = runTest {
        SesionManager.iniciarSesion(supervisor(id = 1L))
        coEvery { resultadoRepo.obtenerPorOportunidad(5L) } returns resultadoPrevio()
        val enviado = slot<ResultadoActivacion>()
        coEvery { resultadoRepo.guardar(capture(enviado)) } returns 12L
        val vm = vmListo()
        vm.onAsistentesChange("150")

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        val r = enviado.captured
        assertEquals(12L, r.idResultado)
        assertEquals(5L, r.idOportunidad)
        assertEquals(150, r.asistentes)
        assertEquals(10, r.contactosGenerados)
        assertEquals(5, r.inscripciones)
        assertEquals(4, r.evaluacion)
        assertEquals("Funcionó bien", r.aprendizajes)
        assertEquals(1L, r.idUsuario)       // queda el usuario que guarda, no el que registró antes
    }

    @Test
    fun `un segundo toque en Guardar mientras guarda o despues de guardar se ignora`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val compuerta = CompletableDeferred<Unit>()
        coEvery { resultadoRepo.guardar(any()) } coAnswers { compuerta.await(); 10L }
        val vm = vmListo()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardando)

        vm.guardar()
        advanceUntilIdle()
        compuerta.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardadoExitoso)
        assertFalse(vm.uiState.value.guardando)

        vm.guardar()
        advanceUntilIdle()

        coVerify(exactly = 1) { resultadoRepo.guardar(any()) }
    }

    @Test
    fun `si el repositorio lanza una excepcion se muestra No se pudo guardar`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { resultadoRepo.guardar(any()) } throws RuntimeException("sin conexión")
        val vm = vmListo()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("No se pudo guardar: sin conexión", s.mensajeError)
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
    }

    @Test
    fun `despues de un error se puede reintentar y el mensaje anterior se limpia`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { resultadoRepo.guardar(any()) } throws RuntimeException("falla") andThen 10L
        val vm = vmListo()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.mensajeError)

        vm.guardar()
        advanceUntilIdle()

        assertNull(vm.uiState.value.mensajeError)
        assertTrue(vm.uiState.value.guardadoExitoso)
    }

    @Test
    fun `DEFECTO 1 si el repositorio devuelve 0L se espera un mensaje de error y no guardadoExitoso`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { resultadoRepo.guardar(any()) } returns 0L
        val vm = vmListo()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.guardadoExitoso)
        assertNotNull(s.mensajeError)
    }
}