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

// AppDatabase: la base de datos de Room (SQLite) de toda la app.
// Es el "contenedor" que define:
// 1. Qué entidades (tablas) tiene.
// 2. Qué versión está (para migraciones futuras).
// 3. Qué DAOs expone (la puerta de entrada a cada tabla).
// 4. Cómo se crea la instancia (singleton) y cómo se siembran los datos ficticios.
//
// ¿Por qué singleton?
// Porque toda la app (los 6 repositorios) necesita la MISMA base de datos.
// Si se creara una instancia nueva cada vez, se perdería la coherencia
// (una parte de la app leería de una base y otra de otra).
//
// ¿Por qué abstract?
// Porque Room genera la implementación en tiempo de compilación (con KSP).
// Tú solo defines la interfaz (qué DAOs expone); Room crea la clase concreta.
@Database(
    // Las 6 tablas de la base de datos (modelo 3FN):
    // - Usuario: cuentas de login (4 ficticias)
    // - Organizacion: empresas, municipalidades, instituciones
    // - Contacto: personas de contacto dentro de cada organización
    // - Oportunidad: eventos corporativos a gestionar (la entidad central)
    // - Seguimiento: historial de gestiones y cambios de estado
    // - Evidencia: fotos capturadas con la cámara, asociadas a una oportunidad
    entities = [
        Usuario::class,
        Organizacion::class,
        Contacto::class,
        Oportunidad::class,
        Seguimiento::class,
        Evidencia::class
    ],
    // Versión actual de la base. Si se agrega o modifica una entidad en el futuro,
    // hay que incrementarla y agregar una Migration (o usar fallbackToDestructiveMigration
    // que borra y recrea la base, perdiendo datos).
    version = 2,
    // exportSchema = false: no genera archivos .json del esquema en build/outputs/schemas.
    // En producción se recomienda true (para pruebas con MigrationTestHelper),
    // pero para un proyecto académico no es necesario.
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAOs: cada uno es la "puerta de entrada" a una tabla.
    // Room genera la implementación de estas funciones en tiempo de compilación.
    // Los repositorios llaman a estos DAOs; los ViewModels nunca los tocan directamente.
    abstract fun usuarioDao(): UsuarioDao
    abstract fun organizacionDao(): OrganizacionDao
    abstract fun contactoDao(): ContactoDao
    abstract fun oportunidadDao(): OportunidadDao
    abstract fun seguimientoDao(): SeguimientoDao
    abstract fun evidenciaDao(): EvidenciaDao

    companion object {
        // Instancia única de la base de datos (patrón Singleton).
        // @Volatile: garantiza que todas las hilos vean la misma instancia
        // (evita que dos hilos creen dos bases distintas simultáneamente).
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Crea o devuelve la instancia única de AppDatabase.
        // Se llama una sola vez desde AppContainer.
        //
        // ¿Por qué synchronized?
        // Porque si dos hilos llaman a getDatabase() al mismo tiempo,
        // solo uno debe crear la base. El otro espera y recibe la misma instancia.
        fun getDatabase(context: Context): AppDatabase {
            // Si ya existe la instancia, la devuelve directamente (no re-entra al synchronized)
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,  // applicationContext (no activityContext) para que no haya memory leaks
                    AppDatabase::class.java,
                    "gestion_oportunidades_db"    // Nombre del archivo .db en el almacenamiento interno
                )
                    // Callbacks: se ejecutan en momentos específicos del ciclo de vida de la base
                    .addCallback(object : RoomDatabase.Callback() {
                        // Se ejecuta UNA SOLA VEZ: cuando la base se crea por primera vez
                        // (primera instalación de la app).
                        // Aquí se siembran los datos ficticios para que la app no arranque vacía.
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // CoroutineScope(Dispatchers.IO): las inserciones son operaciones de E/S (disco),
                            // se ejecutan en un hilo de fondo para no bloquear la UI.
                            // INSTANCE?.let: espera a que la instancia esté asignada antes de sembrar.
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { sembrarDatos(it) }
                            }
                        }
                        // Se ejecuta cuando se borra la base por una migración destructiva
                        // (fallbackToDestructiveMigration). Es decir: si cambia el esquema
                        // y no hay migración definida, Room borra todo y recrea la base.
                        // Al recrearse, también se siembran los datos ficticios de nuevo.
                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { sembrarDatos(it) }
                            }
                        }
                    })
                    // Si la versión de la base cambia y no hay una Migration definida,
                    // Room borra la base y la recrea desde cero (perdiendo todos los datos).
                    // En producción se usan Migration objects para conservar datos,
                    // pero para un proyecto académico esto es suficiente.
                    .fallbackToDestructiveMigration()
                    .build()
                // Asigna la instancia al singleton
                INSTANCE = instance
                instance
            }
        }

        // Siembra los datos ficticios en la base de datos.
        // Solo se ejecuta en onCreate o onDestructiveMigration (nunca en cada apertura de la app).
        //
        // ¿Por qué ficticios?
        // El checklist pide "Utilizar información ficticia para organizaciones, contactos,
        // usuarios y eventos de prueba". No se usan nombres, correos ni teléfonos reales.
        //
        // Estructura de los datos sembrados:
        // - 4 usuarios: 3 ejecutivos + 1 supervisor (mínimo que pide el checklist)
        // - 4 organizaciones: municipalidad, empresa privada, colegio, clínica
        // - 4 contactos: uno por organización
        // - 6 oportunidades: una por cada estado del flujo (Identificada, Contacto realizado,
        //   Propuesta enviada, En espera de respuesta, Confirmada, Descartada)
        // - 4 seguimientos: 1 con fechaProximo en el pasado (atrasado), 1 "Cambio de estado" (historial)
        //
        // Las fechas se calculan relativas a "hoy" (System.currentTimeMillis()) para que
        // siempre haya eventos próximos y atrasados sin importar cuándo se instale la app.
        private suspend fun sembrarDatos(db: AppDatabase){

            // hoy: timestamp actual en milisegundos
            val hoy = System.currentTimeMillis()
            // dia: un día en milisegundos (24h × 60min × 60seg × 1000ms)
            // Se usa para calcular fechas relativas: hoy + 7 * dia = "dentro de 7 días"
            val dia = 24 * 60 * 60 * 1000L

            // --- USUARIOS (4 cuentas ficticias: 3 ejecutivos + 1 supervisor) ---
            // Contraseñas simples para prueba (el checklist no pide cifrado).
            // El rol se usa en Permisos.kt para restringir acciones por tipo de usuario.
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

            // --- ORGANIZACIONES (4 entidades ficticias de distintos tipos) ---
            // Cada una tiene un tipo diferente para cubrir los casos del checklist:
            // municipalidad, empresa privada, institución educativa, clínica.
            // Las comunas son reales (son divisiones administrativas, no datos personales).
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 1, nombre = "Municipalidad de Valle Verde", tipo = "Municipalidad", rubro = "Gobierno", comuna = "Providencia")
            )
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 2, nombre = "Empresa Logística Andina SpA", tipo = "Empresa privada", rubro = "Logística", comuna = "Maipú")
            )
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 3, nombre = "Colegio Los Aromos", tipo = "Institución educativa", rubro = "Educación", comuna = "Las Condes")
            )
            db.organizacionDao().insertarOrganizacion(
                Organizacion(idOrganizacion = 4, nombre = "Clínica Dental Sonrisa", tipo = "Empresa privada", rubro = "Salud", comuna = "Ñuñoa")
            )

            // --- CONTACTOS (uno por organización, con datos ficticios) ---
            // Los correos usan dominios inventados que coinciden con el nombre de la organización.
            // Los teléfonos son secuencias obvias (12345678, 87654321, etc.) para que
            // sea evidente que no son números reales.
            // idOrganizacion (FK): cada contacto pertenece a una organización.
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 1, nombre = "Ana Rojas", cargo = "Directora de Eventos", area = "Comunicaciones", correo = "ana.rojas@munivv.cl", telefono = "+56912345678", idOrganizacion = 1)
            )
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 2, nombre = "Pedro Fuentes", cargo = "Gerente RRHH", area = "Recursos Humanos", correo = "pedro.fuentes@logandina.cl", telefono = "+56987654321", idOrganizacion = 2)
            )
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 3, nombre = "Lucía Vargas", cargo = "Jefa de Administración", area = "Administración", correo = "lucia.vargas@colegioaromos.cl", telefono = "+56955551234", idOrganizacion = 3)
            )
            db.contactoDao().insertarContacto(
                Contacto(idContacto = 4, nombre = "Roberto Díaz", cargo = "Director Comercial", area = "Comercial", correo = "roberto.diaz@clinicasonrisa.cl", telefono = "+56944445678", idOrganizacion = 4)
            )

            // --- OPORTUNIDADES (6, una por cada estado del flujo) ---
            // Esto permite probar el recorrido completo sin tener que crear datos manualmente:
            // - Opp 1: "Identificada" → el ejecutivo puede avanzar el estado
            // - Opp 2: "Contacto realizado" → ya se hizo el primer contacto
            // - Opp 3: "Propuesta enviada" → esperando respuesta
            // - Opp 4: "En espera de respuesta" → el ejecutivo NO puede confirmar (solo supervisor)
            // - Opp 5: "Confirmada" → estado final positivo
            // - Opp 6: "Descartada" → estado final negativo
            //
            // Las fechas son relativas a "hoy" para que siempre haya:
            // - Eventos próximos (+7, +14 días) → aparecen en "Eventos próximos" del dashboard
            // - Eventos lejanos (+30 días) → no son urgentes
            // - fechaDetección en el pasado (-3 días) → ya se detectó
            //
            // idResponsable (FK): distribuido entre los 3 ejecutivos.
            // El supervisor (id 4) no tiene oportunidades asignadas (él las asigna a los ejecutivos).
            // idOrganizacion (FK): cada oportunidad pertenece a una organización.

            // Opp 1: Identificada, Alta prioridad, en 7 días, asignada a ejecutivo1
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 1,
                    nombre = "Feria del Emprendedor Valle Verde",
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
            // Opp 2: Contacto realizado, Media prioridad, en 14 días, asignada a ejecutivo2
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
            // Opp 3: Propuesta enviada, Alta prioridad, en 30 días, asignada a ejecutivo3
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 3,
                    nombre = "Feria Institucional Los Aromos",
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
            // Opp 4: En espera de respuesta, Baja prioridad, en 14 días, asignada a ejecutivo1
            // (ejecutivo1 tiene 2 oportunidades para probar que ve ambas en su lista)
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
            // Opp 5: Confirmada (estado final), Alta prioridad, en 7 días, asignada a ejecutivo2
            // (ejecutivo2 también tiene 2 oportunidades)
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
            // Opp 6: Descartada (estado final), Media prioridad, en 30 días, asignada a ejecutivo3
            // costoReferencial = 0 porque no se avanza (no hay inversión)
            db.oportunidadDao().insertarOportunidad(
                Oportunidad(
                    idOportunidad = 6,
                    nombre = "Jornada Municipal Valle Verde",
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

            // --- SEGUIMIENTOS (4 gestiones con distintos tipos y fechas) ---
            // Permiten probar:
            // - Historial en el detalle de oportunidad
            // - "Gestiones atrasadas" en el dashboard (Seguimiento 4: fechaProximo en el pasado)
            // - "Seguimientos pendientes" en el dashboard (Seguimientos 1-3: fechaProximo en el futuro)
            // - "Cambio de estado" como tipo de historial (Seguimiento 3)

            // Seguimiento 1: Llamada hace 2 días, próxima acción en 2 días (PENDIENTE)
            // Asociado a Opp 1 (Identificada), registrado por ejecutivo1
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
            // Seguimiento 2: Email hace 1 día, próxima acción en 5 días (PENDIENTE)
            // Asociado a Opp 2 (Contacto realizado), registrado por ejecutivo2
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
            // Seguimiento 3: "Cambio de estado" hace 1 día (HISTORIAL)
            // Registra que la Opp 3 pasó a "Propuesta enviada".
            // Este es el ejemplo de cómo se conserva el historial de cambios de estado:
            // no es una gestión real, es un registro automático del flujo.
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
            // Seguimiento 4: Reunión hace 4 días, fechaProximo AYER (ATRASADO)
            // Este es el que aparece en "Gestiones atrasadas" del dashboard.
            // fechaProximo = hoy - 1 * dia → ya venció, el ejecutivo debe haber hecho el seguimiento.
            // Asociado a Opp 4 (En espera de respuesta), registrado por ejecutivo1.
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