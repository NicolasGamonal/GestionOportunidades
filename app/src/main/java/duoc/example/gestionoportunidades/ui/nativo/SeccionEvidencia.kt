package duoc.example.gestionoportunidades.ui.nativo

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun SeccionEvidencia() {
    val context = LocalContext.current

    // Estados para guardar la URI temporal (antes de tomar foto) y la final
    var uriTemporal by remember { mutableStateOf<Uri?>(null) }
    var uriImagenGuardada by remember { mutableStateOf<Uri?>(null) }

    // Estado para saber si el usuario denegó el permiso[cite: 3]
    var permisoDenegado by remember { mutableStateOf(false) }
    // Estado para saber si el usuario canceló la captura[cite: 3]
    var capturaCancelada by remember { mutableStateOf(false) }

    // 1. Launcher para la cámara (captura la foto)[cite: 3]
    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            uriImagenGuardada = uriTemporal // Si toma la foto, guardamos la URI
            capturaCancelada = false
        } else {
            // Manejar la cancelación[cite: 3]
            capturaCancelada = true
        }
    }

    // 2. Launcher para pedir el permiso de la cámara[cite: 3]
    val permisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { otorgado ->
        if (otorgado) {
            permisoDenegado = false
            // Si da el permiso, creamos el archivo y lanzamos la cámara
            val nuevaUri = crearUriArchivo(context)
            uriTemporal = nuevaUri
            camaraLauncher.launch(nuevaUri)
        } else {
            // Manejar la negación[cite: 3]
            permisoDenegado = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Evidencia del Evento", style = MaterialTheme.typography.titleMedium)

        // Botón para iniciar el flujo
        Button(
            onClick = {
                capturaCancelada = false
                permisoLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Tomar Foto")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Capturar Evidencia")
        }

        // Mensajes de feedback para el usuario
        if (permisoDenegado) {
            Text(
                text = "Permiso de cámara denegado. No se puede tomar foto.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (capturaCancelada) {
            Text(
                text = "Captura cancelada por el usuario.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Mostrar la vista previa usando Coil si la foto fue exitosa[cite: 3]
        if (uriImagenGuardada != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = uriImagenGuardada,
                    contentDescription = "Evidencia fotográfica",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}

// Función auxiliar para crear el archivo temporal con FileProvider[cite: 3]
fun crearUriArchivo(context: Context): Uri {
    // Debe coincidir con la carpeta "evidencias" que pusimos en file_paths.xml
    val carpeta = File(context.cacheDir, "evidencias")
    carpeta.mkdirs() // IMPORTANTE: crear la carpeta si no existe

    val archivo = File(carpeta, "evidencia_${System.currentTimeMillis()}.jpg")

    // El authority debe ser EXACTO al que pusiste en el AndroidManifest.xml
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
}