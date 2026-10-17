package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
