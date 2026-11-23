package duoc.example.gestionoportunidades.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class RetrofitClientTest {

    @Test
    fun `apiService se construye sin conectarse y es siempre la misma instancia`() {
        val primera = RetrofitClient.apiService
        val segunda = RetrofitClient.apiService

        assertNotNull(primera)
        assertSame(primera, segunda)
    }

    @Test
    fun `la direccion base apunta al host del emulador de Android`() {
        // BASE_URL es privada: se lee por reflexión para que un cambio accidental falle aquí.
        val campo = RetrofitClient::class.java.getDeclaredField("BASE_URL")
        campo.isAccessible = true

        assertEquals("http://10.0.2.2:8080", campo.get(null))
    }
}
