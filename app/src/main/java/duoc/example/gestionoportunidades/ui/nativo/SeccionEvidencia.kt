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

// SeccionEvidencia: sección de cámara y galería de fotos dentro del detalle de una oportunidad.
//
// ¿Dónde se usa?
// - DetalleOportunidadScreen: se incluye como una sección dentro del detalle,
//   mostrando las fotos asociadas a esa oportunidad y permitiendo tomar nuevas.
//
// ¿Por qué está en ui/nativo/ y no en ui/components/?
// Porque interactúa directamente con APIs del sistema Android:
// - Cámara del dispositivo (ActivityResultContracts.TakePicture)
// - Permiso de cámara (Manifest.permission.CAMERA)
// - Ajustes de la app (Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
// - FileProvider (vía ArchivosEvidencia.uriParaCamara)
// Los componentes de ui/components/ son puramente visuales; ui/nativo/ agrupa
// todo lo que toca hardware o servicios del sistema.
//
// ¿Qué hace?
// 1. Muestra un botón "Tomar foto" que pide permiso y abre la cámara.
// 2. Al capturar, guarda la ruta en el UiState del ViewModel (onFotoCapturada).
// 3. Muestra la galería de fotos existentes (LazyRow con thumbnails).
// 4. Al tocar una foto, la muestra ampliada en un AlertDialog.
// 5. Permite eliminar una foto (con confirmación).
// 6. Maneja: cancelación de cámara, permiso denegado, sin app de cámara, archivo vacío.
//
// Cumple el checklist:
// - "Implementar la cámara del dispositivo para capturar fotografías" → TakePicture
// - "Mostrar una vista previa de las fotografías capturadas" → AsyncImage + AlertDialog ampliado
// - "Asociar y conservar las evidencias fotográficas con su oportunidad" → onFotoCapturada → Room
// - "Gestionar correctamente archivos, URIs, permisos y cancelaciones" → todo el flujo de error

