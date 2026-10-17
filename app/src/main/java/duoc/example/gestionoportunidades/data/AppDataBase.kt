package duoc.example.gestionoportunidades.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import duoc.example.gestionoportunidades.model.Evidencia
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Seguimiento
import duoc.example.gestionoportunidades.data.local.dao.UsuarioDao
import duoc.example.gestionoportunidades.data.local.dao.OrganizacionDao
import duoc.example.gestionoportunidades.data.local.dao.ContactoDao
import duoc.example.gestionoportunidades.data.local.dao.EvidenciaDao
import duoc.example.gestionoportunidades.data.local.dao.OportunidadDao
import duoc.example.gestionoportunidades.data.local.dao.SeguimientoDao
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { sembrarDatos(it) }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun sembrarDatos(db: AppDatabase){

            val hoy = System.currentTimeMillis()
            val dia = 24 * 60 * 60 * 1000L

            // --- USUARIOS ---
            db.usuarioDao().insertarUsuarioSync(
                Usuario(idUsuario = 1, username = "ejecutivo1", contrasena = "123456", nombreCompleto = "Juan Pérez", rol = "EJECUTIVO")
            )
            db.usuarioDao().insertarUsuarioSync(
                Usuario(idUsuario = 2, username = "ejecutivo2", contrasena = "123456", nombreCompleto = "María José", rol = "EJECUTIVO")
            )
            db.usuarioDao().insertarUsuarioSync(
                Usuario(idUsuario = 3, username = "ejecutivo3", contrasena = "123456", nombreCompleto = "Diego Silva", rol = "EJECUTIVO")
            )
            db.usuarioDao().insertarUsuarioSync(
                Usuario(idUsuario = 4, username = "supervisor1", contrasena = "admin123", nombreCompleto = "Carlos Mendoza", rol = "SUPERVISOR")
            )

            // --- ORGANIZACIONES ---
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 1, nombre = "Municipalidad de Providencia", tipo = "Municipalidad", rubro = "Gobierno", comuna = "Providencia")
            )
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 2, nombre = "Empresa Logística Andina SpA", tipo = "Empresa privada", rubro = "Logística", comuna = "Maipú")
            )
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 3, nombre = "Colegio San Miguel Arcángel", tipo = "Institución educativa", rubro = "Educación", comuna = "Las Condes")
            )
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 4, nombre = "Clínica Dental Sonrisa", tipo = "Empresa privada", rubro = "Salud", comuna = "Ñuñoa")
            )

            // --- CONTACTOS ---
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 1, nombre = "Ana Rojas", cargo = "Directora de Eventos", area = "Comunicaciones", correo = "ana.rojas@muniprov.cl", telefono = "+56912345678", idOrganizacion = 1)
            )
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 2, nombre = "Pedro Fuentes", cargo = "Gerente RRHH", area = "Recursos Humanos", correo = "pedro.fuentes@logandina.cl", telefono = "+56987654321", idOrganizacion = 2)
            )
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 3, nombre = "Lucía Vargas", cargo = "Jefa de Administración", area = "Administración", correo = "lucia.vargas@colegiosanmiguel.cl", telefono = "+56955551234", idOrganizacion = 3)
            )
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 4, nombre = "Roberto Díaz", cargo = "Director Comercial", area = "Comercial", correo = "roberto.diaz@clinicasonrisa.cl", telefono = "+56944445678", idOrganizacion = 4)
            )

            // --- OPORTUNIDADES ---
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 1,
                    nombre = "Feria del Emprendedor Providencia",
                    tipoEvento = "Feria municipal",
                    fechaDeteccion = hoy - 3 * dia,
                    fechaInicio = hoy + 7 * dia,
                    fechaTermino = hoy + 9 * dia,
                    comuna = "Providencia",
                    publicoEsperado = 2500,
                    prioridad = "Alta",
                    estado = "Identificada",
                    costoReferencial = 850000.0,
                    observaciones = "Feria anual con alta concurrencia de familias",
                    idOrganizacion = 1,
                    idResponsable = 1
                )
            )
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 2,
                    nombre = "Jornada de Beneficios Andina",
                    tipoEvento = "Jornada de beneficios",
                    fechaDeteccion = hoy - 3 * dia,
                    fechaInicio = hoy + 14 * dia,
                    fechaTermino = hoy + 14 * dia,
                    comuna = "Maipú",
                    publicoEsperado = 800,
                    prioridad = "Media",
                    estado = "Contacto realizado",
                    costoReferencial = 420000.0,
                    observaciones = "Jornada interna para colaboradores",
                    idOrganizacion = 2,
                    idResponsable = 2
                )
            )
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 3,
                    nombre = "Feria Institucional San Miguel",
                    tipoEvento = "Feria institucional",
                    fechaDeteccion = hoy - 3 * dia,
                    fechaInicio = hoy + 30 * dia,
                    fechaTermino = hoy + 30 * dia,
                    comuna = "Las Condes",
                    publicoEsperado = 1200,
                    prioridad = "Alta",
                    estado = "Propuesta enviada",
                    costoReferencial = 650000.0,
                    observaciones = "Actividad anual de fin de semestre",
                    idOrganizacion = 3,
                    idResponsable = 3
                )
            )
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 4,
                    nombre = "Activación Dental Sonrisa",
                    tipoEvento = "Evento corporativo",
                    fechaDeteccion = hoy - 3 * dia,
                    fechaInicio = hoy + 14 * dia,
                    fechaTermino = hoy + 14 * dia,
                    comuna = "Ñuñoa",
                    publicoEsperado = 500,
                    prioridad = "Baja",
                    estado = "En espera de respuesta",
                    costoReferencial = 300000.0,
                    observaciones = "Activación en sucursal principal",
                    idOrganizacion = 4,
                    idResponsable = 1
                )
            )
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 5,
                    nombre = "Feria Empresarial Maipú",
                    tipoEvento = "Feria empresarial",
                    fechaDeteccion = hoy - 3 * dia,
                    fechaInicio = hoy + 7 * dia,
                    fechaTermino = hoy + 7 * dia,
                    comuna = "Maipú",
                    publicoEsperado = 3000,
                    prioridad = "Alta",
                    estado = "Confirmada",
                    costoReferencial = 1200000.0,
                    observaciones = "Feria con stands de servicios",
                    idOrganizacion = 2,
                    idResponsable = 2
                )
            )
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 6,
                    nombre = "Jornada Municipal Providencia",
                    tipoEvento = "Actividad municipal",
                    fechaDeteccion = hoy - 3 * dia,
                    fechaInicio = hoy + 30 * dia,
                    fechaTermino = hoy + 30 * dia,
                    comuna = "Providencia",
                    publicoEsperado = 1000,
                    prioridad = "Media",
                    estado = "Descartada",
                    costoReferencial = 0.0,
                    observaciones = "No hubo presupuesto disponible",
                    idOrganizacion = 1,
                    idResponsable = 3
                )
            )

            // --- SEGUIMIENTOS ---
            db.seguimientoDao().insertarSeguimiento(
                Seguimiento(
                    idSeguimiento = 1,
                    tipoAccion = "Llamada",
                    fechaHora = hoy - 2 * dia,
                    resultado = "Contacto realizado con Ana Rojas, interesada en propuesta",
                    proximaAccion = "Enviar propuesta formal",
                    fechaProximo = hoy + 2 * dia,
                    idOportunidad = 1,
                    idUsuario = 1
                )
            )
            db.seguimientoDao().insertarSeguimiento(
                Seguimiento(
                    idSeguimiento = 2,
                    tipoAccion = "Email",
                    fechaHora = hoy - 1 * dia,
                    resultado = "Enviada propuesta a Pedro Fuentes, esperando respuesta",
                    proximaAccion = "Seguimiento telefónico",
                    fechaProximo = hoy + 5 * dia,
                    idOportunidad = 2,
                    idUsuario = 2
                )
            )
            db.seguimientoDao().insertarSeguimiento(
                Seguimiento(
                    idSeguimiento = 3,
                    tipoAccion = "Cambio de estado",
                    fechaHora = hoy - 1 * dia,
                    resultado = "Estado cambiado a: Propuesta enviada",
                    proximaAccion = "Esperar respuesta de Lucía Vargas",
                    fechaProximo = hoy + 7 * dia,
                    idOportunidad = 3,
                    idUsuario = 3
                )
            )
            db.seguimientoDao().insertarSeguimiento(
                Seguimiento(
                    idSeguimiento = 4,
                    tipoAccion = "Reunión",
                    fechaHora = hoy - 4 * dia,
                    resultado = "Reunión con Roberto Díaz, presentó condiciones",
                    proximaAccion = "Enviar propuesta revisada",
                    fechaProximo = hoy - 1 * dia,
                    idOportunidad = 4,
                    idUsuario = 1
                )
            )
        }
    }
}