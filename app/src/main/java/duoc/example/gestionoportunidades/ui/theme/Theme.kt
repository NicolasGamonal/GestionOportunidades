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

private val LocalColoresSemanticos = staticCompositionLocalOf { ColoresSemanticosClaro }

/** Acceso a los colores semánticos: MaterialTheme.colores.exito, .paraEstado(...), etc. */
val MaterialTheme.colores: ColoresSemanticos
    @Composable
    @ReadOnlyComposable
    get() = LocalColoresSemanticos.current

@Composable
fun GestionOportunidadesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Apagado por defecto (lo exige el plan): la app usa su propia paleta, no la del fondo de pantalla
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EsquemaOscuro
        else -> EsquemaClaro
    }
    val semanticos = if (darkTheme) ColoresSemanticosOscuro else ColoresSemanticosClaro

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // La barra superior de la app es del color primario: los íconos de la barra de
            // estado deben contrastar con él (oscuros sobre un primario claro, y al revés)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme.primary.luminance() > 0.5f
        }
    }

    CompositionLocalProvider(LocalColoresSemanticos provides semanticos) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}