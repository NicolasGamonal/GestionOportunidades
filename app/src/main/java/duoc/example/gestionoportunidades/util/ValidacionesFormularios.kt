package duoc.example.gestionoportunidades.util

// Validaciones por formulario.
//
// Aquí vive TODA la lógica de "¿este formulario es válido?". Los ViewModels solo llaman
// a estas funciones y guardan el resultado (Map campo -> mensaje) en su UiState; las
// Screens solo muestran los mensajes. Así se mantiene la separación MVVM:
//
//   Validaciones.kt             -> reglas atómicas (obligatorio, largo, correo, fechas...)
//   ValidacionesFormularios.kt  -> combina esas reglas para cada formulario completo
//   ViewModel                   -> orquesta (llama, guarda errores en el estado, persiste)
//   Screen                      -> muestra
//
// Las funciones reciben valores simples (String, Long?) y NO el UiState, para que util/
// no dependa de la capa ui/ y se puedan probar con tests unitarios sin Android.

// ---------------------------------------------------------------------------
// Identificadores de campo (claves del mapa de errores de cada formulario).
// Ventaja sobre usar Strings: el compilador detecta typos.
// ---------------------------------------------------------------------------

enum class CampoOportunidad {
    NOMBRE, TIPO, ORGANIZACION, DETECCION, INICIO, TERMINO, COMUNA, PUBLICO, COSTO, OBSERVACIONES,
    // Campos agregados para las historias HU-03, HU-08, HU-10 y HU-11
    DESCRIPCION, FUENTE, HORA_TERMINO, PUBLICO_OBJETIVO, CONDICIONES, REQUISITOS, ESPACIOS
}

enum class CampoOrganizacion { NOMBRE, TIPO, RUBRO, COMUNA }

enum class CampoContacto { NOMBRE, CARGO, AREA, CORREO, TELEFONO }

enum class CampoSeguimiento { TIPO, RESULTADO, PROXIMA_ACCION, FECHA_PROXIMA }

// Resultado de una activación (HU-24)
enum class CampoResultado { ASISTENTES, CONTACTOS, INSCRIPCIONES, APRENDIZAJES }

// Convierte la lista de (campo, error?) en un mapa que solo contiene los campos con error.
// Si el mapa queda vacío -> el formulario es válido.
private fun <C> List<Pair<C, String?>>.soloErrores(): Map<C, String> =
    mapNotNull { (campo, error) -> error?.let { campo to it } }.toMap()

object ValidacionesFormularios {

    // --- Límites de largo de cada campo ---
    private const val NOMBRE_MIN = 3
    private const val NOMBRE_OPORTUNIDAD_MAX = 80
    private const val NOMBRE_ORGANIZACION_MAX = 80
    private const val NOMBRE_CONTACTO_MAX = 60
    private const val CARGO_MAX = 60
    private const val RUBRO_MAX = 50
    private const val OBSERVACIONES_MAX = 500
    private const val DESCRIPCION_MAX = 500
    private const val FUENTE_MAX = 100
    private const val PUBLICO_OBJETIVO_MAX = 150
    private const val TEXTO_LARGO_MAX = 500   // condiciones, requisitos y aprendizajes
    private const val RESULTADO_MIN = 5
    private const val RESULTADO_MAX = 300
    private const val PROXIMA_ACCION_MAX = 120

    // -----------------------------------------------------------------------
    // Oportunidad
    // -----------------------------------------------------------------------

