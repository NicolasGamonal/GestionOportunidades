package duoc.example.gestionoportunidades.ui.theme

import androidx.compose.ui.graphics.Color

// Colores base de la marca (Ejemplo)
val Primary = Color(0xFF0D47A1)
val Secondary = Color(0xFF1976D2)
val Background = Color(0xFFF5F5F6)

// Colores semánticos (Éxito, Advertencia, Error)
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFFC107)
val ErrorColor = Color(0xFFB3261E) // Evitamos llamarlo "Error" para no chocar con la clase base

// Colores para estados de oportunidad
val EstadoIdentificada = Color(0xFF9E9E9E)
val EstadoContactoRealizado = Color(0xFF2196F3)
val EstadoPropuestaEnviada = Color(0xFFFF9800)
val EstadoConfirmada = Success
val EstadoDescartada = ErrorColor
