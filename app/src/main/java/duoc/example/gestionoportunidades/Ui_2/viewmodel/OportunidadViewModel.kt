package duoc.example.gestionoportunidades.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import duoc.example.gestionoportunidades.data.Oportunidad

/**
 * VIEWMODEL: OportunidadViewModel
 * Gestiona y mantiene la lista de oportunidades en memoria durante el ciclo de vida de la app.
 */
class OportunidadViewModel : ViewModel() {

    // Lista reactiva observable por Jetpack Compose
    private val _listaOportunidades = mutableStateListOf<Oportunidad>()

    // Exposición de la lista como solo lectura para la interfaz gráfica
    val listaOportunidades: List<Oportunidad> get() = _listaOportunidades

    // Función para añadir una nueva oportunidad
    fun agregarOportunidad(nuevaOportunidad: Oportunidad) {
        _listaOportunidades.add(nuevaOportunidad)

        println("=== OPORTUNIDAD GUARDADA EN VIEWMODEL ===")
        println("Evento: ${nuevaOportunidad.nombreEvento}")
        println("Total acumulado: ${_listaOportunidades.size}")
    }
}