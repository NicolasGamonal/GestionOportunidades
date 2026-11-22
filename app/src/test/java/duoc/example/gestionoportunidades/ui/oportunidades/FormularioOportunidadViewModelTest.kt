package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.SavedStateHandle
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.FechasPrueba
import duoc.example.gestionoportunidades.testutil.MainDispatcherRule
import duoc.example.gestionoportunidades.testutil.SesionRule
import duoc.example.gestionoportunidades.testutil.ZonaHorariaRule
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.CampoOportunidad
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pruebas de FormularioOportunidadViewModel (Fase 3 del plan).
 * Repositorios con MockK, SavedStateHandle con el id de la ruta y SesionManager según el rol.
 * La zona horaria se fija en America/Santiago para que las fechas den lo mismo en cualquier computador.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioOportunidadViewModelTest {

    @Rule @JvmField val zonaHoraria = ZonaHorariaRule()
    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()

    private val oportunidadRepo = mockk<OportunidadRepository>()
    private val organizacionRepo = mockk<OrganizacionRepository>()

    private val organizaciones = listOf(
        Organizacion(idOrganizacion = 1L, nombre = "Municipalidad de Ñuñoa", tipo = "Municipalidad", rubro = "Gobierno local", comuna = "Ñuñoa"),
        Organizacion(idOrganizacion = 2L, nombre = "Cámara de Comercio", tipo = "Gremio", rubro = "Comercio", comuna = "Providencia")
    )

    @Before
    fun preparar() {
        every { organizacionRepo.obtenerTodas() } returns flowOf(organizaciones)
    }

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

    /** Oportunidad existente: 05/03/2026 15:20 detección, evento 10/03 14:30 al 12/03 16:00 (hora de Santiago). */
    private fun oportunidad(
        id: Long = 5L,
        estado: String = "Identificada",
        idResponsable: Long? = 7L
    ) = Oportunidad(
        idOportunidad = id,
        nombre = "Feria de Ñuñoa",
        tipoEvento = "Feria municipal",
        fechaDeteccion = FechasPrueba.hora("2026-03-05T15:20:00"),
        fechaInicio = FechasPrueba.hora("2026-03-10T14:30:00"),
        fechaTermino = FechasPrueba.hora("2026-03-12T16:00:00"),
        comuna = "Ñuñoa",
        publicoEsperado = 1500,
        prioridad = "Alta",
        estado = estado,
        costoReferencial = 850000.0,
        observaciones = "Llevar material",
        idOrganizacion = 1L,
        idResponsable = idResponsable,
        descripcion = "Feria de emprendedores",
        fuenteInformacion = "Sitio web municipal",
        potencialComercial = "Alto",
        afinidad = "Alta",
        publicoObjetivo = "Familias",
        condicionesParticipacion = "Stand propio",
        requisitos = "Permiso municipal",
        espaciosDisponibles = 20
    )

    private fun crearViewModel(id: Long? = null) = FormularioOportunidadViewModel(
        savedStateHandle = SavedStateHandle(if (id == null) emptyMap() else mapOf("id" to id)),
        oportunidadRepository = oportunidadRepo,
        organizacionRepository = organizacionRepo
    )

    /** Crea el ViewModel y deja correr su inicialización. */
    private fun TestScope.vmListo(id: Long? = null): FormularioOportunidadViewModel =
        crearViewModel(id).also { advanceUntilIdle() }

    /** Llena todos los campos con datos válidos (fechas del 2026, horas 10:30 a 17:45). */
    private fun llenarFormularioValido(vm: FormularioOportunidadViewModel) {
        vm.onNombreChange("  Feria de Ñuñoa  ")
        vm.onTipoChange("Feria municipal")
        vm.onOrganizacionChange(1L)
        vm.onDeteccionChange("01/03/2026")
        vm.onInicioChange("10/03/2026")
        vm.onTerminoChange("12/03/2026")
        vm.onHoraInicioChange("10:30")
        vm.onHoraTerminoChange("17:45")
        vm.onComunaChange("Ñuñoa")
        vm.onPublicoChange("1500")
        vm.onPrioridadChange("Alta")
        vm.onCostoChange("850000")
        vm.onObservacionesChange("  Llevar material  ")
        vm.onDescripcionChange("  Feria de emprendedores  ")
        vm.onFuenteChange("  Sitio web municipal  ")
        vm.onPotencialChange("Alto")
        vm.onAfinidadChange("Alta")
        vm.onPublicoObjetivoChange("  Familias  ")
        vm.onCondicionesChange("  Stand propio  ")
        vm.onRequisitosChange("  Permiso municipal  ")
        vm.onEspaciosChange("20")
    }

    /** Deja los 17 campos con error al presionar Guardar. */
    private fun provocarTodosLosErrores(vm: FormularioOportunidadViewModel) {
        vm.onDeteccionChange("")
        vm.onInicioChange("xx")
        vm.onTerminoChange("xx")
        vm.onHoraInicioChange("zz")          // con inicio == término, la hora inválida marca HORA_TERMINO
        vm.onObservacionesChange("a".repeat(501))
        vm.onDescripcionChange("a".repeat(501))
        vm.onCondicionesChange("a".repeat(501))
        vm.onRequisitosChange("a".repeat(501))
        vm.onEspaciosChange("")
        // nombre, tipo, organización, comuna, público, costo, fuente y público objetivo quedan vacíos
        vm.guardar()
    }

    private val todosLosCampos = CampoOportunidad.entries.toSet()

    private fun caso(campo: CampoOportunidad, accion: (FormularioOportunidadViewModel) -> Unit) = campo to accion

    private val casosDeLimpieza: List<Pair<CampoOportunidad, (FormularioOportunidadViewModel) -> Unit>> = listOf(
        caso(CampoOportunidad.NOMBRE) { it.onNombreChange("Feria") },
        caso(CampoOportunidad.TIPO) { it.onTipoChange("Feria municipal") },
        caso(CampoOportunidad.ORGANIZACION) { it.onOrganizacionChange(1L) },
        caso(CampoOportunidad.DETECCION) { it.onDeteccionChange("01/03/2026") },
        caso(CampoOportunidad.INICIO) { it.onInicioChange("10/03/2026") },
        caso(CampoOportunidad.TERMINO) { it.onTerminoChange("12/03/2026") },
        caso(CampoOportunidad.COMUNA) { it.onComunaChange("Ñuñoa") },
        caso(CampoOportunidad.PUBLICO) { it.onPublicoChange("100") },
        caso(CampoOportunidad.COSTO) { it.onCostoChange("1000") },
        caso(CampoOportunidad.OBSERVACIONES) { it.onObservacionesChange("Texto") },
        caso(CampoOportunidad.DESCRIPCION) { it.onDescripcionChange("Texto") },
        caso(CampoOportunidad.FUENTE) { it.onFuenteChange("Web") },
        // Hay un único error de horario (HORA_TERMINO): lo limpian tanto la hora de inicio como la de término.
        caso(CampoOportunidad.HORA_TERMINO) { it.onHoraInicioChange("10:00") },
        caso(CampoOportunidad.HORA_TERMINO) { it.onHoraTerminoChange("18:00") },
        caso(CampoOportunidad.PUBLICO_OBJETIVO) { it.onPublicoObjetivoChange("Familias") },
        caso(CampoOportunidad.CONDICIONES) { it.onCondicionesChange("Texto") },
        caso(CampoOportunidad.REQUISITOS) { it.onRequisitosChange("Texto") },
        caso(CampoOportunidad.ESPACIOS) { it.onEspaciosChange("5") }
    )

    // =========================================================================
    // Creación: estado inicial
    // =========================================================================

    @Test
    fun `en creacion la fecha de deteccion inicial es hoy`() = runTest {
        val hoy = LocalDate.now(FechasPrueba.SANTIAGO).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        val vm = vmListo()

        assertEquals(hoy, vm.uiState.value.fechaDeteccion)
    }

    @Test
    fun `en creacion el formulario parte con los valores por defecto`() = runTest {
        val s = vmListo().uiState.value

        assertFalse(s.esEdicion)
        assertFalse(s.cargando)
        assertFalse(s.sinAcceso)
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
        assertFalse(s.puedeEliminar)
        assertNull(s.mensajeError)
        assertEquals("", s.nombre)
        assertEquals("Media", s.prioridad)
        assertEquals("09:00", s.horaInicio)
        assertEquals("18:00", s.horaTermino)
        assertEquals("Medio", s.potencialComercial)
        assertEquals("Media", s.afinidad)
        assertEquals("0", s.espaciosDisponibles)
        assertTrue(s.errores.isEmpty())
    }

    @Test
    fun `en creacion no se consulta la oportunidad al repositorio`() = runTest {
        vmListo()

        coVerify(exactly = 0) { oportunidadRepo.obtenerPorId(any()) }
    }

    @Test
    fun `un id ausente 0 o negativo se trata como creacion`() = runTest {
        assertFalse(vmListo().uiState.value.esEdicion)
        assertFalse(vmListo(-1L).uiState.value.esEdicion)
        assertFalse(vmListo(0L).uiState.value.esEdicion)
    }

    @Test
    fun `la lista de organizaciones se carga y se actualiza cuando el repositorio emite`() = runTest {
        val fuente = MutableStateFlow(organizaciones)
        every { organizacionRepo.obtenerTodas() } returns fuente

        val vm = vmListo()
        assertEquals(organizaciones, vm.uiState.value.organizaciones)

        val nueva = Organizacion(idOrganizacion = 3L, nombre = "Colegio San Pedro", tipo = "Colegio", rubro = "Educación", comuna = "Maipú")
        fuente.value = organizaciones + nueva
        advanceUntilIdle()

        assertEquals(organizaciones + nueva, vm.uiState.value.organizaciones)
    }

    // =========================================================================
    // Callbacks de campos
    // =========================================================================

    @Test
    fun `los callbacks actualizan el valor de su campo`() = runTest {
        val vm = vmListo()

        llenarFormularioValido(vm)
        val s = vm.uiState.value

        assertEquals("  Feria de Ñuñoa  ", s.nombre)       // sin recortar: el trim se aplica al guardar
        assertEquals("Feria municipal", s.tipoEvento)
        assertEquals(1L, s.idOrganizacion)
        assertEquals("01/03/2026", s.fechaDeteccion)
        assertEquals("10/03/2026", s.fechaInicio)
        assertEquals("12/03/2026", s.fechaTermino)
        assertEquals("10:30", s.horaInicio)
        assertEquals("17:45", s.horaTermino)
        assertEquals("Ñuñoa", s.comuna)
        assertEquals("1500", s.publicoEsperado)
        assertEquals("Alta", s.prioridad)
        assertEquals("850000", s.costoReferencial)
        assertEquals("Alto", s.potencialComercial)
        assertEquals("Alta", s.afinidad)
        assertEquals("20", s.espaciosDisponibles)
    }

    @Test
    fun `los filtros dejan solo digitos y respetan el largo maximo de 7 10 y 4`() = runTest {
        val vm = vmListo()

        vm.onPublicoChange("12a3456789")
        vm.onCostoChange("1.234.567.890.123")
        vm.onEspaciosChange("12-345")

        val s = vm.uiState.value
        assertEquals("1234567", s.publicoEsperado)
        assertEquals("1234567890", s.costoReferencial)
        assertEquals("1234", s.espaciosDisponibles)
    }

    @Test
    fun `la tabla de limpieza cubre los 17 campos del formulario`() {
        assertEquals(todosLosCampos, casosDeLimpieza.map { it.first }.toSet())
    }

    @Test
    fun `guardar con el formulario vacio marca los 17 campos con error`() = runTest {
        val vm = vmListo()

        provocarTodosLosErrores(vm)

        assertEquals(todosLosCampos, vm.uiState.value.errores.keys)
    }

    @Test
    fun `cada onChange limpia solo el error de su propio campo`() = runTest {
        casosDeLimpieza.forEach { (campo, accion) ->
            val vm = vmListo()
            provocarTodosLosErrores(vm)
            assertEquals(todosLosCampos, vm.uiState.value.errores.keys)

            accion(vm)

            assertEquals("tras editar $campo", todosLosCampos - campo, vm.uiState.value.errores.keys)
        }
    }

    @Test
    fun `prioridad potencial y afinidad no tienen error propio y no limpian ninguno`() = runTest {
        val vm = vmListo()
        provocarTodosLosErrores(vm)

        vm.onPrioridadChange("Baja")
        vm.onPotencialChange("Bajo")
        vm.onAfinidadChange("Baja")

        val s = vm.uiState.value
        assertEquals(todosLosCampos, s.errores.keys)
        assertEquals("Baja", s.prioridad)
        assertEquals("Bajo", s.potencialComercial)
        assertEquals("Baja", s.afinidad)
    }

    @Test
    fun `guardar con errores conserva los valores escritos`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())
        val vm = vmListo()
        vm.onNombreChange("ab")                  // demasiado corto

        vm.guardar()

        val s = vm.uiState.value
        assertEquals("ab", s.nombre)
        assertNotNull(s.errores[CampoOportunidad.NOMBRE])
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
    }

    @Test
    fun `limpiarMensajeError deja el mensaje en null`() = runTest {
        val vm = vmListo()
        llenarFormularioValido(vm)
        vm.guardar()                             // sin sesión: queda un mensaje de error
        assertNotNull(vm.uiState.value.mensajeError)

        vm.limpiarMensajeError()

        assertNull(vm.uiState.value.mensajeError)
    }

    // =========================================================================
    // Creación: guardar
    // =========================================================================

    @Test
    fun `guardar con errores no llama al repositorio`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())
        val vm = vmListo()

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errores.isNotEmpty())
        coVerify(exactly = 0) { oportunidadRepo.guardarOportunidad(any()) }
        coVerify(exactly = 0) { oportunidadRepo.modificarOportunidad(any()) }
    }

    @Test
    fun `guardar sin sesion muestra Tu sesion expiro y no llama al repositorio`() = runTest {
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("Tu sesión expiró. Vuelve a iniciar sesión.", s.mensajeError)
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
        coVerify(exactly = 0) { oportunidadRepo.guardarOportunidad(any()) }
    }

    @Test
    fun `guardar en creacion arma la oportunidad con datos recortados y estado Identificada`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.guardarOportunidad(capture(enviada)) } returns 10L
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.guardadoExitoso)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        assertEquals(
            Oportunidad(
                idOportunidad = 0L,
                nombre = "Feria de Ñuñoa",
                tipoEvento = "Feria municipal",
                fechaDeteccion = FechasPrueba.dia("2026-03-01"),               // 00:00 de Santiago
                fechaInicio = FechasPrueba.hora("2026-03-10T10:30:00"),
                fechaTermino = FechasPrueba.hora("2026-03-12T17:45:00"),
                comuna = "Ñuñoa",
                publicoEsperado = 1500,
                prioridad = "Alta",
                estado = "Identificada",
                costoReferencial = 850000.0,
                observaciones = "Llevar material",
                idOrganizacion = 1L,
                idResponsable = 7L,
                descripcion = "Feria de emprendedores",
                fuenteInformacion = "Sitio web municipal",
                potencialComercial = "Alto",
                afinidad = "Alta",
                publicoObjetivo = "Familias",
                condicionesParticipacion = "Stand propio",
                requisitos = "Permiso municipal",
                espaciosDisponibles = 20
            ),
            enviada.captured
        )
        coVerify(exactly = 0) { oportunidadRepo.modificarOportunidad(any()) }
    }

    @Test
    fun `el ejecutivo que crea queda como responsable`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.guardarOportunidad(capture(enviada)) } returns 10L
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        assertEquals(7L, enviada.captured.idResponsable)
    }

    @Test
    fun `el supervisor que crea deja la oportunidad sin responsable`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.guardarOportunidad(capture(enviada)) } returns 10L
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        assertNull(enviada.captured.idResponsable)
    }

    @Test
    fun `un usuario de marketing que guarda tampoco queda como responsable`() = runTest {
        SesionManager.iniciarSesion(marketing())
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.guardarOportunidad(capture(enviada)) } returns 10L
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        assertNull(enviada.captured.idResponsable)
    }

    @Test
    fun `una hora invalida usa la de respaldo 09 00 y 18 00 cuando las fechas son distintas`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.guardarOportunidad(capture(enviada)) } returns 10L
        val vm = vmListo()
        llenarFormularioValido(vm)
        vm.onHoraInicioChange("abc")
        vm.onHoraTerminoChange("")

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        assertEquals(FechasPrueba.hora("2026-03-10T09:00:00"), enviada.captured.fechaInicio)
        assertEquals(FechasPrueba.hora("2026-03-12T18:00:00"), enviada.captured.fechaTermino)
    }

    @Test
    fun `un segundo toque en Guardar mientras guarda o despues de guardar se ignora`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())
        val compuerta = CompletableDeferred<Unit>()
        coEvery { oportunidadRepo.guardarOportunidad(any()) } coAnswers { compuerta.await(); 10L }
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardando)

        vm.guardar()                             // segundo toque mientras guarda
        advanceUntilIdle()
        compuerta.complete(Unit)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.guardando)
        assertTrue(vm.uiState.value.guardadoExitoso)

        vm.guardar()                             // tercer toque, ya guardado
        advanceUntilIdle()

        coVerify(exactly = 1) { oportunidadRepo.guardarOportunidad(any()) }
    }

    @Test
    fun `si el repositorio lanza una excepcion se muestra No se pudo guardar`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())
        coEvery { oportunidadRepo.guardarOportunidad(any()) } throws RuntimeException("sin conexión")
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("No se pudo guardar: sin conexión", s.mensajeError)
        assertFalse(s.guardando)
        assertFalse(s.guardadoExitoso)
    }

    @Test
    fun `despues de un error se puede reintentar y el mensaje anterior se limpia`() = runTest {
        SesionManager.iniciarSesion(ejecutivo())
        coEvery { oportunidadRepo.guardarOportunidad(any()) } throws RuntimeException("falla") andThen 10L
        val vm = vmListo()
        llenarFormularioValido(vm)

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
        SesionManager.iniciarSesion(ejecutivo())
        coEvery { oportunidadRepo.guardarOportunidad(any()) } returns 0L
        val vm = vmListo()
        llenarFormularioValido(vm)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.guardadoExitoso)
        assertNotNull(s.mensajeError)
    }

    // =========================================================================
    // Edición: carga y acceso
    // =========================================================================

    @Test
    fun `en edicion parte cargando y despues carga todos los datos y las horas`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        val vm = crearViewModel(5L)

        assertTrue(vm.uiState.value.esEdicion)
        assertTrue(vm.uiState.value.cargando)

        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.cargando)
        assertFalse(s.sinAcceso)
        assertEquals("Feria de Ñuñoa", s.nombre)
        assertEquals("Feria municipal", s.tipoEvento)
        assertEquals(1L, s.idOrganizacion)
        assertEquals("05/03/2026", s.fechaDeteccion)
        assertEquals("10/03/2026", s.fechaInicio)
        assertEquals("12/03/2026", s.fechaTermino)
        assertEquals("14:30", s.horaInicio)
        assertEquals("16:00", s.horaTermino)
        assertEquals("Ñuñoa", s.comuna)
        assertEquals("1500", s.publicoEsperado)
        assertEquals("Alta", s.prioridad)
        assertEquals("850000", s.costoReferencial)       // sin ".0"
        assertEquals("Llevar material", s.observaciones)
        assertEquals("Feria de emprendedores", s.descripcion)
        assertEquals("Sitio web municipal", s.fuenteInformacion)
        assertEquals("Alto", s.potencialComercial)
        assertEquals("Alta", s.afinidad)
        assertEquals("Familias", s.publicoObjetivo)
        assertEquals("Stand propio", s.condicionesParticipacion)
        assertEquals("Permiso municipal", s.requisitos)
        assertEquals("20", s.espaciosDisponibles)
        assertEquals(organizaciones, s.organizaciones)
    }

    @Test
    fun `en edicion una oportunidad inexistente deja sinAcceso`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns null

        val s = vmListo(5L).uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.cargando)
        assertEquals("", s.nombre)
    }

    @Test
    fun `en edicion sin sesion deja sinAcceso`() = runTest {
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()

        val s = vmListo(5L).uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.cargando)
        assertEquals("", s.nombre)
    }

    @Test
    fun `en edicion un ejecutivo ajeno no tiene acceso`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 8L))
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(idResponsable = 7L)

        val s = vmListo(5L).uiState.value

        assertTrue(s.sinAcceso)
        assertFalse(s.puedeEliminar)
        assertEquals("", s.nombre)
    }

    @Test
    fun `en edicion un usuario de marketing no tiene acceso`() = runTest {
        SesionManager.iniciarSesion(marketing())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(estado = "Confirmada")

        val s = vmListo(5L).uiState.value

        assertTrue(s.sinAcceso)
    }

    @Test
    fun `en edicion el ejecutivo responsable tiene acceso`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(idResponsable = 7L)

        val s = vmListo(5L).uiState.value

        assertFalse(s.sinAcceso)
        assertEquals("Feria de Ñuñoa", s.nombre)
    }

    // =========================================================================
    // Edición: puedeEliminar
    // =========================================================================

    @Test
    fun `puedeEliminar es true para el supervisor solo cuando la oportunidad esta Identificada`() = runTest {
        SesionManager.iniciarSesion(supervisor())

        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(estado = "Identificada")
        assertTrue(vmListo(5L).uiState.value.puedeEliminar)

        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(estado = "Contacto realizado")
        assertFalse(vmListo(5L).uiState.value.puedeEliminar)

        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(estado = "Confirmada")
        assertFalse(vmListo(5L).uiState.value.puedeEliminar)
    }

    @Test
    fun `puedeEliminar es true para el ejecutivo responsable de una oportunidad Identificada`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 7L))
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(estado = "Identificada", idResponsable = 7L)

        assertTrue(vmListo(5L).uiState.value.puedeEliminar)
    }

    // =========================================================================
    // Edición: guardar
    // =========================================================================

    @Test
    fun `en edicion guardar usa modificarOportunidad y conserva estado responsable y horas`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        val original = oportunidad(estado = "Propuesta enviada", idResponsable = 3L)
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns original
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.modificarOportunidad(capture(enviada)) } just Runs
        val vm = vmListo(5L)
        vm.onNombreChange("  Feria renombrada  ")

        vm.guardar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.guardadoExitoso)
        // Igual a la original salvo el nombre: id, estado, responsable y la hora de detección (15:20) se conservan.
        assertEquals(original.copy(nombre = "Feria renombrada"), enviada.captured)
        coVerify(exactly = 0) { oportunidadRepo.guardarOportunidad(any()) }
    }

    @Test
    fun `en edicion si el supervisor edita una oportunidad sin asignar sigue sin responsable`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(idResponsable = null)
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.modificarOportunidad(capture(enviada)) } just Runs
        val vm = vmListo(5L)

        vm.guardar()
        advanceUntilIdle()

        assertNull(enviada.captured.idResponsable)
    }

    @Test
    fun `en edicion cambiar la fecha de deteccion la lleva a medianoche del nuevo dia`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        val enviada = slot<Oportunidad>()
        coEvery { oportunidadRepo.modificarOportunidad(capture(enviada)) } just Runs
        val vm = vmListo(5L)
        vm.onDeteccionChange("06/03/2026")

        vm.guardar()
        advanceUntilIdle()

        assertEquals(FechasPrueba.dia("2026-03-06"), enviada.captured.fechaDeteccion)
    }

    @Test
    fun `en edicion si el repositorio lanza una excepcion se muestra No se pudo guardar`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        coEvery { oportunidadRepo.modificarOportunidad(any()) } throws RuntimeException("timeout")
        val vm = vmListo(5L)

        vm.guardar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("No se pudo guardar: timeout", s.mensajeError)
        assertFalse(s.guardadoExitoso)
        assertFalse(s.guardando)
    }

    // =========================================================================
    // Eliminar
    // =========================================================================

    @Test
    fun `eliminar con exito marca eliminado y llama al repositorio con el id de la ruta`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        coEvery { oportunidadRepo.eliminar(5L) } returns null
        val vm = vmListo(5L)

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.eliminado)
        assertFalse(s.guardando)
        assertNull(s.mensajeError)
        coVerify(exactly = 1) { oportunidadRepo.eliminar(5L) }
    }

    @Test
    fun `eliminar con un motivo de rechazo lo muestra como mensaje y no marca eliminado`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        coEvery { oportunidadRepo.eliminar(5L) } returns "No se puede eliminar: tiene seguimientos registrados"
        val vm = vmListo(5L)

        vm.eliminar()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.eliminado)
        assertFalse(s.guardando)
        assertEquals("No se puede eliminar: tiene seguimientos registrados", s.mensajeError)
    }

    @Test
    fun `eliminar con una excepcion muestra No se pudo eliminar`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        coEvery { oportunidadRepo.eliminar(5L) } throws RuntimeException("sin red")
        val vm = vmListo(5L)

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
        coVerify(exactly = 0) { oportunidadRepo.eliminar(any()) }
    }

    @Test
    fun `eliminar se ignora si la oportunidad no esta Identificada`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(estado = "Contacto realizado")
        val vm = vmListo(5L)
        assertFalse(vm.uiState.value.puedeEliminar)

        vm.eliminar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { oportunidadRepo.eliminar(any()) }
    }

    @Test
    fun `eliminar se ignora si el usuario no tiene acceso`() = runTest {
        SesionManager.iniciarSesion(ejecutivo(id = 8L))
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad(idResponsable = 7L)
        val vm = vmListo(5L)

        vm.eliminar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { oportunidadRepo.eliminar(any()) }
    }

    @Test
    fun `un segundo eliminar despues de eliminado se ignora`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        coEvery { oportunidadRepo.eliminar(5L) } returns null
        val vm = vmListo(5L)

        vm.eliminar()
        advanceUntilIdle()
        vm.eliminar()
        advanceUntilIdle()

        coVerify(exactly = 1) { oportunidadRepo.eliminar(5L) }
    }

    @Test
    fun `eliminar y guardar no se pisan mientras hay una operacion en curso`() = runTest {
        SesionManager.iniciarSesion(supervisor())
        coEvery { oportunidadRepo.obtenerPorId(5L) } returns oportunidad()
        val compuerta = CompletableDeferred<Unit>()
        coEvery { oportunidadRepo.eliminar(5L) } coAnswers { compuerta.await(); null }
        val vm = vmListo(5L)

        vm.eliminar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.guardando)

        vm.guardar()                             // ignorado: ya hay una operación en curso
        vm.eliminar()                            // ignorado también
        compuerta.complete(Unit)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.eliminado)
        coVerify(exactly = 0) { oportunidadRepo.modificarOportunidad(any()) }
        coVerify(exactly = 1) { oportunidadRepo.eliminar(5L) }
    }
}