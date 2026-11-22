package duoc.example.gestionoportunidades.testutil

import duoc.example.gestionoportunidades.data.remote.ContactoRemote
import duoc.example.gestionoportunidades.data.remote.EvidenciaRemote
import duoc.example.gestionoportunidades.data.remote.NotaMarketingRemote
import duoc.example.gestionoportunidades.data.remote.OportunidadRef
import duoc.example.gestionoportunidades.data.remote.OportunidadRemote
import duoc.example.gestionoportunidades.data.remote.OrganizacionRef
import duoc.example.gestionoportunidades.data.remote.OrganizacionRemote
import duoc.example.gestionoportunidades.data.remote.ResultadoActivacionRemote
import duoc.example.gestionoportunidades.data.remote.SeguimientoRemote
import duoc.example.gestionoportunidades.data.remote.UsuarioRef
import duoc.example.gestionoportunidades.data.remote.UsuarioRemote
import duoc.example.gestionoportunidades.model.Contacto
import duoc.example.gestionoportunidades.model.Evidencia
import duoc.example.gestionoportunidades.model.NotaMarketing
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.model.Organizacion
import duoc.example.gestionoportunidades.model.ResultadoActivacion
import duoc.example.gestionoportunidades.model.Seguimiento
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import retrofit2.Response

// =============================================================================
// Constructores de datos remotos (lo que devuelve la API). Todos tienen valores
// por defecto válidos: cada prueba cambia solo lo que le importa.
// =============================================================================

fun orgRemota(
    id: Long? = 1L,
    nombre: String = "Municipalidad de Puente Alto",
    tipo: String = "Municipalidad",
    rubro: String? = "Gobierno local",
    comuna: String? = "Puente Alto",
    region: String? = "Metropolitana",
    sitioWeb: String? = null,
    publicoAsociado: String? = null,
    observaciones: String? = null
) = OrganizacionRemote(
    id = id, nombre = nombre, tipo = tipo, rubro = rubro, comuna = comuna, region = region,
    sitioWeb = sitioWeb, publicoAsociado = publicoAsociado, observaciones = observaciones
)

fun usuarioRemoto(
    id: Long = 5L,
    nombre: String = "Ana Pérez",
    correo: String = "ana@agencia.cl",
    password: String? = null,
    rol: String = "EJECUTIVO",
    area: String? = "Ventas",
    sucursal: String? = "Santiago Centro",
    activo: Boolean = true
) = UsuarioRemote(
    id = id, nombre = nombre, correo = correo, password = password, rol = rol,
    area = area, sucursal = sucursal, activo = activo
)

fun oportunidadRemota(
    id: Long? = 10L,
    nombre: String = "Feria del Emprendedor",
    tipo: String = "Feria municipal",
    descripcion: String? = "Descripción del evento",
    fuenteInformacion: String? = "Sitio web",
    fechaDeteccion: String? = "2026-03-01",
    fechaEvento: String? = "2026-03-15",
    fechaTermino: String? = "2026-03-16",
    horarioEvento: String? = "09:00 - 18:00",
    direccion: String? = "Plaza de Armas",
    comuna: String? = "Puente Alto",
    region: String? = "Metropolitana",
    publicoObjetivo: String? = "Familias",
    publicoEsperado: Int? = 500,
    costoReferencial: Double? = 150000.0,
    prioridad: String = "Alta",
    potencialComercial: String? = "Alto",
    afinidad: String? = "Alta",
    estado: String = "Identificada",
    condicionesParticipacion: String? = "Arriendo de stand",
    requisitos: String? = "Permiso municipal",
    espaciosDisponibles: Int? = 3,
    observaciones: String? = "Observación",
    organizacion: OrganizacionRemote = orgRemota(),
    responsable: UsuarioRemote? = usuarioRemoto()
) = OportunidadRemote(
    id = id, nombre = nombre, tipo = tipo, descripcion = descripcion,
    fuenteInformacion = fuenteInformacion, fechaDeteccion = fechaDeteccion,
    fechaEvento = fechaEvento, fechaTermino = fechaTermino, horarioEvento = horarioEvento,
    direccion = direccion, comuna = comuna, region = region, publicoObjetivo = publicoObjetivo,
    publicoEsperado = publicoEsperado, costoReferencial = costoReferencial,
    prioridad = prioridad, potencialComercial = potencialComercial, afinidad = afinidad,
    estado = estado, condicionesParticipacion = condicionesParticipacion,
    requisitos = requisitos, espaciosDisponibles = espaciosDisponibles,
    observaciones = observaciones, organizacion = organizacion, responsable = responsable
)

fun contactoRemoto(
    id: Long? = 20L,
    nombre: String = "Carla Rojas",
    cargo: String? = "Encargada de eventos",
    area: String? = "Extensión",
    correo: String = "carla@muni.cl",
    telefono: String = "+56911112222",
    medioPreferente: String? = "WhatsApp",
    estado: String? = "Activo",
    observaciones: String? = "",
    idOrganizacion: Long = 1L
) = ContactoRemote(
    id = id, nombre = nombre, cargo = cargo, area = area, correo = correo, telefono = telefono,
    medioPreferente = medioPreferente, estado = estado, observaciones = observaciones,
    organizacion = OrganizacionRef(idOrganizacion)
)

