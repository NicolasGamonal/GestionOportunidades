package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.ContactoRemote
import duoc.example.gestionoportunidades.data.remote.OportunidadRef
import duoc.example.gestionoportunidades.data.remote.OportunidadRemote
import duoc.example.gestionoportunidades.data.remote.OrganizacionRef
import duoc.example.gestionoportunidades.data.remote.OrganizacionRemote
import duoc.example.gestionoportunidades.data.remote.RetrofitClient
import duoc.example.gestionoportunidades.data.remote.SeguimientoRemote
import duoc.example.gestionoportunidades.data.remote.UsuarioRef
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.ConteoEstado
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.util.Regiones
import duoc.example.gestionoportunidades.util.comoHoraOrNull
import duoc.example.gestionoportunidades.util.horaComoTexto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class OportunidadRepository(private val api: ApiService = RetrofitClient.apiService) {

    private val zonaHoraria = ZoneId.systemDefault()

    // Región que se envía cuando la comuna no está en el catálogo (valor histórico de la app).
    private val regionPorDefecto = "Metropolitana"

    // La región se deduce de la comuna del evento (antes se enviaba siempre "Metropolitana",
    // así que un evento de Valparaíso quedaba guardado como de la Región Metropolitana).
    private fun regionDeLaComuna(comuna: String): String = Regiones.regionDe(comuna) ?: regionPorDefecto

    // =========================================================================
    // 1. HELPERS DE CONVERSIÓN (Fechas y Modelos)
    // =========================================================================

    // "yyyy-MM-dd" (se leen solo los primeros 10 caracteres) -> milisegundos.
    // Con hora == null devuelve la medianoche del día; con hora, ese instante del día.
    // Si la fecha falta o no se puede leer, devuelve el momento actual.
    private fun String?.toMillis(hora: LocalTime? = null): Long {
        if (this.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val fecha = LocalDate.parse(this.take(10))
            val inicio = if (hora == null) fecha.atStartOfDay(zonaHoraria) else fecha.atTime(hora).atZone(zonaHoraria)
            inicio.toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    // "HH:mm - HH:mm" (hora de inicio y de término) -> par de horas; null si falta o no se puede leer.
    // El servidor solo guarda la fecha del evento y este texto, así que la hora viaja aquí.
    private fun String?.toHorario(): Pair<LocalTime, LocalTime>? {
        if (this.isNullOrBlank()) return null
        val partes = this.split("-").map { it.trim() }
        if (partes.size != 2) return null
        val inicio = partes[0].comoHoraOrNull() ?: return null
        val termino = partes[1].comoHoraOrNull() ?: return null
        return inicio to termino
    }

    // Milisegundos -> "HH:mm" en la zona horaria de la app.
    private fun Long.toHora(): String {
        val hora = Instant.ofEpochMilli(this).atZone(zonaHoraria).toLocalTime()
        return horaComoTexto(hora.hour, hora.minute)
    }

    private fun Long.toIsoDate(): String {
        return Instant.ofEpochMilli(this)
            .atZone(zonaHoraria)
            .toLocalDate()
            .toString()
    }

    private fun OportunidadRemote.toLocalModel(): Oportunidad {
        // Se recupera la hora del evento desde horarioEvento; si falta o es ilegible, queda a medianoche.
        val horario = this.horarioEvento.toHorario()
        return Oportunidad(
            idOportunidad = this.id ?: 0L,
            nombre = this.nombre,
            tipoEvento = this.tipo,
            fechaDeteccion = this.fechaDeteccion.toMillis(),
            fechaInicio = this.fechaEvento.toMillis(horario?.first),
            fechaTermino = (this.fechaTermino ?: this.fechaEvento).toMillis(horario?.second),
            comuna = this.comuna ?: "",
            publicoEsperado = this.publicoEsperado ?: 0,
            prioridad = this.prioridad,
            estado = this.estado,
            costoReferencial = this.costoReferencial ?: 0.0,
            observaciones = this.observaciones ?: "",
            idOrganizacion = this.organizacion.id ?: 0L,
            idResponsable = this.responsable?.id,
            descripcion = this.descripcion ?: "",
            fuenteInformacion = this.fuenteInformacion ?: "",
            potencialComercial = this.potencialComercial ?: "Medio",
            afinidad = this.afinidad ?: "Media",
            publicoObjetivo = this.publicoObjetivo ?: "",
            condicionesParticipacion = this.condicionesParticipacion ?: "",
            requisitos = this.requisitos ?: "",
            espaciosDisponibles = this.espaciosDisponibles ?: 0
        )
    }

    private fun Oportunidad.toPayloadMap(): Map<String, Any?> {
        return mapOf(
            "nombre" to this.nombre,
            "tipo" to this.tipoEvento,
            "descripcion" to this.descripcion,
            "fuenteInformacion" to this.fuenteInformacion,
            "fechaDeteccion" to this.fechaDeteccion.toIsoDate(),
            "fechaEvento" to this.fechaInicio.toIsoDate(),
            "fechaTermino" to this.fechaTermino.toIsoDate(),
            "horarioEvento" to "${this.fechaInicio.toHora()} - ${this.fechaTermino.toHora()}",
            "direccion" to this.comuna,
            "comuna" to this.comuna,
            "region" to regionDeLaComuna(this.comuna),
            "publicoObjetivo" to this.publicoObjetivo,
            "publicoEsperado" to this.publicoEsperado,
            "costoReferencial" to this.costoReferencial,
            "prioridad" to this.prioridad,
            "potencialComercial" to this.potencialComercial,
            "afinidad" to this.afinidad,
            "estado" to this.estado,
            "condicionesParticipacion" to this.condicionesParticipacion,
            "requisitos" to this.requisitos,
            "espaciosDisponibles" to this.espaciosDisponibles,
            "observaciones" to this.observaciones,
            "organizacion" to mapOf("id" to this.idOrganizacion),
            "responsable" to this.idResponsable?.let { mapOf("id" to it) }
        )
    }

    // =========================================================================
    // 2. MÓDULO OPORTUNIDADES (CRUD y Consultas)
    // =========================================================================

    suspend fun guardarOportunidad(oportunidad: Oportunidad): Long {
        return try {
            val response = api.crearOportunidad(oportunidad.toPayloadMap())
            if (response.isSuccessful && response.body()?.id != null) {
                response.body()!!.id!!
            } else {
                0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    // Devuelve true solo si la API aceptó el cambio; false si respondió error o no hubo red.
    suspend fun modificarOportunidad(oportunidad: Oportunidad): Boolean {
        return try {
            val actorId = oportunidad.idResponsable ?: 1L
            api.actualizarOportunidad(
                id = oportunidad.idOportunidad,
                usuarioId = actorId,
                oportunidad = oportunidad.toPayloadMap()
            ).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun obtenerPorId(id: Long): Oportunidad? {
        return try {
            api.obtenerOportunidad(id).toLocalModel()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun observarPorId(id: Long): Flow<Oportunidad?> = flow {
        emit(obtenerPorId(id))
    }

    fun obtenerTodasLasOportunidades(busqueda: String): Flow<List<Oportunidad>> = flow {
        try {
            val lista = api.listarOportunidades(null)
                .map { it.toLocalModel() }
                .filter { busqueda.isBlank() || it.nombre.contains(busqueda.trim(), ignoreCase = true) }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun filtrarOportunidades(estado: String, prioridad: String, busqueda: String): Flow<List<Oportunidad>> = flow {
        try {
            val lista = api.listarOportunidades(null)
                .map { it.toLocalModel() }
                .filter { op ->
                    (estado.isBlank() || op.estado.equals(estado, ignoreCase = true)) &&
                            (prioridad.isBlank() || op.prioridad.equals(prioridad, ignoreCase = true)) &&
                            (busqueda.isBlank() || op.nombre.contains(busqueda.trim(), ignoreCase = true))
                }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun obtenerPorResponsable(idUsuario: Long): Flow<List<Oportunidad>> = flow {
        try {
            val lista = api.listarOportunidades(idUsuario).map { it.toLocalModel() }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    // Las 5 consultas del dashboard NO atrapan la excepción: si la red falla, el Flow falla y
    // DashboardViewModel la captura para mostrar un error (antes emitían lista vacía y se veía "sin datos").
    fun obtenerEventosProximos(desde: Long, idResponsable: Long): Flow<List<Oportunidad>> = flow {
        val filtroUser = if (idResponsable == 0L) null else idResponsable
        val lista = api.listarOportunidades(filtroUser)
            .map { it.toLocalModel() }
            .filter { it.fechaInicio >= desde && !it.estado.equals("Descartada", ignoreCase = true) }
            .sortedBy { it.fechaInicio }
        emit(lista)
    }

    fun buscar(estado: String, prioridad: String, busqueda: String, idResponsable: Long): Flow<List<Oportunidad>> = flow {
        try {
            val filtroUser = if (idResponsable == 0L) null else idResponsable
            val lista = api.listarOportunidades(filtroUser)
                .map { it.toLocalModel() }
                .filter { op ->
                    (estado.isBlank() || op.estado.equals(estado, ignoreCase = true)) &&
                            (prioridad.isBlank() || op.prioridad.equals(prioridad, ignoreCase = true)) &&
                            (busqueda.isBlank() || op.nombre.contains(busqueda.trim(), ignoreCase = true))
                }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    // =========================================================================
    // 3. MÓDULO ORGANIZACIONES Y CONTACTOS (Compatibilidad con ViewModels)
    // =========================================================================

    suspend fun guardarOrganizacion(organizacion: Organizacion): Long {
        return try {
            val remote = OrganizacionRemote(
                nombre = organizacion.nombre,
                tipo = organizacion.tipo,
                rubro = organizacion.rubro,
                comuna = organizacion.comuna,
                region = regionDeLaComuna(organizacion.comuna),
                sitioWeb = "",
                publicoAsociado = "",
                observaciones = ""
            )
            val res = api.crearOrganizacion(remote)
            if (res.isSuccessful) res.body()?.id ?: 0L else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun obtenerOrganizaciones(): Flow<List<Organizacion>> = flow {
        try {
            val lista = api.listarOrganizaciones().map {
                Organizacion(
                    idOrganizacion = it.id ?: 0L,
                    nombre = it.nombre,
                    tipo = it.tipo,
                    rubro = it.rubro ?: "",
                    comuna = it.comuna ?: ""
                )
            }
            emit(lista)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun guardarContacto(contacto: Contacto): Long {
        return try {
            val remote = ContactoRemote(
                nombre = contacto.nombre,
                cargo = contacto.cargo,
                area = contacto.area,
                correo = contacto.correo,
                telefono = contacto.telefono,
                medioPreferente = contacto.medioPreferente,
                estado = "Activo",
                observaciones = "",
                organizacion = OrganizacionRef(contacto.idOrganizacion)
            )
            val res = api.crearContacto(remote)
            if (res.isSuccessful) res.body()?.id ?: 0L else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun obtenerContactosPorOrganizacion(idOrg: Long): Flow<List<Contacto>> = flow {
        try {
            val lista = api.listarContactos(idOrg).map {
                Contacto(
                    idContacto = it.id ?: 0L,
                    nombre = it.nombre,
                    cargo = it.cargo ?: "",
                    area = it.area ?: "",
                    correo = it.correo,
                    telefono = it.telefono,
                    idOrganizacion = it.organizacion.id,
                    medioPreferente = it.medioPreferente ?: "Correo"
                )
            }
            emit(lista)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // =========================================================================
    // 4. ALERTAS, INDICADORES Y CAMBIOS DE ESTADO
    // =========================================================================

    fun obtenerSinGestiones(idResponsable: Long): Flow<List<Oportunidad>> = flow {
        val filtroUser = if (idResponsable == 0L) null else idResponsable
        val ops = api.listarOportunidades(filtroUser).map { it.toLocalModel() }
        val sinGestiones = ops.filter { op ->
            api.listarSeguimientos(op.idOportunidad).isEmpty()
        }
        emit(sinGestiones)
    }

    fun obtenerSinContacto(idResponsable: Long): Flow<List<Oportunidad>> = flow {
        val filtroUser = if (idResponsable == 0L) null else idResponsable
        val ops = api.listarOportunidades(filtroUser).map { it.toLocalModel() }
        val sinContacto = ops.filter { op ->
            api.listarContactos(op.idOrganizacion).isEmpty()
        }
        emit(sinContacto)
    }

    fun contarPorEstado(idResponsable: Long): Flow<List<ConteoEstado>> = flow {
        val filtroUser = if (idResponsable == 0L) null else idResponsable
        val conteos = api.listarOportunidades(filtroUser)
            .groupBy { it.estado }
            .map { (estado, lista) -> ConteoEstado(estado = estado, cantidad = lista.size) }
        emit(conteos)
    }

    suspend fun actualizarEstado(id: Long, estado: String) {
        try {
            val actual = obtenerPorId(id) ?: return
            val payload = actual.copy(estado = estado).toPayloadMap()
            api.actualizarOportunidad(id, actual.idResponsable ?: 1L, payload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun cambiarEstado(
        idOportunidad: Long,
        estadoAnterior: String,
        nuevoEstado: String,
        idUsuario: Long
    ) {
        val actual = obtenerPorId(idOportunidad) ?: throw Exception("Oportunidad no encontrada")
        val payload = actual.copy(estado = nuevoEstado).toPayloadMap()
        val response = api.actualizarOportunidad(idOportunidad, idUsuario, payload)
        if (!response.isSuccessful) {
            val raw = response.errorBody()?.string() ?: "No se pudo cambiar el estado"
            val msg = try { JSONObject(raw).optString("mensaje", raw) } catch (_: Exception) { raw }
            throw Exception(msg)
        }
        // Registrar el cambio en el historial (Seguimiento)
        api.crearSeguimiento(
            SeguimientoRemote(
                tipoAccion = "Cambio de estado",
                resultado = "$estadoAnterior → $nuevoEstado",
                proximaAccion = "",
                fechaProximoSeguimiento = LocalDate.now().toString(),
                observaciones = "",
                oportunidad = OportunidadRef(idOportunidad),
                usuario = UsuarioRef(idUsuario)
            )
        )
    }

    suspend fun eliminar(id: Long): String? {
        return try {
            val oportunidad = obtenerPorId(id) ?: return "La oportunidad ya no existe."
            if (oportunidad.estado != "Identificada") {
                return "Solo se pueden eliminar oportunidades en estado Identificada. Las demás se descartan para conservar su historial."
            }
            if (api.listarSeguimientos(id).isNotEmpty()) {
                return "Tiene gestiones registradas; no se puede eliminar para conservar el historial."
            }
            if (api.listarEvidencias(id).isNotEmpty()) {
                return "Tiene fotos de evidencia; elimínalas primero desde el detalle."
            }
            val response = api.eliminarOportunidad(id)
            if (response.isSuccessful) null else "No se pudo eliminar la oportunidad."
        } catch (e: Exception) {
            "Error de conexión: ${e.message}"
        }
    }
}