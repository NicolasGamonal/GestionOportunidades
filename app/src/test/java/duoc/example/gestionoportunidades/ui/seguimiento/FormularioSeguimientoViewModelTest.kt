package duoc.example.gestionoportunidades.ui.seguimiento

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.model.Seguimiento
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.testutil.contactoLocal
import duoc.example.gestionoportunidades.testutil.oportunidadLocal
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.CampoSeguimiento
import duoc.example.gestionoportunidades.util.Recordatorios
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pruebas de FormularioSeguimientoViewModel (Fase 3).
 *
 * - Los tres repositorios son simulados con MockK; no hay red ni Android real.
 * - Recordatorios (usa AlarmManager) se simula con mockkObject: aquí solo se comprueba que el
 *   ViewModel lo llama con los datos correctos. Su lógica real se prueba en la Fase 4 (Robolectric).
 * - La clave del SavedStateHandle es "idOportunidad" (igual que la ruta formulario_seguimiento/{idOportunidad}).
 * - La validación usa la fecha de hoy real, así que las fechas de las pruebas se calculan
 *   relativas a hoy (en America/Santiago, que fija ZonaHorariaRule).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioSeguimientoViewModelTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    private val seguimientoRepo = mockk<SeguimientoRepository>()
    private val oportunidadRepo = mockk<OportunidadRepository>()
    private val contactoRepo = mockk<ContactoRepository>()
    private val contexto = mockk<Context>(relaxed = true)

    @Before
    fun prepararSimulaciones() {
        // Por defecto: la oportunidad 10 existe, es del ejecutivo 5 y su organización no tiene contactos.
        coEvery { oportunidadRepo.obtenerPorId(ID_OPORTUNIDAD) } returns
                oportunidadLocal(idOportunidad = ID_OPORTUNIDAD, idResponsable = ID_EJECUTIVO, idOrganizacion = ID_ORGANIZACION)
        every { contactoRepo.obtenerPorOrganizacion(ID_ORGANIZACION) } returns flowOf(emptyList())

        // Recordatorios real usa AlarmManager/PendingIntent: se reemplaza por un doble que no hace nada.
        mockkObject(Recordatorios)
        every { Recordatorios.programarSeguimiento(any(), any(), any(), any(), any()) } just Runs
    }

    @After
    fun limpiarSimulaciones() {
        unmockkObject(Recordatorios)
    }

    // =========================================================================
    // Ayudas
    // =========================================================================

    private fun iniciarSesion(rol: String = "EJECUTIVO", id: Long = ID_EJECUTIVO) {
        SesionManager.iniciarSesion(
            Usuario(
                idUsuario = id, username = "usuario$id", contrasena = "clave",
                nombreCompleto = "Usuario $id", rol = rol
            )
        )
    }

    private fun crearViewModel(conId: Boolean = true): FormularioSeguimientoViewModel {
        val estado = if (conId) SavedStateHandle(mapOf("idOportunidad" to ID_OPORTUNIDAD)) else SavedStateHandle()
        return FormularioSeguimientoViewModel(estado, seguimientoRepo, oportunidadRepo, contactoRepo, contexto)
    }

    /** Crea el ViewModel y deja correr el init (carga de la oportunidad y de los contactos). */
    private fun TestScope.crearYCargar(): FormularioSeguimientoViewModel {
        val vm = crearViewModel()
        advanceUntilIdle()
        return vm
    }

    /** Fecha de hoy + [dias], en el formato del formulario (dd/MM/yyyy). */
    private fun fechaCorta(dias: Long): String = LocalDate.now().plusDays(dias).format(FORMATO_FECHA)

    /** Fin del día (23:59, Santiago) de hoy + [dias], en milisegundos: lo que debe guardarse. */
    private fun finDelDia(dias: Long): Long =
        FechasPrueba.hora("${LocalDate.now().plusDays(dias)}T23:59:00")

    private fun llenarFormularioValido(vm: FormularioSeguimientoViewModel, fecha: String = fechaCorta(7)) {
        vm.onTipoChange("Llamada")
        vm.onResultadoChange("Contacto interesado en la propuesta")
        vm.onProximaAccionChange("Enviar propuesta")
        vm.onFechaProximaChange(fecha)
    }

    // =========================================================================
    // Estado inicial y SavedStateHandle
    // =========================================================================

    @Test
    fun `el estado inicial esta cargando y vacio`() {
        val s = crearViewModel().uiState.value

        assertTrue(s.cargando)
        assertFalse(s.sinAcceso)
        assertEquals("", s.nombreOportunidad)
        assertTrue(s.contactos.isEmpty())
        assertNull(s.idContacto)
        assertEquals("", s.tipoAccion)
        assertEquals("", s.resultado)
        assertEquals("", s.proximaAccion)
        assertEquals("", s.fechaProximo)
        assertTrue(s.errores.isEmpty())
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
        assertNull(s.mensajeError)
    }

    @Test(expected = IllegalStateException::class)
    fun `sin idOportunidad en el SavedStateHandle falla al crearse`() {
        crearViewModel(conId = false)
    }

    // =========================================================================
    // Carga inicial y permisos (init)
    // =========================================================================

    @Test
    fun `el ejecutivo responsable carga el nombre de la oportunidad`() = runTest {
        iniciarSesion("EJECUTIVO", ID_EJECUTIVO)

        val s = crearYCargar().uiState.value

        assertFalse(s.cargando)
        assertFalse(s.sinAcceso)
        assertEquals("Feria del Emprendedor", s.nombreOportunidad)
    }

    @Test
    fun `el supervisor puede registrar gestiones en una oportunidad de otro ejecutivo`() = runTest {
        iniciarSesion("SUPERVISOR", 99L)

        val s = crearYCargar().uiState.value

        assertFalse(s.cargando)
        assertFalse(s.sinAcceso)
        assertEquals("Feria del Emprendedor", s.nombreOportunidad)
    }

    @Test
    fun `un ejecutivo que no es el responsable queda sin acceso`() = runTest {
        iniciarSesion("EJECUTIVO", 6L)

        val s = crearYCargar().uiState.value

        assertFalse(s.cargando)
        assertTrue(s.sinAcceso)
        assertEquals("", s.nombreOportunidad)
        verify(exactly = 0) { contactoRepo.obtenerPorOrganizacion(any()) }
    }

    @Test
    fun `una oportunidad sin responsable queda sin acceso para el ejecutivo`() = runTest {
        coEvery { oportunidadRepo.obtenerPorId(ID_OPORTUNIDAD) } returns
                oportunidadLocal(idOportunidad = ID_OPORTUNIDAD, idResponsable = null)
        iniciarSesion("EJECUTIVO", ID_EJECUTIVO)

        val s = crearYCargar().uiState.value

        assertTrue(s.sinAcceso)
    }

    @Test
    fun `marketing queda sin acceso aunque la oportunidad este confirmada`() = runTest {
        // Marketing solo lee: no puede registrar gestiones (el ViewModel no usa su permiso de ver confirmadas).
        coEvery { oportunidadRepo.obtenerPorId(ID_OPORTUNIDAD) } returns
                oportunidadLocal(idOportunidad = ID_OPORTUNIDAD, estado = "Confirmada")
        iniciarSesion("MARKETING", 7L)

        val s = crearYCargar().uiState.value

        assertFalse(s.cargando)
        assertTrue(s.sinAcceso)
    }

    @Test
    fun `una oportunidad inexistente queda sin acceso`() = runTest {
        coEvery { oportunidadRepo.obtenerPorId(ID_OPORTUNIDAD) } returns null
        iniciarSesion("SUPERVISOR", 99L)

        val s = crearYCargar().uiState.value

        assertFalse(s.cargando)
        assertTrue(s.sinAcceso)
        verify(exactly = 0) { contactoRepo.obtenerPorOrganizacion(any()) }
    }

    @Test
    fun `sin sesion iniciada queda sin acceso`() = runTest {
        val s = crearYCargar().uiState.value

        assertFalse(s.cargando)
        assertTrue(s.sinAcceso)
    }

    @Test
    fun `carga los contactos de la organizacion de la oportunidad`() = runTest {
        val carla = contactoLocal(idContacto = 20L, nombre = "Carla Rojas")
        val pedro = contactoLocal(idContacto = 21L, nombre = "Pedro Soto")
        every { contactoRepo.obtenerPorOrganizacion(ID_ORGANIZACION) } returns flowOf(listOf(carla, pedro))
        iniciarSesion()

        val s = crearYCargar().uiState.value

        assertEquals(listOf(carla, pedro), s.contactos)
        verify(exactly = 1) { contactoRepo.obtenerPorOrganizacion(ID_ORGANIZACION) }
    }

    @Test
    fun `la lista de contactos se actualiza cuando el flujo emite una lista nueva`() = runTest {
        val carla = contactoLocal(idContacto = 20L, nombre = "Carla Rojas")
        val pedro = contactoLocal(idContacto = 21L, nombre = "Pedro Soto")
        val contactos = MutableStateFlow(listOf(carla))
        every { contactoRepo.obtenerPorOrganizacion(ID_ORGANIZACION) } returns contactos
        iniciarSesion()
        val vm = crearYCargar()
        assertEquals(listOf(carla), vm.uiState.value.contactos)

        contactos.value = listOf(carla, pedro)
        advanceUntilIdle()

        assertEquals(listOf(carla, pedro), vm.uiState.value.contactos)
    }

    // =========================================================================
    // Edición de campos
    // =========================================================================

    @Test
    fun `onTipoChange actualiza el tipo de accion`() = runTest {
        val vm = crearYCargar()
        vm.onTipoChange("Reunión")
        assertEquals("Reunión", vm.uiState.value.tipoAccion)
    }

    @Test
    fun `onResultadoChange actualiza el resultado`() = runTest {
        val vm = crearYCargar()
        vm.onResultadoChange("Quedaron en revisar la propuesta")
        assertEquals("Quedaron en revisar la propuesta", vm.uiState.value.resultado)
    }

    @Test
    fun `onProximaAccionChange actualiza la proxima accion`() = runTest {
        val vm = crearYCargar()
        vm.onProximaAccionChange("Llamar el lunes")
        assertEquals("Llamar el lunes", vm.uiState.value.proximaAccion)
    }

    @Test
    fun `onFechaProximaChange actualiza la fecha del proximo seguimiento`() = runTest {
        val vm = crearYCargar()
        vm.onFechaProximaChange("20/03/2026")
        assertEquals("20/03/2026", vm.uiState.value.fechaProximo)
    }

    @Test
    fun `editar un campo limpia solo el error de ese campo`() = runTest {
        val vm = crearYCargar()
        vm.guardar()
        assertEquals(TODOS_LOS_CAMPOS, vm.uiState.value.errores.keys)

        vm.onTipoChange("Llamada")
        assertEquals(
            setOf(CampoSeguimiento.RESULTADO, CampoSeguimiento.PROXIMA_ACCION, CampoSeguimiento.FECHA_PROXIMA),
            vm.uiState.value.errores.keys
        )

        vm.onResultadoChange("Contacto interesado")
        assertEquals(
            setOf(CampoSeguimiento.PROXIMA_ACCION, CampoSeguimiento.FECHA_PROXIMA),
            vm.uiState.value.errores.keys
        )

        vm.onProximaAccionChange("Enviar propuesta")
        assertEquals(setOf(CampoSeguimiento.FECHA_PROXIMA), vm.uiState.value.errores.keys)

        vm.onFechaProximaChange(fechaCorta(1))
        assertTrue(vm.uiState.value.errores.isEmpty())
    }

    @Test
    fun `onContactoChange guarda el contacto y acepta null sin tocar los errores`() = runTest {
        val vm = crearYCargar()
        vm.guardar()
        assertEquals(TODOS_LOS_CAMPOS, vm.uiState.value.errores.keys)

        vm.onContactoChange(20L)
        assertEquals(20L, vm.uiState.value.idContacto)
        assertEquals(TODOS_LOS_CAMPOS, vm.uiState.value.errores.keys)

        vm.onContactoChange(null)
        assertNull(vm.uiState.value.idContacto)
    }

    // =========================================================================
    // guardar(): validación
    // =========================================================================

    @Test
    fun `guardar con el formulario vacio marca los 4 campos y no llama al repositorio`() = runTest {
        iniciarSesion()
        val vm = crearYCargar()

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(TODOS_LOS_CAMPOS, s.errores.keys)
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
        assertNull(s.mensajeError)
        coVerify(exactly = 0) { seguimientoRepo.insertar(any()) }
    }

    @Test
    fun `guardar con un resultado demasiado corto marca solo el resultado`() = runTest {
        iniciarSesion()
        val vm = crearYCargar()
        llenarFormularioValido(vm)
        vm.onResultadoChange("abc")

        vm.guardar()
        advanceUntilIdle()

        assertEquals(setOf(CampoSeguimiento.RESULTADO), vm.uiState.value.errores.keys)
        coVerify(exactly = 0) { seguimientoRepo.insertar(any()) }
    }

    @Test
    fun `guardar con una fecha en formato invalido marca la fecha`() = runTest {
        iniciarSesion()
        val vm = crearYCargar()
        llenarFormularioValido(vm, fecha = "abc")

        vm.guardar()
        advanceUntilIdle()

        assertEquals(setOf(CampoSeguimiento.FECHA_PROXIMA), vm.uiState.value.errores.keys)
        coVerify(exactly = 0) { seguimientoRepo.insertar(any()) }
    }

    @Test
    fun `guardar con una fecha anterior a hoy marca la fecha`() = runTest {
        iniciarSesion()
        val vm = crearYCargar()
        llenarFormularioValido(vm, fecha = fechaCorta(-1))

        vm.guardar()
        advanceUntilIdle()

        assertEquals(
            "El próximo seguimiento no puede ser anterior a la fecha de la gestión",
            vm.uiState.value.errores[CampoSeguimiento.FECHA_PROXIMA]
        )
        assertEquals(1, vm.uiState.value.errores.size)
        coVerify(exactly = 0) { seguimientoRepo.insertar(any()) }
    }

    @Test
    fun `guardar con la fecha de hoy es valido`() = runTest {
        iniciarSesion()
        coEvery { seguimientoRepo.insertar(any()) } returns 77L
        val vm = crearYCargar()
        llenarFormularioValido(vm, fecha = fechaCorta(0))

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errores.isEmpty())
        assertTrue(vm.uiState.value.guardadoExitoso)
    }

    // =========================================================================
    // guardar(): sesión
    // =========================================================================

    @Test
    fun `guardar sin sesion muestra el mensaje de sesion expirada y no guarda`() = runTest {
        iniciarSesion()
        val vm = crearYCargar()
        llenarFormularioValido(vm)
        SesionManager.cerrarSesion() // la sesión expira después de abrir el formulario

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("Tu sesión expiró. Vuelve a iniciar sesión.", s.mensajeError)
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
        coVerify(exactly = 0) { seguimientoRepo.insertar(any()) }
    }

    // =========================================================================
    // guardar(): éxito
    // =========================================================================

    @Test
    fun `guardar construye el seguimiento con los datos del formulario`() = runTest {
        iniciarSesion("EJECUTIVO", ID_EJECUTIVO)
        val capturado = slot<Seguimiento>()
        coEvery { seguimientoRepo.insertar(capture(capturado)) } returns 77L
        val vm = crearYCargar()
        vm.onTipoChange("Reunión")
        vm.onResultadoChange("  Reunión realizada con el equipo  ")
        vm.onProximaAccionChange("  Enviar cotización  ")
        vm.onFechaProximaChange(fechaCorta(7))

        val antes = System.currentTimeMillis()
        vm.guardar()
        advanceUntilIdle()
        val despues = System.currentTimeMillis()

        val seg = capturado.captured
        assertEquals(0L, seg.idSeguimiento)
        assertEquals(ID_OPORTUNIDAD, seg.idOportunidad)
        assertEquals(ID_EJECUTIVO, seg.idUsuario)
        assertEquals("Reunión", seg.tipoAccion)
        assertEquals("Reunión realizada con el equipo", seg.resultado)   // sin espacios sobrantes
        assertEquals("Enviar cotización", seg.proximaAccion)
        assertEquals(finDelDia(7), seg.fechaProximo)                      // 23:59 de Santiago
        assertNull(seg.idContacto)
        assertTrue("fechaHora debe ser el momento de guardar", seg.fechaHora in antes..despues)
    }

    @Test
    fun `guardar usa como autor al usuario de la sesion`() = runTest {
        iniciarSesion("SUPERVISOR", 99L)
        val capturado = slot<Seguimiento>()
        coEvery { seguimientoRepo.insertar(capture(capturado)) } returns 77L
        val vm = crearYCargar()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        assertEquals(99L, capturado.captured.idUsuario)
        assertEquals(ID_OPORTUNIDAD, capturado.captured.idOportunidad)
    }

    @Test
    fun `guardar envia el contacto elegido`() = runTest {
        iniciarSesion()
        val capturado = slot<Seguimiento>()
        coEvery { seguimientoRepo.insertar(capture(capturado)) } returns 77L
        val vm = crearYCargar()
        llenarFormularioValido(vm)
        vm.onContactoChange(20L)

        vm.guardar()
        advanceUntilIdle()

        assertEquals(20L, capturado.captured.idContacto)
    }

    @Test
    fun `guardar sin contacto envia idContacto null`() = runTest {
        iniciarSesion()
        val capturado = slot<Seguimiento>()
        coEvery { seguimientoRepo.insertar(capture(capturado)) } returns 77L
        val vm = crearYCargar()
        llenarFormularioValido(vm)
        vm.onContactoChange(20L)
        vm.onContactoChange(null)   // vuelve a "Sin contacto"

        vm.guardar()
        advanceUntilIdle()

        assertNull(capturado.captured.idContacto)
    }

    @Test
    fun `guardar con exito marca guardadoExitoso y programa el recordatorio`() = runTest {
        iniciarSesion()
        val capturado = slot<Seguimiento>()
        coEvery { seguimientoRepo.insertar(capture(capturado)) } returns 77L
        val vm = crearYCargar()
        llenarFormularioValido(vm, fecha = fechaCorta(7))

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.guardadoExitoso)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        assertTrue(s.errores.isEmpty())
        verify(exactly = 1) {
            Recordatorios.programarSeguimiento(
                context = contexto,
                idSeguimiento = 77L,
                nombreOportunidad = "Feria del Emprendedor",
                proximaAccion = "Enviar propuesta",
                fechaProximoMillis = finDelDia(7)
            )
        }
        assertEquals(finDelDia(7), capturado.captured.fechaProximo)
    }

    @Test
    fun `mientras guarda marca guardando y ignora un segundo guardar`() = runTest {
        iniciarSesion()
        val respuesta = CompletableDeferred<Long>()
        coEvery { seguimientoRepo.insertar(any()) } coAnswers { respuesta.await() }
        val vm = crearYCargar()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardando)
        assertFalse(vm.uiState.value.guardadoExitoso)

        vm.guardar() // doble toque: no debe crear otra gestión
        advanceUntilIdle()
        coVerify(exactly = 1) { seguimientoRepo.insertar(any()) }

        respuesta.complete(77L)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.guardando)
        assertTrue(vm.uiState.value.guardadoExitoso)
    }

    @Test
    fun `despues de guardar con exito un nuevo guardar no hace nada`() = runTest {
        iniciarSesion()
        coEvery { seguimientoRepo.insertar(any()) } returns 77L
        val vm = crearYCargar()
        llenarFormularioValido(vm)
        vm.guardar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardadoExitoso)

        vm.guardar()
        advanceUntilIdle()

        coVerify(exactly = 1) { seguimientoRepo.insertar(any()) }
        verify(exactly = 1) { Recordatorios.programarSeguimiento(any(), any(), any(), any(), any()) }
    }

    // =========================================================================
    // guardar(): errores
    // =========================================================================

    @Test
    fun `si insertar lanza una excepcion muestra el mensaje y no marca exito`() = runTest {
        iniciarSesion()
        coEvery { seguimientoRepo.insertar(any()) } throws RuntimeException("sin conexión")
        val vm = crearYCargar()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("No se pudo guardar: sin conexión", s.mensajeError)
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
        verify(exactly = 0) { Recordatorios.programarSeguimiento(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `limpiarMensajeError borra el mensaje de error`() = runTest {
        iniciarSesion()
        coEvery { seguimientoRepo.insertar(any()) } throws RuntimeException("falla")
        val vm = crearYCargar()
        llenarFormularioValido(vm)
        vm.guardar()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.mensajeError)

        vm.limpiarMensajeError()

        assertNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun `despues de un error se puede reintentar y el mensaje anterior se borra`() = runTest {
        iniciarSesion()
        coEvery { seguimientoRepo.insertar(any()) } throws RuntimeException("sin conexión") andThen 78L
        val vm = crearYCargar()
        llenarFormularioValido(vm)
        vm.guardar()
        advanceUntilIdle()
        assertEquals("No se pudo guardar: sin conexión", vm.uiState.value.mensajeError)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertNull(s.mensajeError)
        assertTrue(s.guardadoExitoso)
        coVerify(exactly = 2) { seguimientoRepo.insertar(any()) }
        verify(exactly = 1) {
            Recordatorios.programarSeguimiento(
                context = contexto,
                idSeguimiento = 78L,
                nombreOportunidad = "Feria del Emprendedor",
                proximaAccion = "Enviar propuesta",
                fechaProximoMillis = finDelDia(7)
            )
        }
    }

    @Test
    fun `si insertar devuelve 0 porque la API fallo no debe marcarse como guardado`() = runTest {
        iniciarSesion()
        coEvery { seguimientoRepo.insertar(any()) } returns 0L
        val vm = crearYCargar()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse("No debe marcarse como guardado si la API falló", s.guardadoExitoso)
        assertNotNull("Debe avisar al usuario que no se pudo guardar", s.mensajeError)
        verify(exactly = 0) { Recordatorios.programarSeguimiento(any(), any(), any(), any(), any()) }
    }

    private companion object {
        const val ID_OPORTUNIDAD = 10L
        const val ID_ORGANIZACION = 1L
        const val ID_EJECUTIVO = 5L

        val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val TODOS_LOS_CAMPOS = setOf(
            CampoSeguimiento.TIPO,
            CampoSeguimiento.RESULTADO,
            CampoSeguimiento.PROXIMA_ACCION,
            CampoSeguimiento.FECHA_PROXIMA
        )
    }
}