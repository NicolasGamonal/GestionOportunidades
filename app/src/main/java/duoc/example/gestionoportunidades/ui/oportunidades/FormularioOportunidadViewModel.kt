package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Permisos
import duoc.example.gestionoportunidades.util.Validaciones
import duoc.example.gestionoportunidades.util.comoFechaCorta
import duoc.example.gestionoportunidades.util.fechaCortaAMillis
import duoc.example.gestionoportunidades.util.hoyComoFechaCorta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

// FormularioOportunidadViewModel: lógica detrás del formulario de crear/editar oportunidad.
//
// Responsabilidad:
// - Manejar el estado de los 11 campos del formulario (valores + errores por campo).
// - Validar en tiempo real (en cada tecla) y al presionar "Guardar" (validación completa).
// - Cargar los datos existentes en modo edición (y verificar permisos de acceso).
// - Guardar en Room (crear o actualizar) con la lógica de negocio:
//   - El ejecutivo que crea queda como responsable.
//   - El supervisor que crea deja la oportunidad sin asignar (idResponsable = null).
//   - Si la fecha no cambió respecto a la original, se conserva la hora (no se resetea a medianoche).
// - Eliminar la oportunidad (solo en modo edición, con permisos y reglas del repositorio).
// - Cargar la lista de organizaciones para el dropdown (reactivo: si se agrega una org,
//   el dropdown se actualiza solo).
//
// Patrón MVVM:
// - Recibe 2 repositorios por inyección (desde AppViewModelProvider.Factory → AppContainer).
// - Expone un único StateFlow<FormularioUiState> que la Screen observa.
// - No conoce Composables: la validación usa Validaciones.kt (util/), separado de los Composables (checklist).
//
// ¿Por qué los campos numéricos (publicoEsperado, costoReferencial) son String en el UiState?
// Porque el campo de texto del formulario trabaja con String. Si se guardaran como Int/Double
// en el UiState, cada tecla ("1", "12", "123") requeriría una conversión y un manejo de
// "campo vacío" (que no es ni 0 ni null). Al ser String, el campo vacío es simplemente "".
// La conversión a Int/Double se hace una sola vez al guardar (s.publicoEsperado.toInt()).
//
// ¿Por qué SavedStateHandle?
// Porque la ruta tiene un argumento "id" (default = -1).
// - id = -1 → modo CREAR (campos vacíos, fechaDeteccion = hoy)
// - id > 0 → modo EDITAR (carga los datos de Room)

// Enum: identifica cada campo del formulario para el mapa de errores.
// Se usa como clave en FormularioUiState.errores: Map<CampoOportunidad, String>.
// Ventaja sobre usar Strings ("nombre", "tipo", ...): el compilador detecta typos.
enum class CampoOportunidad {
    NOMBRE, TIPO, ORGANIZACION, DETECCION, INICIO, TERMINO, COMUNA, PUBLICO, COSTO, OBSERVACIONES
}

