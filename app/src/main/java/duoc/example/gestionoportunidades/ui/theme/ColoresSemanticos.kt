package duoc.example.gestionoportunidades.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorEstado(val fondo: Color, val texto: Color)

/** Colores con significado propio de la app: estados de oportunidad, éxito y advertencia. */
@Immutable
data class ColoresSemanticos(
    val exito: Color,
    val sobreExito: Color,
    val advertencia: Color,
    val sobreAdvertencia: Color,
    val identificada: ColorEstado,
    val contactoRealizado: ColorEstado,
    val propuestaEnviada: ColorEstado,
    val enEspera: ColorEstado,
    val confirmada: ColorEstado,
    val descartada: ColorEstado
) {
    /** null si el estado no es uno de los conocidos. */
    fun paraEstado(estado: String): ColorEstado? = when (estado) {
        "Identificada" -> identificada
        "Contacto realizado" -> contactoRealizado
        "Propuesta enviada" -> propuestaEnviada
        "En espera de respuesta" -> enEspera
        "Confirmada" -> confirmada
        "Descartada" -> descartada
        else -> null
    }
}

val ColoresSemanticosClaro = ColoresSemanticos(
    exito = Color(0xFF2E7D32),
    sobreExito = Color(0xFFFFFFFF),
    advertencia = Color(0xFF9A5B00),
    sobreAdvertencia = Color(0xFFFFFFFF),
    identificada = ColorEstado(Color(0xFF546E7A), Color(0xFFFFFFFF)),
    contactoRealizado = ColorEstado(Color(0xFF1565C0), Color(0xFFFFFFFF)),
    propuestaEnviada = ColorEstado(Color(0xFFFFC107), Color(0xFF212121)),
    enEspera = ColorEstado(Color(0xFF6A1B9A), Color(0xFFFFFFFF)),
    confirmada = ColorEstado(Color(0xFF2E7D32), Color(0xFFFFFFFF)),
    descartada = ColorEstado(Color(0xFFB3261E), Color(0xFFFFFFFF))
)

val ColoresSemanticosOscuro = ColoresSemanticos(
    exito = Color(0xFF81C784),
    sobreExito = Color(0xFF0B2A0E),
    advertencia = Color(0xFFFFB74D),
    sobreAdvertencia = Color(0xFF2B1A00),
    identificada = ColorEstado(Color(0xFF90A4AE), Color(0xFF102027)),
    contactoRealizado = ColorEstado(Color(0xFF90CAF9), Color(0xFF0D2B4E)),
    propuestaEnviada = ColorEstado(Color(0xFFFFD54F), Color(0xFF261C00)),
    enEspera = ColorEstado(Color(0xFFCE93D8), Color(0xFF2A0A33)),
    confirmada = ColorEstado(Color(0xFF81C784), Color(0xFF0B2A0E)),
    descartada = ColorEstado(Color(0xFFEF9A9A), Color(0xFF4A0B0B))
)