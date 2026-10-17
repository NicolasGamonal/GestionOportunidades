package duoc.example.gestionoportunidades.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Validaciones {

    private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // --- Campos obligatorios ---

    fun validarObligatorio(valor: String, nombreCampo: String): String? {
        return if (valor.isBlank()) "$nombreCampo es obligatorio" else null
    }

    // --- Longitud ---

    fun validarLongitudMin(valor: String, min: Int, nombreCampo: String): String? {
        return if (valor.length < min) "$nombreCampo debe tener al menos $min caracteres" else null
    }

    fun validarLongitudMax(valor: String, max: Int, nombreCampo: String): String? {
        return if (valor.length > max) "$nombreCampo no puede superar $max caracteres" else null
    }

    // --- Correo ---

    fun validarCorreo(valor: String): String? {
        if (valor.isBlank()) return "El correo es obligatorio"
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return if (!regex.matches(valor)) "Correo inválido" else null
    }

    // --- Teléfono ---

    fun validarTelefono(valor: String): String? {
        if (valor.isBlank()) return "El teléfono es obligatorio"
        val regex = Regex("^\\+?\\d{8,15}$")
        return if (!regex.matches(valor.replace(" ", "").replace("-", ""))) "Teléfono inválido" else null
    }

    // --- Números ---

    fun validarNumeroNoNegativo(valor: String, nombreCampo: String): String? {
        if (valor.isBlank()) return "$nombreCampo es obligatorio"
        val num = valor.toDoubleOrNull()
        return when {
            num == null -> "$nombreCampo debe ser un número"
            num < 0 -> "$nombreCampo no puede ser negativo"
            else -> null
        }
    }

    fun validarNumeroPositivo(valor: String, nombreCampo: String): String? {
        if (valor.isBlank()) return "$nombreCampo es obligatorio"
        val num = valor.toDoubleOrNull()
        return when {
            num == null -> "$nombreCampo debe ser un número"
            num <= 0 -> "$nombreCampo debe ser mayor que 0"
            else -> null
        }
    }

    // --- Fechas ---

    fun validarFecha(valor: String, nombreCampo: String): String? {
        if (valor.isBlank()) return "$nombreCampo es obligatorio"
        return try {
            LocalDate.parse(valor, formatoFecha)
            null
        } catch (e: Exception) {
            "$nombreCampo debe estar en formato dd/MM/yyyy"
        }
    }

    fun validarFechaTerminoDespuesInicio(fechaInicio: String, fechaTermino: String): String? {
        if (fechaInicio.isBlank() || fechaTermino.isBlank()) return null
        return try {
            val inicio = LocalDate.parse(fechaInicio, formatoFecha)
            val termino = LocalDate.parse(fechaTermino, formatoFecha)
            if (termino.isBefore(inicio)) "La fecha de término no puede ser anterior a la de inicio"
            else null
        } catch (e: Exception) {
            null
        }
    }

    fun validarDeteccionAntesInicio(fechaDeteccion: String, fechaInicio: String): String? {
        if (fechaDeteccion.isBlank() || fechaInicio.isBlank()) return null
        return try {
            val deteccion = LocalDate.parse(fechaDeteccion, formatoFecha)
            val inicio = LocalDate.parse(fechaInicio, formatoFecha)
            if (deteccion.isAfter(inicio)) "La fecha de detección no puede ser posterior al inicio"
            else null
        } catch (e: Exception) {
            null
        }
    }

    fun validarProximoSeguimiento(fechaGestion: String, fechaProximo: String): String? {
        if (fechaGestion.isBlank() || fechaProximo.isBlank()) return null
        return try {
            val gestion = LocalDate.parse(fechaGestion, formatoFecha)
            val proximo = LocalDate.parse(fechaProximo, formatoFecha)
            if (proximo.isBefore(gestion)) "El próximo seguimiento no puede ser anterior a la fecha de la gestión"
            else null
        } catch (e: Exception) {
            null
        }
    }
}   