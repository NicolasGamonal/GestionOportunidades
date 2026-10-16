package duoc.example.gestionoportunidades.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val idUsuario: Long = 0,
    val username: String,       // El nombre de usuario para el login local ficticio (ej: ejecutivo1)
    val contrasena: String,     // La clave local de prueba
    val nombreCompleto: String, // Nombre real para mostrar en los Dashboards
    val rol: String             // "EJECUTIVO" o "SUPERVISOR"
)
