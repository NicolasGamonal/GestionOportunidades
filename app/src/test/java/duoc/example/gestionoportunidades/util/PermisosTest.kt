package duoc.example.gestionoportunidades.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermisosTest {

    private val roles = listOf("SUPERVISOR", "EJECUTIVO", "MARKETING", "", "supervisor", "ADMIN")

    // --- puedeVerOportunidad ---

    @Test
    fun `supervisor ve cualquier oportunidad incluso sin responsable`() {
        assertTrue(Permisos.puedeVerOportunidad("SUPERVISOR", 1L, 99L))
        assertTrue(Permisos.puedeVerOportunidad("SUPERVISOR", 1L, null))
    }

    @Test
    fun `ejecutivo solo ve las suyas`() {
        assertTrue(Permisos.puedeVerOportunidad("EJECUTIVO", 5L, 5L))
        assertFalse(Permisos.puedeVerOportunidad("EJECUTIVO", 5L, 6L))
        assertFalse(Permisos.puedeVerOportunidad("EJECUTIVO", 5L, null))
    }

    @Test
    fun `marketing solo ve oportunidades confirmadas`() {
        assertTrue(Permisos.puedeVerOportunidad("MARKETING", 3L, 1L, "Confirmada"))
        assertFalse(Permisos.puedeVerOportunidad("MARKETING", 3L, 1L, "Identificada"))
        assertFalse(Permisos.puedeVerOportunidad("MARKETING", 3L, 1L, "Descartada"))
        assertFalse(Permisos.puedeVerOportunidad("MARKETING", 3L, 1L)) // sin estado
    }

    @Test
    fun `rol desconocido o vacio no ve nada`() {
        assertFalse(Permisos.puedeVerOportunidad("", 1L, 1L, "Confirmada"))
        assertFalse(Permisos.puedeVerOportunidad("ADMIN", 1L, 1L, "Confirmada"))
        assertFalse(Permisos.puedeVerOportunidad("supervisor", 1L, 1L)) // distingue mayúsculas
    }

    // --- Permisos simples por rol (tabla rol x funcion) ---

    @Test
    fun `puedeGestionarOportunidades solo para supervisor y ejecutivo`() {
        val esperado = mapOf("SUPERVISOR" to true, "EJECUTIVO" to true, "MARKETING" to false, "" to false, "supervisor" to false, "ADMIN" to false)
        roles.forEach { assertEquals("rol '$it'", esperado[it], Permisos.puedeGestionarOportunidades(it)) }
    }

    @Test
    fun `puedeRegistrarNotas solo para marketing y supervisor`() {
        val esperado = mapOf("SUPERVISOR" to true, "EJECUTIVO" to false, "MARKETING" to true, "" to false, "supervisor" to false, "ADMIN" to false)
        roles.forEach { assertEquals("rol '$it'", esperado[it], Permisos.puedeRegistrarNotas(it)) }
    }

    @Test
    fun `solo el supervisor puede asignar confirmar descartar y administrar organizaciones`() {
        roles.forEach { rol ->
            val esSupervisor = rol == "SUPERVISOR"
            assertEquals("asignar '$rol'", esSupervisor, Permisos.puedeAsignar(rol))
            assertEquals("confirmar '$rol'", esSupervisor, Permisos.puedeConfirmarODescartar(rol))
            assertEquals("editar org '$rol'", esSupervisor, Permisos.puedeEditarOrganizacion(rol))
            assertEquals("eliminar org '$rol'", esSupervisor, Permisos.puedeEliminarOrganizacion(rol))
        }
    }

    // --- puedeCambiarEstado: tabla completa 6 x 6 por rol ---

    private val todos = Estados.todos

    // Transiciones permitidas escritas a mano (no se calculan con la misma regla que el código).
    private val permitidasSupervisor = mapOf(
        "Identificada" to setOf("Contacto realizado", "Confirmada", "Descartada"),
        "Contacto realizado" to setOf("Propuesta enviada", "Confirmada", "Descartada"),
        "Propuesta enviada" to setOf("En espera de respuesta", "Confirmada", "Descartada"),
        "En espera de respuesta" to setOf("Confirmada", "Descartada"),
        "Confirmada" to emptySet(),
        "Descartada" to emptySet()
    )

    private val permitidasEjecutivo = mapOf(
        "Identificada" to setOf("Contacto realizado"),
        "Contacto realizado" to setOf("Propuesta enviada"),
        "Propuesta enviada" to setOf("En espera de respuesta"),
        "En espera de respuesta" to emptySet(),   // el siguiente paso es final: solo el supervisor
        "Confirmada" to emptySet(),
        "Descartada" to emptySet()
    )

    @Test
    fun `supervisor puede cambiar estado en las transiciones esperadas de la tabla 6x6`() {
        for (actual in todos) for (nuevo in todos) {
            val esperado = nuevo in permitidasSupervisor.getValue(actual)
            assertEquals("SUPERVISOR $actual -> $nuevo", esperado, Permisos.puedeCambiarEstado("SUPERVISOR", actual, nuevo))
        }
    }

    @Test
    fun `ejecutivo solo avanza un paso y nunca a estados finales en la tabla 6x6`() {
        for (actual in todos) for (nuevo in todos) {
            val esperado = nuevo in permitidasEjecutivo.getValue(actual)
            assertEquals("EJECUTIVO $actual -> $nuevo", esperado, Permisos.puedeCambiarEstado("EJECUTIVO", actual, nuevo))
        }
    }

    @Test
    fun `marketing rol vacio y rol desconocido nunca cambian estado`() {
        listOf("MARKETING", "", "ADMIN", "supervisor").forEach { rol ->
            for (actual in todos) for (nuevo in todos) {
                assertFalse("$rol $actual -> $nuevo", Permisos.puedeCambiarEstado(rol, actual, nuevo))
            }
        }
    }

    @Test
    fun `los estados finales son inmutables incluso para el supervisor`() {
        assertFalse(Permisos.puedeCambiarEstado("SUPERVISOR", "Confirmada", "Descartada"))
        assertFalse(Permisos.puedeCambiarEstado("SUPERVISOR", "Descartada", "Identificada"))
    }

    @Test
    fun `cambiar al mismo estado no esta permitido`() {
        assertFalse(Permisos.puedeCambiarEstado("SUPERVISOR", "Identificada", "Identificada"))
        assertFalse(Permisos.puedeCambiarEstado("EJECUTIVO", "Contacto realizado", "Contacto realizado"))
    }

    @Test
    fun `estados inexistentes no permiten cambio`() {
        assertFalse(Permisos.puedeCambiarEstado("SUPERVISOR", "Inventado", "Confirmada"))
        assertFalse(Permisos.puedeCambiarEstado("SUPERVISOR", "Identificada", "Inventado"))
        assertFalse(Permisos.puedeCambiarEstado("EJECUTIVO", "", ""))
    }

    @Test
    fun `supervisor no puede retroceder ni saltar dos pasos hacia estados no finales`() {
        assertFalse(Permisos.puedeCambiarEstado("SUPERVISOR", "Propuesta enviada", "Identificada"))
        assertFalse(Permisos.puedeCambiarEstado("SUPERVISOR", "Identificada", "Propuesta enviada"))
    }

    // --- puedeEliminarOportunidad ---

    @Test
    fun `ejecutivo elimina solo las suyas y supervisor cualquiera`() {
        assertTrue(Permisos.puedeEliminarOportunidad("EJECUTIVO", 5L, 5L))
        assertFalse(Permisos.puedeEliminarOportunidad("EJECUTIVO", 5L, 6L))
        assertFalse(Permisos.puedeEliminarOportunidad("EJECUTIVO", 5L, null))
        assertTrue(Permisos.puedeEliminarOportunidad("SUPERVISOR", 1L, 99L))
        assertTrue(Permisos.puedeEliminarOportunidad("SUPERVISOR", 1L, null))
    }

    @Test
    fun `marketing no elimina oportunidades`() {
        assertFalse(Permisos.puedeEliminarOportunidad("MARKETING", 1L, 1L))
    }

    // --- puedeRegistrarResultado ---

    @Test
    fun `resultado solo se registra en oportunidades Confirmadas`() {
        listOf("Identificada", "Contacto realizado", "Propuesta enviada", "En espera de respuesta", "Descartada", "").forEach {
            assertFalse("estado '$it'", Permisos.puedeRegistrarResultado("SUPERVISOR", 1L, 1L, it))
        }
        assertTrue(Permisos.puedeRegistrarResultado("SUPERVISOR", 1L, 1L, "Confirmada"))
    }

    @Test
    fun `resultado lo registran el ejecutivo responsable y el supervisor`() {
        assertTrue(Permisos.puedeRegistrarResultado("EJECUTIVO", 5L, 5L, "Confirmada"))
        assertFalse(Permisos.puedeRegistrarResultado("EJECUTIVO", 5L, 6L, "Confirmada"))
        assertFalse(Permisos.puedeRegistrarResultado("EJECUTIVO", 5L, null, "Confirmada"))
        assertTrue(Permisos.puedeRegistrarResultado("SUPERVISOR", 1L, 99L, "Confirmada"))
    }

    @Test
    fun `marketing no registra resultados`() {
        assertFalse(Permisos.puedeRegistrarResultado("MARKETING", 3L, 1L, "Confirmada"))
    }
}
