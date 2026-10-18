package duoc.example.gestionoportunidades.ui.nativo

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import duoc.example.gestionoportunidades.model.Evidencia
import duoc.example.gestionoportunidades.util.comoFechaHora

@Composable
fun SeccionEvidencia(
    evidencias: List<Evidencia>,
    onFotoCapturada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // rememberSaveable: la ruta pendiente sobrevive a una rotación mientras la cámara está abierta
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var permisoDenegado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var fotoAmpliada by remember { mutableStateOf<Evidencia?>(null) }

    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val ruta = rutaPendiente
        rutaPendiente = null
        if (ruta != null) {
            val archivo = ArchivosEvidencia.archivo(context, ruta)
            if (exito && archivo.exists() && archivo.length() > 0) {
                mensaje = null
                onFotoCapturada(ruta)
            } else {
                archivo.delete()      // no dejar archivos vacíos si se canceló
                mensaje = "Captura cancelada. No se guardó ninguna foto."
            }
        }
    }

    fun abrirCamara() {
        val ruta = ArchivosEvidencia.nuevaRutaRelativa()
        try {
            rutaPendiente = ruta
            camaraLauncher.launch(
                ArchivosEvidencia.uriParaCamara(context, ArchivosEvidencia.archivo(context, ruta))
            )
        } catch (e: ActivityNotFoundException) {
            rutaPendiente = null
            mensaje = "No hay una aplicación de cámara disponible en este dispositivo."
        }
    }

    val permisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { otorgado ->
        permisoDenegado = !otorgado
        if (otorgado) abrirCamara()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Evidencia fotográfica", style = MaterialTheme.typography.titleMedium)
        Text(
            "Usa solo imágenes de objetos o ficticias; no fotografíes personas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                mensaje = null
                permisoLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tomar foto")
        }

        if (permisoDenegado) {
            Text(
                "Permiso de cámara denegado. No se puede tomar la foto.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            // Tras denegar dos veces, Android deja de preguntar: solo se activa desde ajustes
            TextButton(onClick = { abrirAjustesDeLaApp(context) }) {
                Text("Abrir ajustes de la app")
            }
        }
        mensaje?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }

        if (evidencias.isEmpty()) {
            Text(
                "Aún no hay fotos para esta oportunidad.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(evidencias, key = { it.idEvidencia }) { evidencia ->
                    Card(
                        onClick = { fotoAmpliada = evidencia },
                        modifier = Modifier.size(120.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = ArchivosEvidencia.archivo(context, evidencia.rutaArchivo),
                            contentDescription = "Evidencia fotográfica",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    fotoAmpliada?.let { evidencia ->
        AlertDialog(
            onDismissRequest = { fotoAmpliada = null },
            title = { Text(evidencia.fecha.comoFechaHora()) },
            text = {
                AsyncImage(
                    model = ArchivosEvidencia.archivo(context, evidencia.rutaArchivo),
                    contentDescription = "Evidencia fotográfica ampliada",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = { TextButton(onClick = { fotoAmpliada = null }) { Text("Cerrar") } }
        )
    }
}

private fun abrirAjustesDeLaApp(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}