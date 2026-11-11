package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import duoc.example.gestionoportunidades.model.ResultadoActivacion
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultadoActivacionDao {

    // REPLACE: si ya existe un resultado para la oportunidad (índice único), lo reemplaza.
    // Para editar se envía el mismo idResultado y así se conserva la fila.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(resultado: ResultadoActivacion): Long

    // Flow: la pantalla se actualiza sola cuando el resultado se crea o se edita.
    @Query("SELECT * FROM resultados_activacion WHERE idOportunidad = :idOportunidad LIMIT 1")
    fun observarPorOportunidad(idOportunidad: Long): Flow<ResultadoActivacion?>

    @Query("SELECT * FROM resultados_activacion WHERE idOportunidad = :idOportunidad LIMIT 1")
    suspend fun obtenerPorOportunidad(idOportunidad: Long): ResultadoActivacion?
}
