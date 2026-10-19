package duoc.example.gestionoportunidades.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

// TopBar: barra superior reutilizable para todas las pantallas de la app.
//
// ¿Dónde se usa?
// En TODAS las pantallas (Dashboard, Oportunidades, Detalle, Organizaciones,
// Seguimiento, Supervisor, Auth). Cada una lo instancia dentro de su Scaffold
// con el título y los botones que necesita.
//
// ¿Por qué está en ui/components/ y no en cada pantalla?
// Porque se usa en 8+ pantallas. Si cada una implementara su propio TopAppBar,
// habría 8 copias del mismo código con los mismos colores y estructura.
//
// ¿Qué hace?
// - Muestra el título de la pantalla (ej: "Oportunidades", "Detalle").
// - Opcionalmente muestra un botón de "Volver" (flecha izquierda) a la izquierda.
// - Opcionalmente muestra un botón de "Cerrar sesión" (ícono de logout) a la derecha.
// - Usa colores del tema (primary para fondo, onPrimary para texto e íconos)
//   → no hay hexadecimales sueltos (checklist).
//
// ¿Por qué los parámetros son opcionales (con valores por defecto)?
// Porque no todas las pantallas necesitan todos los botones:
// - Dashboard: solo título + cerrar sesión (sin "volver", es la pantalla inicial).
// - Detalle de oportunidad: título + "volver" + cerrar sesión.
// - Login: no usa TopBar (tiene su propio diseño).
//
// @OptIn(ExperimentalMaterial3Api::class): TopAppBar en Material 3 aún es
// "experimental" (puede cambiar su API en futuras versiones). El OptIn
// silencia el warning del compilador.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    // Título de la pantalla (ej: "Oportunidades", "Detalle", "Organizaciones").
    // Se muestra centrado (o a la izquierda, según la configuración de TopAppBar).
    titulo: String,

    // Si es true, se muestra el botón de "Volver" (flecha ←) a la izquierda.
    // Por defecto false: pantallas "raíz" (Dashboard) no lo muestran.
    // Pantallas "hijas" (Detalle, Formulario) lo muestran para regresar.
    mostrarBotonVolver: Boolean = false,

    // Acción al presionar "Volver". Por defecto {} (no hace nada).
    // La Screen lo implementa como: { navController.popBackStack() }
    // Solo tiene efecto si mostrarBotonVolver = true.
    onVolverClick: () -> Unit = {},

    // Acción al presionar "Cerrar sesión". Por defecto null (no se muestra el botón).
    // Si se pasa un lambda, aparece el ícono de logout a la derecha.
    // La Screen lo implementa como: { viewModel.cerrarSesion() }
    // Es nullable (no Boolean) porque permite 3 estados:
    //   null → no mostrar botón
    //   { ... } → mostrar botón con esa acción
    onCerrarSesion: (() -> Unit)? = null
) {
    TopAppBar(
        // Título de la barra
        title = {
            Text(text = titulo, style = MaterialTheme.typography.titleLarge)
        },
        // Ícono de navegación (izquierda): solo se muestra si mostrarBotonVolver = true.
        // Si es false, la navegaciónIcon no se compone (no ocupa espacio).
        navigationIcon = {
            if (mostrarBotonVolver) {
                IconButton(onClick = onVolverClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        // AutoMirrored: la flecha se invierte automáticamente en RTL
                        // (idiomas de derecha a izquierda como árabe). En LTR (español)
                        // apunta a la izquierda como se espera.
                        contentDescription = "Volver"
                    )
                }
            }
        },
        // Acciones (derecha): solo se muestra el botón de cerrar sesión
        // si onCerrarSesion no es null.
        actions = {
            if (onCerrarSesion != null) {
                IconButton(onClick = onCerrarSesion) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Cerrar sesión"
                    )
                }
            }
        },
        // Colores de la barra: todos derivados del tema (sin hexadecimales).
        // - containerColor: fondo de la barra (color primario del tema).
        // - titleContentColor: color del texto del título (contraste sobre primary).
        // - navigationIconContentColor: color de la flecha de "volver".
        // - actionIconContentColor: color del ícono de "cerrar sesión".
        // Usar onPrimary garantiza contraste accesible en ambos temas (claro/oscuro).
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}