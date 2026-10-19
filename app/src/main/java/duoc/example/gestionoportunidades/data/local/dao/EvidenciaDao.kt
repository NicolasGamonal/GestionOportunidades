package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import duoc.example.gestionoportunidades.model.Evidencia
import kotlinx.coroutines.flow.Flow

// DAO para la tabla "evidencias" (fotos capturadas con la cámara).
// Cada evidencia está asociada a una oportunidad por su idOportunidad (FK).
// Se usa en la pantalla de detalle para mostrar la galería de fotos del evento.
@Dao
interface EvidenciaDao {

    // Inserta una evidencia nueva (una foto).
    // No usa REPLACE: cada foto tiene un id único y no se sobrescribe.
    // Devuelve el id de la evidencia insertada.
    @Insert
    suspend fun insertar(evidencia: Evidencia): Long

    // Devuelve todas las fotos asociadas a una oportunidad,
    // ordenadas de más reciente a más antigua (fecha DESC).
    // Flow: la galería se actualiza sola cuando se agrega o borra una foto.
    @Query("SELECT * FROM evidencias WHERE idOportunidad = :idOportunidad ORDER BY fecha DESC")
    fun obtenerPorOportunidad(idOportunidad: Long): Flow<List<Evidencia>>

    // Elimina TODAS las evidencias de una oportunidad.
    // Se usa si se borra la oportunidad completa (limpieza en cascada).
    @Query("DELETE FROM evidencias WHERE idOportunidad = :idOportunidad")
    suspend fun eliminarPorOportunidad(idOportunidad: Long)

    // Cuenta cuántas fotos tiene una oportunidad.
    // Se usa para mostrar un contador en la UI (ej: "3 fotos")
    // o para validar que no se supere un límite si lo hubiera.
    @Query("SELECT COUNT(*) FROM evidencias WHERE idOportunidad = :idOportunidad")
    suspend fun contarPorOportunidad(idOportunidad: Long): Int

    // Elimina una evidencia específica por su id.
    // Se usa cuando el usuario borra una foto individual desde la galería.
    @Query("DELETE FROM evidencias WHERE idEvidencia = :idEvidencia")
    suspend fun eliminarPorId(idEvidencia: Long)
}