fun seguimientoRemoto(
    id: Long? = 30L,
    tipoAccion: String = "Llamada",
    fechaHora: String? = "2026-03-10T14:30:00",
    resultado: String = "Contacto interesado",
    proximaAccion: String? = "Enviar propuesta",
    fechaProximoSeguimiento: String? = "2026-03-20",
    observaciones: String? = "",
    idOportunidad: Long = 10L,
    idUsuario: Long = 5L
) = SeguimientoRemote(
    id = id, tipoAccion = tipoAccion, fechaHora = fechaHora, resultado = resultado,
    proximaAccion = proximaAccion, fechaProximoSeguimiento = fechaProximoSeguimiento,
    observaciones = observaciones, oportunidad = OportunidadRef(idOportunidad),
    usuario = UsuarioRef(idUsuario)
)

fun evidenciaRemota(
    id: Long? = 40L,
    descripcion: String? = "evidencias/foto1.jpg",
    fechaCaptura: String? = "2026-03-10T14:30:00",
    imagenBase64: String? = "",
    idOportunidad: Long = 10L
) = EvidenciaRemote(
    id = id, descripcion = descripcion, fechaCaptura = fechaCaptura,
    imagenBase64 = imagenBase64, oportunidad = OportunidadRef(idOportunidad)
)

fun notaRemota(
    id: Long? = 50L,
    contenido: String = "Llevar pendones",
    materialRequerido: String? = "Material",
    fechaCreacion: String? = "2026-03-10T14:30:00",
    autorId: Long? = 5L,
    idOportunidad: Long = 10L
) = NotaMarketingRemote(
    id = id, contenido = contenido, materialRequerido = materialRequerido,
    fechaCreacion = fechaCreacion, autorId = autorId, oportunidad = OportunidadRef(idOportunidad)
)

fun resultadoRemoto(
    id: Long? = 60L,
    fechaRealizacion: String? = "2026-03-16",
    estadoFinal: String? = "Realizada",
    asistentesReales: Int? = 420,
    contactosGenerados: Int? = 35,
    personasInteresadas: Int? = 35,
    inscripcionesLogradas: Int? = 12,
    evaluacionGeneral: String? = "4",
    observaciones: String? = "Funcionó bien el stand",
    usuarioId: Long? = 5L,
    idOportunidad: Long = 10L
) = ResultadoActivacionRemote(
    id = id, fechaRealizacion = fechaRealizacion, estadoFinal = estadoFinal,
    asistentesReales = asistentesReales, contactosGenerados = contactosGenerados,
    personasInteresadas = personasInteresadas, inscripcionesLogradas = inscripcionesLogradas,
    evaluacionGeneral = evaluacionGeneral, observaciones = observaciones,
    usuarioId = usuarioId, oportunidad = OportunidadRef(idOportunidad)
)

// =============================================================================
// Constructores de modelos locales (lo que envía la app a los repositorios).
// =============================================================================

fun oportunidadLocal(
    idOportunidad: Long = 10L,
    nombre: String = "Feria del Emprendedor",
    tipoEvento: String = "Feria municipal",
    fechaDeteccion: Long = FechasPrueba.dia("2026-03-01"),
    fechaInicio: Long = FechasPrueba.dia("2026-03-15"),
    fechaTermino: Long = FechasPrueba.dia("2026-03-16"),
    comuna: String = "Puente Alto",
    publicoEsperado: Int = 500,
    prioridad: String = "Alta",
    estado: String = "Identificada",
    costoReferencial: Double = 150000.0,
    observaciones: String = "Observación",
    idOrganizacion: Long = 1L,
    idResponsable: Long? = 5L,
    descripcion: String = "Descripción del evento",
    fuenteInformacion: String = "Sitio web",
    potencialComercial: String = "Alto",
    afinidad: String = "Alta",
    publicoObjetivo: String = "Familias",
    condicionesParticipacion: String = "Arriendo de stand",
    requisitos: String = "Permiso municipal",
    espaciosDisponibles: Int = 3
) = Oportunidad(
    idOportunidad = idOportunidad, nombre = nombre, tipoEvento = tipoEvento,
    fechaDeteccion = fechaDeteccion, fechaInicio = fechaInicio, fechaTermino = fechaTermino,
    comuna = comuna, publicoEsperado = publicoEsperado, prioridad = prioridad, estado = estado,
    costoReferencial = costoReferencial, observaciones = observaciones,
    idOrganizacion = idOrganizacion, idResponsable = idResponsable, descripcion = descripcion,
    fuenteInformacion = fuenteInformacion, potencialComercial = potencialComercial,
    afinidad = afinidad, publicoObjetivo = publicoObjetivo,
    condicionesParticipacion = condicionesParticipacion, requisitos = requisitos,
    espaciosDisponibles = espaciosDisponibles
)

