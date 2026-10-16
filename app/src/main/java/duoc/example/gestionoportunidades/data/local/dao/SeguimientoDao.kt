package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import duoc.example.gestionoportunidades.model.Seguimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface SeguimientoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSeguimiento(seguimiento: Seguimiento): Long

    @Query("SELECT * FROM seguimientos WHERE idOportunidad = :idOportunidad ORDER BY fechaHora DESC")
    fun obtenerHistorialPorOportunidad(idOportunidad: Long): Flow<List<Seguimiento>>

    @Query("SELECT * FROM seguimientos WHERE fechaProximo < :fechaActual AND tipoAccion != 'Cambio de estado'")
    fun obtenerSeguimientosAtrasados(fechaActual: Long): Flow<List<Seguimiento>>
}