    // Valida TODOS los campos y devuelve solo los que fallan.
    // Orden de validación por campo (el primero que falla "gana"):
    // - NOMBRE: obligatorio -> mínimo 3 -> máximo 80
    // - TIPO: obligatorio
    // - ORGANIZACION: debe estar seleccionada (id != null)
    // - DETECCION: fecha válida -> debe ser <= fechaInicio
    // - INICIO: fecha válida
    // - TERMINO: fecha válida -> debe ser >= fechaInicio
    // - COMUNA: obligatorio -> máximo 40
    // - PUBLICO: número > 0
    // - COSTO: número >= 0
    // - OBSERVACIONES: máximo 500 (opcional)
    fun validarOportunidad(
        nombre: String,
        tipoEvento: String,
        idOrganizacion: Long?,
        fechaDeteccion: String,
        fechaInicio: String,
        fechaTermino: String,
        horaInicio: String,
        horaTermino: String,
        comuna: String,
        publicoEsperado: String,
        costoReferencial: String,
        observaciones: String,
        descripcion: String,
        fuenteInformacion: String,
        publicoObjetivo: String,
        condicionesParticipacion: String,
        requisitos: String,
        espaciosDisponibles: String
    ): Map<CampoOportunidad, String> = listOf<Pair<CampoOportunidad, String?>>(
        CampoOportunidad.NOMBRE to (
                Validaciones.validarObligatorio(nombre, "El nombre")
                    ?: Validaciones.validarLongitudMin(nombre.trim(), NOMBRE_MIN, "El nombre")
                    ?: Validaciones.validarLongitudMax(nombre, NOMBRE_OPORTUNIDAD_MAX, "El nombre")
                ),
        CampoOportunidad.TIPO to Validaciones.validarObligatorio(tipoEvento, "El tipo de evento"),
        CampoOportunidad.ORGANIZACION to Validaciones.validarSeleccion(idOrganizacion, "Selecciona una organización"),
        CampoOportunidad.DETECCION to (
                Validaciones.validarFecha(fechaDeteccion, "La fecha de detección")
                    ?: Validaciones.validarDeteccionAntesInicio(fechaDeteccion, fechaInicio)
                ),
        CampoOportunidad.INICIO to Validaciones.validarFecha(fechaInicio, "La fecha de inicio"),
        CampoOportunidad.TERMINO to (
                Validaciones.validarFecha(fechaTermino, "La fecha de término")
                    ?: Validaciones.validarFechaTerminoDespuesInicio(fechaInicio, fechaTermino)
                ),
        // Si el evento es de un solo día, la hora de término debe ser posterior a la de inicio.
        CampoOportunidad.HORA_TERMINO to validarHorario(fechaInicio, fechaTermino, horaInicio, horaTermino),
        CampoOportunidad.COMUNA to validarComuna(comuna),
        CampoOportunidad.PUBLICO to Validaciones.validarNumeroPositivo(publicoEsperado, "El público esperado"),
        CampoOportunidad.COSTO to Validaciones.validarNumeroNoNegativo(costoReferencial, "El costo referencial"),
        CampoOportunidad.OBSERVACIONES to Validaciones.validarLongitudMax(observaciones, OBSERVACIONES_MAX, "Las observaciones"),
        // La descripción es opcional (solo se limita su largo); la fuente es obligatoria (HU-03).
        CampoOportunidad.DESCRIPCION to Validaciones.validarLongitudMax(descripcion, DESCRIPCION_MAX, "La descripción"),
        CampoOportunidad.FUENTE to (
                Validaciones.validarObligatorio(fuenteInformacion, "La fuente de información")
                    ?: Validaciones.validarLongitudMax(fuenteInformacion, FUENTE_MAX, "La fuente de información")
                ),
        CampoOportunidad.PUBLICO_OBJETIVO to (
                Validaciones.validarObligatorio(publicoObjetivo, "El público objetivo")
                    ?: Validaciones.validarLongitudMax(publicoObjetivo, PUBLICO_OBJETIVO_MAX, "El público objetivo")
                ),
        CampoOportunidad.CONDICIONES to Validaciones.validarLongitudMax(condicionesParticipacion, TEXTO_LARGO_MAX, "Las condiciones"),
        CampoOportunidad.REQUISITOS to Validaciones.validarLongitudMax(requisitos, TEXTO_LARGO_MAX, "Los requisitos"),
        CampoOportunidad.ESPACIOS to Validaciones.validarNumeroNoNegativo(espaciosDisponibles, "Los espacios disponibles")
    ).soloErrores()

    // La comuna debe ser obligatoria y pertenecer al catálogo de comunas de Chile (util/Regiones.kt).
    // El paréntesis en "a ?: if (...) null else ..." es necesario para que Kotlin lo interprete bien.
    private fun validarComuna(comuna: String): String? =
        Validaciones.validarObligatorio(comuna, "La comuna")
            ?: (if (Regiones.esComunaValida(comuna)) null else "Selecciona una comuna de la lista")

    // Compara las horas solo cuando el evento empieza y termina el mismo día.
    private fun validarHorario(fechaInicio: String, fechaTermino: String, horaInicio: String, horaTermino: String): String? {
        if (fechaInicio.isBlank() || fechaInicio != fechaTermino) return null
        val inicio = horaInicio.comoHoraOrNull() ?: return "La hora de inicio no es válida"
        val termino = horaTermino.comoHoraOrNull() ?: return "La hora de término no es válida"
        return if (!termino.isAfter(inicio)) "La hora de término debe ser posterior a la de inicio" else null
    }

    // -----------------------------------------------------------------------
    // Organización
    // -----------------------------------------------------------------------

