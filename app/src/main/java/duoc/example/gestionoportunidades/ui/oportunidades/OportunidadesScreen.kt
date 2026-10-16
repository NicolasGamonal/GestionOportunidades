package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.data.Oportunidad
import duoc.example.gestionoportunidades.ui.components.PanelFiltrosAnimado
import duoc.example.gestionoportunidades.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OportunidadesScreen(
    onVerDetalle: () -> Unit = {}
) {
    // Estados temporales para la UI (MOCK mientras llega el ViewModel de Room)
    var searchQuery by remember { mutableStateOf("") }
    var mostrarFiltros by remember { mutableStateOf(false) }


    // Datos de ejemplo según pide la planificación
    val oportunidadesMock = listOf(
        // Nota: Asegúrate de que los parámetros coincidan con tu data class Oportunidad actual
        Oportunidad(id = 1, nombreEvento = "Feria Empresarial Sur", organizacion = "Empresa Alpha",
            correoContacto = "contacto@alpha.cl", estado = "Identificada", prioridad = "Alta"),
        Oportunidad(id = 2, nombreEvento = "Activación Municipal",organizacion = "Muni Florida",
            correoContacto = "eventos@florida.cl", estado = "Contacto realizado", prioridad = "Media"),
        Oportunidad(id = 3, nombreEvento = "Jornada de Beneficios", organizacion = "Corpo Santiago",
            correoContacto = "rrhh@corposantiago.cl", estado = "Propuesta enviada", prioridad = "Alta")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de búsqueda y botón de filtros
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar por nombre...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filtros")
            }
        }

        // Panel de filtros animado (Tarea J1 integrada aquí)
        PanelFiltrosAnimado(visible = mostrarFiltros)

        // Estado vacío o Lista de Oportunidades
        if (oportunidadesMock.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay oportunidades registradas", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Lista con LazyColumn y Card requerida por J2[cite: 1]
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(oportunidadesMock) { oportunidad ->
                    OportunidadCard(oportunidad = oportunidad, onClick = onVerDetalle)
                }
            }
        }
        LazyColumn(
            // ...
        ) {
            items(oportunidadesMock) { oportunidad ->
                OportunidadCard(
                    oportunidad = oportunidad,
                    onClick = onVerDetalle // <-- 2. Conectamos el clic de la tarjeta
                )
            }
        }
    }
}

@Composable
fun OportunidadCard(oportunidad: Oportunidad, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = oportunidad.nombreEvento, // Ajusta según tu entidad Oportunidad
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // Uso del Chip animado de la Tarea J1
                StatusChip(estado = oportunidad.estado) // Ajusta según tu entidad
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Prioridad: ${oportunidad.prioridad}", // Ajusta según tu entidad
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}