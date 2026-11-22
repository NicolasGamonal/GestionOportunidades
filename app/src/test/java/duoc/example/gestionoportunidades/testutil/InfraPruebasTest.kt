package duoc.example.gestionoportunidades.testutil

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.EvidenciaRepository
import duoc.example.gestionoportunidades.data.repository.NotaMarketingRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.data.repository.ResultadoActivacionRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Prueba de humo de la Fase 0: comprueba que las dependencias (MockK, coroutines-test),
 * las reglas compartidas y la inyección de ApiService funcionan antes de escribir
 * las pruebas reales de las fases 1 a 4.
 */
class InfraPruebasTest {

    @Rule @JvmField val mainDispatcherRule = MainDispatcherRule()
    @Rule @JvmField val sesionRule = SesionRule()
    @Rule @JvmField val zonaHorariaRule = ZonaHorariaRule()

    @Test
    fun `los 8 repositorios aceptan un ApiService simulado por constructor`() {
        val api = mockk<ApiService>(relaxed = true)

        assertNotNull(AuthRepository(api))
        assertNotNull(OportunidadRepository(api))
        assertNotNull(OrganizacionRepository(api))
        assertNotNull(ContactoRepository(api))
        assertNotNull(SeguimientoRepository(api))
        assertNotNull(ResultadoActivacionRepository(api))
        assertNotNull(NotaMarketingRepository(api))
        assertNotNull(EvidenciaRepository(File("."), api))
    }

    @Test
    fun `MainDispatcherRule reemplaza Dispatchers Main`() = runTest {
        val resultado = withContext(Dispatchers.Main) { 2 + 2 }
        assertEquals(4, resultado)
    }

    @Test
    fun `SesionRule deja la sesion limpia`() {
        assertFalse(SesionManager.estaLogueado())
    }

    @Test
    fun `ZonaHorariaRule fija America Santiago`() {
        assertEquals("America/Santiago", ZoneId.systemDefault().id)
    }
}
