package com.agencia.oportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room3.Entity

@Entity(
    tableName = "evidencias",
    foreignKeys = [
        ForeignKey(
            entity = Oportunidad::class,
            parentColumns = ["id"],
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
    val idOportunidad: Int
)