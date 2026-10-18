package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "seguimientos",
    foreignKeys = [
        ForeignKey(
            entity = Oportunidad::class,
            parentColumns = ["idOportunidad"],
            childColumns = ["idOportunidad"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Contacto::class,
            parentColumns = ["idContacto"],
            childColumns = ["idContacto"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["idOportunidad"]),
        Index(value = ["idUsuario"]),
        Index(value = ["idContacto"])
    ]
)
data class Seguimiento(
    @PrimaryKey(autoGenerate = true)
    val idSeguimiento: Long = 0,
    val idOportunidad: Long,
    val idUsuario: Long,
    val tipoAccion: String,
    val fechaHora: Long,
    val resultado: String,
    val proximaAccion: String,
    val fechaProximo: Long,
    val idContacto: Long? = null      // contacto con quien se hizo la gestión (opcional)
)