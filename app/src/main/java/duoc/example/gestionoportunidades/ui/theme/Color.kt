package duoc.example.gestionoportunidades.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Todos los colores de la app viven en ui/theme. Las pantallas usan solo
// MaterialTheme.colorScheme y MaterialTheme.colores (colores semánticos).

val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF1976D2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE8F7),
    onSecondaryContainer = Color(0xFF0F2A4A),
    tertiary = Color(0xFF00695C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBDF0E6),
    onTertiaryContainer = Color(0xFF00201B),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF5F5F6),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFAFAFC),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    surfaceTint = Color(0xFF0D47A1),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFA9C7FF),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceBright = Color(0xFFFAFAFC),
    surfaceDim = Color(0xFFDADADD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F6FA),
    surfaceContainer = Color(0xFFEFF0F5),
    surfaceContainerHigh = Color(0xFFE9EAEF),
    surfaceContainerHighest = Color(0xFFE3E4E9)
)

val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF0F3F85),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFB0C6EA),
    onSecondary = Color(0xFF1B3050),
    secondaryContainer = Color(0xFF2E4770),
    onSecondaryContainer = Color(0xFFD9E3FA),
    tertiary = Color(0xFF7FD9C9),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = Color(0xFFBDF0E6),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceTint = Color(0xFFA9C7FF),
    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF2D5FA8),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    surfaceBright = Color(0xFF37393E),
    surfaceDim = Color(0xFF111318),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A)
)