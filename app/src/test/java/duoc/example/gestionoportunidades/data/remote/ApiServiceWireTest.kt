package duoc.example.gestionoportunidades.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * Pruebas "por el cable": ApiService real + Retrofit + Gson contra un servidor local falso.
 * Comprueban rutas, métodos HTTP, parámetros, JSON enviado y JSON leído.
 *
 * RetrofitClient apunta fijo a http://10.0.2.2:8080 (no se puede cambiar), por eso aquí se arma
 * un Retrofit con la misma configuración (Gson, sin interceptores) apuntando al servidor falso.
 */
@Ignore
class ApiServiceWireTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService

    @Before
    fun iniciarServidor() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun detenerServidor() {
        runCatching { server.shutdown() }
    }

    private fun json(cuerpo: String, codigo: Int = 200) = MockResponse()
        .setResponseCode(codigo)
        .setHeader("Content-Type", "application/json")
        .setBody(cuerpo)

    private val usuarioJson =
        """{"id":5,"nombre":"Ana Pérez","correo":"ana@agencia.cl","rol":"EJECUTIVO","area":"Ventas","sucursal":"Santiago Centro","activo":true}"""

    private val oportunidadJson = """{
        "id":10,"nombre":"Feria del Emprendedor","tipo":"Feria municipal","descripcion":"d",
        "fuenteInformacion":"web","fechaDeteccion":"2026-03-01","fechaEvento":"2026-03-15",
        "fechaTermino":"2026-03-16","horarioEvento":"09:00 - 18:00","direccion":"Plaza",
        "comuna":"Puente Alto","region":"Metropolitana","publicoObjetivo":"Familias",
        "publicoEsperado":500,"costoReferencial":150000.0,"prioridad":"Alta",
        "potencialComercial":"Alto","afinidad":"Alta","estado":"Identificada",
        "condicionesParticipacion":"c","requisitos":"r","espaciosDisponibles":3,"observaciones":"o",
        "organizacion":{"id":1,"nombre":"Municipalidad","tipo":"Municipalidad","rubro":"Gobierno",
            "comuna":"Puente Alto","region":"Metropolitana"},
        "responsable":$usuarioJson
    }"""

    // =========================================================================
    // Autenticación
    // =========================================================================

    @Test
    fun `login envia un POST con el JSON de credenciales y lee el usuario`() {
        server.enqueue(json(usuarioJson))

        val respuesta = runBlocking { api.login(LoginRequestRemote("ana@agencia.cl", "clave123")) }

        val peticion = server.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/api/auth/login", peticion.path)
        assertTrue(peticion.getHeader("Content-Type")!!.startsWith("application/json"))
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("ana@agencia.cl", cuerpo.getString("correo"))
        assertEquals("clave123", cuerpo.getString("password"))

        assertTrue(respuesta.isSuccessful)
        val usuario = respuesta.body()!!
        assertEquals(5L, usuario.id)
        assertEquals("Ana Pérez", usuario.nombre)
        assertEquals("EJECUTIVO", usuario.rol)
        assertEquals("Santiago Centro", usuario.sucursal)
        assertTrue(usuario.activo)
        assertNull(usuario.password)
    }

    @Test
    fun `login con credenciales malas devuelve una respuesta de error legible`() {
        server.enqueue(json("""{"mensaje":"Credenciales inválidas"}""", codigo = 401))

        val respuesta = runBlocking { api.login(LoginRequestRemote("ana@agencia.cl", "mala")) }

        assertFalse(respuesta.isSuccessful)
        assertEquals(401, respuesta.code())
        assertEquals("Credenciales inválidas", JSONObject(respuesta.errorBody()!!.string()).getString("mensaje"))
        assertNull(respuesta.body())
    }

    // =========================================================================
    // Oportunidades: rutas, parámetros y JSON
    // =========================================================================

    @Test
    fun `listarOportunidades agrega usuarioId solo cuando se indica y lee el JSON anidado`() {
        server.enqueue(json("[$oportunidadJson]"))
        server.enqueue(json("[]"))

        val todas = runBlocking { api.listarOportunidades(null) }
        val deUsuario = runBlocking { api.listarOportunidades(7L) }

        val primera = server.takeRequest()
        assertEquals("GET", primera.method)
        assertEquals("/api/oportunidades", primera.path)
        assertEquals("/api/oportunidades?usuarioId=7", server.takeRequest().path)

        assertEquals(1, todas.size)
        val op = todas[0]
        assertEquals(10L, op.id)
        assertEquals("Feria del Emprendedor", op.nombre)
        assertEquals(500, op.publicoEsperado)
        assertEquals(150000.0, op.costoReferencial!!, 0.0)
        assertEquals(1L, op.organizacion.id)
        assertEquals("Municipalidad", op.organizacion.nombre)
        assertEquals("Ana Pérez", op.responsable?.nombre)
        assertTrue(deUsuario.isEmpty())
    }

    @Test
    fun `actualizarOportunidad envia un PUT con la ruta el usuario y el mapa como JSON`() {
        server.enqueue(json(oportunidadJson))
        val payload = mapOf(
            "nombre" to "Feria del Emprendedor",
            "publicoEsperado" to 500,
            "costoReferencial" to 150000.0,
            "organizacion" to mapOf("id" to 1L)
        )

        val respuesta = runBlocking { api.actualizarOportunidad(10L, 3L, payload) }

        val peticion = server.takeRequest()
        assertEquals("PUT", peticion.method)
        assertEquals("/api/oportunidades/10?usuarioId=3", peticion.path)
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("Feria del Emprendedor", cuerpo.getString("nombre"))
        assertEquals(500, cuerpo.getInt("publicoEsperado"))
        assertEquals(150000.0, cuerpo.getDouble("costoReferencial"), 0.0)
        assertEquals(1L, cuerpo.getJSONObject("organizacion").getLong("id"))

        assertTrue(respuesta.isSuccessful)
        assertEquals(10L, respuesta.body()!!.id)
    }

    @Test
    fun `eliminarOportunidad envia un DELETE y acepta 204 sin cuerpo`() {
        server.enqueue(MockResponse().setResponseCode(204))

        val respuesta = runBlocking { api.eliminarOportunidad(10L) }

        val peticion = server.takeRequest()
        assertEquals("DELETE", peticion.method)
        assertEquals("/api/oportunidades/10", peticion.path)
        assertTrue(respuesta.isSuccessful)
    }

    // =========================================================================
    // Otros recursos
    // =========================================================================

    @Test
    fun `crearContacto serializa la organizacion como referencia anidada`() {
        server.enqueue(
            json("""{"id":20,"nombre":"Carla Rojas","correo":"carla@muni.cl","telefono":"+56911112222","organizacion":{"id":3}}""")
        )
        val contacto = ContactoRemote(
            nombre = "Carla Rojas", cargo = "Encargada", area = "Extensión", correo = "carla@muni.cl",
            telefono = "+56911112222", medioPreferente = "WhatsApp", estado = "Activo",
            observaciones = "", organizacion = OrganizacionRef(3L)
        )

        val respuesta = runBlocking { api.crearContacto(contacto) }

        val peticion = server.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/api/contactos", peticion.path)
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("Carla Rojas", cuerpo.getString("nombre"))
        assertEquals("WhatsApp", cuerpo.getString("medioPreferente"))
        assertEquals(3L, cuerpo.getJSONObject("organizacion").getLong("id"))
        assertFalse("sin id el campo no se envía", cuerpo.has("id"))
        assertEquals(20L, respuesta.body()!!.id)
        assertEquals(3L, respuesta.body()!!.organizacion.id)
    }

    @Test
    fun `guardarResultado serializa la oportunidad y obtenerResultado distingue 200 de 404`() {
        server.enqueue(json("""{"id":60,"asistentesReales":420,"evaluacionGeneral":"4","oportunidad":{"id":10}}"""))
        server.enqueue(json("""{"id":60,"asistentesReales":420,"evaluacionGeneral":"4","oportunidad":{"id":10}}"""))
        server.enqueue(MockResponse().setResponseCode(404))

        val guardado = runBlocking {
            api.guardarResultado(
                ResultadoActivacionRemote(asistentesReales = 420, evaluacionGeneral = "4", oportunidad = OportunidadRef(10L))
            )
        }
        val existente = runBlocking { api.obtenerResultado(10L) }
        val inexistente = runBlocking { api.obtenerResultado(11L) }

        val peticionPost = server.takeRequest()
        assertEquals("POST", peticionPost.method)
        assertEquals("/api/resultados", peticionPost.path)
        val cuerpo = JSONObject(peticionPost.body.readUtf8())
        assertEquals(420, cuerpo.getInt("asistentesReales"))
        assertEquals(10L, cuerpo.getJSONObject("oportunidad").getLong("id"))
        assertEquals(60L, guardado.body()!!.id)

        assertEquals("/api/resultados/oportunidad/10", server.takeRequest().path)
        assertEquals(420, existente.body()!!.asistentesReales)

        assertEquals("/api/resultados/oportunidad/11", server.takeRequest().path)
        assertFalse(inexistente.isSuccessful)
        assertEquals(404, inexistente.code())
    }

    @Test
    fun `obtenerDashboard lee los totales y las listas anidadas`() {
        server.enqueue(
            json(
                """{"totalOportunidades":12,"pendientes":5,"confirmadas":3,
                    "eventosProximos":[$oportunidadJson],
                    "seguimientosAtrasados":[{"id":1,"tipoAccion":"Llamada","fechaHora":"2026-03-10T14:30:00",
                        "resultado":"ok","proximaAccion":"x","fechaProximoSeguimiento":"2026-03-12",
                        "observaciones":"","oportunidad":{"id":10},"usuario":{"id":5}}]}"""
            )
        )

        val resumen = runBlocking { api.obtenerDashboard(null) }

        assertEquals("/api/dashboard/resumen", server.takeRequest().path)
        assertEquals(12L, resumen.totalOportunidades)
        assertEquals(5L, resumen.pendientes)
        assertEquals(3L, resumen.confirmadas)
        assertEquals("Feria del Emprendedor", resumen.eventosProximos.single().nombre)
        assertEquals(10L, resumen.seguimientosAtrasados.single().oportunidad.id)
        assertEquals(5L, resumen.seguimientosAtrasados.single().usuario.id)
    }

    // =========================================================================
    // Errores de HTTP y de conexión
    // =========================================================================

    @Test
    fun `una respuesta 500 en un metodo que devuelve lista lanza HttpException`() {
        server.enqueue(json("""{"mensaje":"falla interna"}""", codigo = 500))

        val error = runCatching { runBlocking { api.listarOportunidades(null) } }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error is HttpException)
        assertEquals(500, (error as HttpException).code())
    }

    @Test
    fun `una respuesta 400 en un metodo que devuelve Response no lanza y conserva el cuerpo del error`() {
        server.enqueue(json("""{"mensaje":"datos inválidos"}""", codigo = 400))

        val respuesta = runBlocking { api.crearOportunidad(mapOf("nombre" to "")) }

        assertFalse(respuesta.isSuccessful)
        assertEquals(400, respuesta.code())
        assertEquals("datos inválidos", JSONObject(respuesta.errorBody()!!.string()).getString("mensaje"))
    }

    @Test
    fun `si el servidor no responde la llamada lanza IOException`() {
        server.shutdown()

        val error = runCatching { runBlocking { api.listarUsuarios() } }.exceptionOrNull()

        assertTrue("Se esperaba IOException pero fue $error", error is IOException)
    }
}