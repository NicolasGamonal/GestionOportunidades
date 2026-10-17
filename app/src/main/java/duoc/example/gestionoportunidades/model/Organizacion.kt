package duoc.example.gestionoportunidades.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "organizaciones")
data class Organizacion(
    @PrimaryKey(autoGenerate = true)
    val idOrganizacion: Long = 0,
    val nombre: String,
    val tipo: String,
    val rubro: String,
    val comuna: String
)
