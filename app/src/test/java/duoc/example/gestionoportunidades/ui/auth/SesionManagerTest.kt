package duoc.example.gestionoportunidades.ui.auth

import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.testutil.SesionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SesionManagerTest {

    // SesionManager es un singleton global: la regla lo limpia antes y después de cada prueba.
    @Rule @JvmField val sesionRule = SesionRule()

    private fun usuario(rol: String, id: Long = 1L) = Usuario(
        idUsuario = id, username = "u$id", contrasena = "1234", nombreCompleto = "Usuario $id", rol = rol
    )

    @Test
    fun `sin iniciar sesion no hay usuario ni rol`() {
        assertFalse(SesionManager.estaLogueado())
        assertNull(SesionManager.usuarioActual)
        assertEquals("", SesionManager.getRol())
    }

    @Test
    fun `iniciarSesion deja al usuario logueado con su rol`() {
        val ana = usuario("EJECUTIVO")
        SesionManager.iniciarSesion(ana)
        assertTrue(SesionManager.estaLogueado())
        assertSame(ana, SesionManager.usuarioActual)
        assertEquals("EJECUTIVO", SesionManager.getRol())
    }

    @Test
    fun `cerrarSesion limpia usuario y rol`() {
        SesionManager.iniciarSesion(usuario("SUPERVISOR"))
        SesionManager.cerrarSesion()
        assertFalse(SesionManager.estaLogueado())
        assertNull(SesionManager.usuarioActual)
        assertEquals("", SesionManager.getRol())
    }

    @Test
    fun `iniciar sesion de nuevo reemplaza al usuario anterior`() {
        SesionManager.iniciarSesion(usuario("EJECUTIVO", 1L))
        SesionManager.iniciarSesion(usuario("MARKETING", 2L))
        assertEquals(2L, SesionManager.usuarioActual?.idUsuario)
        assertEquals("MARKETING", SesionManager.getRol())
    }

    @Test
    fun `cerrar sesion sin haber iniciado no falla`() {
        SesionManager.cerrarSesion()
        assertFalse(SesionManager.estaLogueado())
    }
}
