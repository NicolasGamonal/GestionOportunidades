package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.Oportunidad
import kotlinx.coroutines.flow.Flow

// DAO para la tabla "oportunidades". Es el más complejo del proyecto
// porque contiene las consultas de búsqueda, filtros, permisos por rol
// y las alertas del dashboard (eventos próximos).
// El repositorio llama a estas funciones; el ViewModel nunca toca este archivo.
@Dao
interface OportunidadDao {

    // Inserta una oportunidad nueva.
    // REPLACE: si ya existe una con el mismo id, la sobrescribe (útil al editar).
    // Devuelve el id de la oportunidad insertada.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOportunidad(oportunidad: Oportunidad): Long

    // Actualiza una oportunidad existente (por su id).
    // Se usa al editar campos desde el formulario.
    @Update
    suspend fun actualizarOportunidad(oportunidad: Oportunidad)

    // Busca una oportunidad específica por su id.
    // Devuelve la oportunidad o null si no existe.
    // Se usa al abrir el detalle desde la lista.
    @Query("SELECT * FROM oportunidades WHERE idOportunidad = :id")
    suspend fun obtenerPorId(id: Long): Oportunidad?

    // Devuelve TODAS las oportunidades sin filtro.
    // Se usa en el panel del supervisor (ve todo el equipo).
    @Query("SELECT * FROM oportunidades")
    fun obtenerTodas(): Flow<List<Oportunidad>>

    // Búsqueda por nombre (parcial). Ej: "Feria" encuentra "Feria del Emprendedor Valle Verde".
    // Se usa en el campo de búsqueda de la lista de oportunidades.
    @Query("SELECT * FROM oportunidades WHERE nombre LIKE '%' || :busqueda || '%'")
    fun buscarPorNombre(busqueda: String): Flow<List<Oportunidad>>

    // Filtra por estado Y prioridad Y nombre a la vez.
    // Se usa cuando el usuario activa los filtros en la UI (chips de estado + prioridad + texto).
    @Query("SELECT * FROM oportunidades WHERE estado = :estado AND prioridad = :prioridad AND nombre LIKE '%' || :busqueda || '%'")
    fun filtrarOportunidades(estado: String, prioridad: String, busqueda: String): Flow<List<Oportunidad>>

    // Devuelve solo las oportunidades asignadas a un usuario específico.
    // Se usa para el rol EJECUTIVO: solo ve las suyas (idResponsable = su id).
    // El supervisor llama a obtenerTodas() en su lugar.
    @Query("SELECT * FROM oportunidades WHERE idResponsable = :idUsuario")
    fun obtenerPorResponsable(idUsuario: Long): Flow<List<Oportunidad>>

    // Devuelve oportunidades con fecha de inicio a partir de una fecha dada,
    // que NO estén descartadas, y opcionalmente filtradas por responsable.
    // idResponsable = 0 significa "todas" (supervisor); si es un id real, solo las de ese ejecutivo.
    // Se usa en el Dashboard para mostrar "eventos próximos".
    @Query("""
    SELECT * FROM oportunidades
    WHERE fechaInicio >= :desde
      AND estado != 'Descartada'
      AND (:idResponsable = 0 OR idResponsable = :idResponsable)
    ORDER BY fechaInicio ASC
""")
    fun obtenerEventosProximos(desde: Long, idResponsable: Long): Flow<List<Oportunidad>>

    // Cambia el estado de una oportunidad (ej: "Identificada" → "Contacto realizado").
    // Se llama desde el ViewModel después de verificar permisos con Permisos.kt.
    // El ViewModel también crea un Seguimiento de tipo "Cambio de estado" para el historial.
    @Query("UPDATE oportunidades SET estado = :estado WHERE idOportunidad = :id")
    suspend fun actualizarEstado(id: Long, estado: String)

    // Observa una oportunidad en tiempo real (Flow).
    // Si el supervisor cambia el estado, la UI del ejecutivo se actualiza sola.
    // Se usa en la pantalla de detalle para mantener los datos siempre frescos.
    @Query("SELECT * FROM oportunidades WHERE idOportunidad = :id")
    fun observarPorId(id: Long): Flow<Oportunidad?>

    // Consulta flexible: combina estado, prioridad, búsqueda por nombre y responsable.
    // Los parámetros vacíos ('') o 0 significan "no aplicar ese filtro".
    // Ej: estado='' y prioridad='' y busqueda='' y idResponsable=1 → todas las de ese ejecutivo.
    // Se usa en la lista principal con los filtros opcionales de la UI.
    @Query("""
    SELECT * FROM oportunidades
    WHERE (:estado = '' OR estado = :estado)
      AND (:prioridad = '' OR prioridad = :prioridad)
      AND nombre LIKE '%' || :busqueda || '%'
      AND (:idResponsable = 0 OR idResponsable = :idResponsable)
    ORDER BY fechaInicio ASC
""")
    fun buscar(estado: String, prioridad: String, busqueda: String, idResponsable: Long): Flow<List<Oportunidad>>

    // Cuenta cuántas oportunidades tiene una organización.
    // Se usa en la pantalla de organizaciones para mostrar un contador (ej: "3 oportunidades").
    @Query("SELECT COUNT(*) FROM oportunidades WHERE idOrganizacion = :idOrganizacion")
    suspend fun contarPorOrganizacion(idOrganizacion: Long): Int

    // Elimina una oportunidad por su id.
    // Ojo: hay que borrar primero los seguimientos y evidencias asociados (FK).
    // El repositorio maneja el orden de borrado en cascada.
    @Query("DELETE FROM oportunidades WHERE idOportunidad = :id")
    suspend fun eliminarPorId(id: Long)
}