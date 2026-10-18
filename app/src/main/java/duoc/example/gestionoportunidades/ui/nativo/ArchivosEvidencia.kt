package duoc.example.gestionoportunidades.ui.nativo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ArchivosEvidencia {
    private const val CARPETA = "evidencias"

    /** Ruta relativa a filesDir; es la que se guarda en Room. */
    fun nuevaRutaRelativa(): String = "$CARPETA/evidencia_${System.currentTimeMillis()}.jpg"

    fun archivo(context: Context, rutaRelativa: String): File = File(context.filesDir, rutaRelativa)

    /** URI segura (FileProvider) para que la app de cámara escriba la foto. */
    fun uriParaCamara(context: Context, archivo: File): Uri {
        archivo.parentFile?.mkdirs()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }
}