// Estado completo del formulario que la Screen observa.
//
// ¿Por qué un solo data class y no varios StateFlows?
// Porque los campos están relacionados (la validación de fechaTermino depende de fechaInicio).
// Al estar en un solo objeto, el ViewModel puede validarlos todos a la vez y emitir
// un solo update (atómico). Con StateFlows separados, habría actualizaciones parciales.
data class FormularioUiState(
    // --- CAMPOS DEL FORMULARIO (valores actuales) ---
    // Nombre de la oportunidad (texto libre, 3-80 caracteres)
    val nombre: String = "",
    // Tipo de evento (dropdown: "Feria municipal", "Jornada de beneficios", etc.)
    val tipoEvento: String = "",
    // Id de la organización seleccionada (null = no seleccionada)
    val idOrganizacion: Long? = null,
    // Fecha de detección en formato "dd/MM/yyyy" (String para el campo de texto)
    val fechaDeteccion: String = "",
    // Fecha de inicio del evento en formato "dd/MM/yyyy"
    val fechaInicio: String = "",
    // Fecha de término del evento en formato "dd/MM/yyyy"
    val fechaTermino: String = "",
    // Comuna donde se realiza el evento (texto libre)
    val comuna: String = "",
    // Público esperado (String: se convierte a Int al guardar). Máx 7 dígitos.
    val publicoEsperado: String = "",
    // Prioridad: "Alta", "Media" o "Baja" (default "Media")
    val prioridad: String = "Media",
    // Costo referencial en CLP (String: se convierte a Double al guardar). Máx 10 dígitos.
    val costoReferencial: String = "",
    // Observaciones (texto libre, máx 500 caracteres, opcional)
    val observaciones: String = "",

    // --- ERRORES DE VALIDACIÓN ---
    // Mapa de campo → mensaje de error. Si un campo no tiene clave en el mapa, es válido.
    // Se actualiza en tiempo real (al escribir) y al presionar "Guardar" (validación completa).
    val errores: Map<CampoOportunidad, String> = emptyMap(),

    // --- DATOS DE APOYO ---
    // Lista de organizaciones para el dropdown (viene de Room, se actualiza en tiempo real)
    val organizaciones: List<Organizacion> = emptyList(),

    // --- ESTADO DE LA PANTALLA ---
    // true si es modo edición (id > 0). Cambia el texto del botón y muestra "Eliminar".
    val esEdicion: Boolean = false,
    // true mientras se cargan los datos de Room (solo en modo edición)
    val cargando: Boolean = false,
    // true si el usuario no tiene permiso para editar esta oportunidad
    val sinAcceso: Boolean = false,
    // true mientras se escribe en Room (spinner en el botón de guardar)
    val guardando: Boolean = false,
    // true después del guardado exitoso (dispara el banner + retorno automático)
    val guardadoExitoso: Boolean = false,
    // Mensaje de error general (no por campo): "No se pudo guardar", "Tu sesión expiró", etc.
    val mensajeError: String? = null,
    // true si el usuario puede eliminar (solo en edición + estado "Identificada" + permisos)
    val puedeEliminar: Boolean = false,
    // true después de la eliminación exitosa (dispara el banner + navegación a la lista)
    val eliminado: Boolean = false
)

