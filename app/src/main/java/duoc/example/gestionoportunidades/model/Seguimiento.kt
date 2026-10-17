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
        )
    ],
    indices = [
        Index(value = ["idOportunidad"]),
        Index(value = ["idUsuario"])
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
    val rutaFotoEvidencia: String? = null
)