// Sección de evidencia fotográfica (cámara + galería).
//
// Parámetros:
// - evidencias: lista de fotos ya asociadas a la oportunidad (viene del UiState del ViewModel).
//   Es un Flow observado con collectAsStateWithLifecycle en la Screen.
// - onFotoCapturada: callback que se llama con la ruta relativa cuando se captura una foto válida.
//   El ViewModel la usa para crear el registro en Room (EvidenciaRepository.registrar).
// - onEliminarFoto: callback que se llama con la Evidencia a borrar.
//   El ViewModel la usa para borrar de Room Y del disco (EvidenciaRepository.eliminar).
// - modifier: para que la Screen controle la posición dentro del detalle.
@Composable
fun SeccionEvidencia(
    evidencias: List<Evidencia>,
    onFotoCapturada: (String) -> Unit,
    onEliminarFoto: (Evidencia) -> Unit,
    modifier: Modifier = Modifier
) {
    // Context de Android: necesario para Intents, FileProvider y acceso a archivos.
    val context = LocalContext.current

    // --- ESTADO LOCAL DE LA PANTALLA ---

    // Ruta de la foto que se está capturando (pendiente de confirmar).
    // rememberSaveable (no remember): si el usuario rota la pantalla MIENTRAS la cámara
    // está abierta, el estado se conserva. Con remember normal, la ruta se perdería
    // y al volver de la cámara no se sabría a qué archivo corresponde la foto.
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    // Si el usuario denegó el permiso de cámara.
    // Se usa para mostrar el mensaje de error y el botón "Abrir ajustes".
    var permisoDenegado by remember { mutableStateOf(false) }

    // Mensaje informativo temporal (ej: "Captura cancelada", "No hay app de cámara").
    // Se limpia cada vez que el usuario presiona "Tomar foto" de nuevo.
    var mensaje by remember { mutableStateOf<String?>(null) }

    // Foto seleccionada para ver ampliada (AlertDialog).
    // null = no hay foto ampliada.
    var fotoAmpliada by remember { mutableStateOf<Evidencia?>(null) }

    // Foto seleccionada para eliminar (AlertDialog de confirmación).
    // null = no hay eliminación en curso.
    var fotoPorEliminar by remember { mutableStateOf<Evidencia?>(null) }

    // --- LAUNCHER DE CÁMARA ---
    // rememberLauncherForActivityResult: registra un "listener" que se activa
    // cuando la app de cámara termina (sea por captura o por cancelación).
    //
    // ActivityResultContracts.TakePicture(): abre la cámara del sistema y espera
    // un resultado booleano: true = el usuario tomó la foto, false = canceló.
    //
    // ¿Por qué no TakePicturePreview?
    // TakePicturePreview devuelve un Bitmap en memoria (pesado, no guarda archivo).
    // TakePicture guarda el archivo en la ruta que tú le pasas (FileProvider URI)
    // y solo devuelve true/false. Es más eficiente y permite guardar en Room la ruta.
    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        // Al volver de la cámara:
        val ruta = rutaPendiente
        rutaPendiente = null  // Limpiar siempre (evita fugas si se re-encadena)

        if (ruta != null) {
            // Resuelve la ruta relativa a la ruta absoluta del archivo en filesDir
            val archivo = ArchivosEvidencia.archivo(context, ruta)

            if (exito && archivo.exists() && archivo.length() > 0) {
                // ÉXITO: la foto se tomó, el archivo existe y no está vacío.
                // Se limpia el mensaje anterior (si había uno de error).
                mensaje = null
                // Notifica al ViewModel: "guarda esta ruta en Room como Evidencia".
                onFotoCapturada(ruta)
            } else {
                // CANCELACIÓN o ARCHIVO VACÍO:
                // - exito = false → el usuario presionó "Cancelar" o "X" en la cámara.
                // - archivo.length() == 0 → la cámara creó el archivo pero no escribió datos.
                // Se borra el archivo para no dejar basura en el almacenamiento.
                archivo.delete()
                // Se muestra un mensaje informativo (no es un error grave).
                mensaje = "Captura cancelada. No se guardó ninguna foto."
            }
        }
    }

    // Función local que abre la cámara.
    // No es un Composable: es una función de Android que prepara el archivo y lanza el Intent.
    fun abrirCamara() {
        // Genera una ruta relativa única (ej: "evidencias/evidencia_1761504000000.jpg")
        val ruta = ArchivosEvidencia.nuevaRutaRelativa()
        try {
            // Guarda la ruta como "pendiente" (se confirma o descarta en el callback del launcher)
            rutaPendiente = ruta
            // Lanza la cámara con una URI de FileProvider (la app de cámara necesita
            // un URI de contenido para escribir el archivo, no una ruta absoluta).
            camaraLauncher.launch(
                ArchivosEvidencia.uriParaCamara(context, ArchivosEvidencia.archivo(context, ruta))
            )
        } catch (e: ActivityNotFoundException) {
            // No hay app de cámara en el dispositivo (raro en un teléfono, posible en un emulador).
            rutaPendiente = null  // Limpiar: no hay captura pendiente
            mensaje = "No hay una aplicación de cámara disponible en este dispositivo."
        }
    }

    // --- LAUNCHER DE PERMISO ---
    // Pide el permiso de cámara al sistema. Android muestra el diálogo nativo
    // ("¿GestionOportunidades puede acceder a la cámara?").
    //
    // ¿Por qué no pedir el permiso en el AndroidManifest y ya?
    // Porque desde Android 6+ (API 23), los "dangerous permissions" (cámara, ubicación, etc.)
    // se deben pedir en tiempo de ejecución. El manifest solo declara que la app
    // NECESITA el permiso; el launcher lo pide al usuario.
    //
    // Comportamiento:
    // - Primera vez: Android muestra el diálogo → usuario acepta o deniega.
    // - Si deniega 1 vez: la próxima vez se vuelve a preguntar.
    // - Si deniega 2 veces: Android NO vuelve a preguntar (queda "bloqueado").
    //   En ese caso, el usuario debe ir manualmente a Ajustes → App → Permisos → Cámara.
    //   Por eso se muestra el botón "Abrir ajustes de la app" cuando permisoDenegado = true.
    val permisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { otorgado ->
        // otorgado = true: el usuario aceptó → se abre la cámara directamente.
        // otorgado = false: el usuario denegó → se muestra el mensaje de error.
        permisoDenegado = !otorgado
        if (otorgado) abrirCamara()  // Si se otorgó, abrir la cámara de inmediato
    }

    // --- UI: COLUMNA PRINCIPAL DE LA SECCIÓN ---
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)  // 8dp entre cada elemento
    ) {
        // Título de la sección
        Text("Evidencia fotográfica", style = MaterialTheme.typography.titleMedium)

        // Nota informativa: el checklist pide "Solo imágenes ficticias o de objetos".
        // Se muestra como recordatorio para el usuario (y para el docente en la demo).
        Text(
            "Usa solo imágenes de objetos o ficticias; no fotografíes personas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // --- BOTÓN: TOMAR FOTO ---
        // Al presionar:
        // 1. Limpia mensajes anteriores.
        // 2. Lanza el permiso launcher (si ya tiene permiso, Android lo "otorga" al instante
        //    sin mostrar diálogo, y el callback se ejecuta con otorgado = true).
        // 3. En el callback del permiso, si otorgado = true → abrirCamara().
        Button(
            onClick = {
                mensaje = null  // Limpiar mensajes anteriores
                permisoLauncher.launch(Manifest.permission.CAMERA)  // Pedir permiso (o confirmar si ya lo tiene)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tomar foto")
        }

        // --- MENSAJE DE PERMISO DENEGADO ---
        // Solo se muestra si el usuario denegó el permiso.
        // Incluye un botón para ir a Ajustes (necesario cuando Android deja de preguntar).
        if (permisoDenegado) {
            Text(
                "Permiso de cámara denegado. No se puede tomar la foto.",
                color = MaterialTheme.colorScheme.error,  // Color semántico de error
                style = MaterialTheme.typography.bodySmall
            )
            // Tras denegar dos veces, Android deja de preguntar: solo se activa desde ajustes
            TextButton(onClick = { abrirAjustesDeLaApp(context) }) {
                Text("Abrir ajustes de la app")
            }
        }

        // --- MENSAJE INFORMATIVO TEMPORAL ---
        // (ej: "Captura cancelada", "No hay app de cámara")
        // Se muestra en color secondary (no es un error grave, es informativo).
        mensaje?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }

        // --- GALERÍA DE FOTOS ---
        if (evidencias.isEmpty()) {
            // Estado vacío: no hay fotos todavía (checklist: "Mostrar estados vacíos")
            Text(
                "Aún no hay fotos para esta oportunidad.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            // LazyRow: fila horizontal scrollable de thumbnails.
            // Solo crea los items visibles (eficiente si hay muchas fotos).
            // key = { it.idEvidencia }: Compose usa el id para identificar cada item
            // (si se borra una foto del medio, no se desfasan las demás).
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(evidencias, key = { it.idEvidencia }) { evidencia ->
                    // Tarjeta clicable: al tocar, muestra la foto ampliada.
                    // size(120.dp): thumbnail cuadrado de 120x120dp.
                    Card(
                        onClick = { fotoAmpliada = evidencia },  // Abrir el AlertDialog ampliado
                        modifier = Modifier.size(120.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        // AsyncImage (Coil 3): carga la imagen del archivo en un hilo de fondo
                        // y la muestra cuando está lista (no bloquea la UI).
                        // model: la ruta absoluta del archivo (Coil la resuelve).
                        // ContentScale.Crop: recorta la imagen para llenar el cuadro de 120dp
                        // (mantiene la proporción, pero puede cortar bordes).
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

    // --- DIALOGO: FOTO AMPLIADA ---
    // Se muestra cuando el usuario toca un thumbnail (fotoAmpliada != null).
    // Muestra la foto a tamaño completo (máx 400dp de alto) con la fecha de captura.
    // Incluye un botón "Eliminar foto" (que abre el diálogo de confirmación) y "Cerrar".
    fotoAmpliada?.let { evidencia ->
        AlertDialog(
            onDismissRequest = { fotoAmpliada = null },  // Tocar fuera → cerrar
            title = { Text(evidencia.fecha.comoFechaHora()) },  // Fecha de captura
            text = {
                // Imagen ampliada: ContentScale.Fit (no recorta, muestra toda la imagen
                // aunque deje espacios en blanco). heightIn(max = 400.dp): no supera 400dp.
                AsyncImage(
                    model = ArchivosEvidencia.archivo(context, evidencia.rutaArchivo),
                    contentDescription = "Evidencia fotográfica ampliada",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    contentScale = ContentScale.Fit
                )
            },
            dismissButton = {
                // Botón "Eliminar foto" en rojo: cierra este diálogo y abre el de confirmación.
                TextButton(
                    onClick = {
                        fotoPorEliminar = evidencia  // Inicia el flujo de eliminación
                        fotoAmpliada = null           // Cierra el diálogo ampliado
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar foto") }
            },
            confirmButton = {
                // Botón "Cerrar": solo cierra el diálogo, no elimina nada.
                TextButton(onClick = { fotoAmpliada = null }) { Text("Cerrar") }
            }
        )
    }

    // --- DIALOGO: CONFIRMACIÓN DE ELIMINACIÓN ---
    // Se muestra cuando el usuario presiona "Eliminar foto" en el diálogo ampliado.
    // Obliga a confirmar antes de borrar (acción irreversible).
    fotoPorEliminar?.let { evidencia ->
        AlertDialog(
            onDismissRequest = { fotoPorEliminar = null },  // Tocar fuera → cancelar
            title = { Text("¿Eliminar esta foto?") },
            text = { Text("Se borrará de forma permanente. Esta acción no se puede deshacer.") },
            confirmButton = {
                // "Eliminar": borra de Room Y del disco (vía onEliminarFoto → ViewModel → Repository).
                TextButton(
                    onClick = {
                        onEliminarFoto(evidencia)  // Notifica al ViewModel
                        fotoPorEliminar = null     // Cierra el diálogo
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                // "Cancelar": solo cierra, no elimina.
                TextButton(onClick = { fotoPorEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

// Abre los ajustes de la app en el sistema (para que el usuario otorgue el permiso manualmente).
// Se usa cuando Android deja de preguntar por el permiso (tras 2 denegaciones).
//
// ¿Cómo funciona?
// - Settings.ACTION_APPLICATION_DETAILS_SETTINGS: abre la página de detalles de la app.
// - Uri.fromParts("package", packageName, null): especifica QUÉ app (la nuestra).
// - El usuario ve: Ajustes → Apps → GestionOportunidades → Permisos → Cámara → Permitir.
//
// Es private: solo se usa dentro de este archivo.
private fun abrirAjustesDeLaApp(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}