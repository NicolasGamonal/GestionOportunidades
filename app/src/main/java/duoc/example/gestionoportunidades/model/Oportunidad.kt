package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.model.Organizacion


@Entity(
    tableName = "oportunidades",
    foreignKeys = [
        ForeignKey(
            entity = Organizacion::class,
            parentColumns = ["idOrganizacion"],
            childColumns = ["idOrganizacion"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idResponsable"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["idOrganizacion"]),
        Index(value = ["idResponsable"])
    ]
)
data class Oportunidad(
    @PrimaryKey(autoGenerate = true)
    val idOportunidad: Long = 0,
    val nombre: String,
    val tipoEvento: String,
    val fechaDeteccion: Long,
    val fechaInicio: Long,
    val fechaTermino: Long,
    val comuna: String,
    val publicoEsperado: Int,
    val prioridad: String,
    val estado: String,
    val costoReferencial: Double,
    val observaciones: String,
    val idOrganizacion: Long,
    val idResponsable: Long? = null
)
