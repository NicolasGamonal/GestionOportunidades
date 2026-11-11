package duoc.example.gestionoportunidades.util

object Permisos {

    // --- Ver oportunidades ---

    /** El ejecutivo solo ve las suyas; el supervisor ve todas. */
    fun puedeVerOportunidad(
        rol: String,
        idUsuario: Long,
        idResponsableOportunidad: Long?,
        estado: String = ""          // solo lo usa MARKETING
    ): Boolean {
        return when (rol) {
            "SUPERVISOR" -> true
            "EJECUTIVO" -> idResponsableOportunidad == idUsuario
            "MARKETING" -> estado == "Confirmada"   // solo ve oportunidades confirmadas
            else -> false
        }
    }

    // Crear/editar oportunidades, evidencia fotográfica e historial de gestiones (HU-27: Marketing solo lee)
    fun puedeGestionarOportunidades(rol: String): Boolean =
        rol == "SUPERVISOR" || rol == "EJECUTIVO"

    // Registrar material u observaciones de marketing (HU-27)
    fun puedeRegistrarNotas(rol: String): Boolean =
        rol == "MARKETING" || rol == "SUPERVISOR"

    // --- Cambiar estado ---

    /**
     * Regla: el ejecutivo avanza un paso hacia adelante (hasta "En espera de respuesta").
     * El supervisor puede avanzar o pasar a estados finales (Confirmada / Descartada).
     */
    fun puedeCambiarEstado(rol: String, estadoActual: String, nuevoEstado: String): Boolean {
        // Los estados finales no se modifican, y "cambiar" al mismo estado no tiene sentido
        if (estadoActual in Estados.finales || estadoActual == nuevoEstado) return false

        val idxActual = Estados.todos.indexOf(estadoActual)
        val idxNuevo = Estados.todos.indexOf(nuevoEstado)
        if (idxActual == -1 || idxNuevo == -1) return false

        return when (rol) {
            "SUPERVISOR" -> idxNuevo == idxActual + 1 || nuevoEstado in Estados.finales
            "EJECUTIVO" -> idxNuevo == idxActual + 1 && nuevoEstado !in Estados.finales
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
    /** Solo el supervisor modifica los datos de una organización (son compartidos por todo el equipo). **/
    fun puedeEditarOrganizacion(rol: String): Boolean = rol == "SUPERVISOR"

    /** El ejecutivo elimina solo las suyas; el supervisor, cualquiera. (El repositorio exige además estado "Identificada" y sin historial.) */
    fun puedeEliminarOportunidad(rol: String, idUsuario: Long, idResponsable: Long?): Boolean =
        puedeVerOportunidad(rol, idUsuario, idResponsable)

    /** Las organizaciones son datos compartidos por todo el equipo: solo el supervisor las elimina. */
    fun puedeEliminarOrganizacion(rol: String): Boolean = rol == "SUPERVISOR"

    // --- Resultado de la activación (HU-24) ---

    /**
     * Solo se registra el resultado de oportunidades Confirmadas (la agencia participó en el evento).
     * Pueden hacerlo quienes ven la oportunidad: el ejecutivo responsable y el supervisor.
     */
    fun puedeRegistrarResultado(rol: String, idUsuario: Long, idResponsable: Long?, estado: String): Boolean =
        estado == "Confirmada" && puedeVerOportunidad(rol, idUsuario, idResponsable)
}
