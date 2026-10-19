package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.EvidenciaRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.model.Evidencia
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.util.Estados
import duoc.example.gestionoportunidades.util.Permisos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// DetalleOportunidadViewModel: lógica detrás de la pantalla de detalle de oportunidad.
//
// Responsabilidad:
// - Cargar la oportunidad (y sus datos relacionados: organización, responsable) en tiempo real.
// - Verificar permisos de acceso (el ejecutivo solo ve las suyas).
// - Calcular qué estados puede asumir el usuario (estadosPermitidos vía Permisos.kt).
// - Permitir cambiar el estado (con verificación de permisos + registro de historial).
// - Registrar y eliminar evidencias fotográficas.
// - Emitir mensajes de éxito/error para que la Screen los muestre en un Snackbar.
//
// Patrón MVVM:
// - Recibe 4 repositorios por inyección (desde AppViewModelProvider.Factory → AppContainer).
// - Expone un único StateFlow<DetalleUiState> que la Screen observa.
// - No conoce Composables ni navegación: solo actualiza el estado.
//
// ¿Por qué SavedStateHandle?
// Porque la ruta de navegación incluye un argumento "id" (ej: "detalle_oportunidad/3").
// Navigation Compose pasa ese argumento al ViewModel vía SavedStateHandle.
// El ViewModel lo extrae en el constructor para saber QUÉ oportunidad cargar.
// Si la pantalla se destruye y se recrea (rotación), SavedStateHandle conserva el id.

// Estado que la Screen observa. Contiene todos los datos que el detalle necesita mostrar.
data class DetalleUiState(
    // La oportunidad a mostrar (null = no encontrada o aún cargando)
    val oportunidad: Oportunidad? = null,
    // Nombre de la organización (resuelto desde el FK idOrganizacion)
    val organizacion: String = "",
    // Nombre del responsable (resuelto desde el FK idResponsable; "Sin asignar" si es null)
    val responsable: String = "",
    // Estados a los que el usuario PUEDE avanzar (calculado con Permisos.puedeCambiarEstado)
    // Ej: ejecutivo en "Identificada" → ["Contacto realizado"]
    //     supervisor en "En espera" → ["Confirmada", "Descartada"]
    val estadosPermitidos: List<String> = emptyList(),
    // Fotos asociadas a esta oportunidad (se actualiza en tiempo real vía Flow)
    val evidencias: List<Evidencia> = emptyList(),
    // true si el usuario no tiene permiso para ver esta oportunidad (ejecutivo viendo la de otro)
    val sinAcceso: Boolean = false,
    // Mensaje de éxito temporal (ej: "Estado actualizado a: Contacto realizado")
    // La Screen lo muestra en un Snackbar y luego llama a limpiarMensajes().
    val mensajeExito: String? = null,
    // Mensaje de error temporal (ej: "No tienes permiso para este cambio de estado")
    val mensajeError: String? = null,
    // true mientras se carga la información inicial (muestra CircularProgressIndicator)
    val isLoading: Boolean = true
)

// Data class interna para manejar los mensajes de éxito/error de forma atómica.
// Se usa un MutableStateFlow<Mensajes> en vez de 2 StateFlows separados
// para que el combine() los trate como una sola fuente (evita actualizaciones parciales).
private data class Mensajes(val exito: String? = null, val error: String? = null)