    fun validarOrganizacion(
        nombre: String,
        tipo: String,
        rubro: String,
        comuna: String
    ): Map<CampoOrganizacion, String> = listOf<Pair<CampoOrganizacion, String?>>(
        CampoOrganizacion.NOMBRE to (
                Validaciones.validarObligatorio(nombre, "El nombre")
                    ?: Validaciones.validarLongitudMin(nombre.trim(), NOMBRE_MIN, "El nombre")
                    ?: Validaciones.validarLongitudMax(nombre, NOMBRE_ORGANIZACION_MAX, "El nombre")
                ),
        CampoOrganizacion.TIPO to Validaciones.validarObligatorio(tipo, "El tipo de organización"),
        CampoOrganizacion.RUBRO to (
                Validaciones.validarObligatorio(rubro, "El rubro")
                    ?: Validaciones.validarLongitudMax(rubro, RUBRO_MAX, "El rubro")
                ),
        CampoOrganizacion.COMUNA to validarComuna(comuna)
    ).soloErrores()

    // -----------------------------------------------------------------------
    // Contacto
    // -----------------------------------------------------------------------

    fun validarContacto(
        nombre: String,
        cargo: String,
        area: String,
        correo: String,
        telefono: String
    ): Map<CampoContacto, String> = listOf<Pair<CampoContacto, String?>>(
        CampoContacto.NOMBRE to (
                Validaciones.validarObligatorio(nombre, "El nombre")
                    ?: Validaciones.validarLongitudMin(nombre.trim(), NOMBRE_MIN, "El nombre")
                    ?: Validaciones.validarLongitudMax(nombre, NOMBRE_CONTACTO_MAX, "El nombre")
                ),
        CampoContacto.CARGO to (
                Validaciones.validarObligatorio(cargo, "El cargo")
                    ?: Validaciones.validarLongitudMax(cargo, CARGO_MAX, "El cargo")
                ),
        CampoContacto.AREA to Validaciones.validarObligatorio(area, "El área"),
        CampoContacto.CORREO to Validaciones.validarCorreo(correo.trim()),
        CampoContacto.TELEFONO to Validaciones.validarTelefono(telefono.trim())
    ).soloErrores()

    // -----------------------------------------------------------------------
    // Seguimiento (nueva gestión)
    // -----------------------------------------------------------------------

    // `hoy` es parámetro (con valor por defecto) para poder probar la regla con una fecha fija.
    fun validarSeguimiento(
        tipoAccion: String,
        resultado: String,
        proximaAccion: String,
        fechaProximo: String,
        hoy: String = hoyComoFechaCorta()
    ): Map<CampoSeguimiento, String> = listOf<Pair<CampoSeguimiento, String?>>(
        CampoSeguimiento.TIPO to Validaciones.validarObligatorio(tipoAccion, "El tipo de acción"),
        CampoSeguimiento.RESULTADO to (
                Validaciones.validarObligatorio(resultado, "El resultado")
                    ?: Validaciones.validarLongitudMin(resultado.trim(), RESULTADO_MIN, "El resultado")
                    ?: Validaciones.validarLongitudMax(resultado, RESULTADO_MAX, "El resultado")
                ),
        CampoSeguimiento.PROXIMA_ACCION to (
                Validaciones.validarObligatorio(proximaAccion, "La próxima acción")
                    ?: Validaciones.validarLongitudMax(proximaAccion, PROXIMA_ACCION_MAX, "La próxima acción")
                ),
        // Fecha válida Y >= hoy (no se puede programar un seguimiento en el pasado)
        CampoSeguimiento.FECHA_PROXIMA to (
                Validaciones.validarFecha(fechaProximo, "La fecha del próximo seguimiento")
                    ?: Validaciones.validarProximoSeguimiento(hoy, fechaProximo)
                )
    ).soloErrores()

    // -----------------------------------------------------------------------
    // Resultado de una activación (HU-24)
    // -----------------------------------------------------------------------
    // La evaluación (1 a 5) se elige con chips y siempre tiene un valor, por eso no se valida aquí.
    fun validarResultado(
        asistentes: String,
        contactosGenerados: String,
        inscripciones: String,
        aprendizajes: String
    ): Map<CampoResultado, String> = listOf<Pair<CampoResultado, String?>>(
        CampoResultado.ASISTENTES to Validaciones.validarNumeroNoNegativo(asistentes, "Los asistentes"),
        CampoResultado.CONTACTOS to Validaciones.validarNumeroNoNegativo(contactosGenerados, "Los contactos generados"),
        CampoResultado.INSCRIPCIONES to Validaciones.validarNumeroNoNegativo(inscripciones, "Las inscripciones"),
        CampoResultado.APRENDIZAJES to (
                Validaciones.validarObligatorio(aprendizajes, "Los aprendizajes")
                    ?: Validaciones.validarLongitudMax(aprendizajes, TEXTO_LARGO_MAX, "Los aprendizajes")
                )
    ).soloErrores()
}
