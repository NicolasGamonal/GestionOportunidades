package duoc.example.gestionoportunidades.ui.oportunidades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.data.Oportunidad
import duoc.example.gestionoportunidades.ui.components.PanelFiltrosAnimado
import duoc.example.gestionoportunidades.ui.components.StatusChip

@Composable
fun OportunidadesScreen(
    onVerDetalle: () -> Unit = {}
) {
    // Estados temporales para la UI (MOCK mientras llega el ViewModel de Room en J2)
    var searchQuery by remember { mutableStateOf("") }
    var mostrarFiltros by remember { mutableStateOf(false) }

    val oportunidadesMock = listOf(
        Oportunidad(
            id = 1,
            nombre = "Feria Tecnológica Sur",
            tipoEvento = "Feria",
            fechaDeteccion = "2026-10-10",
            fechaInicio = "2026-10-25 10:00",
            fechaTermino = "2026-10-25 18:00",
            comuna = "Puente Alto",
            publicoEsperado = 1000,
            prioridad = "Alta",
            estado = "Identificada",
            costoReferencial = 250000,
            observaciones = "Gran oportunidad de visibilidad.",
            idOrganizacion = 1
        ),
        Oportunidad(
            id = 2,
            nombre = "Jornada de Salud",
            tipoEvento = "Jornada",
            fechaDeteccion = "2026-10-12",
            fechaInicio = "2026-11-05 09:00",
            fechaTermino = "2026-11-05 13:00",
            comuna = "La Florida",
            publicoEsperado = 300,
            prioridad = "Media",
            estado = "Contacto realizado",
            costoReferencial = 100000,
            observaciones = "Se requiere confirmación de stand.",
            idOrganizacion = 2
        )
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

        // Panel de filtros animado (animación de J1)
        PanelFiltrosAnimado(visible = mostrarFiltros)

        // Estado vacío o lista de oportunidades
        if (oportunidadesMock.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay oportunidades registradas", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
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
                    text = oportunidad.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(estado = oportunidad.estado)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Prioridad: ${oportunidad.prioridad}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}