package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entidad "Seguimientos": historial de gestiones y cambios de estado de cada oportunidad.
// Es la tabla que conserva el "rastro" de todo lo que se hizo con una oportunidad:
// llamadas, emails, reuniones y también los cambios de estado.
//
// Relación con otras entidades:
// - Oportunidad → Seguimiento: 1 a muchos (una oportunidad tiene varias gestiones)
// - Usuario → Seguimiento: 1 a muchos (un ejecutivo registra varias gestiones)
// - Contacto → Seguimiento: 1 a muchos (OPCIONAL, un contacto puede aparecer en varias gestiones)
//
// 3FN: no se duplica el nombre de la oportunidad, del usuario ni del contacto.
// Solo se guardan los ids (FK) y los datos propios de la gestión
// (tipo, fecha, resultado, próxima acción).
//
// ¿Por qué idContacto es opcional (nullable)?
// Porque no toda gestión involucra a un contacto específico.
// Ej: un "Cambio de estado" no tiene contacto asociado.
// Una "Llamada" sí puede tener contacto (a quién llamaste).
@Entity(
    tableName = "seguimientos",
    foreignKeys = [
        // FK a Oportunidad: cada seguimiento DEBE pertenecer a una oportunidad.
        // onDelete = CASCADE: si se borra la oportunidad, sus seguimientos se borran automáticamente.
        // (En la práctica, OportunidadRepository.eliminar() impide borrar oportunidades
        //  que tienen seguimientos, así que esta cascada es una red de seguridad.)
        ForeignKey(
            entity = Oportunidad::class,
            parentColumns = ["idOportunidad"],
            childColumns = ["idOportunidad"],
            onDelete = ForeignKey.CASCADE
        ),
        // FK a Usuario: quién registró esta gestión.
        // onDelete = RESTRICT: NO se puede borrar un usuario que tenga seguimientos registrados.
        // Es diferente de CASCADE (que borraría el historial) y de SET_NULL (que perdería
        // la información de quién hizo la gestión). El historial debe ser inmutable:
        // si Juan Pérez hizo una llamada, eso no se borra aunque se elimine su cuenta.
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.RESTRICT
        ),
        // FK a Contacto: con quién se hizo la gestión (opcional).
        // onDelete = SET_NULL: si se borra un contacto, los seguimientos NO se borran,
        //  sino que quedan sin contacto asociado (idContacto pasa a null).
        //  Se conserva el historial de la gestión, solo se pierde la referencia al contacto.
        //  (En la práctica, ContactoRepository.eliminar() impide borrar contactos con historial,
        //   así que esta SET_NULL es una red de seguridad.)
        ForeignKey(
            entity = Contacto::class,
            parentColumns = ["idContacto"],
            childColumns = ["idContacto"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    // Índices: aceleran las 3 consultas más frecuentes sobre esta tabla.
    // - idOportunidad: "obtener historial de una oportunidad" (detalle)
    // - idUsuario: "obtener todas las gestiones de un ejecutivo" (panel del supervisor)
    // - idContacto: "obtener gestiones asociadas a un contacto" (verificación antes de borrar)
    indices = [
        Index(value = ["idOportunidad"]),
        Index(value = ["idUsuario"]),
        Index(value = ["idContacto"])
    ]
)
data class Seguimiento(
    // Clave primaria con autoincremento.
    @PrimaryKey(autoGenerate = true)
    val idSeguimiento: Long = 0,

    // FK a Oportunidad: a qué oportunidad pertenece esta gestión.
    // No es nullable: toda gestión debe estar asociada a una oportunidad.
    val idOportunidad: Long,

    // FK a Usuario: quién registró esta gestión (el ejecutivo o supervisor que la hizo).
    // No es nullable: toda gestión debe tener un autor.
    // RESTRICT: este usuario no se puede borrar mientras tenga gestiones.
    val idUsuario: Long,

    // Tipo de acción realizada. Valores posibles:
    // - "Llamada"
    // - "Email"
    // - "Reunión"
    // - "Cambio de estado" (registro automático del flujo, no es una gestión real)
    // - Otros tipos que se agreguen en el futuro
    val tipoAccion: String,

    // Fecha y hora en que se realizó la gestión (timestamp en milisegundos).
    // Se usa para ordenar el historial (más reciente primero) y para mostrar la fecha en la UI.
    val fechaHora: Long,

    // Resultado o resumen de lo que pasó en la gestión.
    // Ej: "Contacto realizado con Ana Rojas, interesada en propuesta"
    // En cambios de estado: "Identificada → Contacto realizado"
    val resultado: String,

    // Próxima acción a realizar (lo que el ejecutivo debe hacer después).
    // Ej: "Enviar propuesta formal", "Seguimiento telefónico"
    // En cambios de estado puede ser "" (vacío) o una nota.
    val proximaAccion: String,

    // Fecha programada para la próxima acción (timestamp en milisegundos).
    // Se usa en el Dashboard para:
    // - "Seguimientos pendientes": fechaProximo >= hoy
    // - "Gestiones atrasadas": fechaProximo < hoy
    // Debe ser >= fechaHora (lo valida Validaciones.kt).
    val fechaProximo: Long,

    // FK a Contacto (OPCIONAL): con quién se hizo la gestión.
    // ES NULLABLE: no toda gestión involucra a un contacto específico.
    // Ej: un "Cambio de estado" no tiene contacto; una "Llamada" sí puede tenerlo.
    // SET_NULL: si se borra el contacto, la gestión se conserva pero pierde la referencia.
    val idContacto: Long? = null
)