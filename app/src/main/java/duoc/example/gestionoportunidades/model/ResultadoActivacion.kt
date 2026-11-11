package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entidad "ResultadoActivacion" (HU-24): lo que pasó realmente en un evento en el que la agencia
// participó (asistentes, contactos generados, inscripciones, evaluación y aprendizajes).
//
// Relación: Oportunidad → ResultadoActivacion es 1 a 0..1 (una oportunidad tiene como máximo un
// resultado). Esa regla se garantiza con el índice ÚNICO sobre idOportunidad.
//
// 3FN: el nombre de la oportunidad NO se repite aquí; se referencia por idOportunidad.
@Entity(
    tableName = "resultados_activacion",
    foreignKeys = [
        // Si se borra la oportunidad, su resultado se borra con ella.
        ForeignKey(
            entity = Oportunidad::class,
            parentColumns = ["idOportunidad"],
            childColumns = ["idOportunidad"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idOportunidad"], unique = true)]
)
data class ResultadoActivacion(
    @PrimaryKey(autoGenerate = true)
    val idResultado: Long = 0,

    val idOportunidad: Long,

    // Personas que asistieron realmente al evento (>= 0).
    val asistentes: Int,

    // Contactos comerciales generados durante la activación (>= 0).
    val contactosGenerados: Int,

    // Inscripciones o convenios captados (>= 0).
    val inscripciones: Int,

    // Evaluación global de la activación, de 1 (mala) a 5 (excelente).
    val evaluacion: Int,

    // Aprendizajes y observaciones para futuras activaciones.
    val aprendizajes: String,

    // Cuándo se registró el resultado (timestamp en milisegundos).
    val fechaRegistro: Long,

    // Quién lo registró (id del usuario). Se guarda como dato; no es clave foránea para no
    // bloquear cambios en la tabla de usuarios.
    val idUsuario: Long
)
