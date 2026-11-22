package duoc.example.gestionoportunidades.model

import duoc.example.gestionoportunidades.ui.alertas.DashboardUiState
import duoc.example.gestionoportunidades.ui.alertas.IndicadoresEquipo
import duoc.example.gestionoportunidades.ui.auth.AuthUiState
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosTest {

    // Comprueba lo que una data class debe cumplir: copy igual, equals/hashCode coherentes y toString con el nombre.
    private fun <T : Any> verificarDataClass(original: T, copia: T, distinta: T) {
        assertEquals(original, copia)
        assertEquals(original.hashCode(), copia.hashCode())
        assertFalse(original == distinta)
        assertTrue(original.toString().startsWith(original::class.simpleName!! + "("))
    }

    private fun oportunidad() = Oportunidad(
        nombre = "Feria", tipoEvento = "Feria empresarial", fechaDeteccion = 1L, fechaInicio = 2L,
        fechaTermino = 3L, comuna = "Santiago", publicoEsperado = 100, prioridad = "Alta",
        estado = "Identificada", costoReferencial = 0.0, observaciones = "", idOrganizacion = 7L
    )

    // --- Valores por defecto ---

    @Test
    fun `Oportunidad tiene los valores por defecto esperados`() {
        val o = oportunidad()
        assertEquals(0L, o.idOportunidad)
        assertNull(o.idResponsable)
        assertEquals("", o.descripcion)
        assertEquals("", o.fuenteInformacion)
        assertEquals("Medio", o.potencialComercial)
        assertEquals("Media", o.afinidad)
        assertEquals("", o.publicoObjetivo)
        assertEquals("", o.condicionesParticipacion)
        assertEquals("", o.requisitos)
        assertEquals(0, o.espaciosDisponibles)
    }

    @Test
    fun `Contacto usa Correo como medio preferente por defecto`() {
        val c = Contacto(nombre = "Ana", cargo = "Gerente", area = "Marketing", correo = "a@b.cl", telefono = "12345678", idOrganizacion = 1L)
        assertEquals("Correo", c.medioPreferente)
        assertEquals(0L, c.idContacto)
    }

    @Test
    fun `Usuario tiene area y zona vacias por defecto`() {
        val u = Usuario(username = "ejecutivo1", contrasena = "1234", nombreCompleto = "Ana Pérez", rol = "EJECUTIVO")
        assertEquals("", u.area)
        assertEquals("", u.zona)
        assertEquals(0L, u.idUsuario)
    }

    @Test
    fun `Seguimiento no tiene contacto por defecto`() {
        val s = Seguimiento(idOportunidad = 1L, idUsuario = 2L, tipoAccion = "Llamada", fechaHora = 1L, resultado = "ok", proximaAccion = "x", fechaProximo = 2L)
        assertNull(s.idContacto)
        assertEquals(0L, s.idSeguimiento)
    }

    @Test
    fun `los ids autogenerados parten en cero`() {
        assertEquals(0L, Organizacion(nombre = "a", tipo = "b", rubro = "c", comuna = "d").idOrganizacion)
        assertEquals(0L, Evidencia(rutaArchivo = "r", fecha = 1L, idOportunidad = 1L).idEvidencia)
        assertEquals(0L, NotaMarketing(idOportunidad = 1L, tipo = "Material", texto = "t", fechaHora = 1L, idUsuario = 1L).idNota)
        assertEquals(
            0L,
            ResultadoActivacion(idOportunidad = 1L, asistentes = 1, contactosGenerados = 1, inscripciones = 1, evaluacion = 3, aprendizajes = "a", fechaRegistro = 1L, idUsuario = 1L).idResultado
        )
    }

    // --- copy / equals / hashCode / toString ---

    @Test
    fun `Oportunidad se comporta como data class`() {
        val o = oportunidad()
        verificarDataClass(o, o.copy(), o.copy(estado = "Confirmada"))
        assertEquals("Confirmada", o.copy(estado = "Confirmada").estado)
        assertEquals("Identificada", o.estado) // el original no cambia
        assertEquals(7L, o.component13())
    }

    @Test
    fun `Contacto Usuario y Organizacion se comportan como data class`() {
        val c = Contacto(nombre = "Ana", cargo = "G", area = "M", correo = "a@b.cl", telefono = "12345678", idOrganizacion = 1L)
        verificarDataClass(c, c.copy(), c.copy(nombre = "Luis"))
        val u = Usuario(username = "u", contrasena = "p", nombreCompleto = "N", rol = "EJECUTIVO")
        verificarDataClass(u, u.copy(), u.copy(rol = "SUPERVISOR"))
        val org = Organizacion(nombre = "Org", tipo = "Empresa", rubro = "Retail", comuna = "Santiago")
        verificarDataClass(org, org.copy(), org.copy(nombre = "Otra"))
    }

    @Test
    fun `Seguimiento Evidencia NotaMarketing y ResultadoActivacion se comportan como data class`() {
        val s = Seguimiento(idOportunidad = 1L, idUsuario = 2L, tipoAccion = "Llamada", fechaHora = 1L, resultado = "ok", proximaAccion = "x", fechaProximo = 2L)
        verificarDataClass(s, s.copy(), s.copy(idContacto = 9L))
        val e = Evidencia(rutaArchivo = "evidencias/a.jpg", fecha = 1L, idOportunidad = 1L)
        verificarDataClass(e, e.copy(), e.copy(rutaArchivo = "evidencias/b.jpg"))
        val n = NotaMarketing(idOportunidad = 1L, tipo = "Material", texto = "t", fechaHora = 1L, idUsuario = 1L)
        verificarDataClass(n, n.copy(), n.copy(texto = "otro"))
        val r = ResultadoActivacion(idOportunidad = 1L, asistentes = 1, contactosGenerados = 1, inscripciones = 1, evaluacion = 3, aprendizajes = "a", fechaRegistro = 1L, idUsuario = 1L)
        verificarDataClass(r, r.copy(), r.copy(evaluacion = 5))
    }

    @Test
    fun `modelos de consulta se comportan como data class`() {
        val a = AlertaSeguimiento(1L, "Feria", null, "Llamada", "Enviar propuesta", 5L)
        verificarDataClass(a, a.copy(), a.copy(nombreResponsable = "Ana"))
        assertNull(a.nombreResponsable)

        val c = ConteoEstado("Identificada", 3)
        verificarDataClass(c, c.copy(), c.copy(cantidad = 4))

        val g = GestionOrganizacion(1L, 2L, "Llamada", "ok", 3L, "Feria", "Ana", null)
        verificarDataClass(g, g.copy(), g.copy(nombreContacto = "Luis"))
        assertNull(g.nombreContacto)

        val n = NotaMarketingConAutor(1L, "Material", "texto", 5L, "Equipo Marketing")
        verificarDataClass(n, n.copy(), n.copy(autor = "Ana"))
    }

    // --- Estados de UI por defecto ---

    @Test
    fun `IndicadoresEquipo por defecto esta vacio y sin conversion`() {
        val i = IndicadoresEquipo()
        assertEquals(0, i.total)
        assertTrue(i.porEstado.isEmpty())
        assertNull(i.conversionPorcentaje)
        verificarDataClass(i, i.copy(), i.copy(total = 5))
    }

    @Test
    fun `DashboardUiState por defecto esta cargando y sin datos`() {
        val d = DashboardUiState()
        assertTrue(d.isLoading)
        assertNull(d.error)
        assertTrue(d.eventosProximos.isEmpty())
        assertTrue(d.seguimientosPendientes.isEmpty())
        assertTrue(d.seguimientosAtrasados.isEmpty())
        assertTrue(d.sinSeguimiento.isEmpty())
        assertTrue(d.sinContacto.isEmpty())
        assertEquals(IndicadoresEquipo(), d.indicadores)
        verificarDataClass(d, d.copy(), d.copy(isLoading = false))
    }

    @Test
    fun `SupervisorUiState por defecto esta cargando sin mensajes`() {
        val s = SupervisorUiState()
        assertTrue(s.isLoading)
        assertTrue(s.listaOportunidadesGlobal.isEmpty())
        assertTrue(s.ejecutivos.isEmpty())
        assertNull(s.mensajeExito)
        assertNull(s.mensajeError)
        verificarDataClass(s, s.copy(), s.copy(mensajeError = "x"))
    }

    @Test
    fun `AuthUiState por defecto no esta cargando ni logueado`() {
        val a = AuthUiState()
        assertFalse(a.isLoading)
        assertNull(a.errorMensaje)
        assertFalse(a.loginExitoso)
        assertNull(a.usuarioLogueado)
        verificarDataClass(a, a.copy(), a.copy(loginExitoso = true))
    }
}
