package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import duoc.example.gestionoportunidades.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    // Usado por AppDatabase para inyectar los 4 usuarios ficticios de prueba
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarUsuarioSync(usuario: Usuario)

    // Consulta para validar credenciales en el login local ficticio
    @Query("SELECT * FROM usuarios WHERE username = :username AND contrasena = :contrasena LIMIT 1")
    suspend fun login(username: String, contrasena: String): Usuario?

    // Permite al Panel del Supervisor listar los ejecutivos disponibles para asignación
    @Query("SELECT * FROM usuarios WHERE rol = 'EJECUTIVO'")
    fun obtenerEjecutivos(): Flow<List<Usuario>>

    // Recupera un usuario específico por su ID relacional
    @Query("SELECT * FROM usuarios WHERE idUsuario = :idUsuario")
    suspend fun obtenerUsuarioPorId(idUsuario: Long): Usuario?
}
