package duoc.example.gestionoportunidades.data.repository

import androidx.room.withTransaction
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

// Repositorio de oportunidades. Es el más grande del proyecto porque agrupa
// la lógica de 3 módulos: Oportunidades, Organizaciones y Contactos.
// Esto se debe a que el ViewModel de organizaciones/contactos (OrgContactosViewModel)
// también necesita crear oportunidades y contactos en el mismo flujo.
//
// Capa intermedia entre los ViewModels y los DAOs.
// El ViewModel nunca llama directo a un DAO; siempre pasa por este repositorio.
// Aquí se centraliza la lógica de negocio que involucra múltiples tablas
// (por ejemplo, cambiar estado + registrar historial en una sola transacción).
class OportunidadRepository(private val db: AppDatabase) {

    // Referencias a los 4 DAOs que este repositorio necesita.
    // Se obtienen una sola vez al construir el repositorio.
    private val oportunidadDao = db.oportunidadDao()
    private val organizacionDao = db.organizacionDao()
    private val contactoDao = db.contactoDao()
    private val seguimientoDao = db.seguimientoDao()

    // --- Módulo Oportunidades ---

    // Guarda una oportunidad nueva (o la sobrescribe si el id ya existe, por REPLACE).
    // Se llama desde el formulario de crear/editar oportunidad.
    // Devuelve el id asignado por Room.
    suspend fun guardarOportunidad(oportunidad: Oportunidad): Long {
        return oportunidadDao.insertarOportunidad(oportunidad)
    }

    // Actualiza una oportunidad existente (por su id).
    // Se usa al editar campos desde el formulario sin cambiar el estado.
    suspend fun modificarOportunidad(oportunidad: Oportunidad) {
        oportunidadDao.actualizarOportunidad(oportunidad)
    }

    // Busca una oportunidad específica por su id (consulta puntual, no reactiva).
    // Se usa al abrir el detalle desde la lista.
    // Devuelve la oportunidad o null si no existe.
    suspend fun obtenerPorId(id: Long): Oportunidad? {
        return oportunidadDao.obtenerPorId(id)
    }

    // Devuelve todas las oportunidades filtradas por nombre (búsqueda parcial).
    // Si busqueda es "" (vacío), el LIKE '%'' || '' || ''% coincide con todo → devuelve todas.
    // Se usa en la lista principal cuando el usuario no ha activado filtros específicos.
    fun obtenerTodasLasOportunidades(busqueda: String): Flow<List<Oportunidad>> {
        return oportunidadDao.buscarPorNombre(busqueda)
    }

    // Filtra por estado Y prioridad Y nombre a la vez (los 3 deben coincidir).
    // Se usa cuando el usuario activa los chips de filtro en la UI.
    // Diferente de obtenerTodasLasOportunidades: aquí los filtros son obligatorios.
    fun filtrarOportunidades(estado: String, prioridad: String, busqueda: String): Flow<List<Oportunidad>> {
        return oportunidadDao.filtrarOportunidades(estado, prioridad, busqueda)
    }

    // Devuelve solo las oportunidades asignadas a un usuario específico.
    // Se usa para el rol EJECUTIVO: solo ve las suyas.
    // El supervisor usa obtenerTodasLasOportunidades() o buscar() con idResponsable=0.
    fun obtenerPorResponsable(idUsuario: Long): Flow<List<Oportunidad>> {
        return oportunidadDao.obtenerPorResponsable(idUsuario)
    }

    // Devuelve oportunidades con fecha de inicio a partir de "desde" que no estén descartadas.
    // idResponsable = 0 → todas (supervisor); otro valor → solo de ese ejecutivo.
    // Se usa en el Dashboard para la sección "Eventos próximos".
    fun obtenerEventosProximos(desde: Long, idResponsable: Long): Flow<List<Oportunidad>> {
        return oportunidadDao.obtenerEventosProximos(desde, idResponsable)
    }

    // Cambia el estado de una oportunidad SIN registrar historial.
    // Se usa en contextos donde no se necesita guardar el "Cambio de estado"
    // (por ejemplo, en el seed o en migraciones).
    // Para el flujo normal de la app, usar cambiarEstado() que sí registra historial.
    suspend fun actualizarEstado(id: Long, estado: String) {
        oportunidadDao.actualizarEstado(id, estado)
    }

    // --- Módulo Organizaciones ---

    // Guarda una organización nueva (o la sobrescribe por REPLACE).
    // Se llama desde el formulario de crear/editar organización.
    // Devuelve el id asignado.
    suspend fun guardarOrganizacion(organizacion: Organizacion): Long {
        return organizacionDao.insertarOrganizacion(organizacion)
    }

    // Devuelve todas las organizaciones sin filtro.
    // Se usa en la lista de la pantalla de organizaciones.
    // Flow: se actualiza solo en la UI cuando se agrega o edita una organización.
    fun obtenerOrganizaciones(): Flow<List<Organizacion>> {
        return organizacionDao.obtenerTodas()
    }

    // --- Módulo Contactos ---

    // Guarda un contacto nuevo (o lo sobrescribe por REPLACE).
    // Se llama desde el formulario de crear/editar contacto.
    // Devuelve el id asignado.
    suspend fun guardarContacto(contacto: Contacto): Long {
        return contactoDao.insertarContacto(contacto)
    }

