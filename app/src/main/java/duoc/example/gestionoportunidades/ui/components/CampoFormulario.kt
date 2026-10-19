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

// Archivo de componentes reutilizables de formularios.
// Vive en ui/components/ porque se usa en 2+ pantallas:
// - FormularioOportunidadScreen (crear/editar oportunidad)
// - FormularioOrganizacionScreen (crear/editar organización)
// - FormularioContactoScreen (crear/editar contacto)
// - FormularioSeguimientoScreen (nueva gestión)
//
// ¿Por qué no están dentro de cada pantalla?
// Porque el patrón de error (isError + supportingText + ícono) es idéntico
// en todos los formularios. Duplicarlo 4 veces violaría DRY (Don't Repeat Yourself).
//
// Cumple el checklist:
// - "Mostrar mensajes específicos debajo de los campos incorrectos" → supportingText
// - "Utilizar indicadores visuales de error, supportingText e íconos apropiados" → isError + IconoError
// - "Mantener los valores ingresados cuando falle una validación" → el valor no se borra, solo se muestra el error

// Ícono de error (triángulo con exclamación) que aparece a la derecha del campo
// cuando hay un error de validación.
// Es private: solo se usa dentro de este archivo (en CampoFormulario y CampoSelector).
// tint = MaterialTheme.colorScheme.error: usa el color semántico de error del tema (no un hex suelto).
@Composable
private fun IconoError() {
    Icon(
        imageVector = Icons.Filled.Error,
        contentDescription = "Campo con error",
        tint = MaterialTheme.colorScheme.error
    )
}

// Campo de texto reutilizable con el patrón de error del checklist.
// Se usa para todos los campos de texto en los formularios (nombre, correo, teléfono, etc.).
//
// Parámetros:
// - valor: el texto actual del campo (lo controla el ViewModel vía UiState).
// - onValorChange: se llama en cada tecla para actualizar el estado del ViewModel.
//   El ViewModel valida y actualiza el error en el UiState.
// - etiqueta: el label del campo (ej: "Nombre", "Correo electrónico").
// - error: mensaje de error o null (null = válido, no se muestra nada).
//   Viene del UiState del ViewModel (que llama a Validaciones.kt).
// - modifier: para que el padre controle el ancho/margen.
// - keyboardType: tipo de teclado (Text, Phone, Email, Number, etc.).
// - singleLine: si es true, no permite saltos de línea (campos cortos).
// - minLines: líneas mínimas del campo (para campos de texto largo como "observaciones").
//
// ¿Por qué el valor viene de afuera (no es remember)?
// Porque el checklist pide "Mantener los valores ingresados cuando falle una validación".
// Si el valor viviera en remember local, al fallar la validación y el ViewModel
// actualizar el UiState, el campo se resetearía. Al venir del UiState, el valor
// se conserva porque el ViewModel no lo borra, solo agrega el error.
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
        isError = error != null,                          // Borde rojo si hay error
        supportingText = { if (error != null) Text(error) },  // Mensaje debajo del campo
        trailingIcon = { if (error != null) IconoError() },   // Ícono de error a la derecha
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier.fillMaxWidth()
    )
}

