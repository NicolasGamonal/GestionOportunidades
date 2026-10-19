package duoc.example.gestionoportunidades.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Tema de la app: define el colorScheme, la tipografía y los colores semánticos.
// dynamicColor = false por defecto (el plan lo exige: la app usa su propia paleta).
// Los colores semánticos (exito, advertencia, error, estados de oportunidad)
// se exponen vía MaterialTheme.colores (CompositionLocal).

// CompositionLocal: permite acceder a los colores semánticos desde cualquier Composable
// sin pasarlos como parámetro. Se inyecta en GestionOportunidadesTheme.
private val LocalColoresSemanticos = staticCompositionLocalOf { ColoresSemanticosClaro }

// Extensión de MaterialTheme para acceder a los colores semánticos:
// MaterialTheme.colores.exito, MaterialTheme.colores.advertencia,
// MaterialTheme.colores.paraEstado("Confirmada"), etc.
val MaterialTheme.colores: ColoresSemanticos
    @Composable
    @ReadOnlyComposable
    get() = LocalColoresSemanticos.current

@Composable
fun GestionOportunidadesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Apagado por defecto: la app no adapta colores al wallpaper del usuario
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Elegir el colorScheme según tema y dynamicColor
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EsquemaOscuro
        else -> EsquemaClaro
    }
    // Colores semánticos según tema (valores distintos para claro/oscuro)
    val semanticos = if (darkTheme) ColoresSemanticosOscuro else ColoresSemanticosClaro

    // Ajustar la barra de estado del sistema: los íconos (reloj, batería)
    // deben contrastar con el color primario de la TopBar.
    // Si el primario es claro (luminancia > 0.5) → íconos oscuros, y viceversa.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme.primary.luminance() > 0.5f
        }
    }

    // Proveye los colores semánticos + aplica el tema de Material 3
    CompositionLocalProvider(LocalColoresSemanticos provides semanticos) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}