    // Devuelve los contactos de una organización específica.
    // Se usa en la pantalla de detalle de organización para mostrar su equipo.
    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>> {
        return contactoDao.obtenerContactosPorOrganizacion(idOrg)
    }

    // Búsqueda flexible: combina estado, prioridad, nombre y responsable.
    // Los parámetros vacíos ('') o 0 significan "no aplicar ese filtro".
    // Es la consulta principal de la lista de oportunidades con los filtros opcionales de la UI.
    // idResponsable = 0 → todas (supervisor); otro valor → solo de ese ejecutivo.
    fun buscar(estado: String, prioridad: String, busqueda: String, idResponsable: Long): Flow<List<Oportunidad>> =
        oportunidadDao.buscar(estado, prioridad, busqueda, idResponsable)

    // Observa una oportunidad en tiempo real (Flow).
    // Si el supervisor cambia el estado o reasigna, la UI del ejecutivo se actualiza sola.
    // Se usa en la pantalla de detalle para mantener los datos siempre frescos.
    fun observarPorId(id: Long): Flow<Oportunidad?> = oportunidadDao.observarPorId(id)

    // Cambia el estado de una oportunidad Y registra el historial en una sola transacción.
    //
    // ¿Por qué withTransaction?
    // Porque son 2 operaciones atómicas: actualizar el estado + insertar el seguimiento.
    // Si la primera falla, la segunda no se ejecuta (y viceversa).
    // Esto garantiza que nunca quede una oportunidad con estado cambiado pero sin historial,
    // o un seguimiento de "Cambio de estado" sin que el estado haya cambiado.
    //
    // ¿Por qué se registra como Seguimiento?
    // Porque el checklist pide "conservar el historial de gestiones".
    // Cada cambio de estado se guarda como tipoAccion = "Cambio de estado",
    // así el historial se consulta en un solo lugar (la tabla seguimientos).
    //
    // Parámetros:
    // - idOportunidad: la oportunidad a la que se le cambia el estado.
    // - estadoAnterior: para registrar en el resultado ("Identificada → Contacto realizado").
    // - nuevoEstado: el estado al que pasa.
    // - idUsuario: quién hizo el cambio (se guarda en el seguimiento).
    suspend fun cambiarEstado(
        idOportunidad: Long,
        estadoAnterior: String,
        nuevoEstado: String,
        idUsuario: Long
    ) {
        val ahora = System.currentTimeMillis()
        db.withTransaction {
            // Paso 1: actualizar el estado en la tabla oportunidades
            oportunidadDao.actualizarEstado(idOportunidad, nuevoEstado)
            // Paso 2: registrar el cambio en la tabla seguimientos (historial)
            seguimientoDao.insertarSeguimiento(
                Seguimiento(
                    idOportunidad = idOportunidad,
                    idUsuario = idUsuario,
                    tipoAccion = "Cambio de estado",
                    fechaHora = ahora,
                    resultado = "$estadoAnterior → $nuevoEstado",
                    proximaAccion = "",
                    fechaProximo = ahora
                )
            )
        }
    }

    // Elimina una oportunidad con reglas de negocio (no se puede borrar cualquier oportunidad).
    //
    // Reglas (en orden de prioridad):
    // 1. Si la oportunidad ya no existe → mensaje de error.
    // 2. Si el estado NO es "Identificada" → no se puede eliminar.
    //    Razón: las oportunidades que avanzaron de estado tienen historial de valor.
    //    La alternativa es descartarlas (que es una acción del supervisor).
    // 3. Si tiene seguimientos registrados → no se puede eliminar (conservar historial).
    // 4. Si tiene fotos de evidencia → no se puede eliminar (hay que borrarlas primero).
    // 5. Si pasa todas las reglas → se elimina y devuelve null (éxito).
    //
    // ¿Por qué withTransaction?
    // Porque leemos (obtenerPorId, contarPorOportunidad, contarPorOportunidad de evidencias)
    // y luego escribimos (eliminarPorId). La transacción garantiza consistencia.
    //
    // Devuelve String? : mensaje de error si no se pudo eliminar, o null si fue exitoso.
    // El ViewModel muestra el mensaje en un Snackbar si no es null.
    suspend fun eliminar(id: Long): String? = db.withTransaction {
        // Verificar que la oportunidad existe
        val oportunidad = oportunidadDao.obtenerPorId(id)
            ?: return@withTransaction "La oportunidad ya no existe."
        when {
            // Regla 1: solo se eliminan las que están en "Identificada" (aún no se trabajaron)
            oportunidad.estado != "Identificada" ->
                "Solo se pueden eliminar oportunidades en estado Identificada. Las demás se descartan para conservar su historial."
            // Regla 2: si tiene gestiones, no se borra
            seguimientoDao.contarPorOportunidad(id) > 0 ->
                "Tiene gestiones registradas; no se puede eliminar para conservar el historial."
            // Regla 3: si tiene fotos, hay que borrarlas primero desde la UI
            db.evidenciaDao().contarPorOportunidad(id) > 0 ->
                "Tiene fotos de evidencia; elimínalas primero desde el detalle."
            // Regla 4: pasa todas las verificaciones → se elimina
            else -> {
                oportunidadDao.eliminarPorId(id)
                null  // null = éxito
            }
        }
    }
}