class FormularioOportunidadViewModel(
    // SavedStateHandle: contiene el argumento "id" de la ruta de navegación.
    // -1 = crear nuevo (default), > 0 = editar esa oportunidad.
    savedStateHandle: SavedStateHandle,

    // Repositorio de oportunidades: para obtener (edición), guardar y eliminar.
    private val oportunidadRepository: OportunidadRepository,

    // Repositorio de organizaciones: para cargar la lista del dropdown.
    // No es private porque se usa dentro del init (fuera del alcance de las funciones).
    organizacionRepository: OrganizacionRepository
) : ViewModel() {

    // El id de la oportunidad a editar, o -1 si es creación nueva.
    // Se extrae una sola vez del SavedStateHandle (sobrevive a rotación de pantalla).
    private val idEdicion: Long = savedStateHandle.get<Long>("id") ?: -1L

    // La oportunidad ORIGINAL (antes de editar). Se usa para:
    // 1. Conservar la hora original si la fecha no cambió (aMillis).
    // 2. Conservar el estado (no se cambia desde el formulario).
    // 3. Conservar el idResponsable (no se reasigna desde el formulario).
    // null en modo creación.
    private var original: Oportunidad? = null

    // Estado mutable del formulario.
    // En modo edición: cargando = true (muestra spinner hasta que se cargan los datos).
    // En modo creación: fechaDeteccion se pre-llena con hoy (conveniencia: la detección
    // casi siempre es "hoy" o "ayer", no hace falta que el usuario elija la fecha).
    private val _uiState = MutableStateFlow(
        FormularioUiState(
            esEdicion = idEdicion > 0,
            cargando = idEdicion > 0,
            fechaDeteccion = hoyComoFechaCorta()  // Pre-llena con la fecha de hoy
        )
    )

    // Estado inmutable (público): la Screen solo puede leer/observar.
    val uiState: StateFlow<FormularioUiState> = _uiState.asStateFlow()

    // --- INICIALIZACIÓN ---
    init {
        // 1. Cargar la lista de organizaciones para el dropdown (reactivo).
        // collect: se re-ejecuta cada vez que Room emite un cambio (si se agrega/elimina una org).
        // Esto significa que si el supervisor crea una org nueva mientras el ejecutivo
        // tiene el formulario abierto, el dropdown se actualiza solo.
        viewModelScope.launch {
            organizacionRepository.obtenerTodas().collect { lista ->
                _uiState.update { it.copy(organizaciones = lista) }
            }
        }

        // 2. Si es modo edición, cargar los datos de la oportunidad existente.
        if (idEdicion > 0) cargarParaEdicion()
    }

    // Carga los datos de la oportunidad existente para pre-llenar el formulario.
    // También verifica permisos: si el usuario no puede ver esta oportunidad,
    // se pone sinAcceso = true y la Screen muestra un mensaje de error.
    private fun cargarParaEdicion() {
        viewModelScope.launch {
            // Obtener la oportunidad por id (consulta puntual, no reactiva)
            val op = oportunidadRepository.obtenerPorId(idEdicion)
            val usuario = SesionManager.usuarioActual

            // Verificar: ¿existe la oportunidad? ¿hay sesión? ¿tiene permiso?
            if (op == null || usuario == null ||
                !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, op.idResponsable)
            ) {
                // Sin acceso: se detiene la carga y se muestra el mensaje en la Screen
                _uiState.update { it.copy(cargando = false, sinAcceso = true) }
                return@launch
            }

            // Guardar la oportunidad original (para conservar hora, estado y responsable)
            original = op

            // Pre-llenar todos los campos con los datos existentes.
            // Las fechas se convierten de Long (ms) a String "dd/MM/yyyy" (comoFechaCorta).
            // El costo se convierte de Double a String (toLong().toString() para evitar "850000.0").
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = op.nombre,
                    tipoEvento = op.tipoEvento,
                    idOrganizacion = op.idOrganizacion,
                    fechaDeteccion = op.fechaDeteccion.comoFechaCorta(),
                    fechaInicio = op.fechaInicio.comoFechaCorta(),
                    fechaTermino = op.fechaTermino.comoFechaCorta(),
                    comuna = op.comuna,
                    publicoEsperado = op.publicoEsperado.toString(),
                    prioridad = op.prioridad,
                    costoReferencial = op.costoReferencial.toLong().toString(),
                    observaciones = op.observaciones,
                    // Calcular si se puede eliminar:
                    // 1. Debe estar en estado "Identificada" (regla del repositorio)
                    // 2. El usuario debe tener permiso (Permisos.puedeEliminarOportunidad)
                    puedeEliminar = op.estado == "Identificada" &&
                            Permisos.puedeEliminarOportunidad(usuario.rol, usuario.idUsuario, op.idResponsable)
                )
            }
        }
    }

    // --- EVENTOS DE LA PANTALLA: al editar un campo se borra su error ---

    // Función genérica que actualiza un campo y limpia su error de validación.
    //
    // ¿Por qué limpiar el error al escribir?
    // Porque si el usuario corrige el campo (ej: escribe el nombre que estaba vacío),
    // el error "El nombre es obligatorio" debería desaparecer de inmediato.
    // No hay que esperar a presionar "Guardar" para que se limpie.
    //
    // Parámetros:
    // - campo: el campo que se está editando (null = no limpiar errores, ej: prioridad)
    // - cambio: lambda que transforma el estado (it.copy(nombre = v), etc.)
    private fun editar(campo: CampoOportunidad?, cambio: (FormularioUiState) -> FormularioUiState) =
        _uiState.update { actual ->
            val nuevo = cambio(actual)
            // Si se indica un campo, se elimina su error del mapa (si existía)
            // Si campo es null (prioridad), no se limpia nada (no tiene error propio)
            if (campo != null) nuevo.copy(errores = nuevo.errores - campo) else nuevo
        }

    // --- CALLBACKS DE CADA CAMPO ---
    // Cada uno llama a editar() con el campo correspondiente.
    // La Screen los conecta con onValorChange / onSeleccion de los componentes.

    // Nombre: texto libre
    fun onNombreChange(v: String) = editar(CampoOportunidad.NOMBRE) { it.copy(nombre = v) }

    // Tipo de evento: selección desde dropdown
    fun onTipoChange(v: String) = editar(CampoOportunidad.TIPO) { it.copy(tipoEvento = v) }

    // Organización: selección desde dropdown (pasa el id, no el objeto)
    fun onOrganizacionChange(id: Long) = editar(CampoOportunidad.ORGANIZACION) { it.copy(idOrganizacion = id) }

    // Fecha de detección: selección desde date picker (formato "dd/MM/yyyy")
    fun onDeteccionChange(v: String) = editar(CampoOportunidad.DETECCION) { it.copy(fechaDeteccion = v) }

    // Fecha de inicio: selección desde date picker
    fun onInicioChange(v: String) = editar(CampoOportunidad.INICIO) { it.copy(fechaInicio = v) }

    // Fecha de término: selección desde date picker
    fun onTerminoChange(v: String) = editar(CampoOportunidad.TERMINO) { it.copy(fechaTermino = v) }

    // Comuna: texto libre
    fun onComunaChange(v: String) = editar(CampoOportunidad.COMUNA) { it.copy(comuna = v) }

    // Público esperado: solo acepta dígitos (filter) y máx 7 caracteres (take).
    // Esto evita que el usuario escriba letras o números absurdos (> 10 millones).
    // La validación completa (debe ser > 0) la hace validar() al guardar.
    fun onPublicoChange(v: String) =
        editar(CampoOportunidad.PUBLICO) { it.copy(publicoEsperado = v.filter(Char::isDigit).take(7)) }

    // Prioridad: no tiene campo de error propio (siempre hay una seleccionada, default "Media").
    // Por eso campo = null (no limpia nada).
    fun onPrioridadChange(v: String) = editar(null) { it.copy(prioridad = v) }

    // Costo referencial: solo acepta dígitos y máx 10 caracteres (~$2.1 billones CLP).
    // Evita que se escriban puntos decimales (el costo se maneja como entero en la UI).
    fun onCostoChange(v: String) =
        editar(CampoOportunidad.COSTO) { it.copy(costoReferencial = v.filter(Char::isDigit).take(10)) }

    // Observaciones: texto libre (multilinea)
    fun onObservacionesChange(v: String) =
        editar(CampoOportunidad.OBSERVACIONES) { it.copy(observaciones = v) }

    // Limpia el mensaje de error general (lo pone a null).
    // Se llama desde FormularioContenedor después de mostrar el Snackbar.
    fun limpiarMensajeError() = _uiState.update { it.copy(mensajeError = null) }

    // --- VALIDACIÓN (usa util/Validaciones.kt, separado de los Composables) ---

    // Valida TODOS los campos y devuelve un mapa de errores (solo los que fallan).
    // Si el mapa está vacío → todos los campos son válidos → se puede guardar.
    //
    // ¿Por qué un solo mapa y no validar campo por campo?
    // Porque al presionar "Guardar", se quiere marcar TODOS los errores a la vez
    // (el usuario ve de un vistazo qué campos corregir). Si se validara uno por uno,
    // tendría que presionar "Guardar" varias veces.
    //
    // Orden de validación por campo (se evalúan en secuencia, el primero que falla "gana"):
    // - NOMBRE: obligatorio → longitud mínima 3 → longitud máxima 80
    // - TIPO: obligatorio
    // - ORGANIZACION: debe estar seleccionada (idOrganizacion != null)
    // - DETECCION: fecha válida → debe ser ≤ fechaInicio
    // - INICIO: fecha válida
    // - TERMINO: fecha válida → debe ser ≥ fechaInicio
    // - COMUNA: obligatorio → longitud máxima 40
    // - PUBLICO: número positivo (> 0)
    // - COSTO: número no negativo (≥ 0)
    // - OBSERVACIONES: longitud máxima 500 (opcional: si está vacío, no falla)
    private fun validar(s: FormularioUiState): Map<CampoOportunidad, String> {
        // Lista de pares (campo, error o null). null = válido.
        val resultados = listOf<Pair<CampoOportunidad, String?>>(
            // NOMBRE: 3 validaciones en cadena (?: = "si es null, prueba la siguiente")
            CampoOportunidad.NOMBRE to (
                    Validaciones.validarObligatorio(s.nombre, "El nombre")
                        ?: Validaciones.validarLongitudMin(s.nombre.trim(), 3, "El nombre")
                        ?: Validaciones.validarLongitudMax(s.nombre, 80, "El nombre")
                    ),
            // TIPO: solo obligatorio (viene de un dropdown, siempre tiene formato válido)
            CampoOportunidad.TIPO to Validaciones.validarObligatorio(s.tipoEvento, "El tipo de evento"),
            // ORGANIZACION: debe haber seleccionado una (idOrganizacion no puede ser null)
            CampoOportunidad.ORGANIZACION to
                    if (s.idOrganizacion == null) "Selecciona una organización" else null,
            // DETECCION: fecha válida + regla de negocio (detección ≤ inicio)
            CampoOportunidad.DETECCION to (
                    Validaciones.validarFecha(s.fechaDeteccion, "La fecha de detección")
                        ?: Validaciones.validarDeteccionAntesInicio(s.fechaDeteccion, s.fechaInicio)
                    ),
            // INICIO: solo fecha válida (la regla con término se valida en TERMINO)
            CampoOportunidad.INICIO to Validaciones.validarFecha(s.fechaInicio, "La fecha de inicio"),
            // TERMINO: fecha válida + regla de negocio (término ≥ inicio)
            CampoOportunidad.TERMINO to (
                    Validaciones.validarFecha(s.fechaTermino, "La fecha de término")
                        ?: Validaciones.validarFechaTerminoDespuesInicio(s.fechaInicio, s.fechaTermino)
                    ),
            // COMUNA: obligatorio + longitud máxima
            CampoOportunidad.COMUNA to (
                    Validaciones.validarObligatorio(s.comuna, "La comuna")
                        ?: Validaciones.validarLongitudMax(s.comuna, 40, "La comuna")
                    ),
            // PUBLICO: debe ser un número > 0 (validarNumeroPositivo devuelve null si es válido)
            CampoOportunidad.PUBLICO to Validaciones.validarNumeroPositivo(s.publicoEsperado, "El público esperado"),
            // COSTO: debe ser un número ≥ 0 (puede ser 0 si se descarta)
            CampoOportunidad.COSTO to Validaciones.validarNumeroNoNegativo(s.costoReferencial, "El costo referencial"),
            // OBSERVACIONES: opcional (si está vacío, validarLongitudMax no falla), máx 500
            CampoOportunidad.OBSERVACIONES to Validaciones.validarLongitudMax(s.observaciones, 500, "Las observaciones")
        )
        // Filtra los null (válidos) y convierte a Map (solo los que tienen error)
        return resultados.mapNotNull { (campo, error) -> error?.let { campo to it } }.toMap()
    }

    // --- CONVERSIÓN DE FECHAS ---

    // Convierte una fecha "dd/MM/yyyy" a milisegundos, conservando la hora original si no cambió.
    //
    // ¿Por qué conservar la hora original?
    // Porque Room guarda fechas como timestamp completo (fecha + hora). Si el usuario
    // edita el nombre de la oportunidad sin tocar la fecha, no queremos que la hora
    // se resetee a medianoche. Solo si el usuario CAMBIA la fecha, se usa una hora por defecto.
    //
    // Parámetros:
    // - fecha: la fecha actual del campo (String "dd/MM/yyyy")
    // - original: el timestamp original (Long) o null (modo creación)
    // - horaPorDefecto: la hora a usar si la fecha cambió (o si es creación)
    //   - Detección: MIDNIGHT (00:00) → solo importa el día
    //   - Inicio: 09:00 → hora típica de inicio de eventos
    //   - Término: 18:00 → hora típica de término de eventos
    private fun aMillis(fecha: String, original: Long?, horaPorDefecto: LocalTime): Long =
        if (original != null && original.comoFechaCorta() == fecha) original
        else fecha.fechaCortaAMillis(horaPorDefecto)

    // --- GUARDADO ---

    // Valida todos los campos y, si no hay errores, guarda en Room.
    // Se llama desde la Screen cuando el usuario presiona "Guardar".
    fun guardar() {
        val s = _uiState.value

        // Guarda de seguridad: si ya está guardando o ya se guardó, no se re-ejecuta.
        if (s.guardando || s.guardadoExitoso) return

        // Validación completa: si hay errores, se marcan en la UI y se sale.
        // Los valores NO se borran (están en el UiState, no en remember local).
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            // Se marcan los errores y se conservan todos los valores escritos
            _uiState.update { it.copy(errores = errores) }
            return
        }

        // Verificar que hay sesión activa (no debería pasar, pero es una guarda)
        val usuario = SesionManager.usuarioActual
        if (usuario == null) {
            _uiState.update { it.copy(mensajeError = "Tu sesión expiró. Vuelve a iniciar sesión.") }
            return
        }

        // Construir el objeto Oportunidad para guardar en Room.
        val base = original  // null en modo creación, la oportunidad existente en modo edición
        val oportunidad = Oportunidad(
            // id: 0 en creación (Room lo genera con autoGenerate), el id original en edición
            idOportunidad = base?.idOportunidad ?: 0,
            // trim(): elimina espacios al inicio/fin (el usuario puede haber presionado espacio)
            nombre = s.nombre.trim(),
            tipoEvento = s.tipoEvento,
            // Fechas: se convierten de "dd/MM/yyyy" a Long (ms), conservando la hora original si no cambió
            fechaDeteccion = aMillis(s.fechaDeteccion, base?.fechaDeteccion, LocalTime.MIDNIGHT),
            fechaInicio = aMillis(s.fechaInicio, base?.fechaInicio, LocalTime.of(9, 0)),
            fechaTermino = aMillis(s.fechaTermino, base?.fechaTermino, LocalTime.of(18, 0)),
            comuna = s.comuna.trim(),
            // Conversión de String a Int (ya validó que es un número positivo)
            publicoEsperado = s.publicoEsperado.toInt(),
            prioridad = s.prioridad,
            // Estado: se conserva el original (no se cambia desde el formulario).
            // En creación: siempre "Identificada" (el primer estado del flujo).
            estado = base?.estado ?: "Identificada",
            // Conversión de String a Double (ya validó que es un número no negativo)
            costoReferencial = s.costoReferencial.toDouble(),
            observaciones = s.observaciones.trim(),
            // checkNotNull: ya validó que idOrganizacion no es null, así que esto no debería fallar
            idOrganizacion = checkNotNull(s.idOrganizacion),
            // Lógica de negocio para el responsable:
            // - Si es edición → se conserva el responsable original (no se reasigna desde el formulario)
            // - Si es creación y el usuario es EJECUTIVO → queda asignado a él
            // - Si es creación y el usuario es SUPERVISOR → se deja sin asignar (null)
            //   (el supervisor la asigna después desde el panel)
            idResponsable = base?.idResponsable
                ?: if (usuario.rol == "EJECUTIVO") usuario.idUsuario else null
        )

        // Ejecutar el guardado en un hilo de fondo (suspend)
        viewModelScope.launch {
            // Activar el spinner y limpiar errores anteriores
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                if (base == null) {
                    // Modo CREACIÓN: inserta una nueva oportunidad
                    oportunidadRepository.guardarOportunidad(oportunidad)
                } else {
                    // Modo EDICIÓN: actualiza la oportunidad existente
                    oportunidadRepository.modificarOportunidad(oportunidad)
                }
                // Éxito: desactiva spinner, activa guardadoExitoso (dispara banner + retorno)
                _uiState.update { it.copy(guardando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                // Error de Room: muestra el mensaje en el Snackbar
                _uiState.update {
                    it.copy(guardando = false, mensajeError = "No se pudo guardar: ${e.localizedMessage}")
                }
            }
        }
    }

    // Elimina la oportunidad (solo en modo edición, con permisos y reglas del repositorio).
    // Se llama desde la Screen cuando el usuario confirma el AlertDialog de BotonEliminar.
    fun eliminar() {
        val s = _uiState.value
        // Guarda de seguridad: solo se elimina si es edición, tiene permiso,
        // no está guardando, no se guardó y no se eliminó antes.
        if (!s.esEdicion || !s.puedeEliminar || s.guardando || s.guardadoExitoso || s.eliminado) return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                // Llama al repositorio que aplica las reglas de negocio:
                // - ¿Estado es "Identificada"? Si no → mensaje de error.
                // - ¿Tiene seguimientos? Si sí → mensaje de error.
                // - ¿Tiene evidencias? Si sí → mensaje de error.
                // - Si pasa todo → elimina y devuelve null.
                val motivo = oportunidadRepository.eliminar(idEdicion)
                _uiState.update {
                    if (motivo == null) {
                        // Éxito: se eliminó → dispara banner "Oportunidad eliminada" + navegación
                        it.copy(guardando = false, eliminado = true)
                    } else {
                        // El repositorio devolvió un motivo (no se pudo eliminar)
                        it.copy(guardando = false, mensajeError = motivo)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(guardando = false, mensajeError = "No se pudo eliminar: ${e.localizedMessage}")
                }
            }
        }
    }
}