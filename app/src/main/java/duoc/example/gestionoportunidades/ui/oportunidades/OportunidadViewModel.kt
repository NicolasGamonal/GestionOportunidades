package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OportunidadViewModel(private val repository: OportunidadRepository) : ViewModel() {

    private val _listaOportunidades = MutableStateFlow<List<Oportunidad>>(emptyList())
    val listaOportunidades: StateFlow<List<Oportunidad>> = _listaOportunidades.asStateFlow()

    fun agregarOportunidad(nuevaOportunidad: Oportunidad) {
        viewModelScope.launch {
            try {
                // Guarda de verdad en la base de datos local Room
                repository.guardarOportunidad(nuevaOportunidad)
                println("--- OPORTUNIDAD GUARDADA EN ROOM ---")
                println("Nombre: ${nuevaOportunidad.nombre}")
            } catch (e: Exception) {
                println("Error al guardar: ${e.localizedMessage}")
            }
        }
    }
}
