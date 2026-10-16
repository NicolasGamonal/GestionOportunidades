package duoc.example.gestionoportunidades.util

import java.util.regex.Pattern

object Validaciones {

    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    )

    // Valida números de 9 dígitos chilenos o con el prefijo +56
    private val TELEFONO_PATTERN = Pattern.compile(
        "^(\\+?56)?(\\s?)(9)(\\s?)[0-9]{8}$"
    )

    fun validarCampoObligatorio(texto: String): String? {
        return if (texto.isBlank()) "Este campo es obligatorio" else null
    }

    fun validarCorreo(correo: String): String? {
        if (correo.isBlank()) return "El correo es obligatorio"
        return if (!EMAIL_PATTERN.matcher(correo).matches()) "Formato de correo inválido" else null
    }

    fun validarTelefono(telefono: String): String? {
        if (telefono.isBlank()) return "El teléfono es obligatorio"
        return if (!TELEFONO_PATTERN.matcher(telefono).matches()) "Formato telefónico inválido (Ej: 912345678)" else null
    }

    fun validarCostoNoNegativo(costo: Double): String? {
        return if (costo < 0) "El valor comercial no puede ser negativo" else null
    }

    fun validarPublicoEsperado(publico: Int): String? {
        return if (publico <= 0) "El público esperado debe ser mayor a cero" else null
    }

    // Comprobación de reglas de negocio temporales
    fun validarCoherenciaFechas(fechaInicio: Long, fechaTermino: Long): String? {
        return if (fechaTermino < fechaInicio) "La fecha de término no puede ser anterior a la de inicio" else null
    }
}
