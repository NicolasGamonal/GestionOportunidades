package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.components.MensajeConfirmacion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioOrganizacionScreen(
    onVolver: () -> Unit
) {
    // Estados Organización
    var nombreOrg by remember { mutableStateOf("") }
    var tipoOrg by remember { mutableStateOf("") }
    var rubro by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }

    // Estados Contacto Principal
    var nombreContacto by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    // Estado animación de éxito
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MensajeConfirmacion(visible = mostrarConfirmacion, mensaje = "Organización guardada")

        Text("Nueva Organización", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(value = nombreOrg, onValueChange = { nombreOrg = it }, label = { Text("Nombre Organización") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = tipoOrg, onValueChange = { tipoOrg = it }, label = { Text("Tipo (Ej: Empresa)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = rubro, onValueChange = { rubro = it }, label = { Text("Rubro") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = comuna, onValueChange = { comuna = it }, label = { Text("Comuna") }, modifier = Modifier.fillMaxWidth())

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Contacto Principal", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)

        OutlinedTextField(value = nombreContacto, onValueChange = { nombreContacto = it }, label = { Text("Nombre del Contacto") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = cargo, onValueChange = { cargo = it }, label = { Text("Cargo") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Área") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(
                onClick = {
                    coroutineScope.launch {
                        mostrarConfirmacion = true
                        delay(2000)
                        mostrarConfirmacion = false
                        onVolver()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = nombreOrg.isNotBlank()
            ) { Text("Guardar") }
        }
    }
}