package duoc.example.gestionoportunidades.data.remote

data class LoginRequestRemote(
    val correo: String,
    val password: String
)

data class AsignarRequestRemote(
    val nuevoResponsableId: Long,
    val nuevaPrioridad: String? = null
)

data class UsuarioRemote(
    val id: Long,
    val nombre: String,
    val correo: String,
    val password: String? = null,
    val rol: String,
    val area: String?,
    val sucursal: String?,
    val activo: Boolean = true
)

data class OrganizacionRemote(
    val id: Long? = null,
    val nombre: String,
    val tipo: String,
    val rubro: String?,
    val comuna: String?,
    val region: String?,
    val sitioWeb: String?,
    val publicoAsociado: String?,
    val observaciones: String?
)

data class ContactoRemote(
    val id: Long? = null,
    val nombre: String,
    val cargo: String?,
    val area: String?,
    val correo: String,
    val telefono: String,
    val medioPreferente: String?,
    val estado: String?,
    val observaciones: String?,
    val organizacion: OrganizacionRef
)

data class OrganizacionRef(val id: Long)
data class UsuarioRef(val id: Long)
data class OportunidadRef(val id: Long)

data class OportunidadRemote(
    val id: Long? = null,
    val nombre: String,
    val tipo: String,
    val descripcion: String?,
    val fuenteInformacion: String?,
    val fechaDeteccion: String?,
    val fechaEvento: String?,
    val fechaTermino: String? = null,
    val horarioEvento: String?,
    val direccion: String?,
    val comuna: String?,
    val region: String?,
    val publicoObjetivo: String?,
    val publicoEsperado: Int?,
    val costoReferencial: Double?,
    val prioridad: String,
    val potencialComercial: String?,
    val afinidad: String? = "Media",
    val estado: String,
    val condicionesParticipacion: String?,
    val requisitos: String? = "",
    val espaciosDisponibles: Int? = 0,
    val observaciones: String?,
    val organizacion: OrganizacionRemote,
    val responsable: UsuarioRemote?
)


data class SeguimientoRemote(
    val id: Long? = null,
    val tipoAccion: String,
    val fechaHora: String? = null,
    val resultado: String,
    val proximaAccion: String?,
    val fechaProximoSeguimiento: String?,
    val observaciones: String?,
    val oportunidad: OportunidadRef,
    val usuario: UsuarioRef
)

data class EvidenciaRemote(
    val id: Long? = null,
    val descripcion: String?,
    val fechaCaptura: String? = null,
    val imagenBase64: String?,
    val oportunidad: OportunidadRef
)

data class DashboardResumenRemote(
    val totalOportunidades: Long,
    val pendientes: Long,
    val confirmadas: Long,
    val eventosProximos: List<OportunidadRemote>,
    val seguimientosAtrasados: List<SeguimientoRemote>
)

    data class NotaMarketingRemote(
val id: Long? = null,
val contenido: String,
val materialRequerido: String? = null,
val fechaCreacion: String? = null,
val autorId: Long? = null,
val oportunidad: OportunidadRef
)

data class ResultadoActivacionRemote(
    val id: Long? = null,
    val fechaRealizacion: String? = null,
    val estadoFinal: String? = "Realizada",
    val asistentesReales: Int? = 0,
    val contactosGenerados: Int? = 0,
    val personasInteresadas: Int? = 0,
    val inscripcionesLogradas: Int? = 0,
    val evaluacionGeneral: String? = "5",
    val observaciones: String? = "",
    val usuarioId: Long? = null,
    val oportunidad: OportunidadRef
)

