package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.remote.ApiService
import duoc.example.gestionoportunidades.data.remote.NotaMarketingRemote
import duoc.example.gestionoportunidades.data.remote.OportunidadRef
import duoc.example.gestionoportunidades.data.remote.RetrofitClient
import duoc.example.gestionoportunidades.model.NotaMarketing
import duoc.example.gestionoportunidades.model.NotaMarketingConAutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.ZoneId

class NotaMarketingRepository(private val api: ApiService = RetrofitClient.apiService) {

    private val zonaHoraria = ZoneId.systemDefault()

    private fun String?.toDateTimeMillis(): Long {
        if (this.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            LocalDateTime.parse(this.take(19))
                .atZone(zonaHoraria)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun observarPorOportunidad(idOportunidad: Long): Flow<List<NotaMarketingConAutor>> = flow {
        try {
            val usuariosMap = api.listarUsuarios().associateBy { it.id }
            val lista = api.listarNotas(idOportunidad).map { remote ->
                val autorNombre = remote.autorId?.let { usuariosMap[it]?.nombre } ?: "Equipo Marketing"
                NotaMarketingConAutor(
                    idNota = remote.id ?: 0L,
                    tipo = remote.materialRequerido ?: "Observación",
                    texto = remote.contenido,
                    fechaHora = remote.fechaCreacion.toDateTimeMillis(),
                    autor = autorNombre
                )
            }.sortedByDescending { it.fechaHora }
            emit(lista)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun insertar(nota: NotaMarketing): Long {
        return try {
            val remote = NotaMarketingRemote(
                contenido = nota.texto,
                materialRequerido = nota.tipo,
                autorId = nota.idUsuario,
                oportunidad = OportunidadRef(id = nota.idOportunidad)
            )
            val response = api.crearNota(remote)
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
