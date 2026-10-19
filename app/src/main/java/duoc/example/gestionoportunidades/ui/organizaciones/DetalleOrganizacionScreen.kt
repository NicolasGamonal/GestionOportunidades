package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.ui.components.PantallaMensaje
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleItem

// DetalleOrganizacionScreen: pantalla de detalle de una organización.
//
// ¿Dónde se llega?
// - Desde OrganizacionesScreen: tocar una tarjeta en la lista.
//
// ¿Qué muestra?
// 1. Cabecera: ícono de edificio + nombre de la organización.
// 2. Datos: tipo, rubro, comuna (DetalleItem × 3).
// 3. Botón "Editar organización" (solo si tiene permiso: supervisor).
// 4. Divider + sección de contactos:
//    - Título con contador: "Contactos (3)"
//    - Estado vacío: "Esta organización aún no tiene contactos."
//    - Lista de ContactoCard (una por cada contacto).
// 5. Botón "Agregar contacto" (siempre visible).
//
// ¿Por qué reutiliza DetalleItem de ui/oportunidades/?
// Porque es el mismo patrón visual (label gris + valor negro) y no tiene
// nada específico de oportunidades. Está en un archivo público (no private)
// precisamente para ser reutilizado. Si en el futuro se usara en 3+ pantallas,
// se movería a ui/components/.
//
// ¿Por qué puedeEditar es un Boolean en el UiState y no se calcula aquí?
// Porque la regla de permisos (solo el supervisor edita organizaciones)
// vive en el ViewModel (que usa Permisos.kt). La Screen solo dibuja
// el botón si el ViewModel dice que se puede.
//
// Patrón MVVM:
// - Observa el uiState del DetalleOrganizacionViewModel con collectAsStateWithLifecycle().
// - No tiene lógica de negocio: los permisos y la carga de datos los maneja el ViewModel.
// - Las acciones (editar, nuevo contacto, editar contacto) se delegan al padre (Navegacion.kt)
//   vía callbacks.

