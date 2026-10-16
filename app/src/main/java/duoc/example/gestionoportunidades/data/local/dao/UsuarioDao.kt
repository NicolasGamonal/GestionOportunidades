package duoc.example.gestionoportunidades.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import duoc.example.gestionoportunidades.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarUsuarioSync(usuario: Usuario)

    @Update
    suspend fun actualizarUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE username = :username AND contrasena = :contrasena LIMIT 1")
    suspend fun login(username: String, contrasena: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE idUsuario = :idUsuario")
    suspend fun obtenerUsuarioPorId(idUsuario: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE rol = 'EJECUTIVO'")
    fun obtenerEjecutivos(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios")
    fun obtenerTodos(): Flow<List<Usuario>>
}