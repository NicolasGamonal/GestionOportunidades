package duoc.example.gestionoportunidades

import android.app.Application
import duoc.example.gestionoportunidades.data.AppContainer
import duoc.example.gestionoportunidades.util.Recordatorios

class GestionApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // El canal de notificaciones debe existir antes de mostrar cualquier recordatorio (HU-21).
        Recordatorios.crearCanal(this)
    }
}