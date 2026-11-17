package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.OportunidadRef
import duoc.example.gestionoportunidades.data.remote.ResultadoActivacionRemote
import duoc.example.gestionoportunidades.data.remote.RetrofitClient
import duoc.example.gestionoportunidades.model.ResultadoActivacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ResultadoActivacionRepository(db: AppDatabase) {

    private val api: ApiService = RetrofitClient.apiService
    private val zonaHoraria = ZoneId.systemDefault()

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

    private fun ResultadoActivacionRemote.toLocalModel(): ResultadoActivacion {
        val notaNumerica = this.evaluacionGeneral?.toIntOrNull() ?: 5
        return ResultadoActivacion(
            idResultado = this.id ?: 0L,
            idOportunidad = this.oportunidad.id,
            asistentes = this.asistentesReales ?: 0,
            contactosGenerados = this.contactosGenerados ?: 0,
            inscripciones = this.inscripcionesLogradas ?: 0,
            evaluacion = notaNumerica,
            aprendizajes = this.observaciones ?: "",
            fechaRegistro = this.fechaRealizacion.toDateMillis(),
            idUsuario = this.usuarioId ?: 1L
        )
    }

    fun observarPorOportunidad(idOportunidad: Long): Flow<ResultadoActivacion?> = flow {
        emit(obtenerPorOportunidad(idOportunidad))
    }

    suspend fun obtenerPorOportunidad(idOportunidad: Long): ResultadoActivacion? {
        return try {
            val response = api.obtenerResultado(idOportunidad)
            if (response.isSuccessful) {
                response.body()?.toLocalModel()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun guardar(resultado: ResultadoActivacion): Long {
        return try {
            val remote = ResultadoActivacionRemote(
                id = if (resultado.idResultado > 0) resultado.idResultado else null,
                fechaRealizacion = resultado.fechaRegistro.toIsoDate(),
                estadoFinal = "Realizada",
                asistentesReales = resultado.asistentes,
                contactosGenerados = resultado.contactosGenerados,
                personasInteresadas = resultado.contactosGenerados,
                inscripcionesLogradas = resultado.inscripciones,
                evaluacionGeneral = resultado.evaluacion.toString(),
                observaciones = resultado.aprendizajes,
                usuarioId = resultado.idUsuario,
                oportunidad = OportunidadRef(id = resultado.idOportunidad)
            )
            val response = api.guardarResultado(remote)
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
}
