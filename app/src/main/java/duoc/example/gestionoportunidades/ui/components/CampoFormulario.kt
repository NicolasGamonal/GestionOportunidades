package duoc.example.gestionoportunidades.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import duoc.example.gestionoportunidades.util.deDatePickerAFechaCorta
import duoc.example.gestionoportunidades.util.fechaCortaADatePicker

@Composable
private fun IconoError() {
    Icon(
        imageVector = Icons.Filled.Error,
        contentDescription = "Campo con error",
        tint = MaterialTheme.colorScheme.error
    )
}

/** Campo de texto con el patrón de error del checklist: isError, supportingText e ícono. */
@Composable
fun CampoFormulario(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    error: String?,                       // null = válido
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        trailingIcon = { if (error != null) IconoError() },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier.fillMaxWidth()
    )
}

/** Campo de solo lectura que reacciona al toque (base de la fecha y del desplegable). */
@Composable
fun CampoSelector(
    valor: String,
    etiqueta: String,
    error: String?,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            isError = error != null,
            supportingText = { if (error != null) Text(error) },
            trailingIcon = {
                if (error != null) IconoError() else Icon(icono, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente: un campo de solo lectura no entrega el toque por sí solo
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
fun <T> CampoDesplegable(
    valor: String,
    etiqueta: String,
    opciones: List<T>,
    textoDe: (T) -> String,
    error: String?,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        CampoSelector(
            valor = valor,
            etiqueta = etiqueta,
            error = error,
            icono = Icons.Filled.ArrowDropDown,
            onClick = { expandido = true }
        )
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    onClick = {
                        onSeleccion(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    valor: String,                         // "dd/MM/yyyy" o vacío
    etiqueta: String,
    error: String?,
    onFechaElegida: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    CampoSelector(
        valor = valor,
        etiqueta = etiqueta,
        error = error,
        icono = Icons.Filled.CalendarMonth,
        onClick = { abierto = true },
        modifier = modifier
    )

    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = valor.fechaCortaADatePicker())
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onFechaElegida(it.deDatePickerAFechaCorta()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}