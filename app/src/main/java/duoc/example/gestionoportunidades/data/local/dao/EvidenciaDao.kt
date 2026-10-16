package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.agencia.oportunidades.model.Evidencia
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