// Campo de solo lectura que reacciona al toque.
// Es la base de CampoDesplegable (dropdown) y CampoFecha (date picker).
//
// ¿Por qué no usar directamente un OutlinedTextField con readOnly?
// Porque un campo readOnly en Compose NO entrega el evento de clic al usuario
// (el toque lo "traga" el campo). Para hacer que sea clicable, se superpone
// una Box transparente con .clickable() encima del campo.
//
// matchParentSize(): hace que la Box transparente tenga exactamente el mismo
// tamaño que el OutlinedTextField (que es el primer hijo del Box padre).
//
// Parámetros:
// - valor: el texto a mostrar (ej: "15/10/2026" o "Alta").
// - etiqueta: el label del campo.
// - error: mensaje de error o null.
// - icono: el ícono a mostrar a la derecha (flecha para dropdown, calendario para fecha).
//   Si hay error, se reemplaza por el IconoError.
// - onClick: acción al tocar (abrir dropdown o date picker).
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
        // El campo visual: solo lectura, no se puede escribir en él.
        OutlinedTextField(
            value = valor,
            onValueChange = {},            // Vacío: no se puede modificar por teclado
            readOnly = true,
            label = { Text(etiqueta) },
            isError = error != null,
            supportingText = { if (error != null) Text(error) },
            trailingIcon = {
                // Si hay error → ícono de error; si no → el ícono del tipo de selector
                if (error != null) IconoError() else Icon(icono, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente clicable: se superpone al campo para capturar el toque.
        // matchParentSize(): ocupa exactamente el mismo espacio que el OutlinedTextField.
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

// Campo desplegable (dropdown) genérico para seleccionar de una lista de opciones.
// Se usa para: prioridad (Alta/Media/Baja), estado, tipo de evento, tipo de acción, etc.
//
// Es genérico en T: las opciones pueden ser Strings, enums, o cualquier tipo.
// - opciones: la lista de elementos a mostrar.
// - textoDe: lambda que extrae el texto a mostrar de cada elemento.
//   Ej: { it } si son Strings, { it.nombre } si son data classes.
// - onSeleccion: se llama con el elemento seleccionado (el ViewModel lo procesa).
//
// Funcionamiento:
// 1. El usuario toca el campo → expandido = true → aparece el DropdownMenu.
// 2. El usuario toca una opción → onSeleccion(opcion) → expandido = false (se cierra).
// 3. El usuario toca fuera del menú → onDismissRequest → expandido = false.
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
    // Estado local: si el menú está abierto o no.
    // No va en el UiState del ViewModel porque es un detalle visual efímero
    // (no afecta la lógica de negocio ni persiste entre composiciones).
    var expandido by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        // El campo visual (solo lectura con ícono de flecha hacia abajo)
        CampoSelector(
            valor = valor,
            etiqueta = etiqueta,
            error = error,
            icono = Icons.Filled.ArrowDropDown,
            onClick = { expandido = true }
        )
        // El menú desplegable: se muestra solo cuando expandido = true
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            // Itera las opciones y crea un item por cada una
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },  // Texto a mostrar (ej: "Alta", "Media")
                    onClick = {
                        onSeleccion(opcion)   // Notifica al ViewModel la selección
                        expandido = false     // Cierra el menú
                    }
                )
            }
        }
    }
}

// Campo de fecha con DatePicker de Material 3.
// Se usa para: fechaDeteccion, fechaInicio, fechaTermino (oportunidad),
// fechaProximo (seguimiento).
//
// Funcionamiento:
// 1. El usuario toca el campo → abierto = true → aparece el DatePickerDialog.
// 2. El usuario elige una fecha y presiona "Aceptar" →
//    onFechaElegida(seleccion en formato "dd/MM/yyyy") → abierto = false.
// 3. El usuario presiona "Cancelar" o toca fuera → abierto = false (no se cambia la fecha).
//
// Conversión de formatos:
// - fechaCortaADatePicker(): "15/10/2026" → timestamp en milisegundos (para el DatePicker).
// - deDatePickerAFechaCorta(): timestamp en milisegundos → "15/10/2026" (para guardar en el UiState).
// Estas funciones viven en util/ (separadas de los Composables, como pide el checklist).
//
// ¿Por qué se guarda como String "dd/MM/yyyy" en el UiState y no como Long?
// Porque el UiState es lo que se muestra en el campo de texto. Si se guardara como Long,
// habría que convertirlo a String en cada composición. Al guardarlo ya formateado,
// el campo lo muestra directamente. La conversión a Long (para guardar en Room)
// la hace el ViewModel al momento de insertar/actualizar.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    valor: String,                         // "dd/MM/yyyy" o vacío
    etiqueta: String,
    error: String?,
    onFechaElegida: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado local: si el date picker está abierto.
    var abierto by remember { mutableStateOf(false) }

    // El campo visual (solo lectura con ícono de calendario)
    CampoSelector(
        valor = valor,
        etiqueta = etiqueta,
        error = error,
        icono = Icons.Filled.CalendarMonth,
        onClick = { abierto = true },
        modifier = modifier
    )

    // El diálogo de fecha: solo se compone cuando abierto = true (eficiente).
    if (abierto) {
        // rememberDatePickerState: recuerda la fecha seleccionada mientras el diálogo está abierto.
        // initialSelectedDateMillis: pre-selecciona la fecha actual del campo (si hay una).
        // Si valor es "" (vacío), fechaCortaADatePicker() devuelve null → no hay pre-selección.
        val estado = rememberDatePickerState(initialSelectedDateMillis = valor.fechaCortaADatePicker())
        DatePickerDialog(
            onDismissRequest = { abierto = false },  // Tocar fuera del diálogo → cerrar
            confirmButton = {
                // Botón "Aceptar": toma la fecha seleccionada, la convierte a "dd/MM/yyyy"
                // y la pasa al ViewModel vía onFechaElegida.
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onFechaElegida(it.deDatePickerAFechaCorta()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                // Botón "Cancelar": cierra sin cambiar la fecha.
                TextButton(onClick = { abierto = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estado)  // El widget de calendario de Material 3
        }
    }
}