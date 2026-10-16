package duoc.example.gestionoportunidades.ui.seguimiento

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import duoc.example.gestionoportunidades.data.Seguimiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 1. Definimos el Estado de la Interfaz (UiState)[cite: 4]
data class SeguimientoUiState(
    val seguimientos: List<Seguimiento> = emptyList(),
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

// 2. Definimos el ViewModel
class SeguimientoViewModel : ViewModel() {

    // Estado interno modificable
    private val _uiState = MutableStateFlow(SeguimientoUiState())
    // Estado público de solo lectura para la vista (Compose)[cite: 4]
    val uiState: StateFlow<SeguimientoUiState> = _uiState.asStateFlow()

    init {
        cargarHistorialMock()
    }

    // Simulamos la carga de datos (luego tu compañero lo conectará a Room)
    private fun cargarHistorialMock() {
        _uiState.update { it.copy(estaCargando = true) }

        val historialPrueba = listOf(
            Seguimiento(
                id = 1, tipoAccion = "Cambio de estado", fechaHora = "2026-10-15 10:00",
                resultado = "La oportunidad pasó a 'Identificada'", proximaAccion = "Buscar contacto",
                fechaProximo = "2026-10-16", idOportunidad = 1, idUsuario = 1
            ),
            Seguimiento(
                id = 2, tipoAccion = "Llamada", fechaHora = "2026-10-16 11:30",
                resultado = "Se conversó con RRHH, mostraron interés.", proximaAccion = "Enviar propuesta",
                fechaProximo = "2026-10-18", idOportunidad = 1, idUsuario = 1, idContacto = 1
            )
        )

        _uiState.update {
            it.copy(
                seguimientos = historialPrueba,
                estaCargando = false
            )
        }
    }

    // Función para simular guardar un nuevo seguimiento
    fun guardarSeguimiento(nuevo: Seguimiento) {
        val listaActual = _uiState.value.seguimientos.toMutableList()
        // Le asignamos un ID falso sumando 1 al tamaño actual
        listaActual.add(nuevo.copy(id = listaActual.size + 1))
        _uiState.update { it.copy(seguimientos = listaActual) }
    }
}

