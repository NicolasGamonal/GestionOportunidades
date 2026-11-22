package duoc.example.gestionoportunidades.testutil

import duoc.example.gestionoportunidades.ui.auth.SesionManager
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * SesionManager es un singleton global: esta regla lo deja limpio antes y después
 * de cada prueba para que ninguna dependa del estado que dejó otra.
 */
class SesionRule : TestWatcher() {
    override fun starting(description: Description) {
        SesionManager.cerrarSesion()
    }

    override fun finished(description: Description) {
        SesionManager.cerrarSesion()
    }
}
