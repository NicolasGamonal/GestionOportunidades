package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @PrimaryKey(autoGenerate = true) val idEvidencia: Long = 0,
    val rutaArchivo: String,      // relativa a filesDir, p. ej. "evidencias/evidencia_123.jpg"
    val fecha: Long,
    val idOportunidad: Long
)