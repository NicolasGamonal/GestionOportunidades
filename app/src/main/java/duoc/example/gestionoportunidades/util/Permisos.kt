package duoc.example.gestionoportunidades.util

object Permisos {

    // --- Ver oportunidades ---

    /** El ejecutivo solo ve las suyas; el supervisor ve todas. */
    fun puedeVerOportunidad(rol: String, idUsuario: Long, idResponsableOportunidad: Long?): Boolean {
        return when (rol) {
            "SUPERVISOR" -> true
            "EJECUTIVO" -> idResponsableOportunidad == idUsuario
            else -> false
        }
    }

    // --- Cambiar estado ---

    /**
     * Regla: el ejecutivo avanza un paso hacia adelante (hasta "En espera de respuesta").
     * El supervisor puede avanzar o pasar a estados finales (Confirmada / Descartada).
     */
    fun puedeCambiarEstado(rol: String, estadoActual: String, nuevoEstado: String): Boolean {
        val orden = listOf(
            "Identificada",
            "Contacto realizado",
            "Propuesta enviada",
            "En espera de respuesta",
            "Confirmada",
            "Descartada"
        )

        val idxActual = orden.indexOf(estadoActual)
        val idxNuevo = orden.indexOf(nuevoEstado)

        if (idxActual == -1 || idxNuevo == -1) return false

        return when (rol) {
            "SUPERVISOR" -> {
                // El supervisor puede avanzar un paso, o ir a Confirmada/Descartada desde cualquier estado no final
                idxNuevo == idxActual + 1 || nuevoEstado in listOf("Confirmada", "Descartada")
            }
            "EJECUTIVO" -> {
                // Solo un paso hacia adelante y NUNCA a estados finales
                idxNuevo == idxActual + 1 && nuevoEstado !in listOf("Confirmada", "Descartada")
            }
            else -> false
        }
    }

    // --- Asignar responsable ---

    /** Solo el supervisor puede asignar o reasignar. */
    fun puedeAsignar(rol: String): Boolean {
        return rol == "SUPERVISOR"
    }

    // --- Confirmar o descartar ---

    /** Solo el supervisor puede confirmar o descartar definitivamente. */
    fun puedeConfirmarODescartar(rol: String): Boolean {
        return rol == "SUPERVISOR"
    }
}