class DetalleOportunidadViewModel(
    // SavedStateHandle: contiene los argumentos de la ruta de navegación.
    // En este caso: "id" (el id de la oportunidad a mostrar).
    // Navigation Compose lo inyecta automáticamente cuando se usa viewModel() en un composable
    // que tiene argumentos definidos en la ruta.
    savedStateHandle: SavedStateHandle,

    // Repositorio de oportunidades: para observar la oportunidad, cambiar estado y eliminar.
    private val oportunidadRepository: OportunidadRepository,

    // Repositorio de organizaciones: para resolver el nombre de la organización (FK → nombre).
    // No es private porque se usa dentro del map{} (fuera del alcance de las funciones del VM).
    organizacionRepository: OrganizacionRepository,

    // Repositorio de autenticación: para resolver el nombre del responsable (FK → nombre).
    authRepository: AuthRepository,

    // Repositorio de evidencias: para observar, registrar y eliminar fotos.
    private val evidenciaRepository: EvidenciaRepository
) : ViewModel() {

    // El id de la oportunidad a mostrar. Se extrae de la ruta de navegación.
    // checkNotNull: si por algún bug el argumento "id" no existe, lanza una excepción
    // (es mejor crashear aquí que mostrar una pantalla vacía sin saber por qué).
    private val idOportunidad: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    // Estado mutable de los mensajes (éxito/error).
    // Es privado: solo el ViewModel lo modifica. La Screen lo ve a través del uiState combinado.
    private val _mensajes = MutableStateFlow(Mensajes())

    // Flow principal: observa la oportunidad en tiempo real y calcula todos los datos del detalle.
    //
    // ¿Cómo funciona?
    // 1. oportunidadRepository.observarPorId(idOportunidad) → Flow<Oportunidad?>
    //    Se actualiza solo cuando la oportunidad cambia en Room (ej: el supervisor cambia el estado).
    //
    // 2. .map { op -> ... } → transforma cada emisión en un DetalleUiState completo:
    //    - Si op == null → oportunidad eliminada → DetalleUiState(isLoading = false)
    //    - Si no hay sesión o no tiene permiso → DetalleUiState(sinAcceso = true)
    //    - Si tiene acceso → DetalleUiState completo con:
    //      * oportunidad: la oportunidad actual
    //      * organizacion: nombre resuelto desde el FK (consulta puntual al repo)
    //      * responsable: nombre resuelto desde el FK (o "Sin asignar" si es null)
    //      * estadosPermitidos: lista de estados a los que puede avanzar (Permisos.kt)
    //      * isLoading = false (ya hay datos)
    //
    // ¿Por qué las consultas de organizacion y responsable son suspend (no Flow)?
    // Porque son datos "estáticos" (no cambian mientras la oportunidad existe).
    // No necesitan ser reactivos: si la organización se renombra, no es crítico
    // que el detalle se actualice al instante. Se resuelven una vez por cada emisión del Flow.
    private val datos = oportunidadRepository.observarPorId(idOportunidad).map { op ->
        // Caso 1: la oportunidad no existe (se eliminó o el id es inválido)
        if (op == null) return@map DetalleUiState(isLoading = false)

        // Verificar permisos de acceso:
        // - ¿Hay usuario logueado? (no debería pasar, pero es una guarda de seguridad)
        // - ¿El usuario puede ver esta oportunidad? (Permisos.puedeVerOportunidad)
        //   - Supervisor → siempre true
        //   - Ejecutivo → solo si idResponsable == su idUsuario
        val usuario = SesionManager.usuarioActual
        if (usuario == null ||
            !Permisos.puedeVerOportunidad(usuario.rol, usuario.idUsuario, op.idResponsable)
        ) {
            // Sin permiso: se muestra el mensaje de "No tienes permiso" en la Screen
            return@map DetalleUiState(sinAcceso = true, isLoading = false)
        }

        // Caso normal: construir el estado completo
        DetalleUiState(
            oportunidad = op,
            // Resolver el nombre de la organización desde el FK.
            // obtenerPorId es suspend (consulta puntual a Room).
            // Si no existe (no debería pasar por la FK CASCADE) → "Sin organización".
            organizacion = organizacionRepository.obtenerPorId(op.idOrganizacion)?.nombre
                ?: "Sin organización",
            // Resolver el nombre del responsable desde el FK.
            // idResponsable es nullable: si es null → "Sin asignar" (la oportunidad no tiene dueño).
            responsable = op.idResponsable
                ?.let { authRepository.obtenerUsuarioPorId(it)?.nombreCompleto }
                ?: "Sin asignar",
            // Calcular qué estados puede asumir el usuario desde el estado actual.
            // Itera TODOS los estados y filtra los que Permisos.puedeCambiarEstado() permite.
            // Ej: ejecutivo en "Identificada" → solo "Contacto realizado" pasa el filtro.
            //     supervisor en "En espera" → "Confirmada" y "Descartada" pasan el filtro.
            //     cualquiera en "Confirmada" → ninguno pasa (estado final).
            estadosPermitidos = Estados.todos.filter {
                Permisos.puedeCambiarEstado(usuario.rol, op.estado, it)
            },
            isLoading = false
        )
    }

    // El estado final que la Screen observa.
    // Combina 3 fuentes:
    // 1. datos → la oportunidad + organización + responsable + estadosPermitidos
    // 2. evidenciaRepository.observarPorOportunidad(idOportunidad) → las fotos (Flow reactivo)
    // 3. _mensajes → los mensajes de éxito/error (transitorios)
    //
    // combine(): se re-ejecuta cuando CUALQUIERA de las 3 fuentes emite.
    // - Si la oportunidad cambia (el supervisor avanza el estado) → datos emite → se recalcula.
    // - Si se agrega/elimina una foto → evidencias emite → se actualiza la lista.
    // - Si hay un mensaje nuevo → _mensajes emite → la Screen muestra el Snackbar.
    //
    // stateIn(WhileSubscribed(5_000)): se cancela 5 segundos después de que la Screen se va.
    val uiState: StateFlow<DetalleUiState> = combine(
        datos,
        evidenciaRepository.observarPorOportunidad(idOportunidad),
        _mensajes
    ) { d, evidencias, m ->
        // Toma el estado de datos y le agrega las evidencias y los mensajes
        d.copy(evidencias = evidencias, mensajeExito = m.exito, mensajeError = m.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    // Cambia el estado de la oportunidad.
    // Se llama desde DetalleOportunidadScreen cuando el usuario presiona un botón de estado.
    //
    // Flujo:
    // 1. Verificar permisos (doble seguridad: la Screen ya no muestra botones no permitidos,
    //    pero el ViewModel lo verifica de nuevo por si acaso).
    // 2. Si no tiene permiso → mensaje de error.
    // 3. Si tiene permiso → llamar a oportunidadRepository.cambiarEstado()
    //    (que actualiza Room Y registra el historial en una transacción).
    // 4. Si exito → mensaje de éxito.
    // 5. Si falla (excepción de Room) → mensaje de error.
    fun cambiarEstado(nuevoEstado: String) {
        // Guarda de seguridad: si no hay usuario o no hay oportunidad, no se hace nada.
        val usuario = SesionManager.usuarioActual ?: return
        val actual = uiState.value.oportunidad ?: return

        // Verificación de permisos (doble seguridad con Permisos.kt).
        // Si el rol no permite esta transición → se muestra el error y se sale.
        if (!Permisos.puedeCambiarEstado(usuario.rol, actual.estado, nuevoEstado)) {
            _mensajes.value = Mensajes(error = "No tienes permiso para este cambio de estado")
            return
        }

        // Ejecutar el cambio en un hilo de fondo (suspend).
        viewModelScope.launch {
            try {
                // Llama al repositorio que hace la transacción atómica:
                // 1. UPDATE oportunidades SET estado = nuevoEstado WHERE id = ...
                // 2. INSERT INTO seguimientos (tipoAccion = "Cambio de estado", ...)
                oportunidadRepository.cambiarEstado(
                    idOportunidad = actual.idOportunidad,
                    estadoAnterior = actual.estado,
                    nuevoEstado = nuevoEstado,
                    idUsuario = usuario.idUsuario
                )
                // Éxito: se muestra el mensaje en el Snackbar de la Screen.
                // El Flow de datos se re-ejecuta automáticamente (Room emite el cambio),
                // así la UI se actualiza sola (nuevo estado, nuevos estadosPermitidos).
                _mensajes.value = Mensajes(exito = "Estado actualizado a $nuevoEstado")
            } catch (e: Exception) {
                // Error de Room (raro, pero posible si la base está corrupta).
                _mensajes.value = Mensajes(error = "No se pudo actualizar: ${e.localizedMessage}")
            }
        }
    }

    // Registra una evidencia fotográfica nueva en Room, asociada a esta oportunidad.
    // Se llama desde SeccionEvidencia cuando la cámara devuelve una foto válida.
    //
    // Parámetro:
    // - rutaArchivo: la ruta relativa del archivo (ej: "evidencias/evidencia_1761504000000.jpg").
    //   El archivo ya fue copiado a filesDir por la pantalla de cámara (SeccionEvidencia).
    //   Aquí solo se guarda la referencia en Room.
    fun registrarEvidencia(rutaArchivo: String) {
        viewModelScope.launch {
            try {
                // Crea la entidad Evidencia con:
                // - rutaArchivo: la ruta del archivo en filesDir
                // - fecha: timestamp actual (cuándo se tomó la foto)
                // - idOportunidad: la oportunidad a la que se asocia
                // - idEvidencia: no se pasa (Room lo genera con autoGenerate = true)
                evidenciaRepository.registrar(
                    Evidencia(
                        rutaArchivo = rutaArchivo,
                        fecha = System.currentTimeMillis(),
                        idOportunidad = idOportunidad
                    )
                )
                // Éxito: mensaje en el Snackbar.
                // El Flow de evidencias se re-ejecuta (Room emite) → la galería se actualiza sola.
                _mensajes.value = Mensajes(exito = "Foto guardada en la oportunidad")
            } catch (e: Exception) {
                _mensajes.value = Mensajes(error = "No se pudo guardar la foto: ${e.localizedMessage}")
            }
        }
    }

    // Elimina una evidencia fotográfica: borra el registro de Room Y el archivo del disco.
    // Se llama desde SeccionEvidencia cuando el usuario confirma la eliminación.
    //
    // Parámetro:
    // - evidencia: el objeto Evidencia completo (necesita el idEvidencia para borrar de Room
    //   y el rutaArchivo para borrar el archivo físico).
    fun eliminarEvidencia(evidencia: Evidencia) {
        viewModelScope.launch {
            try {
                // EvidenciaRepository.eliminar() hace 2 cosas:
                // 1. dao.eliminarPorId(evidencia.idEvidencia) → borra la fila de Room
                // 2. File(filesDir, evidencia.rutaArchivo).delete() → borra el archivo del disco
                evidenciaRepository.eliminar(evidencia)
                // Éxito: mensaje en el Snackbar.
                // El Flow de evidencias se re-ejecuta → la galería se actualiza sola.
                _mensajes.value = Mensajes(exito = "Foto eliminada")
            } catch (e: Exception) {
                _mensajes.value = Mensajes(error = "No se pudo eliminar la foto: ${e.localizedMessage}")
            }
        }
    }

    // Limpia los mensajes de éxito/error (los pone a null).
    // Se llama desde la Screen DESPUÉS de mostrar el Snackbar.
    // Sin esto, el LaunchedEffect de la Screen no se re-ejecutaría el próximo vez
    // (porque el key del LaunchedEffect no cambiaría: seguiría siendo el mismo mensaje).
    fun limpiarMensajes() {
        _mensajes.value = Mensajes()  // Mensajes(exito = null, error = null)
    }
}