package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Nota de marketing sobre una oportunidad confirmada (HU-27): material u observaciones para preparar la activación.
@Entity(
    tableName = "notas_marketing",
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
data class NotaMarketing(
    @PrimaryKey(autoGenerate = true)
    val idNota: Long = 0,
    val idOportunidad: Long,
    val tipo: String,          // "Observación" o "Material"
    val texto: String,
    val fechaHora: Long,
    val idUsuario: Long        // quién la escribió (el nombre se obtiene con JOIN, no se repite aquí: 3FN)
)

// Resultado de la consulta con JOIN (no es una tabla). Los alias del DAO deben coincidir con estos nombres.
data class NotaMarketingConAutor(
    val idNota: Long,
    val tipo: String,
    val texto: String,
    val fechaHora: Long,
    val autor: String
)