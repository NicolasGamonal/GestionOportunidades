package duoc.example.gestionoportunidades.data.repository

import duoc.example.gestionoportunidades.data.local.AppDatabase
import duoc.example.gestionoportunidades.model.Evidencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

// Repositorio de evidencias (fotos capturadas con la cámara).
// Capa intermedia entre el ViewModel (DetalleOportunidadViewModel) y el DAO (EvidenciaDao).
// A diferencia de los otros repositorios, este también maneja archivos en el disco:
// las fotos se guardan como archivos físicos en el almacenamiento interno de la app,
// y en Room solo se guarda la ruta (rutaArchivo) para poder recuperarlas.
class EvidenciaRepository(db: AppDatabase, private val directorioArchivos: File) {

    // Referencia al DAO de evidencias. Se obtiene una sola vez al construir el repositorio.
    private val dao = db.evidenciaDao()

    // directorioArchivos: la carpeta en el almacenamiento interno de la app donde se guardan
    // los archivos de las fotos (ej: /data/data/duoc.example.gestionoportunidades/files/fotos/).
    // Se inyecta desde AppContainer para que el repositorio sepa dónde leer/escribir.

    // Devuelve todas las fotos asociadas a una oportunidad, en tiempo real (Flow).
    // Se usa en la pantalla de detalle para mostrar la galería de fotos del evento.
    // Si se captura una foto nueva, la galería se actualiza sola sin recargar.
    fun observarPorOportunidad(idOportunidad: Long): Flow<List<Evidencia>> =
        dao.obtenerPorOportunidad(idOportunidad)

    // Registra una evidencia nueva en la base de datos (guarda la ruta del archivo).
    // El archivo de la foto ya fue copiado previamente a directorioArchivos
    // (eso lo hace la pantalla de cámara en ui/nativo/).
    // Devuelve el id de la evidencia insertada.
    suspend fun registrar(evidencia: Evidencia): Long = dao.insertar(evidencia)

    // Elimina una evidencia: borra el registro de Room Y el archivo físico de la foto.
    //
    // ¿Por qué dos pasos?
    // 1. dao.eliminarPorId() → borra la fila de la tabla "evidencias" en Room.
    // 2. File(...).delete() → borra el archivo .jpg/.png del almacenamiento interno.
    //
    // Si solo se borrara de Room, el archivo seguiría ocupando espacio en el teléfono
    // (fuga de almacenamiento). Si solo se borrara el archivo, Room seguiría apuntando
    // a una ruta inexistente (error al intentar mostrar la imagen).
    //
    // ¿Por qué withContext(Dispatchers.IO)?
    // Porque borrar un archivo es una operación de E/S (disco) que puede tardar.
    // Dispatchers.IO la mueve a un hilo de fondo para no bloquear la UI.
    suspend fun eliminar(evidencia: Evidencia) {
        // Paso 1: borrar el registro de la base de datos
        dao.eliminarPorId(evidencia.idEvidencia)
        // Paso 2: borrar el archivo físico del disco (en un hilo de E/S)
        withContext(Dispatchers.IO) { File(directorioArchivos, evidencia.rutaArchivo).delete() }
    }
}