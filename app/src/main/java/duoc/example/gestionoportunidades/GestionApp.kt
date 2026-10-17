package duoc.example.gestionoportunidades

import android.app.Application
import duoc.example.gestionoportunidades.data.AppContainer

class GestionApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}