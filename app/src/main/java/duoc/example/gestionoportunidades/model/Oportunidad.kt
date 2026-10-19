package duoc.example.gestionoportunidades.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import duoc.example.gestionoportunidades.model.Usuario
import duoc.example.gestionoportunidades.model.Organizacion

// Entidad "Oportunidades": la entidad central del proyecto.
// Representa un evento corporativo (feria, jornada, activación) que la agencia
// puede aprovechar para ofrecer sus convenios y beneficios.
//
// Relación con otras entidades:
// - Organización → Oportunidad: 1 a muchos (una org puede tener varias oportunidades)
// - Usuario → Oportunidad: 1 a muchos (un ejecutivo puede tener varias oportunidades asignadas)
// - Oportunidad → Seguimiento: 1 a muchos (cada oportunidad tiene un historial de gestiones)
// - Oportunidad → Evidencia: 1 a muchos (cada oportunidad puede tener varias fotos)
//
// 3FN: los datos del evento (fecha, tipo, público, comuna) viven aquí.
// No se duplica el nombre de la organización ni el nombre del responsable;
// se referencia por id (idOrganizacion, idResponsable).
@Entity(
    tableName = "oportunidades",
    foreignKeys = [
        // FK a Organización: cada oportunidad DEBE pertenecer a una organización.
        // onDelete = CASCADE: si se borra la organización, sus oportunidades se borran automáticamente.
        // (En la práctica, OrganizacionRepository.eliminar() impide borrar orgs con oportunidades,
        //  así que esta cascada es una red de seguridad.)
        ForeignKey(
            entity = Organizacion::class,
            parentColumns = ["idOrganizacion"],
            childColumns = ["idOrganizacion"],
            onDelete = ForeignKey.CASCADE
        ),
        // FK a Usuario (responsable): la oportunidad puede estar asignada a un ejecutivo
        // o no estar asignada todavía (idResponsable = null).
        // onDelete = SET_NULL: si se borra un usuario, sus oportunidades NO se borran,
        //  sino que quedan sin responsable (idResponsable pasa a null).
        //  Esto es diferente de CASCADE: se conserva la oportunidad, solo se pierde el vínculo.
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idResponsable"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    // Índices: aceleran las consultas más frecuentes.
    // - idOrganizacion: "obtener oportunidades de una organización"
    // - idResponsable: "obtener oportunidades de un ejecutivo" (la consulta más usada en la app)
    indices = [
        Index(value = ["idOrganizacion"]),
        Index(value = ["idResponsable"])
    ]
)
data class Oportunidad(
    // Clave primaria con autoincremento.
    @PrimaryKey(autoGenerate = true)
    val idOportunidad: Long = 0,

    // Nombre del evento (ej: "Feria del Emprendedor Valle Verde")
    val nombre: String,

    // Tipo de evento (ej: "Feria municipal", "Jornada de beneficios", "Evento corporativo")
    // Se usa para categorizar y filtrar.
    val tipoEvento: String,

    // Fecha en que se detectó la oportunidad (timestamp en milisegundos).
    // Siempre es anterior a fechaInicio (lo valida Validaciones.kt).
    val fechaDeteccion: Long,

    // Fecha de inicio del evento (timestamp en milisegundos).
    // Se usa en el dashboard para "eventos próximos" y en la integración con calendario.
    val fechaInicio: Long,

    // Fecha de término del evento (timestamp en milisegundos).
    // Puede ser igual a fechaInicio (eventos de un solo día).
    // Debe ser >= fechaInicio (lo valida Validaciones.kt).
    val fechaTermino: Long,

    // Comuna donde se realiza el evento.
    // Solo comuna (no región) para evitar la dependencia comuna → región que rompería 3FN.
    val comuna: String,

    // Cantidad estimada de personas que asistirán.
    // Debe ser > 0 (lo valida Validaciones.kt).
    // Se usa para priorizar: eventos de alta afluencia = mayor oportunidad de negocio.
    val publicoEsperado: Int,

    // Prioridad: "Alta", "Media" o "Baja".
    // Se usa en los filtros de la lista y en el chip de prioridad de la UI.
    val prioridad: String,

    // Estado actual en el flujo:
    // "Identificada" → "Contacto realizado" → "Propuesta enviada" → "En espera de respuesta" → "Confirmada" | "Descartada"
    // Los estados finales son "Confirmada" y "Descartada" (no se puede avanzar más).
    // Solo el supervisor puede pasar a Confirmada o Descartada (Permisos.kt).
    val estado: String,

    // Costo referencial de la activación en pesos chilenos.
    // Debe ser >= 0 (lo valida Validaciones.kt).
    // Si la oportunidad se descarta, puede ser 0.
    val costoReferencial: Double,

    // Notas libres del ejecutivo (ej: "Feria anual con alta concurrencia de familias").
    // Campo opcional en la práctica (puede ser "").
    val observaciones: String,

    // FK a Organización: a qué organización pertenece esta oportunidad.
    // No es nullable: toda oportunidad debe tener una organización.
    val idOrganizacion: Long,

    // FK a Usuario (responsable): a qué ejecutivo está asignada.
    // ES NULLABLE: una oportunidad puede existir sin responsable asignado todavía.
    // El supervisor la asigna o reasigna desde el panel.
    // Cuando es null, el ejecutivo no la ve en su lista (solo el supervisor la ve en "sin asignar").
    val idResponsable: Long? = null
)