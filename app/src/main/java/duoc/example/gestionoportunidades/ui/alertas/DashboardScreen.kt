package duoc.example.gestionoportunidades.ui.alertas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.AlertaSeguimiento
import duoc.example.gestionoportunidades.ui.auth.SesionManager
import duoc.example.gestionoportunidades.ui.theme.colores
import duoc.example.gestionoportunidades.util.comoFechaCorta

// DashboardScreen: pantalla principal que se muestra después del login.
// Muestra 3 secciones con información diferente según el rol:
//
// EJECUTIVO:
// - "Gestiones atrasadas": sus seguimientos cuya fechaProximo ya pasó
// - "Seguimientos pendientes": sus próximas gestiones (hoy o futuro)
// - "Eventos próximos": sus oportunidades con fecha de inicio futura
//
// SUPERVISOR:
// - Lo mismo, pero de TODO el equipo (no solo las suyas)
// - Además muestra el nombre del responsable en cada tarjeta de alerta
//
// ¿Dónde vive? En ui/alertas/ (no ui/dashboard/) por decisión del proyecto.
// El plan original lo llamaba "dashboard" pero la carpeta se quedó como "alertas".
//
// Patrón MVVM:
// - Observa el uiState del DashboardViewModel con collectAsStateWithLifecycle()
// - No tiene lógica de negocio: solo dibuja lo que el ViewModel le pasa
// - Las acciones (navegar a una oportunidad) se delegan al padre vía callbacks
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    // Callback: se llama cuando el usuario toca una tarjeta para navegar al detalle
    onVerOportunidad: (Long) -> Unit
) {
    // Observa el estado del ViewModel. Se actualiza solo cuando el ViewModel cambia el uiState.
    // collectAsStateWithLifecycle(): pausa la colección si la pantalla no es visible (ahorra recursos).
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    // Obtiene el rol del usuario logueado directamente de SesionManager (no del ViewModel).
    // Se usa para decidir el texto del encabezado y si mostrar el responsable en las alertas.
    val esSupervisor = SesionManager.getRol() == "SUPERVISOR"

    // Nombre del usuario logueado (para el saludo "Hola, Juan Pérez").
    // Si es null (no debería pasar después del login), se usa "".
    val nombre = SesionManager.usuarioActual?.nombreCompleto ?: ""

    if (ui.isLoading) {
        // Indicador de carga: se muestra mientras el ViewModel consulta Room.
        // El checklist pide "indicadores de carga mientras se consulta Room".
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // LazyColumn: lista scrollable que solo crea los items visibles (eficiente).
        // contentPadding: margen interno para que las tarjetas no toquen los bordes.
        // verticalArrangement: espacio entre elementos (12dp entre tarjetas y secciones).
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- ENCABEZADO (saludo + subtítulo según rol) ---
            item {
                Column {
                    // El supervisor ve "Resumen del equipo"; el ejecutivo ve "Hola, [nombre]"
                    Text(
                        text = if (esSupervisor) "Resumen del equipo" else "Hola, $nombre",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (esSupervisor) "Ves las oportunidades de todo el equipo"
                        else "Estas son tus gestiones y eventos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- SECCIÓN 1: GESTIONES ATRASADAS (alerta roja) ---
            // Seguimientos cuya fechaProximo ya pasó y no son "Cambio de estado".
            // Icono: Warning (triángulo de advertencia). Color: error (rojo).
            // Si el supervisor, muestra quién es el responsable de cada gestión atrasada.
            seccion("Gestiones atrasadas", ui.seguimientosAtrasados, "No hay gestiones atrasadas") { alerta ->
                DashboardCard(
                    titulo = alerta.nombreOportunidad,
                    detalle = detalleAlerta(alerta, esSupervisor, "venció el"),
                    icono = Icons.Default.Warning,
                    colorIcono = MaterialTheme.colorScheme.error,
                    onClick = { onVerOportunidad(alerta.idOportunidad) }
                )
            }

            // --- SECCIÓN 2: SEGUIMIENTOS PENDIENTES (alerta amarilla) ---
            // Seguimientos cuya fechaProximo es hoy o en el futuro.
            // Icono: Schedule (reloj). Color: advertencia (amarillo, del tema).
            seccion("Seguimientos pendientes", ui.seguimientosPendientes, "No hay seguimientos pendientes") { alerta ->
                DashboardCard(
                    titulo = alerta.nombreOportunidad,
                    detalle = detalleAlerta(alerta, esSupervisor, "para el"),
                    icono = Icons.Default.Schedule,
                    colorIcono = MaterialTheme.colores.advertencia,
                    onClick = { onVerOportunidad(alerta.idOportunidad) }
                )
            }

            // --- SECCIÓN 3: EVENTOS PRÓXIMOS (informativo) ---
            // Oportunidades con fecha de inicio futura que no estén descartadas.
            // Icono: Event (calendario). Color: primary (color principal del tema).
            // Muestra: comuna, estado actual y fecha de inicio (formateada con comoFechaCorta).
            seccion("Eventos próximos", ui.eventosProximos, "No hay eventos próximos") { oportunidad ->
                DashboardCard(
                    titulo = oportunidad.nombre,
                    detalle = "${oportunidad.comuna} · ${oportunidad.estado} · " +
                            oportunidad.fechaInicio.comoFechaCorta(),
                    icono = Icons.Default.Event,
                    colorIcono = MaterialTheme.colorScheme.primary,
                    onClick = { onVerOportunidad(oportunidad.idOportunidad) }
                )
            }
        }
    }
}

// Función genérica que crea un bloque de sección en el LazyColumn.
// Evita repetir el mismo patrón 3 veces (título + contador + estado vacío + items).
//
// Parámetros:
// - titulo: nombre de la sección (ej: "Gestiones atrasadas")
// - elementos: la lista de datos (puede ser vacía)
// - textoVacio: mensaje a mostrar si la lista está vacía (checklist: "estados vacíos")
// - tarjeta: lambda @Composable que dibuja cada item individual
//
// Es private: solo se usa dentro de este archivo.
private fun <T> LazyListScope.seccion(
    titulo: String,
    elementos: List<T>,
    textoVacio: String,
    tarjeta: @Composable (T) -> Unit
) {
    // Título de la sección con contador (ej: "Gestiones atrasadas (2)")
    item {
        Text("$titulo (${elementos.size})", style = MaterialTheme.typography.titleMedium)
    }
    if (elementos.isEmpty()) {
        // Estado vacío: tarjeta gris con mensaje (checklist: "Mostrar estados vacíos")
        item {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = textoVacio,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        // Si hay elementos, se dibuja cada uno con la lambda "tarjeta"
        items(elementos) { tarjeta(it) }
    }
}

// Formatea el texto de detalle de una tarjeta de alerta (gestiones atrasadas o pendientes).
//
// Parámetros:
// - alerta: el objeto AlertaSeguimiento (viene del DAO con JOIN: nombre, responsable, etc.)
// - mostrarResponsable: true si es supervisor (necesita saber de quién es cada gestión)
// - verbo: "venció el" (atrasadas) o "para el" (pendientes) → cambia el tono del mensaje
//
// Ejemplo de salida:
// - Ejecutivo: "Enviar propuesta formal · para el 22 Oct 2026"
// - Supervisor: "Enviar propuesta formal · venció el 18 Oct 2026 · Juan Pérez"
private fun detalleAlerta(alerta: AlertaSeguimiento, mostrarResponsable: Boolean, verbo: String): String {
    // Parte base: próxima acción + verbo + fecha (formateada con comoFechaCorta)
    val base = "${alerta.proximaAccion} · $verbo ${alerta.fechaProximo.comoFechaCorta()}"
    // El supervisor necesita saber de quién es cada gestión
    // nombreResponsable es nullable (puede ser null si la oportunidad no tiene responsable asignado)
    return if (mostrarResponsable) "$base · ${alerta.nombreResponsable ?: "Sin asignar"}" else base
}

// Tarjeta reutilizable del dashboard: icono + título + detalle.
// Se usa en las 3 secciones (atrasadas, pendientes, eventos próximos).
// Es un Composable local (no va en ui/components/) porque solo se usa en esta pantalla.
//
// Parámetros:
// - titulo: nombre de la oportunidad (ej: "Feria del Emprendedor Valle Verde")
// - detalle: texto descriptivo (comuna, estado, fecha, responsable)
// - icono: icono Material (Warning, Schedule, Event)
// - colorIcono: color del icono (error, advertencia, primary)
// - onClick: acción al tocar la tarjeta (navegar al detalle de la oportunidad)
@Composable
fun DashboardCard(
    titulo: String,
    detalle: String,
    icono: ImageVector,
    colorIcono: Color,
    onClick: () -> Unit
) {
    // Card con onClick: toda la tarjeta es clicable (navega al detalle)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono a la izquierda, con el color semántico correspondiente a la sección
            Icon(icono, contentDescription = null, tint = colorIcono)
            Spacer(Modifier.width(12.dp))
            Column {
                // Título (nombre de la oportunidad)
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                // Detalle (comuna, estado, fecha, responsable)
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}