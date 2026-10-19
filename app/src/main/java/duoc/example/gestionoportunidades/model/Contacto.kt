package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entidad "Contactos": personas de contacto dentro de una organización.
// Cada contacto pertenece a una organización (FK idOrganizacion).
//
// Relación con otras entidades:
// - Organización → Contacto: 1 a muchos (una org tiene varios contactos)
// - Contacto → Seguimiento: 1 a muchos (opcional, un contacto puede aparecer en gestiones)
//
// 3FN: los datos del contacto (nombre, cargo, correo, teléfono) viven solo aquí.
// No se duplica el nombre de la organización; se referencia por idOrganizacion.
@Entity(
    tableName = "contactos",
    // Clave foránea: cada contacto DEBE pertenecer a una organización.
    // onDelete = CASCADE: si se borra la organización, sus contactos se borran automáticamente.
    // (Esto es lo que hace que en OrganizacionRepository.eliminar() no haya que
    //  borrar los contactos manualmente: Room lo hace por la cascada.)
    foreignKeys = [
        ForeignKey(
            entity = Organizacion::class,
            parentColumns = ["idOrganizacion"],   // Columna en la tabla "organizaciones"
            childColumns = ["idOrganizacion"],    // Columna en la tabla "contactos"
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Índice en idOrganizacion: acelera las consultas "obtener contactos de una organización"
    // (SELECT * FROM contactos WHERE idOrganizacion = X).
    // Sin el índice, Room haría un scan completo de la tabla.
    indices = [Index(value = ["idOrganizacion"])]
)
data class Contacto(
    // Clave primaria con autoincremento: Room asigna el id automáticamente al insertar.
    // El valor por defecto 0 significa "aún no asignado" (Room lo reemplaza con el siguiente id).
    @PrimaryKey(autoGenerate = true)
    val idContacto: Long = 0,

    val nombre: String,


    val cargo: String,


    val area: String,


    val correo: String,


    val telefono: String,

    val idOrganizacion: Long
)