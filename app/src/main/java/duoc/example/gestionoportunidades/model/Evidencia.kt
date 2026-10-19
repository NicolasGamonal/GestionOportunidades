package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entidad "Evidencias": fotos capturadas con la cámara del dispositivo.
// Cada evidencia está asociada a una oportunidad (FK idOportunidad).
//
// Relación con otras entidades:
// - Oportunidad → Evidencia: 1 a muchos (una oportunidad puede tener varias fotos)
//
// 3FN: solo se guarda la ruta del archivo (no el contenido de la foto).
// El archivo físico (.jpg/.png) vive en el almacenamiento interno de la app
// (context.filesDir/evidencias/). Room solo guarda la referencia (rutaArchivo).
//
// ¿Por qué no se guarda la imagen en Room (como BLOB)?
// Porque las fotos pueden pesar varios MB. Guardarlas en SQLite haría la base
// muy pesada y lenta. Es mejor guardar el archivo en el disco y solo la ruta en Room.
@Entity(
    tableName = "evidencias",
    // FK a Oportunidad: cada evidencia DEBE pertenecer a una oportunidad.
    // onDelete = CASCADE: si se borra la oportunidad, sus fotos se borran de Room automáticamente.
    // (El archivo físico en el disco se borra manualmente desde EvidenciaRepository.eliminar().)
    foreignKeys = [
        ForeignKey(
            entity = Oportunidad::class,
            parentColumns = ["idOportunidad"],
            childColumns = ["idOportunidad"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Índice en idOportunidad: acelera la consulta "obtener todas las fotos de una oportunidad"
    // (SELECT * FROM evidencias WHERE idOportunidad = X ORDER BY fecha DESC).
    // Es la consulta más frecuente de esta tabla (se usa en la galería del detalle).
    indices = [Index("idOportunidad")]
)
data class Evidencia(
    // Clave primaria con autoincremento.
    @PrimaryKey(autoGenerate = true)
    val idEvidencia: Long = 0,

    // Ruta relativa del archivo de foto dentro de filesDir.
    // Ejemplo: "evidencias/evidencia_1718700000000.jpg"
    // La ruta es relativa (no absoluta) para que si la app se mueve de directorio,
    // solo hay que cambiar filesDir y las rutas siguen funcionando.
    // El archivo se crea en ui/nativo/ (cámara) y se copia a filesDir/evidencias/.
    val rutaArchivo: String,

    // Fecha de captura en timestamp (milisegundos). Se usa para ordenar la galería
    // (más reciente primero) y para mostrar la fecha en la UI.
    val fecha: Long,

    // FK a Oportunidad: a qué oportunidad pertenece esta foto.
    // No es nullable: toda foto debe estar asociada a una oportunidad.
    val idOportunidad: Long
)