fun organizacionLocal(
    idOrganizacion: Long = 1L,
    nombre: String = "Municipalidad de Puente Alto",
    tipo: String = "Municipalidad",
    rubro: String = "Gobierno local",
    comuna: String = "Puente Alto"
) = Organizacion(
    idOrganizacion = idOrganizacion, nombre = nombre, tipo = tipo, rubro = rubro, comuna = comuna
)

fun contactoLocal(
    idContacto: Long = 20L,
    nombre: String = "Carla Rojas",
    cargo: String = "Encargada de eventos",
    area: String = "Extensión",
    correo: String = "carla@muni.cl",
    telefono: String = "+56911112222",
    idOrganizacion: Long = 1L,
    medioPreferente: String = "WhatsApp"
) = Contacto(
    idContacto = idContacto, nombre = nombre, cargo = cargo, area = area, correo = correo,
    telefono = telefono, idOrganizacion = idOrganizacion, medioPreferente = medioPreferente
)

fun seguimientoLocal(
    idSeguimiento: Long = 30L,
    idOportunidad: Long = 10L,
    idUsuario: Long = 5L,
    tipoAccion: String = "Llamada",
    fechaHora: Long = FechasPrueba.hora("2026-03-10T14:30:00"),
    resultado: String = "Contacto interesado",
    proximaAccion: String = "Enviar propuesta",
    fechaProximo: Long = FechasPrueba.dia("2026-03-20"),
    idContacto: Long? = null
) = Seguimiento(
    idSeguimiento = idSeguimiento, idOportunidad = idOportunidad, idUsuario = idUsuario,
    tipoAccion = tipoAccion, fechaHora = fechaHora, resultado = resultado,
    proximaAccion = proximaAccion, fechaProximo = fechaProximo, idContacto = idContacto
)

fun evidenciaLocal(
    idEvidencia: Long = 40L,
    rutaArchivo: String = "foto1.jpg",
    fecha: Long = FechasPrueba.hora("2026-03-10T14:30:00"),
    idOportunidad: Long = 10L
) = Evidencia(
    idEvidencia = idEvidencia, rutaArchivo = rutaArchivo, fecha = fecha, idOportunidad = idOportunidad
)

fun notaLocal(
    idNota: Long = 0L,
    idOportunidad: Long = 10L,
    tipo: String = "Material",
    texto: String = "Llevar pendones",
    fechaHora: Long = FechasPrueba.hora("2026-03-10T14:30:00"),
    idUsuario: Long = 5L
) = NotaMarketing(
    idNota = idNota, idOportunidad = idOportunidad, tipo = tipo, texto = texto,
    fechaHora = fechaHora, idUsuario = idUsuario
)

fun resultadoLocal(
    idResultado: Long = 0L,
    idOportunidad: Long = 10L,
    asistentes: Int = 420,
    contactosGenerados: Int = 35,
    inscripciones: Int = 12,
    evaluacion: Int = 4,
    aprendizajes: String = "Funcionó bien el stand",
    fechaRegistro: Long = FechasPrueba.dia("2026-03-16"),
    idUsuario: Long = 5L
) = ResultadoActivacion(
    idResultado = idResultado, idOportunidad = idOportunidad, asistentes = asistentes,
    contactosGenerados = contactosGenerados, inscripciones = inscripciones,
    evaluacion = evaluacion, aprendizajes = aprendizajes, fechaRegistro = fechaRegistro,
    idUsuario = idUsuario
)

// =============================================================================
// Respuestas HTTP simuladas
// =============================================================================

/** Respuesta de error de Retrofit (por ejemplo 400 con un cuerpo JSON). */
fun <T> respuestaError(codigo: Int = 400, cuerpo: String = ""): Response<T> =
    Response.error(codigo, cuerpo.toResponseBody("application/json".toMediaType()))

/** Respuesta exitosa (2xx) pero sin cuerpo. */
fun <T> respuestaSinCuerpo(): Response<T> = Response.success<T>(null)

/** Respuesta de error cuyo errorBody() es null (Response.error nunca lo permite, por eso se simula). */
fun respuestaUnitSinCuerpoDeError(): Response<Unit> {
    val respuesta = mockk<Response<Unit>>()
    every { respuesta.isSuccessful } returns false
    every { respuesta.errorBody() } returns null
    return respuesta
}

fun respuestaOportunidadSinCuerpoDeError(): Response<OportunidadRemote> {
    val respuesta = mockk<Response<OportunidadRemote>>()
    every { respuesta.isSuccessful } returns false
    every { respuesta.errorBody() } returns null
    return respuesta
}

// =============================================================================
// Ayudas para Flow y excepciones
// =============================================================================

/** Recoge el Flow completo y comprueba que emitió exactamente una vez. */
suspend fun <T> Flow<T>.unico(): T {
    val emisiones = toList()
    assertEquals("El Flow debe emitir exactamente una vez", 1, emisiones.size)
    return emisiones[0]
}

/** Ejecuta el bloque y devuelve la excepción que lanzó; falla si no lanzó ninguna. */
suspend fun fallaCon(bloque: suspend () -> Unit): Throwable {
    try {
        bloque()
    } catch (e: Throwable) {
        return e
    }
    throw AssertionError("Se esperaba una excepción y el bloque terminó sin lanzarla")
}
