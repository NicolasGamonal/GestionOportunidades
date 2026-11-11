package duoc.example.gestionoportunidades.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.util.Regiones

// Selector de una opción con chips (prioridad, potencial comercial, afinidad, medio de contacto...).
// Es "tonto": recibe qué mostrar y qué hacer al tocar, y no conoce ningún ViewModel; por eso sirve
// para cualquier formulario. FlowRow baja los chips a otra línea si no caben en el ancho.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorChips(
    titulo: String,
    opciones: List<String>,
    seleccionada: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.padding(top = 8.dp)
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = seleccionada == opcion,
                onClick = { onSeleccion(opcion) },
                label = { Text(opcion) }
            )
        }
    }
}

// Selector de comuna en dos pasos: primero la región, luego una comuna de esa región (HU-08, HU-12).
//
// En la base de datos solo se guarda la COMUNA; la región se deriva de ella (Regiones.regionDe).
// Por eso la región elegida es un estado local de este componente y no una columna de Room.
@Composable
fun SelectorComuna(
    comuna: String,
    error: String?,
    onComunaChange: (String) -> Unit
) {
    // Al editar un registro existente, la región se deduce de la comuna guardada.
    var region by remember { mutableStateOf(Regiones.regionDe(comuna) ?: "") }

    // Si la comuna cambia desde fuera (por ejemplo, al terminar de cargar el registro), se sincroniza.
    LaunchedEffect(comuna) {
        Regiones.regionDe(comuna)?.let { region = it }
    }

    CampoDesplegable(
        valor = region,
        etiqueta = "Región",
        opciones = Regiones.nombres,
        textoDe = { it },
        error = null,
        onSeleccion = { nueva ->
            if (nueva != region) {
                region = nueva
                onComunaChange("")   // La comuna anterior ya no pertenece a la región elegida
            }
        }
    )
    CampoDesplegable(
        valor = comuna,
        etiqueta = if (region.isEmpty()) "Comuna (elige primero la región)" else "Comuna",
        opciones = Regiones.comunasDe(region),
        textoDe = { it },
        error = error,
        onSeleccion = onComunaChange
    )
}
