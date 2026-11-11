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
    NOMBRE, TIPO, ORGANIZACION, DETECCION, INICIO, TERMINO, COMUNA, PUBLICO, COSTO, OBSERVACIONES
}

enum class CampoOrganizacion { NOMBRE, TIPO, RUBRO, COMUNA }

enum class CampoContacto { NOMBRE, CARGO, AREA, CORREO, TELEFONO }

enum class CampoSeguimiento { TIPO, RESULTADO, PROXIMA_ACCION, FECHA_PROXIMA }

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
    private const val COMUNA_MAX = 40
    private const val OBSERVACIONES_MAX = 500
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
        comuna: String,
        publicoEsperado: String,
        costoReferencial: String,
        observaciones: String
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
        CampoOportunidad.COMUNA to (
                Validaciones.validarObligatorio(comuna, "La comuna")
                    ?: Validaciones.validarLongitudMax(comuna, COMUNA_MAX, "La comuna")
                ),
        CampoOportunidad.PUBLICO to Validaciones.validarNumeroPositivo(publicoEsperado, "El público esperado"),
        CampoOportunidad.COSTO to Validaciones.validarNumeroNoNegativo(costoReferencial, "El costo referencial"),
        CampoOportunidad.OBSERVACIONES to Validaciones.validarLongitudMax(observaciones, OBSERVACIONES_MAX, "Las observaciones")
    ).soloErrores()

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
        CampoOrganizacion.COMUNA to (
                Validaciones.validarObligatorio(comuna, "La comuna")
                    ?: Validaciones.validarLongitudMax(comuna, COMUNA_MAX, "La comuna")
                )
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
}