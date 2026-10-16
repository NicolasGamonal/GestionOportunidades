package duoc.example.gestionoportunidades.model

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "contactos",
    foreignKeys = [
        ForeignKey(
            entity = Organizacion::class,
            parentColumns = ["idOrganizacion"],
            childColumns = ["idOrganizacion"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idOrganizacion"])]
)
data class Contacto(
    @PrimaryKey(autoGenerate = true)
    val idContacto: Long = 0,
    val nombre: String,
    val cargo: String,
    val area: String,
    val correo: String,
    val telefono: String,
    val idOrganizacion: Long
)
