package duoc.example.gestionoportunidades.data

import android.content.Context
import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.data.repository.AuthRepository
import duoc.example.gestionoportunidades.data.repository.ContactoRepository
import duoc.example.gestionoportunidades.data.repository.EvidenciaRepository
import duoc.example.gestionoportunidades.data.repository.OportunidadRepository
import duoc.example.gestionoportunidades.data.repository.OrganizacionRepository
import duoc.example.gestionoportunidades.data.repository.SeguimientoRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val authRepository = AuthRepository(db)
    val oportunidadRepository = OportunidadRepository(db)
    val organizacionRepository = OrganizacionRepository(db)
    val contactoRepository = ContactoRepository(db)
    val seguimientoRepository = SeguimientoRepository(db)
    val evidenciaRepository = EvidenciaRepository(db)
}