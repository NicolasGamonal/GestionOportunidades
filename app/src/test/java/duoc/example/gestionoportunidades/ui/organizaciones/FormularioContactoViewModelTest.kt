package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.util.CampoContacto
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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de FormularioContactoViewModel (Fase 3 del plan).
 * Argumentos de la ruta: "idOrganizacion" (obligatorio) e "id" (opcional; > 0 = edición).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioContactoViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()

    private val contactoRepo = mockk<ContactoRepository>()
    private val organizacionRepo = mockk<OrganizacionRepository>()

    private val organizacion = Organizacion(
        idOrganizacion = 3L, nombre = "Municipalidad de Ñuñoa", tipo = "Municipalidad",
        rubro = "Gobierno local", comuna = "Ñuñoa"
    )

    // =========================================================================
    // Ayudas
    // =========================================================================

    private fun contacto(idOrganizacion: Long = 3L) = Contacto(
        idContacto = 8L, nombre = "Ana Pérez", cargo = "Jefa de eventos", area = "Marketing",
        correo = "ana@empresa.cl", telefono = "+56912345678", idOrganizacion = idOrganizacion,
        medioPreferente = "Teléfono"
    )

    @Before
    fun preparar() {
        coEvery { organizacionRepo.obtenerPorId(3L) } returns organizacion
        coEvery { contactoRepo.obtenerPorId(8L) } returns contacto()
    }

    private fun crearViewModel(argumentos: Map<String, Any?> = mapOf("idOrganizacion" to 3L)) =
        FormularioContactoViewModel(SavedStateHandle(argumentos), contactoRepo, organizacionRepo)

    private fun TestScope.vmCreacion(): FormularioContactoViewModel =
        crearViewModel().also { advanceUntilIdle() }

    private fun TestScope.vmEdicion(): FormularioContactoViewModel =
        crearViewModel(mapOf("idOrganizacion" to 3L, "id" to 8L)).also { advanceUntilIdle() }

    private fun llenarValido(vm: FormularioContactoViewModel) {
        vm.onNombreChange("  Ana Pérez  ")
        vm.onCargoChange("  Jefa de eventos  ")
        vm.onAreaChange("Marketing")
        vm.onCorreoChange("ana@empresa.cl")
        vm.onTelefonoChange("+56 9 1234 5678")
        vm.onMedioChange("WhatsApp")
    }

    // =========================================================================
    // Carga
    // =========================================================================

    @Test
    fun `sin el argumento idOrganizacion el ViewModel falla al crearse`() {
        assertThrows(IllegalStateException::class.java) { crearViewModel(emptyMap()) }
    }

    @Test
    fun `en creacion carga el nombre de la organizacion y parte con valores por defecto`() = runTest {
        val s = vmCreacion().uiState.value

        assertEquals("Municipalidad de Ñuñoa", s.nombreOrganizacion)
        assertFalse(s.esEdicion)
        assertFalse(s.cargando)
        assertFalse(s.noEncontrado)
        assertEquals("", s.nombre)
        assertEquals("Correo", s.medioPreferente)
        assertTrue(s.errores.isEmpty())
        coVerify(exactly = 0) { contactoRepo.obtenerPorId(any()) }
    }

    @Test
    fun `si la organizacion no existe el nombre queda vacio`() = runTest {
        coEvery { organizacionRepo.obtenerPorId(3L) } returns null

        assertEquals("", vmCreacion().uiState.value.nombreOrganizacion)
    }

    @Test
    fun `un id 0 o negativo se trata como creacion`() = runTest {
        val vm = crearViewModel(mapOf("idOrganizacion" to 3L, "id" to -1L)).also { advanceUntilIdle() }

        assertFalse(vm.uiState.value.esEdicion)
        coVerify(exactly = 0) { contactoRepo.obtenerPorId(any()) }
    }

    @Test
    fun `en edicion parte cargando y despues carga los datos del contacto`() = runTest {
        val vm = crearViewModel(mapOf("idOrganizacion" to 3L, "id" to 8L))
        assertTrue(vm.uiState.value.esEdicion)
        assertTrue(vm.uiState.value.cargando)

        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.cargando)
        assertFalse(s.noEncontrado)
        assertEquals("Municipalidad de Ñuñoa", s.nombreOrganizacion)
        assertEquals("Ana Pérez", s.nombre)
        assertEquals("Jefa de eventos", s.cargo)
        assertEquals("Marketing", s.area)
        assertEquals("ana@empresa.cl", s.correo)
        assertEquals("+56912345678", s.telefono)
        assertEquals("Teléfono", s.medioPreferente)
    }

    @Test
    fun `un contacto de otra organizacion se trata como noEncontrado`() = runTest {
        coEvery { contactoRepo.obtenerPorId(8L) } returns contacto(idOrganizacion = 99L)

        val s = vmEdicion().uiState.value

        assertTrue(s.noEncontrado)
        assertFalse(s.cargando)
        assertEquals("", s.nombre)
        assertEquals("Municipalidad de Ñuñoa", s.nombreOrganizacion)
    }

    @Test
    fun `un contacto inexistente se trata como noEncontrado`() = runTest {
        coEvery { contactoRepo.obtenerPorId(8L) } returns null

        val s = vmEdicion().uiState.value

        assertTrue(s.noEncontrado)
        assertFalse(s.cargando)
    }

    // =========================================================================
    // Callbacks de campos
    // =========================================================================

    @Test
    fun `el filtro de correo quita los espacios`() = runTest {
        val vm = vmCreacion()

        vm.onCorreoChange(" ana @ empresa .cl ")

        assertEquals("ana@empresa.cl", vm.uiState.value.correo)
    }

    @Test
    fun `el filtro de telefono deja digitos mas y espacios con maximo 16 caracteres`() = runTest {
        val vm = vmCreacion()

        vm.onTelefonoChange("+56 (9) 12-34 abc 5678")
        assertEquals("+56 9 1234  5678", vm.uiState.value.telefono)

        vm.onTelefonoChange("12345678901234567890")
        assertEquals("1234567890123456", vm.uiState.value.telefono)
    }

    @Test
    fun `los callbacks actualizan el valor de su campo`() = runTest {
        val vm = vmCreacion()

        llenarValido(vm)

        val s = vm.uiState.value
        assertEquals("  Ana Pérez  ", s.nombre)          // el trim se aplica al guardar
        assertEquals("  Jefa de eventos  ", s.cargo)
        assertEquals("Marketing", s.area)
        assertEquals("ana@empresa.cl", s.correo)
        assertEquals("+56 9 1234 5678", s.telefono)
        assertEquals("WhatsApp", s.medioPreferente)
    }

    @Test
    fun `guardar con el formulario vacio marca los 5 campos con error`() = runTest {
        val vm = vmCreacion()

        vm.guardar()

        assertEquals(CampoContacto.entries.toSet(), vm.uiState.value.errores.keys)
    }

    @Test
    fun `un correo y un telefono invalidos marcan error en su campo`() = runTest {
        val vm = vmCreacion()
        llenarValido(vm)
        vm.onCorreoChange("ana@")
        vm.onTelefonoChange("123")

        vm.guardar()

        val errores = vm.uiState.value.errores
        assertEquals(setOf(CampoContacto.CORREO, CampoContacto.TELEFONO), errores.keys)
        assertEquals("Correo inválido", errores[CampoContacto.CORREO])
        assertEquals("Teléfono inválido", errores[CampoContacto.TELEFONO])
    }

    @Test
    fun `cada onChange limpia solo el error de su propio campo`() = runTest {
        val todos = CampoContacto.entries.toSet()
        val casos: List<Pair<CampoContacto, (FormularioContactoViewModel) -> Unit>> = listOf(
            CampoContacto.NOMBRE to { vm: FormularioContactoViewModel -> vm.onNombreChange("Ana") },
            CampoContacto.CARGO to { vm: FormularioContactoViewModel -> vm.onCargoChange("Jefa") },
            CampoContacto.AREA to { vm: FormularioContactoViewModel -> vm.onAreaChange("Ventas") },
            CampoContacto.CORREO to { vm: FormularioContactoViewModel -> vm.onCorreoChange("a@b.cl") },
            CampoContacto.TELEFONO to { vm: FormularioContactoViewModel -> vm.onTelefonoChange("12345678") }
        )

        casos.forEach { (campo, accion) ->
            val vm = vmCreacion()
            vm.guardar()
            assertEquals(todos, vm.uiState.value.errores.keys)

            accion(vm)

            assertEquals("tras editar $campo", todos - campo, vm.uiState.value.errores.keys)
        }
    }

    @Test
    fun `cambiar el medio preferente no limpia ningun error`() = runTest {
        val vm = vmCreacion()
        vm.guardar()

        vm.onMedioChange("WhatsApp")

        assertEquals(CampoContacto.entries.toSet(), vm.uiState.value.errores.keys)
        assertEquals("WhatsApp", vm.uiState.value.medioPreferente)
    }

    // =========================================================================
    // Guardar
    // =========================================================================

    @Test
    fun `guardar con errores no llama al repositorio y conserva lo escrito`() = runTest {
        val vm = vmCreacion()
        vm.onNombreChange("Ana Pérez")

        vm.guardar()
        advanceUntilIdle()

        assertEquals("Ana Pérez", vm.uiState.value.nombre)
        assertFalse(vm.uiState.value.guardando)
        assertFalse(vm.uiState.value.guardadoExitoso)
        coVerify(exactly = 0) { contactoRepo.insertar(any()) }
        coVerify(exactly = 0) { contactoRepo.actualizar(any()) }
    }

    @Test
    fun `crear envia el contacto recortado con id 0 y la organizacion de la ruta`() = runTest {
        val enviado = slot<Contacto>()
        coEvery { contactoRepo.insertar(capture(enviado)) } returns 10L
        val vm = vmCreacion()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.guardadoExitoso)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        assertEquals(
            Contacto(
                idContacto = 0L, nombre = "Ana Pérez", cargo = "Jefa de eventos", area = "Marketing",
                correo = "ana@empresa.cl", telefono = "+56 9 1234 5678", idOrganizacion = 3L,
                medioPreferente = "WhatsApp"
            ),
            enviado.captured
        )
        coVerify(exactly = 0) { contactoRepo.actualizar(any()) }
    }

    @Test
    fun `actualizar en edicion conserva el id del contacto y la organizacion de la ruta`() = runTest {
        val enviado = slot<Contacto>()
        coEvery { contactoRepo.actualizar(capture(enviado)) } just Runs
        val vm = vmEdicion()
        vm.onNombreChange("Ana Soto")

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        assertEquals(contacto().copy(nombre = "Ana Soto"), enviado.captured)
        coVerify(exactly = 0) { contactoRepo.insertar(any()) }
    }

    @Test
    fun `un segundo toque en Guardar mientras guarda o despues de guardar se ignora`() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        coEvery { contactoRepo.insertar(any()) } coAnswers { compuerta.await(); 10L }
        val vm = vmCreacion()
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

        coVerify(exactly = 1) { contactoRepo.insertar(any()) }
    }

    @Test
    fun `si el repositorio lanza una excepcion al crear se muestra No se pudo guardar`() = runTest {
        coEvery { contactoRepo.insertar(any()) } throws RuntimeException("sin conexión")
        val vm = vmCreacion()
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
        coEvery { contactoRepo.actualizar(any()) } throws RuntimeException("timeout")
        val vm = vmEdicion()

        vm.guardar()
        advanceUntilIdle()

        assertEquals("No se pudo guardar: timeout", vm.uiState.value.mensajeError)
        assertFalse(vm.uiState.value.guardadoExitoso)
    }

    @Test
    fun `despues de un error se puede reintentar y el mensaje anterior se limpia`() = runTest {
        coEvery { contactoRepo.insertar(any()) } throws RuntimeException("falla") andThen 10L
        val vm = vmCreacion()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.mensajeError)

        vm.guardar()
        advanceUntilIdle()

        assertNull(vm.uiState.value.mensajeError)
        assertTrue(vm.uiState.value.guardadoExitoso)
    }

    // DEFECTO 1 del plan: un guardado fallido se muestra como éxito.
    // Si la API falla, ContactoRepository.insertar devuelve 0L (y actualizar devuelve Unit sin avisar),
    // pero el formulario marca guardadoExitoso = true. Para activar la prueba, el ViewModel debe tratar 0L como fallo.
    @Ignore("Defecto 1: un guardado fallido (el repositorio devuelve 0L) se muestra como éxito")
    @Test
    fun `DEFECTO 1 si el repositorio devuelve 0L se espera un mensaje de error y no guardadoExitoso`() = runTest {
        coEvery { contactoRepo.insertar(any()) } returns 0L
        val vm = vmCreacion()
        llenarValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.guardadoExitoso)
        assertNotNull(s.mensajeError)
    }

    @Test
    fun `limpiarMensajeError deja el mensaje en null`() = runTest {
        coEvery { contactoRepo.insertar(any()) } throws RuntimeException("falla")
        val vm = vmCreacion()
        llenarValido(vm)
        vm.guardar()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.mensajeError)

        vm.limpiarMensajeError()

        assertNull(vm.uiState.value.mensajeError)
    }

    // =========================================================================
    // Eliminar
    // =========================================================================

    @Test
    fun `eliminar con exito marca eliminado y llama al repositorio con el id de la ruta`() = runTest {
        coEvery { contactoRepo.eliminar(8L) } returns null
        val vm = vmEdicion()

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.eliminado)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        coVerify(exactly = 1) { contactoRepo.eliminar(8L) }
    }

    @Test
    fun `eliminar con un motivo de rechazo lo muestra y no marca eliminado`() = runTest {
        coEvery { contactoRepo.eliminar(8L) } returns "El contacto tiene seguimientos asociados"
        val vm = vmEdicion()

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("El contacto tiene seguimientos asociados", s.mensajeError)
        assertFalse(s.eliminado)
        assertFalse(s.guardando)
    }

    @Test
    fun `eliminar con una excepcion muestra No se pudo eliminar`() = runTest {
        coEvery { contactoRepo.eliminar(8L) } throws RuntimeException("sin red")
        val vm = vmEdicion()

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("No se pudo eliminar: sin red", s.mensajeError)
        assertFalse(s.eliminado)
        assertFalse(s.guardando)
    }

    @Test
    fun `eliminar se ignora en creacion`() = runTest {
        val vm = vmCreacion()

        vm.eliminar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { contactoRepo.eliminar(any()) }
    }

    @Test
    fun `un segundo eliminar despues de eliminado se ignora`() = runTest {
        coEvery { contactoRepo.eliminar(8L) } returns null
        val vm = vmEdicion()

        vm.eliminar()
        advanceUntilIdle()
        vm.eliminar()
        advanceUntilIdle()

        coVerify(exactly = 1) { contactoRepo.eliminar(8L) }
    }

    @Test
    fun `eliminar y guardar no se pisan mientras hay una operacion en curso`() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        coEvery { contactoRepo.eliminar(8L) } coAnswers { compuerta.await(); null }
        val vm = vmEdicion()

        vm.eliminar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardando)

        vm.guardar()                             // ignorado: ya hay una operación en curso
        vm.eliminar()                            // ignorado también
        compuerta.complete(Unit)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { contactoRepo.actualizar(any()) }
        coVerify(exactly = 1) { contactoRepo.eliminar(8L) }
    }
}