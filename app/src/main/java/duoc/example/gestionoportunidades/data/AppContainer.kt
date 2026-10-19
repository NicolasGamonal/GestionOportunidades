package duoc.example.gestionoportunidades.data

import android.content.Context
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.EvidenciaRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository

// AppContainer: el "centro de distribución" de dependencias de la app.
// Es un patrón de Dependency Injection manual (sin librerías como Hilt o Dagger).
//
// ¿Qué problema resuelve?
// Los ViewModels necesitan repositorios, los repositorios necesitan la base de datos,
// y la base de datos necesita el Context de Android. AppContainer crea todo eso
// UNA SOLA VEZ y lo expone como propiedades públicas para que cualquier ViewModel
// lo reciba sin tener que construirlo él mismo.
//
// ¿Dónde se crea?
// En MainActivity.kt (o en un ViewModelFactory) se instancia AppContainer(context)
// y se pasa a los ViewModels que lo necesiten.
//
// Analogía: es como un "mesón de pedidos". Tú (el ViewModel) no cocinas (no creas
// la base de datos ni los repositorios); solo pides al mesón y te lo entregan listo.
class AppContainer(context: Context) {

    // La base de datos de Room. Se crea una sola vez (singleton dentro de AppDatabase).
    // Todos los repositorios comparten esta misma instancia (no se crean 6 bases distintas).
    private val db = AppDatabase.getDatabase(context)

    // Repositorio de autenticación (login, obtener usuario, obtener ejecutivos).
    // Lo usa: AuthViewModel.
    val authRepository = AuthRepository(db)

    // Repositorio de oportunidades (el más grande: oportunidades + organizaciones + contactos + cambio de estado).
    // Lo usan: OportunidadesViewModel, DetalleOportunidadViewModel, FormularioOportunidadViewModel,
    //          OrgContactosViewModel, SupervisorViewModel.
    val oportunidadRepository = OportunidadRepository(db)

    // Repositorio de organizaciones (con regla de negocio en eliminar).
    // Lo usa: OrgContactosViewModel.
    val organizacionRepository = OrganizacionRepository(db)

    // Repositorio de contactos (con regla de negocio en eliminar: no borrar si tiene historial).
    // Lo usa: OrgContactosViewModel.
    val contactoRepository = ContactoRepository(db)

    // Repositorio de seguimientos (historial, alertas, gestiones).
    // Lo usan: SeguimientoViewModel, DashboardViewModel (alertas).
    val seguimientoRepository = SeguimientoRepository(db)

    // Repositorio de evidencias (fotos). Es el único que recibe un segundo parámetro:
    // context.filesDir → la carpeta de almacenamiento interno de la app donde se guardan
    // los archivos físicos de las fotos (ej: /data/data/.../files/).
    // Lo usa: DetalleOportunidadViewModel (cámara y galería).
    val evidenciaRepository = EvidenciaRepository(db, context.filesDir)
}