package duoc.example.gestionoportunidades.ui.organizaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.data.Organizacion
import duoc.example.gestionoportunidades.data.Contacto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizacionesScreen(
    onNuevaOrganizacion: () -> Unit // Lo usaremos en el siguiente paso para el formulario
) {
    // MOCK: Datos ficticios basados en el modelo de datos del plan
    // Nota: Si los parámetros de tus data classes en la carpeta 'data' son distintos, ajústalos aquí.
    val organizacionesMock = listOf(
        Organizacion(
            id = 1,
            nombre = "Empresa Alpha",
            tipo = "Empresa",
            rubro = "Tecnología",
            comuna = "Providencia"
        ),
        Organizacion(
            id = 2,
            nombre = "Muni Florida",
            tipo = "Municipalidad",
            rubro = "Sector Público",
            comuna = "La Florida"
        )
    )

    // MOCK: Contactos ficticios con cargo o área como pide la tarea J3[cite: 1]
    val contactosMock = listOf(
        Contacto(
            id = 1,
            nombre = "Juan Pérez",
            cargo = "Gerente de RRHH",
            area = "Recursos Humanos",
            correo = "jperez@alpha.cl",
            telefono = "+56912345678",
            idOrganizacion = 1
        ),
        Contacto(
            id = 2,
            nombre = "María Gómez",
            cargo = "Encargada de Eventos",
            area = "Comunicaciones",
            correo = "mgomez@florida.cl",
            telefono = "+56987654321",
            idOrganizacion = 2
        )
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaOrganizacion, // <-- USARLO AQUÍ
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Organización")
            }
        }

    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(organizacionesMock) { org ->
                // Buscamos el contacto que pertenece a esta organización
                val contactoAsociado = contactosMock.find { it.idOrganizacion == org.id }
                OrganizacionCard(organizacion = org, contacto = contactoAsociado)
            }
        }
    }
}

@Composable
fun OrganizacionCard(organizacion: Organizacion, contacto: Contacto?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = organizacion.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${organizacion.tipo} - ${organizacion.rubro} (${organizacion.comuna})")

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (contacto != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contacto Principal",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${contacto.nombre} - ${contacto.cargo} (${contacto.area})", style = MaterialTheme.typography.bodyMedium)
                Text(text = contacto.correo, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = "Sin contacto registrado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}