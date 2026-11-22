package duoc.example.gestionoportunidades.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogosTest {

    // --- Estados ---

    @Test
    fun `hay 6 estados en el orden del flujo`() {
        assertEquals(
            listOf("Identificada", "Contacto realizado", "Propuesta enviada", "En espera de respuesta", "Confirmada", "Descartada"),
            Estados.todos
        )
    }

    @Test
    fun `los estados finales son los dos ultimos y pertenecen a todos`() {
        assertEquals(listOf("Confirmada", "Descartada"), Estados.finales)
        assertTrue(Estados.todos.containsAll(Estados.finales))
        assertEquals(Estados.finales, Estados.todos.takeLast(2))
    }

    @Test
    fun `las prioridades son Alta Media y Baja`() {
        assertEquals(listOf("Alta", "Media", "Baja"), Estados.prioridades)
    }

    // --- Catalogos ---

    private val listasDeTexto = mapOf(
        "tiposEvento" to Catalogos.tiposEvento,
        "tiposOrganizacion" to Catalogos.tiposOrganizacion,
        "areasContacto" to Catalogos.areasContacto,
        "nivelesPotencial" to Catalogos.nivelesPotencial,
        "nivelesAfinidad" to Catalogos.nivelesAfinidad,
        "mediosContacto" to Catalogos.mediosContacto,
        "tiposAccion" to Catalogos.tiposAccion,
        "tiposNotaMarketing" to Catalogos.tiposNotaMarketing,
        "estados.todos" to Estados.todos,
        "estados.prioridades" to Estados.prioridades
    )

    @Test
    fun `ninguna lista de catalogo esta vacia`() {
        listasDeTexto.forEach { (nombre, lista) -> assertTrue("$nombre vacía", lista.isNotEmpty()) }
    }

    @Test
    fun `ninguna lista de catalogo tiene elementos repetidos`() {
        listasDeTexto.forEach { (nombre, lista) -> assertEquals("$nombre con repetidos", lista.size, lista.toSet().size) }
    }

    @Test
    fun `ninguna lista de catalogo tiene elementos en blanco`() {
        listasDeTexto.forEach { (nombre, lista) ->
            lista.forEach { assertTrue("$nombre con elemento en blanco", it.isNotBlank()) }
        }
    }

    @Test
    fun `los tamanos de los catalogos principales son los esperados`() {
        assertEquals(8, Catalogos.tiposEvento.size)
        assertEquals(8, Catalogos.tiposOrganizacion.size)
        assertEquals(8, Catalogos.areasContacto.size)
        assertEquals(12, Catalogos.tiposAccion.size)
    }

    @Test
    fun `niveles medios y notas tienen los valores usados como defecto`() {
        assertEquals(listOf("Alto", "Medio", "Bajo"), Catalogos.nivelesPotencial)
        assertEquals(listOf("Alta", "Media", "Baja"), Catalogos.nivelesAfinidad)
        assertTrue("Correo" in Catalogos.mediosContacto)
        assertEquals(listOf("Observación", "Material"), Catalogos.tiposNotaMarketing)
    }

    @Test
    fun `evaluaciones van de 1 a 5`() {
        assertEquals(listOf(1, 2, 3, 4, 5), Catalogos.evaluaciones)
    }

    @Test
    fun `el tipo Cambio de estado es una constante y no esta entre los tipos de accion elegibles`() {
        assertEquals("Cambio de estado", Catalogos.TIPO_CAMBIO_ESTADO)
        assertTrue(Catalogos.TIPO_CAMBIO_ESTADO !in Catalogos.tiposAccion)
    }
}
