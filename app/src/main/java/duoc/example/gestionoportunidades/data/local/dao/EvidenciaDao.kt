package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import duoc.example.gestionoportunidades.model.Evidencia
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenciaDao {

    @Insert
    suspend fun insertar(evidencia: Evidencia): Long

    @Query("SELECT * FROM evidencias WHERE idOportunidad = :idOportunidad ORDER BY fecha DESC")
    fun obtenerPorOportunidad(idOportunidad: Long): Flow<List<Evidencia>>

    @Query("DELETE FROM evidencias WHERE idOportunidad = :idOportunidad")
    suspend fun eliminarPorOportunidad(idOportunidad: Long)
}