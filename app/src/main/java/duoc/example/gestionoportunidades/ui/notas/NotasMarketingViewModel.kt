package duoc.example.gestionoportunidades.ui.notas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.NotaMarketingRepository
import duoc.example.gestionoportunidades.model.NotaMarketing
import duoc.example.gestionoportunidades.model.NotaMarketingConAutor
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import duoc.example.gestionoportunidades.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotasUiState(
    val notas: List<NotaMarketingConAutor> = emptyList(),
    val tipo: String = "Observación",
    val texto: String = "",
    val error: String? = null
)

class NotasMarketingViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: NotaMarketingRepository
) : ViewModel() {

    // La ruta del detalle ("oportunidades/{id}") ya trae el argumento "id".
    private val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    // Solo guarda lo que el usuario está escribiendo; la lista de notas viene de Room.
    private val _formulario = MutableStateFlow(NotasUiState())

    val uiState: StateFlow<NotasUiState> = combine(
        repository.observarPorOportunidad(idOportunidad),
        _formulario
    ) { notas, form -> form.copy(notas = notas) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotasUiState())

    fun onTipoChange(v: String) = _formulario.update { it.copy(tipo = v) }

    fun onTextoChange(v: String) = _formulario.update { it.copy(texto = v, error = null) }

    fun agregar() {
        val usuario = SesionManager.usuarioActual ?: return
        // Doble seguridad: la UI ya oculta el formulario, pero el permiso se valida también aquí.
        if (!Permisos.puedeRegistrarNotas(usuario.rol)) return

        val s = _formulario.value
        val error = Validaciones.validarObligatorio(s.texto, "La nota")
            ?: Validaciones.validarLongitudMax(s.texto, 500, "La nota")
        if (error != null) {
            _formulario.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            repository.insertar(
                NotaMarketing(
                    idOportunidad = idOportunidad,
                    tipo = s.tipo,
                    texto = s.texto.trim(),
                    fechaHora = System.currentTimeMillis(),
                    idUsuario = usuario.idUsuario
                )
            )
            _formulario.update { it.copy(texto = "", error = null) }
        }
    }
}