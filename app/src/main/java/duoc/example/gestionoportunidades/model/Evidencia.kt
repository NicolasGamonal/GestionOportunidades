package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import duoc.example.gestionoportunidades.model.Oportunidad

@Entity(
    tableName = "evidencias",
    foreignKeys = [
        ForeignKey(
            entity = Oportunidad::class,
            parentColumns = ["idOportunidad"],
            childColumns = ["idOportunidad"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idOportunidad")]
)
data class Evidencia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rutaArchivo: String,
    val fecha: Long,
    val idOportunidad: Long
)