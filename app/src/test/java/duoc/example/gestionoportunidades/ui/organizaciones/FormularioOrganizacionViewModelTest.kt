package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.CampoOrganizacion
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de FormularioOrganizacionViewModel (Fase 3 del plan).
 * Argumento de la ruta: "id" (opcional; > 0 = edición, y solo el supervisor puede editar).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioOrganizacionViewModelTest {

    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<OrganizacionRepository>()

    // =========================================================================
    // Ayudas
    // =========================================================================

    private fun usuario(rol: String, id: Long) = Usuario(
        idUsuario = id, username = "usuario$id@agencia.cl", contrasena = "clave",
        nombreCompleto = "Usuario $id", rol = rol
    )

    private fun supervisor() = usuario("SUPERVISOR", 1L)
    private fun ejecutivo() = usuario("EJECUTIVO", 7L)
    private fun marketing() = usuario("MARKETING", 9L)

    private fun organizacion() = Organizacion(
        idOrganizacion = 4L, nombre = "Colegio San Pedro", tipo = "Colegio",
        rubro = "Educación", comuna = "Maipú"
    )

    @Before
    fun preparar() {
        coEvery { repo.obtenerPorId(4L) } returns organizacion()
    }

    private fun crearViewModel(id: Long? = null) = FormularioOrganizacionViewModel(
        savedStateHandle = SavedStateHandle(if (id == null) emptyMap() else mapOf("id" to id)),
        repository = repo
    )

    private fun TestScope.vmListo(id: Long? = null): FormularioOrganizacionViewModel =
        crearViewModel(id).also { advanceUntilIdle() }

    private fun llenarValido(vm: FormularioOrganizacionViewModel) {
        vm.onNombreChange("  Colegio San Pedro  ")
        vm.onTipoChange("Colegio")
        vm.onRubroChange("  Educación  ")
        vm.onComunaChange("Maipú")
    }

    // =========================================================================
    // Creación
    // =========================================================================

    @Test
    fun `en creacion el formulario parte vacio y sin banderas`() = runTest {
        val s = vmListo().uiState.value

        assertFalse(s.esEdicion)
        assertFalse(s.cargando)
        assertFalse(s.sinPermiso)
        assertFalse(s.noEncontrada)
        assertFalse(s.puedeEliminar)
        assertEquals("", s.nombre)
        assertEquals("", s.tipo)
        assertEquals("", s.rubro)
        assertEquals("", s.comuna)
        assertTrue(s.errores.isEmpty())
        coVerify(exactly = 0) { repo.obtenerPorId(any()) }
    }

    @Test
    fun `un id 0 o negativo se trata como creacion`() = runTest {
        assertFalse(vmListo(0L).uiState.value.esEdicion)
        assertFalse(vmListo(-1L).uiState.value.esEdicion)
    }

    @Test
    fun `crear no exige ser supervisor`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())

        assertFalse(vmListo().uiState.value.sinPermiso)
    }

    // =========================================================================
    // Edición: carga y permisos
    // =========================================================================

    @Test
    fun `en edicion como supervisor parte cargando y despues carga los datos y puedeEliminar`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = crearViewModel(4L)
        assertTrue(vm.uiState.value.esEdicion)
        assertTrue(vm.uiState.value.cargando)

        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.cargando)
        assertFalse(s.sinPermiso)
        assertFalse(s.noEncontrada)
        assertEquals("Colegio San Pedro", s.nombre)
        assertEquals("Colegio", s.tipo)
        assertEquals("Educación", s.rubro)
        assertEquals("Maipú", s.comuna)
        assertTrue(s.puedeEliminar)
    }

    @Test
    fun `en edicion un ejecutivo queda sinPermiso y no se consulta el repositorio`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())

        val s = vmListo(4L).uiState.value

        assertTrue(s.sinPermiso)
        assertFalse(s.cargando)
        assertFalse(s.puedeEliminar)
        assertEquals("", s.nombre)
        coVerify(exactly = 0) { repo.obtenerPorId(any()) }
    }

    @Test
    fun `en edicion marketing queda sinPermiso`() = runTest {
        SesionManager.iniciarSesion(marketing())

        assertTrue(vmListo(4L).uiState.value.sinPermiso)
    }

    @Test
    fun `en edicion sin sesion queda sinPermiso`() = runTest {
        val s = vmListo(4L).uiState.value

        assertTrue(s.sinPermiso)
        assertFalse(s.cargando)
        coVerify(exactly = 0) { repo.obtenerPorId(any()) }
    }

    @Test
    fun `en edicion una organizacion inexistente queda noEncontrada`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { repo.obtenerPorId(4L) } returns null

        val s = vmListo(4L).uiState.value

        assertTrue(s.noEncontrada)
        assertFalse(s.cargando)
        assertFalse(s.puedeEliminar)
        assertEquals("", s.nombre)
    }

    // =========================================================================
    // Callbacks de campos
    // =========================================================================

    @Test
    fun `los callbacks actualizan el valor de su campo`() = runTest {
        val vm = vmListo()

        llenarValido(vm)

        val s = vm.uiState.value
        assertEquals("  Colegio San Pedro  ", s.nombre)   // el trim se aplica al guardar
        assertEquals("Colegio", s.tipo)
        assertEquals("  Educación  ", s.rubro)
        assertEquals("Maipú", s.comuna)
    }

    @Test
    fun `guardar con el formulario vacio marca los 4 campos con error`() = runTest {
        val vm = vmListo()

        vm.guardar()

        assertEquals(CampoOrganizacion.entries.toSet(), vm.uiState.value.errores.keys)
    }

    @Test
    fun `una comuna fuera de la lista marca error en COMUNA`() = runTest {
        val vm = vmListo()
        llenarValido(vm)
        vm.onComunaChange("Atlantis")

        vm.guardar()

        assertEquals(setOf(CampoOrganizacion.COMUNA), vm.uiState.value.errores.keys)
        assertEquals("Selecciona una comuna de la lista", vm.uiState.value.errores[CampoOrganizacion.COMUNA])
    }

    @Test
    fun `cada onChange limpia solo el error de su propio campo`() = runTest {
        val todos = CampoOrganizacion.entries.toSet()
        val casos: List<Pair<CampoOrganizacion, (FormularioOrganizacionViewModel) -> Unit>> = listOf(
            CampoOrganizacion.NOMBRE to { vm: FormularioOrganizacionViewModel -> vm.onNombreChange("Colegio") },
            CampoOrganizacion.TIPO to { vm: FormularioOrganizacionViewModel -> vm.onTipoChange("Colegio") },
            CampoOrganizacion.RUBRO to { vm: FormularioOrganizacionViewModel -> vm.onRubroChange("Educación") },
            CampoOrganizacion.COMUNA to { vm: FormularioOrganizacionViewModel -> vm.onComunaChange("Maipú") }
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
    fun `limpiarMensajeError deja el mensaje en null`() = runTest {
        coEvery { repo.insertar(any()) } throws RuntimeException("falla")
        val vm = vmListo()
        llenarValido(vm)
        vm.guardar()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.mensajeError)

        vm.limpiarMensajeError()

        assertNull(vm.uiState.value.mensajeError)
    }

    // =========================================================================
    // Guardar
    // =========================================================================

    @Test
    fun `guardar con errores no llama al repositorio y conserva lo escrito`() = runTest {
        val vm = vmListo()
        vm.onNombreChange("Colegio San Pedro")

        vm.guardar()
        advanceUntilIdle()

        assertEquals("Colegio San Pedro", vm.uiState.value.nombre)
        assertFalse(vm.uiState.value.guardando)
        assertFalse(vm.uiState.value.guardadoExitoso)
        coVerify(exactly = 0) { repo.insertar(any()) }
        coVerify(exactly = 0) { repo.actualizar(any()) }
    }

    @Test
    fun `crear envia la organizacion recortada con id 0`() = runTest {
        val enviada = slot<Organizacion>()
        coEvery { repo.insertar(capture(enviada)) } returns 10L
        val vm = vmListo()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.guardadoExitoso)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        assertEquals(
            Organizacion(idOrganizacion = 0L, nombre = "Colegio San Pedro", tipo = "Colegio", rubro = "Educación", comuna = "Maipú"),
            enviada.captured
        )
        coVerify(exactly = 0) { repo.actualizar(any()) }
    }

    @Test
    fun `actualizar en edicion conserva el id de la organizacion`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val enviada = slot<Organizacion>()
        coEvery { repo.actualizar(capture(enviada)) } just Runs
        val vm = vmListo(4L)
        vm.onNombreChange("  Colegio San Pedro Norte  ")

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        assertEquals(organizacion().copy(nombre = "Colegio San Pedro Norte"), enviada.captured)
        coVerify(exactly = 0) { repo.insertar(any()) }
    }

    @Test
    fun `un segundo toque en Guardar mientras guarda o despues de guardar se ignora`() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        coEvery { repo.insertar(any()) } coAnswers { compuerta.await(); 10L }
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

        coVerify(exactly = 1) { repo.insertar(any()) }
    }

    @Test
    fun `si el repositorio lanza una excepcion al crear se muestra No se pudo guardar`() = runTest {
        coEvery { repo.insertar(any()) } throws RuntimeException("sin conexión")
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
    fun `si el repositorio lanza una excepcion al actualizar se muestra No se pudo guardar`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { repo.actualizar(any()) } throws RuntimeException("timeout")
        val vm = vmListo(4L)

        vm.guardar()
        advanceUntilIdle()

        assertEquals("No se pudo guardar: timeout", vm.uiState.value.mensajeError)
        assertFalse(vm.uiState.value.guardadoExitoso)
    }

    @Test
    fun `despues de un error se puede reintentar y el mensaje anterior se limpia`() = runTest {
        coEvery { repo.insertar(any()) } throws RuntimeException("falla") andThen 10L
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
        coEvery { repo.insertar(any()) } returns 0L
        val vm = vmListo()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.guardadoExitoso)
        assertNotNull(s.mensajeError)
    }

    // =========================================================================
    // Eliminar
    // =========================================================================

    @Test
    fun `eliminar con exito marca eliminado y llama al repositorio con el id de la ruta`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { repo.eliminar(4L) } returns null
        val vm = vmListo(4L)

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.eliminado)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        coVerify(exactly = 1) { repo.eliminar(4L) }
    }

    @Test
    fun `eliminar con un motivo de rechazo lo muestra y no marca eliminado`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { repo.eliminar(4L) } returns "La organización tiene oportunidades asociadas"
        val vm = vmListo(4L)

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("La organización tiene oportunidades asociadas", s.mensajeError)
        assertFalse(s.eliminado)
        assertFalse(s.guardando)
    }

    @Test
    fun `eliminar con una excepcion muestra No se pudo eliminar`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { repo.eliminar(4L) } throws RuntimeException("sin red")
        val vm = vmListo(4L)

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("No se pudo eliminar: sin red", s.mensajeError)
        assertFalse(s.eliminado)
        assertFalse(s.guardando)
    }

    @Test
    fun `eliminar se ignora en creacion`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val vm = vmListo()

        vm.eliminar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { repo.eliminar(any()) }
    }

    @Test
    fun `eliminar se ignora si el usuario no es supervisor`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())
        val vm = vmListo(4L)
        assertFalse(vm.uiState.value.puedeEliminar)

        vm.eliminar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { repo.eliminar(any()) }
    }

    @Test
    fun `un segundo eliminar despues de eliminado se ignora`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { repo.eliminar(4L) } returns null
        val vm = vmListo(4L)

        vm.eliminar()
        advanceUntilIdle()
        vm.eliminar()
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.eliminar(4L) }
    }

    @Test
    fun `eliminar y guardar no se pisan mientras hay una operacion en curso`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val compuerta = CompletableDeferred<Unit>()
        coEvery { repo.eliminar(4L) } coAnswers { compuerta.await(); null }
        val vm = vmListo(4L)

        vm.eliminar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardando)

        vm.guardar()                             // ignorado: ya hay una operación en curso
        vm.eliminar()                            // ignorado también
        compuerta.complete(Unit)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { repo.actualizar(any()) }
        coVerify(exactly = 1) { repo.eliminar(4L) }
    }
}