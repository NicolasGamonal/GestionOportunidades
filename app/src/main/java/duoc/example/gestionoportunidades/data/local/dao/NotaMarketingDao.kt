package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import duoc.example.gestionoportunidades.model.NotaMarketing
import duoc.example.gestionoportunidades.model.NotaMarketingConAutor
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaMarketingDao {

    @Insert
    suspend fun insertar(nota: NotaMarketing): Long

    @Query("""
        SELECT n.idNota AS idNota,
               n.tipo AS tipo,
               n.texto AS texto,
               n.fechaHora AS fechaHora,
               u.nombreCompleto AS autor
        FROM notas_marketing n
        INNER JOIN usuarios u ON u.idUsuario = n.idUsuario
        WHERE n.idOportunidad = :idOportunidad
        ORDER BY n.fechaHora DESC
    """)
    fun observarPorOportunidad(idOportunidad: Long): Flow<List<NotaMarketingConAutor>>
}