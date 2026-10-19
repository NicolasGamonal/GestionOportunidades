package duoc.example.gestionoportunidades.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import duoc.example.gestionoportunidades.model.Usuario
import kotlinx.coroutines.flow.Flow

// DAO para la tabla "usuarios" (cuentas de login).
// Es el DAO más simple: solo 4 usuarios ficticios, sin relaciones salientes.
// Se usa principalmente en el login y para obtener la lista de ejecutivos
// (para que el supervisor pueda asignar oportunidades).
@Dao
interface UsuarioDao {

    // Inserta un usuario en la base.
    // No es suspend porque se usa dentro de CoroutineScope(Dispatchers.IO)
    // en el seed (sembrarDatos). REPLACE: si el id ya existe, lo sobrescribe.
    // Se usa solo en la carga inicial de datos ficticios.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarUsuarioSync(usuario: Usuario)

    // Actualiza un usuario existente (por su id).
    // Se usa si se edita el nombre, rol o contraseña de un usuario.
    @Update
    suspend fun actualizarUsuario(usuario: Usuario)

    // Verifica las credenciales de login: busca un usuario por username Y contraseña.
    // Devuelve el Usuario si existe (login exitoso) o null si no coincide (login fallido).
    // Se llama desde AuthRepository.iniciarSesion().
    // Nota: la contraseña se guarda en texto plano porque es un proyecto académico
    // sin backend ni cifrado (el checklist no lo pide).
    @Query("SELECT * FROM usuarios WHERE username = :username AND contrasena = :contrasena LIMIT 1")
    suspend fun login(username: String, contrasena: String): Usuario?

    // Obtiene un usuario específico por su id.
    // Se usa para recuperar los datos del usuario logueado (nombre, rol)
    // y mostrarlos en la UI o para verificar permisos.
    @Query("SELECT * FROM usuarios WHERE idUsuario = :idUsuario")
    suspend fun obtenerUsuarioPorId(idUsuario: Long): Usuario?

    // Devuelve solo los usuarios con rol EJECUTIVO.
    // Se usa en el panel del supervisor para mostrar la lista de ejecutivos
    // al momento de asignar o reasignar una oportunidad (dropdown/spinner).
    @Query("SELECT * FROM usuarios WHERE rol = 'EJECUTIVO'")
    fun obtenerEjecutivos(): Flow<List<Usuario>>

    // Devuelve todos los usuarios (ejecutivos y supervisores).
    // Se usa si se necesita una vista global (ej: en el seed o en depuración).
    @Query("SELECT * FROM usuarios")
    fun obtenerTodos(): Flow<List<Usuario>>
}