package duoc.example.gestionoportunidades.testutil

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale
import java.util.TimeZone

/**
 * Formato.kt usa ZoneId.systemDefault() y el locale es-CL: se fijan para que las
 * pruebas den lo mismo en cualquier computador.
 * Uso: @Rule @JvmField val zonaHoraria = ZonaHorariaRule()
 */
class ZonaHorariaRule : TestWatcher() {
    private var zonaOriginal: TimeZone? = null
    private var localeOriginal: Locale? = null

    override fun starting(description: Description) {
        zonaOriginal = TimeZone.getDefault()
        localeOriginal = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"))
        Locale.setDefault(Locale.forLanguageTag("es-CL"))
    }

    override fun finished(description: Description) {
        zonaOriginal?.let { TimeZone.setDefault(it) }
        localeOriginal?.let { Locale.setDefault(it) }
    }
}
