package duoc.example.gestionoportunidades.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.ui.components.PantallaMensaje
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleItem

// Pantalla de perfil del usuario con sesión iniciada (HU-02).
// Muestra nombre, usuario, rol, área y zona. Los datos salen de SesionManager (el usuario que se
// guardó al iniciar sesión), así que no necesita ViewModel ni consultas a Room.
@Composable
fun PerfilScreen(onCerrarSesion: () -> Unit) {
    val usuario = SesionManager.usuarioActual

    if (usuario == null) {
        PantallaMensaje("No hay una sesión activa", onCerrarSesion)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = usuario.nombreCompleto,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = when (usuario.rol) {
                "SUPERVISOR" -> "Supervisor comercial"
                "MARKETING" -> "Marketing"
                else -> "Ejecutivo comercial"
            },
            style = MaterialTheme.typography.titleMedium
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            DetalleItem("Usuario", usuario.username)
            DetalleItem("Rol", usuario.rol)
            DetalleItem("Área", usuario.area.ifBlank { "—" })
            DetalleItem("Zona", usuario.zona.ifBlank { "—" })
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }
}
