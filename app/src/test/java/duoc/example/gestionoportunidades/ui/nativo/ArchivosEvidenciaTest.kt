package duoc.example.gestionoportunidades.ui.nativo

import android.app.Application
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Pruebas de ArchivosEvidencia con Robolectric: usa el Context (filesDir) y el FileProvider
 * declarado en el AndroidManifest, con las carpetas permitidas de res/xml/file_paths.xml
 * (name = "evidencias_images", path = "evidencias/").
 */
@RunWith(RobolectricTestRunner::class)
class ArchivosEvidenciaTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    // FileProvider cachea la carpeta raíz por authority en un campo estático.
    // Robolectric usa un filesDir distinto en cada test, así que hay que vaciarlo.
    @Before
    fun limpiarCacheDelFileProvider() {
        val campo = FileProvider::class.java.getDeclaredField("sCache")
        campo.isAccessible = true
        (campo.get(null) as MutableMap<*, *>).clear()
    }

    // =========================================================================
    // nuevaRutaRelativa
    // =========================================================================

    @Test
    fun `nuevaRutaRelativa tiene la forma evidencias evidencia_marca-de-tiempo jpg`() {
        val ruta = ArchivosEvidencia.nuevaRutaRelativa()

        assertTrue("Ruta inesperada: $ruta", Regex("""evidencias/evidencia_\d+\.jpg""").matches(ruta))
    }

    @Test
    fun `nuevaRutaRelativa usa la hora actual en milisegundos`() {
        val antes = System.currentTimeMillis()
        val ruta = ArchivosEvidencia.nuevaRutaRelativa()
        val despues = System.currentTimeMillis()

        val marca = ruta.substringAfter("evidencia_").substringBefore(".jpg").toLong()
        assertTrue(marca in antes..despues)
    }

    @Test
    fun `nuevaRutaRelativa es relativa y no empieza con barra`() {
        assertFalse(ArchivosEvidencia.nuevaRutaRelativa().startsWith("/"))
    }

    // =========================================================================
    // archivo
    // =========================================================================

    @Test
    fun `archivo resuelve la ruta relativa dentro de filesDir`() {
        val archivo = ArchivosEvidencia.archivo(app, "evidencias/evidencia_123.jpg")

        assertEquals(File(app.filesDir, "evidencias/evidencia_123.jpg"), archivo)
    }

    @Test
    fun `archivo no crea el archivo ni la carpeta`() {
        val archivo = ArchivosEvidencia.archivo(app, "evidencias/evidencia_456.jpg")

        assertFalse(archivo.exists())
    }

    // =========================================================================
    // uriParaCamara
    // =========================================================================

    @Test
    fun `uriParaCamara crea la carpeta del archivo si no existe`() {
        val archivo = ArchivosEvidencia.archivo(app, "evidencias/evidencia_789.jpg")
        archivo.parentFile?.deleteRecursively()
        assertFalse(archivo.parentFile!!.exists())

        ArchivosEvidencia.uriParaCamara(app, archivo)

        assertTrue(archivo.parentFile!!.isDirectory)
    }

    @Test
    fun `uriParaCamara devuelve una URI content del FileProvider de la app`() {
        val archivo = ArchivosEvidencia.archivo(app, "evidencias/evidencia_789.jpg")

        val uri = ArchivosEvidencia.uriParaCamara(app, archivo)

        assertEquals("content", uri.scheme)
        assertEquals("${app.packageName}.fileprovider", uri.authority)
        assertEquals("duoc.example.gestionoportunidades.fileprovider", uri.authority)
        assertTrue(
            "La ruta debe usar la carpeta configurada en file_paths.xml: ${uri.path}",
            uri.path!!.startsWith("/evidencias_images/") && uri.path!!.endsWith("evidencia_789.jpg")
        )
    }

    @Test
    fun `uriParaCamara rechaza un archivo fuera de las carpetas permitidas`() {
        val ajeno = File(app.cacheDir, "foto.jpg")

        assertThrows(IllegalArgumentException::class.java) {
            ArchivosEvidencia.uriParaCamara(app, ajeno)
        }
    }
}