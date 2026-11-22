package duoc.example.gestionoportunidades.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionesTest {

    private val metropolitana = "Región Metropolitana de Santiago"

    @Test
    fun `hay 16 regiones con ids de 0 a 15`() {
        assertEquals(16, Regiones.todas.size)
        assertEquals((0..15).toList(), Regiones.todas.map { it.id })
    }

    @Test
    fun `nombres coincide con las regiones en orden de norte a sur`() {
        assertEquals(16, Regiones.nombres.size)
        assertEquals(Regiones.todas.map { it.nombre }, Regiones.nombres)
        assertEquals("Arica y Parinacota", Regiones.nombres.first())
        assertEquals(metropolitana, Regiones.nombres.last())
    }

    @Test
    fun `los nombres de region no se repiten`() {
        assertEquals(Regiones.nombres.size, Regiones.nombres.toSet().size)
    }

    @Test
    fun `hay 346 comunas y ninguna esta duplicada`() {
        // Si hubiera duplicados, el mapa comuna -> region (toMap) sobrescribiría en silencio.
        val comunas = Regiones.todas.flatMap { it.comunas }
        assertEquals(346, comunas.size)
        assertEquals(comunas.size, comunas.toSet().size)
    }

    @Test
    fun `todas las regiones tienen comunas y ninguna comuna esta en blanco`() {
        Regiones.todas.forEach { region ->
            assertTrue("Región sin comunas: ${region.nombre}", region.comunas.isNotEmpty())
            region.comunas.forEach { assertTrue("Comuna en blanco en ${region.nombre}", it.isNotBlank()) }
        }
    }

    @Test
    fun `regionDe devuelve la region de una comuna conocida`() {
        assertEquals(metropolitana, Regiones.regionDe("Puente Alto"))
        assertEquals(metropolitana, Regiones.regionDe("Santiago"))
        assertEquals("Arica y Parinacota", Regiones.regionDe("Arica"))
    }

    @Test
    fun `regionDe ignora espacios alrededor`() {
        assertEquals(metropolitana, Regiones.regionDe("  Las Condes  "))
    }

    @Test
    fun `regionDe devuelve null con comuna desconocida o vacia`() {
        assertNull(Regiones.regionDe("Narnia"))
        assertNull(Regiones.regionDe(""))
        assertNull(Regiones.regionDe("   "))
    }

    @Test
    fun `regionDe distingue mayusculas`() {
        assertNull(Regiones.regionDe("santiago"))
    }

    @Test
    fun `comunasDe devuelve las comunas de una region`() {
        val comunas = Regiones.comunasDe(metropolitana)
        assertTrue("Puente Alto" in comunas)
        assertTrue("Santiago" in comunas)
        assertFalse("Arica" in comunas)
    }

    @Test
    fun `comunasDe con region inexistente devuelve lista vacia`() {
        assertTrue(Regiones.comunasDe("Región Inventada").isEmpty())
        assertTrue(Regiones.comunasDe("").isEmpty())
    }

    @Test
    fun `esComunaValida acepta comunas del catalogo y rechaza el resto`() {
        assertTrue(Regiones.esComunaValida("Puente Alto"))
        assertFalse(Regiones.esComunaValida("Narnia"))
        assertFalse(Regiones.esComunaValida(""))
    }

    @Test
    fun `cada comuna se resuelve a la region donde esta declarada`() {
        Regiones.todas.forEach { region ->
            region.comunas.forEach { comuna ->
                assertEquals("Comuna $comuna", region.nombre, Regiones.regionDe(comuna))
            }
        }
    }
}
