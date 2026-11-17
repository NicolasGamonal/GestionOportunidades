package duoc.example.gestionoportunidades.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Autenticación y Usuarios
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestRemote): Response<UsuarioRemote>

    @GET("api/usuarios")
    suspend fun listarUsuarios(): List<UsuarioRemote>

    // Oportunidades y Dashboard
    @GET("api/oportunidades")
    suspend fun listarOportunidades(@Query("usuarioId") usuarioId: Long? = null): List<OportunidadRemote>

    @GET("api/oportunidades/{id}")
    suspend fun obtenerOportunidad(@Path("id") id: Long): OportunidadRemote

    @POST("api/oportunidades")
    suspend fun crearOportunidad(@Body oportunidad: Map<String, Any?>): Response<OportunidadRemote>

    @PUT("api/oportunidades/{id}")
    suspend fun actualizarOportunidad(
        @Path("id") id: Long,
        @Query("usuarioId") usuarioId: Long,
        @Body oportunidad: Map<String, Any?>
    ): Response<OportunidadRemote>

    @PATCH("api/oportunidades/{id}/asignar")
    suspend fun reasignarOportunidad(
        @Path("id") id: Long,
        @Query("supervisorId") supervisorId: Long,
        @Body request: AsignarRequestRemote
    ): Response<OportunidadRemote>

    @GET("api/dashboard/resumen")
    suspend fun obtenerDashboard(@Query("usuarioId") usuarioId: Long? = null): DashboardResumenRemote

    // Organizaciones y Contactos
    @GET("api/organizaciones")
    suspend fun listarOrganizaciones(): List<OrganizacionRemote>

    @POST("api/organizaciones")
    suspend fun crearOrganizacion(@Body org: OrganizacionRemote): Response<OrganizacionRemote>

    @GET("api/contactos/organizacion/{orgId}")
    suspend fun listarContactos(@Path("orgId") orgId: Long): List<ContactoRemote>

    @POST("api/contactos")
    suspend fun crearContacto(@Body contacto: ContactoRemote): Response<ContactoRemote>

    // Seguimientos y Evidencias
    @GET("api/seguimientos/oportunidad/{opId}")
    suspend fun listarSeguimientos(@Path("opId") opId: Long): List<SeguimientoRemote>

    @POST("api/seguimientos")
    suspend fun crearSeguimiento(@Body seg: SeguimientoRemote): Response<SeguimientoRemote>

    @GET("api/evidencias/oportunidad/{opId}")
    suspend fun listarEvidencias(@Path("opId") opId: Long): List<EvidenciaRemote>

    @POST("api/evidencias")
    suspend fun crearEvidencia(@Body ev: EvidenciaRemote): Response<EvidenciaRemote>

    @PUT("api/organizaciones/{id}")
    suspend fun actualizarOrganizacion(
        @Path("id") id: Long,
        @Body org: OrganizacionRemote
    ): Response<OrganizacionRemote>

    @DELETE("api/organizaciones/{id}")
    suspend fun eliminarOrganizacion(@Path("id") id: Long): Response<Unit>

    @GET("api/contactos")
    suspend fun listarTodosContactos(): List<ContactoRemote>

    @PUT("api/contactos/{id}")
    suspend fun actualizarContacto(
        @Path("id") id: Long,
        @Body contacto: ContactoRemote
    ): Response<ContactoRemote>

    @DELETE("api/contactos/{id}")
    suspend fun eliminarContacto(@Path("id") id: Long): Response<Unit>

    @DELETE("api/oportunidades/{id}")
    suspend fun eliminarOportunidad(@Path("id") id: Long): Response<Unit>

    @GET("api/seguimientos")
    suspend fun listarTodosSeguimientos(): List<SeguimientoRemote>

    @PUT("api/seguimientos/{id}")
    suspend fun actualizarSeguimiento(
        @Path("id") id: Long,
        @Body seg: SeguimientoRemote
    ): Response<SeguimientoRemote>

    @DELETE("api/evidencias/{id}")
    suspend fun eliminarEvidencia(@Path("id") id: Long): Response<Unit>

    @GET("api/notas/oportunidad/{opId}")
    suspend fun listarNotas(@Path("opId") opId: Long): List<NotaMarketingRemote>

    @POST("api/notas")
    suspend fun crearNota(@Body nota: NotaMarketingRemote): Response<NotaMarketingRemote>

    @GET("api/resultados/oportunidad/{opId}")
    suspend fun obtenerResultado(@Path("opId") opId: Long): Response<ResultadoActivacionRemote?>

    @POST("api/resultados")
    suspend fun guardarResultado(@Body res: ResultadoActivacionRemote): Response<ResultadoActivacionRemote>


}