@Composable
fun DetalleOrganizacionScreen(
    // ViewModel inyectado desde Navegacion.kt (con AppViewModelProvider.Factory).
    // Contiene: organizacion (Flow reactivo), contactos (Flow reactivo),
    // puedeEditar (permiso), isLoading.
    viewModel: DetalleOrganizacionViewModel,
    // Navegar de vuelta a la lista de organizaciones.
    onVolver: () -> Unit,
    // Navegar al formulario de edición de esta organización.
    // Se llama con el idOrganizacion: Navegacion.kt lo usa para
    // navController.navigate(Ruta.FormularioOrganizacion.editar(id))
    onEditar: (Long) -> Unit,
    // Navegar al formulario de creación de un contacto nuevo.
    // Se llama con el idOrganizacion (para saber a qué org pertenece el contacto):
    // navController.navigate(Ruta.FormularioContacto.crear(idOrg))
    onNuevoContacto: (Long) -> Unit,
    // Navegar al formulario de edición de un contacto existente.
    // Se llama con (idOrganizacion, idContacto):
    // navController.navigate(Ruta.FormularioContacto.editar(idOrg, idContacto))
    onEditarContacto: (Long, Long) -> Unit      // (idOrganizacion, idContacto)
) {
    // Observa el estado del ViewModel. Se actualiza cuando:
    // - Termina la carga inicial (isLoading → false)
    // - Se agrega/edita/elimina un contacto (contactos se actualiza vía Flow)
    // - Se edita la organización (organizacion se actualiza vía Flow)
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    // La organización a mostrar (null = no encontrada o aún cargando)
    val org = ui.organizacion

    // Estados de la pantalla (en orden de prioridad):
    when {
        // 1. CARGANDO: spinner centrado mientras el ViewModel consulta Room.
        ui.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        // 2. NO ENCONTRADA: el id no existe en Room (se eliminó o es inválido).
        org == null -> PantallaMensaje("No se encontró la organización", onVolver)

        // 3. CONTENIDO NORMAL: LazyColumn con todos los datos y contactos.
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- CABECERA: ícono + nombre ---
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ícono de edificio (Business): indica que es una organización/empresa.
                    // tint = primary: usa el color primario del tema (no un hex suelto).
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    // Nombre de la organización (título principal, color primario)
                    Text(
                        text = org.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // --- DATOS DE LA ORGANIZACIÓN ---
            // 3 DetalleItem: tipo, rubro y comuna.
            // Reutiliza el componente de ui/oportunidades/ (mismo patrón visual).
            item {
                Column {
                    DetalleItem("Tipo", org.tipo)        // "Municipalidad", "Empresa privada", etc.
                    DetalleItem("Rubro", org.rubro)      // "Gobierno", "Logística", "Salud", etc.
                    DetalleItem("Comuna", org.comuna)    // "Providencia", "Maipú", etc.
                }
            }

            // --- BOTÓN: EDITAR (solo si tiene permiso) ---
            // ui.puedeEditar: lo calcula el ViewModel con Permisos.puedeEditarOrganizacion().
            // Solo el SUPERVISOR puede editar organizaciones (son datos compartidos por todo el equipo).
            // El ejecutivo NO ve este botón.
            if (ui.puedeEditar) {
                item {
                    OutlinedButton(onClick = { onEditar(org.idOrganizacion) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Editar organización")
                    }
                }
            }

            // --- SEPARADOR + TÍTULO DE CONTACTOS ---
            // HorizontalDivider: línea horizontal que separa los datos de la sección de contactos.
            // El contador (ui.contactos.size) muestra cuántos contactos tiene la organización.
            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Contactos (${ui.contactos.size})", style = MaterialTheme.typography.titleMedium)
            }

            // --- LISTA DE CONTACTOS (o estado vacío) ---
            if (ui.contactos.isEmpty()) {
                // Estado vacío: no hay contactos registrados para esta organización.
                // (Checklist: "Mostrar estados vacíos")
                item {
                    Text(
                        "Esta organización aún no tiene contactos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Una ContactoCard por cada contacto.
                // key = { it.idContacto }: Compose identifica cada item por su id
                // (si se borra uno del medio, los demás no se desfasan).
                // Al tocar el ícono de editar → onEditarContacto(idOrg, idContacto).
                items(ui.contactos, key = { it.idContacto }) { contacto ->
                    ContactoCard(contacto) { onEditarContacto(org.idOrganizacion, contacto.idContacto) }
                }
            }

            // --- BOTÓN: AGREGAR CONTACTO ---
            // Siempre visible (no depende de permisos: cualquier rol puede agregar contactos,
            // aunque la edición de la organización sea solo del supervisor).
            // Al presionar: navega al formulario de contacto (modo creación) con el idOrganizacion.
            item {
                Button(onClick = { onNuevoContacto(org.idOrganizacion) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar contacto")
                }
            }
        }
    }
}

// ContactoCard: tarjeta individual de un contacto dentro del detalle de organización.
//
// ¿Por qué es private y no va en ui/components/?
// Porque solo se usa en esta pantalla (DetalleOrganizacionScreen).
// Si en el futuro se necesitara en otra pantalla, se movería a ui/components/.
//
// ¿Qué muestra?
// - Nombre del contacto (título)
// - Cargo + área (línea 2, gris)
// - Correo (línea 3, pequeño)
// - Teléfono (línea 4, pequeño)
// - Ícono de editar a la derecha (navega al formulario del contacto)
//
// Layout: Row con Column (weight 1f) a la izquierda + IconButton a la derecha.
// El padding es asimétrico (end = 4.dp en vez de 16.dp) porque el IconButton
// ya tiene su propio padding interno (48dp de área táctil).
@Composable
private fun ContactoCard(contacto: Contacto, onEditar: () -> Unit) {
    // Card con elevación mínima (1.dp): se distingue sutilmente del fondo sin ser prominente.
    // (La TarjetaOportunidad usa 2.dp; aquí se usa 1.dp porque son items secundarios.)
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            // Padding asimétrico: más espacio a la izquierda (16dp) y menos a la derecha (4dp)
            // porque el IconButton ya tiene área táctil de 48dp (que incluye padding interno).
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna de datos del contacto (ocupa todo el ancho disponible con weight)
            Column(Modifier.weight(1f)) {
                // Nombre (título del contacto)
                Text(contacto.nombre, style = MaterialTheme.typography.titleMedium)
                // Cargo + área (ej: "Directora de Eventos · Comunicaciones")
                Text("${contacto.cargo} · ${contacto.area}", style = MaterialTheme.typography.bodyMedium)
                // Correo electrónico (tamaño pequeño, para no saturar visualmente)
                Text(contacto.correo, style = MaterialTheme.typography.bodySmall)
                // Teléfono (tamaño pequeño)
                Text(contacto.telefono, style = MaterialTheme.typography.bodySmall)
            }
            // Botón de editar: ícono de lápiz.
            // Al presionar: onEditar() → onEditarContacto(idOrg, idContacto) → navega al formulario.
            // contentDescription: accesibilidad (lectores de pantalla leen "Editar contacto").
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar contacto")
            }
        }
    }
}