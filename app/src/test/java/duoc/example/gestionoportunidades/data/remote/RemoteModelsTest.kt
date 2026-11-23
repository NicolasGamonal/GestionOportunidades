package duoc.example.gestionoportunidades.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteModelsTest {

    private val org = OrganizacionRemote(
        nombre = "Muni", tipo = "Municipalidad", rubro = null, comuna = null,
        region = null, sitioWeb = null, publicoAsociado = null, observaciones = null
    )

    @Test
    fun `AsignarRequestRemote no trae prioridad por defecto`() {
        val pedido = AsignarRequestRemote(nuevoResponsableId = 4L)

        assertEquals(4L, pedido.nuevoResponsableId)
        assertNull(pedido.nuevaPrioridad)
    }

    @Test
    fun `UsuarioRemote queda activo y sin password por defecto`() {
        val usuario = UsuarioRemote(id = 1L, nombre = "Ana", correo = "a@a.cl", rol = "EJECUTIVO", area = null, sucursal = null)

        assertTrue(usuario.activo)
        assertNull(usuario.password)
    }

    @Test
    fun `los modelos nuevos no traen id por defecto`() {
        assertNull(org.id)
        assertNull(SeguimientoRemote(tipoAccion = "Llamada", resultado = "ok", proximaAccion = null,
            fechaProximoSeguimiento = null, observaciones = null, oportunidad = OportunidadRef(1L),
            usuario = UsuarioRef(1L)).id)
        assertNull(EvidenciaRemote(descripcion = null, imagenBase64 = null, oportunidad = OportunidadRef(1L)).id)
        assertNull(NotaMarketingRemote(contenido = "x", oportunidad = OportunidadRef(1L)).id)
    }

    @Test
    fun `SeguimientoRemote deja la fecha y hora para que la ponga el servidor`() {
        val seguimiento = SeguimientoRemote(
            tipoAccion = "Llamada", resultado = "ok", proximaAccion = null,
            fechaProximoSeguimiento = null, observaciones = null,
            oportunidad = OportunidadRef(1L), usuario = UsuarioRef(1L)
        )

        assertNull(seguimiento.fechaHora)
    }

    @Test
    fun `OportunidadRemote trae afinidad media y espacios en cero por defecto`() {
        val op = OportunidadRemote(
            nombre = "Feria", tipo = "Feria", descripcion = null, fuenteInformacion = null,
            fechaDeteccion = null, fechaEvento = null, horarioEvento = null, direccion = null,
            comuna = null, region = null, publicoObjetivo = null, publicoEsperado = null,
            costoReferencial = null, prioridad = "Alta", potencialComercial = null, estado = "Identificada",
            condicionesParticipacion = null, observaciones = null, organizacion = org, responsable = null
        )

        assertNull(op.id)
        assertNull(op.fechaTermino)
        assertEquals("Media", op.afinidad)
        assertEquals("", op.requisitos)
        assertEquals(0, op.espaciosDisponibles)
    }

    @Test
    fun `ResultadoActivacionRemote trae los valores por defecto de una activacion realizada`() {
        val resultado = ResultadoActivacionRemote(oportunidad = OportunidadRef(1L))

        assertNull(resultado.id)
        assertEquals("Realizada", resultado.estadoFinal)
        assertEquals(0, resultado.asistentesReales)
        assertEquals(0, resultado.contactosGenerados)
        assertEquals(0, resultado.personasInteresadas)
        assertEquals(0, resultado.inscripcionesLogradas)
        assertEquals("5", resultado.evaluacionGeneral)
        assertEquals("", resultado.observaciones)
        assertNull(resultado.usuarioId)
    }

    @Test
    fun `los modelos remotos son data classes con igualdad copy y toString`() {
        val a = LoginRequestRemote(correo = "a@a.cl", password = "x")
        val b = LoginRequestRemote(correo = "a@a.cl", password = "x")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(password = "y"))
        assertTrue(a.toString().contains("a@a.cl"))
        assertEquals(OportunidadRef(3L), OportunidadRef(3L))
        assertEquals(UsuarioRef(3L), UsuarioRef(3L))
        assertEquals(OrganizacionRef(3L), OrganizacionRef(3L))
        assertEquals(org, org.copy())
        assertNotEquals(org, org.copy(nombre = "Otra"))
    }

    @Test
    fun `DashboardResumenRemote guarda los totales y las listas`() {
        val resumen = DashboardResumenRemote(
            totalOportunidades = 12L, pendientes = 5L, confirmadas = 3L,
            eventosProximos = emptyList(), seguimientosAtrasados = emptyList()
        )

        assertEquals(12L, resumen.totalOportunidades)
        assertEquals(5L, resumen.pendientes)
        assertEquals(3L, resumen.confirmadas)
        assertTrue(resumen.eventosProximos.isEmpty())
        assertTrue(resumen.seguimientosAtrasados.isEmpty())
        assertEquals(resumen, resumen.copy())
    }
}
