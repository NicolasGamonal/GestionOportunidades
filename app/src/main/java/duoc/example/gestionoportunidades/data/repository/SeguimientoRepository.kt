package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.*
import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.model.GestionOrganizacion
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SeguimientoRepository(private val api: ApiService = RetrofitClient.apiService) {

    private val zonaHoraria = ZoneId.systemDefault()

    // --- Helpers de Fecha ---
    private fun String?.toDateTimeMillis(): Long {
        if (this.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            LocalDateTime.parse(this.take(19))
                .atZone(zonaHoraria)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            try {
                LocalDate.parse(this.take(10))
                    .atStartOfDay(zonaHoraria)
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun String?.toDateMillis(): Long {
        if (this.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            LocalDate.parse(this.take(10))
                .atStartOfDay(zonaHoraria)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun Long.toIsoDate(): String {
        return Instant.ofEpochMilli(this)
            .atZone(zonaHoraria)
            .toLocalDate()
            .toString()
    }

    // --- Conversión Remote -> Local Model ---
    private fun SeguimientoRemote.toLocalModel(): Seguimiento {
        return Seguimiento(
            idSeguimiento = this.id ?: 0L,
            idOportunidad = this.oportunidad.id,
            idUsuario = this.usuario.id,
            tipoAccion = this.tipoAccion,
            fechaHora = this.fechaHora.toDateTimeMillis(),
            resultado = this.resultado,
            proximaAccion = this.proximaAccion ?: "",
            fechaProximo = this.fechaProximoSeguimiento.toDateMillis(),
            idContacto = null
        )
    }

    private fun Seguimiento.toRemoteModel(incluirId: Boolean = false): SeguimientoRemote {
        return SeguimientoRemote(
            id = if (incluirId && this.idSeguimiento > 0) this.idSeguimiento else null,
            tipoAccion = this.tipoAccion,
            fechaHora = null, // El backend asigna LocalDateTime.now() si es null
            resultado = this.resultado,
            proximaAccion = this.proximaAccion,
            fechaProximoSeguimiento = this.fechaProximo.toIsoDate(),
            observaciones = "",
            oportunidad = OportunidadRef(id = this.idOportunidad),
            usuario = UsuarioRef(id = this.idUsuario)
        )
    }

    // =========================================================================
    // CONSULTAS Y OPERACIONES
    // =========================================================================

    fun obtenerHistorial(idOportunidad: Long): Flow<List<Seguimiento>> = flow {
        try {
            val lista = api.listarSeguimientos(idOportunidad)
                .map { it.toLocalModel() }
                .sortedByDescending { it.fechaHora }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun obtenerAtrasados(fechaActual: Long): Flow<List<Seguimiento>> = flow {
        try {
            val atrasados = api.listarTodosSeguimientos()
                .map { it.toLocalModel() }
                .filter { !it.tipoAccion.equals("Cambio de estado", ignoreCase = true) && it.fechaProximo < fechaActual }
                .sortedBy { it.fechaProximo }
            emit(atrasados)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun obtenerPendientesPorUsuario(idUsuario: Long, fechaActual: Long): Flow<List<Seguimiento>> = flow {
        try {
            val pendientes = api.listarTodosSeguimientos()
                .map { it.toLocalModel() }
                .filter { seg ->
                    (idUsuario == 0L || seg.idUsuario == idUsuario) &&
                            !seg.tipoAccion.equals("Cambio de estado", ignoreCase = true) &&
                            seg.fechaProximo >= fechaActual
                }
                .sortedBy { it.fechaProximo }
            emit(pendientes)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun obtenerPorUsuario(idUsuario: Long): Flow<List<Seguimiento>> = flow {
        try {
            val lista = api.listarTodosSeguimientos()
                .map { it.toLocalModel() }
                .filter { it.idUsuario == idUsuario }
                .sortedByDescending { it.fechaHora }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun insertar(seguimiento: Seguimiento): Long {
        return try {
            val response = api.crearSeguimiento(seguimiento.toRemoteModel(incluirId = false))
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

    suspend fun actualizar(seguimiento: Seguimiento) {
        try {
            api.actualizarSeguimiento(
                id = seguimiento.idSeguimiento,
                seg = seguimiento.toRemoteModel(incluirId = true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // No atrapa la excepción: si la red falla, el Flow falla y DashboardViewModel la muestra como error.
    fun obtenerUltimasGestiones(idResponsable: Long): Flow<List<AlertaSeguimiento>> = flow {
        val filtroUser = if (idResponsable == 0L) null else idResponsable
        val oportunidades = api.listarOportunidades(filtroUser)
            .filter { !it.estado.equals("Descartada", ignoreCase = true) }

        val alertas = oportunidades.mapNotNull { op ->
            val opId = op.id ?: return@mapNotNull null
            val ultima = api.listarSeguimientos(opId)
                .filter { !it.tipoAccion.equals("Cambio de estado", ignoreCase = true) }
                .maxByOrNull { it.fechaHora.toDateTimeMillis() }

            ultima?.let {
                AlertaSeguimiento(
                    idOportunidad = opId,
                    nombreOportunidad = op.nombre,
                    nombreResponsable = op.responsable?.nombre,
                    tipoAccion = it.tipoAccion,
                    proximaAccion = it.proximaAccion ?: "",
                    fechaProximo = it.fechaProximoSeguimiento.toDateMillis()
                )
            }
        }
        emit(alertas)
    }

    fun obtenerHistorialPorOrganizacion(idOrganizacion: Long, idResponsable: Long): Flow<List<GestionOrganizacion>> = flow {
        try {
            val filtroUser = if (idResponsable == 0L) null else idResponsable
            val usuariosMap = api.listarUsuarios().associateBy { it.id }
            val opsOrg = api.listarOportunidades(filtroUser)
                .filter { it.organizacion.id == idOrganizacion }

            val historial = opsOrg.flatMap { op ->
                val opId = op.id ?: return@flatMap emptyList()
                api.listarSeguimientos(opId).map { seg ->
                    GestionOrganizacion(
                        idSeguimiento = seg.id ?: 0L,
                        fechaHora = seg.fechaHora.toDateTimeMillis(),
                        tipoAccion = seg.tipoAccion,
                        resultado = seg.resultado,
                        idOportunidad = opId,
                        nombreOportunidad = op.nombre,
                        autor = usuariosMap[seg.usuario.id]?.nombre ?: "Usuario #${seg.usuario.id}",
                        nombreContacto = null
                    )
                }
            }.sortedByDescending { it.fechaHora }

            emit(historial)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
