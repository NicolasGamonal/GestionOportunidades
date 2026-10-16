package duoc.example.gestionoportunidades.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agencia.oportunidades.data.EvidenciaDao
import com.agencia.oportunidades.model.Evidencia
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Seguimiento
import duoc.example.gestionoportunidades.data.local.dao.UsuarioDao
import duoc.example.gestionoportunidades.data.local.dao.OrganizacionDao
import duoc.example.gestionoportunidades.data.local.dao.ContactoDao
import duoc.example.gestionoportunidades.data.local.dao.OportunidadDao
import duoc.example.gestionoportunidades.data.local.dao.SeguimientoDao

@Database(
    entities = [
        Usuario::class,
        Organizacion::class,
        Contacto::class,
        Oportunidad::class,
        Seguimiento::class,
        Evidencia::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun organizacionDao(): OrganizacionDao
    abstract fun contactoDao(): ContactoDao
    abstract fun oportunidadDao(): OportunidadDao
    abstract fun seguimientoDao(): SeguimientoDao
    abstract fun evidenciaDao(): EvidenciaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestion_oportunidades_db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override suspend fun onCreate(connection: androidx.sqlite.SQLiteConnection) {
                            super.onCreate(connection)

                            val database = getDatabase(context)

                            // 3 Ejecutivos Comerciales ficticios de prueba
                            database.usuarioDao().insertarUsuarioSync(
                                Usuario(username = "ejecutivo1", contrasena = "123456", nombreCompleto = "Juan Pérez", rol = "EJECUTIVO")
                            )
                            database.usuarioDao().insertarUsuarioSync(
                                Usuario(username = "ejecutivo2", contrasena = "123456", nombreCompleto = "María José", rol = "EJECUTIVO")
                            )
                            database.usuarioDao().insertarUsuarioSync(
                                Usuario(username = "ejecutivo3", contrasena = "123456", nombreCompleto = "Diego Silva", rol = "EJECUTIVO")
                            )

                            // 1 Supervisor Comercial ficticio de prueba
                            database.usuarioDao().insertarUsuarioSync(
                                Usuario(username = "supervisor1", contrasena = "admin123", nombreCompleto = "Carlos Mendoza", rol = "SUPERVISOR")
                            )
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}