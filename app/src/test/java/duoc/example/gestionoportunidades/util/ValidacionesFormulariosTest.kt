package duoc.example.gestionoportunidades.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionesFormulariosTest {

    // ---------------------------------------------------------------------
    // Oportunidad
    // ---------------------------------------------------------------------

    // Formulario válido por defecto; cada prueba cambia solo el campo que quiere romper.
    private fun oportunidad(
        nombre: String = "Feria del Emprendedor",
        tipoEvento: String = "Feria empresarial",
        idOrganizacion: Long? = 1L,
        fechaDeteccion: String = "01/11/2026",
        fechaInicio: String = "10/11/2026",
        fechaTermino: String = "12/11/2026",
        horaInicio: String = "09:00",
        horaTermino: String = "18:00",
        comuna: String = "Santiago",
        publicoEsperado: String = "500",
        costoReferencial: String = "100000",
        observaciones: String = "",
        descripcion: String = "",
        fuenteInformacion: String = "Sitio web",
        publicoObjetivo: String = "Familias",
        condicionesParticipacion: String = "",
        requisitos: String = "",
        espaciosDisponibles: String = "2"
    ) = ValidacionesFormularios.validarOportunidad(
        nombre, tipoEvento, idOrganizacion, fechaDeteccion, fechaInicio, fechaTermino,
        horaInicio, horaTermino, comuna, publicoEsperado, costoReferencial, observaciones,
        descripcion, fuenteInformacion, publicoObjetivo, condicionesParticipacion, requisitos,
        espaciosDisponibles
    )

    @Test
    fun `oportunidad valida devuelve mapa vacio`() {
        assertTrue(oportunidad().isEmpty())
    }

    @Test
    fun `oportunidad con todos los campos en blanco falla en los obligatorios`() {
        val errores = oportunidad(
            nombre = "", tipoEvento = "", idOrganizacion = null, fechaDeteccion = "",
            fechaInicio = "", fechaTermino = "", comuna = "", publicoEsperado = "",
            costoReferencial = "", fuenteInformacion = "", publicoObjetivo = "", espaciosDisponibles = ""
        )
        assertEquals(
            setOf(
                CampoOportunidad.NOMBRE, CampoOportunidad.TIPO, CampoOportunidad.ORGANIZACION,
                CampoOportunidad.DETECCION, CampoOportunidad.INICIO, CampoOportunidad.TERMINO,
                CampoOportunidad.COMUNA, CampoOportunidad.PUBLICO, CampoOportunidad.COSTO,
                CampoOportunidad.FUENTE, CampoOportunidad.PUBLICO_OBJETIVO, CampoOportunidad.ESPACIOS
            ),
            errores.keys
        )
    }

    // Un caso por cada una de las 17 claves de CampoOportunidad.

    private fun assertSoloError(campo: CampoOportunidad, errores: Map<CampoOportunidad, String>) {
        assertEquals(setOf(campo), errores.keys)
    }

    @Test fun `NOMBRE vacio genera error solo en NOMBRE`() =
        assertSoloError(CampoOportunidad.NOMBRE, oportunidad(nombre = ""))

    @Test fun `TIPO vacio genera error solo en TIPO`() =
        assertSoloError(CampoOportunidad.TIPO, oportunidad(tipoEvento = ""))

    @Test fun `ORGANIZACION sin seleccionar genera error solo en ORGANIZACION`() {
        val errores = oportunidad(idOrganizacion = null)
        assertSoloError(CampoOportunidad.ORGANIZACION, errores)
        assertEquals("Selecciona una organización", errores[CampoOportunidad.ORGANIZACION])
    }

    @Test fun `DETECCION con formato invalido genera error solo en DETECCION`() =
        assertSoloError(CampoOportunidad.DETECCION, oportunidad(fechaDeteccion = "2026-11-01"))

    @Test fun `DETECCION posterior al inicio genera error solo en DETECCION`() {
        val errores = oportunidad(fechaDeteccion = "11/11/2026")
        assertSoloError(CampoOportunidad.DETECCION, errores)
        assertEquals("La fecha de detección no puede ser posterior al inicio", errores[CampoOportunidad.DETECCION])
    }

    @Test fun `INICIO vacio genera error solo en INICIO`() =
        assertSoloError(CampoOportunidad.INICIO, oportunidad(fechaInicio = ""))

    @Test fun `TERMINO anterior al inicio genera error solo en TERMINO`() {
        val errores = oportunidad(fechaTermino = "09/11/2026")
        assertSoloError(CampoOportunidad.TERMINO, errores)
        assertEquals("La fecha de término no puede ser anterior a la de inicio", errores[CampoOportunidad.TERMINO])
    }

    @Test fun `TERMINO vacio genera error solo en TERMINO`() =
        assertSoloError(CampoOportunidad.TERMINO, oportunidad(fechaTermino = ""))

    @Test fun `COMUNA vacia genera error de obligatoria`() {
        val errores = oportunidad(comuna = "")
        assertSoloError(CampoOportunidad.COMUNA, errores)
        assertEquals("La comuna es obligatorio", errores[CampoOportunidad.COMUNA])
    }

    @Test fun `COMUNA fuera del catalogo genera error de seleccion`() {
        val errores = oportunidad(comuna = "Narnia")
        assertSoloError(CampoOportunidad.COMUNA, errores)
        assertEquals("Selecciona una comuna de la lista", errores[CampoOportunidad.COMUNA])
    }

    @Test fun `PUBLICO cero genera error solo en PUBLICO`() =
        assertSoloError(CampoOportunidad.PUBLICO, oportunidad(publicoEsperado = "0"))

    @Test fun `COSTO negativo genera error solo en COSTO`() =
        assertSoloError(CampoOportunidad.COSTO, oportunidad(costoReferencial = "-1"))

    @Test fun `OBSERVACIONES de 501 caracteres genera error solo en OBSERVACIONES`() =
        assertSoloError(CampoOportunidad.OBSERVACIONES, oportunidad(observaciones = "a".repeat(501)))

    @Test fun `DESCRIPCION de 501 caracteres genera error solo en DESCRIPCION`() =
        assertSoloError(CampoOportunidad.DESCRIPCION, oportunidad(descripcion = "a".repeat(501)))

    @Test fun `FUENTE vacia genera error solo en FUENTE`() =
        assertSoloError(CampoOportunidad.FUENTE, oportunidad(fuenteInformacion = ""))

    @Test fun `PUBLICO_OBJETIVO vacio genera error solo en PUBLICO_OBJETIVO`() =
        assertSoloError(CampoOportunidad.PUBLICO_OBJETIVO, oportunidad(publicoObjetivo = ""))

    @Test fun `CONDICIONES de 501 caracteres genera error solo en CONDICIONES`() =
        assertSoloError(CampoOportunidad.CONDICIONES, oportunidad(condicionesParticipacion = "a".repeat(501)))

    @Test fun `REQUISITOS de 501 caracteres genera error solo en REQUISITOS`() =
        assertSoloError(CampoOportunidad.REQUISITOS, oportunidad(requisitos = "a".repeat(501)))

    @Test fun `ESPACIOS negativo genera error solo en ESPACIOS`() =
        assertSoloError(CampoOportunidad.ESPACIOS, oportunidad(espaciosDisponibles = "-1"))

    @Test fun `HORA_TERMINO igual a la de inicio el mismo dia genera error solo en HORA_TERMINO`() {
        val errores = oportunidad(fechaInicio = "10/11/2026", fechaTermino = "10/11/2026", horaInicio = "09:00", horaTermino = "09:00")
        assertSoloError(CampoOportunidad.HORA_TERMINO, errores)
        assertEquals("La hora de término debe ser posterior a la de inicio", errores[CampoOportunidad.HORA_TERMINO])
    }

    @Test fun `los 17 campos de CampoOportunidad tienen caso de prueba`() {
        assertEquals(17, CampoOportunidad.values().size)
    }

    // --- Límites exactos de los textos opcionales ---

    @Test
    fun `textos opcionales aceptan exactamente 500 caracteres`() {
        val errores = oportunidad(
            observaciones = "a".repeat(500), descripcion = "a".repeat(500),
            condicionesParticipacion = "a".repeat(500), requisitos = "a".repeat(500)
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `fuente y publico objetivo respetan sus maximos de 100 y 150`() {
        assertTrue(oportunidad(fuenteInformacion = "a".repeat(100), publicoObjetivo = "a".repeat(150)).isEmpty())
        val errores = oportunidad(fuenteInformacion = "a".repeat(101), publicoObjetivo = "a".repeat(151))
        assertEquals(setOf(CampoOportunidad.FUENTE, CampoOportunidad.PUBLICO_OBJETIVO), errores.keys)
    }

    // --- "El primero que falla gana" en NOMBRE ---

    @Test
    fun `nombre vacio muestra el error de obligatorio`() {
        assertEquals("El nombre es obligatorio", oportunidad(nombre = "")[CampoOportunidad.NOMBRE])
    }

    @Test
    fun `nombre de 2 caracteres muestra el error de minimo`() {
        assertEquals("El nombre debe tener al menos 3 caracteres", oportunidad(nombre = "ab")[CampoOportunidad.NOMBRE])
    }

    @Test
    fun `nombre de 2 letras con espacios alrededor tambien falla por minimo`() {
        assertEquals("El nombre debe tener al menos 3 caracteres", oportunidad(nombre = "  ab  ")[CampoOportunidad.NOMBRE])
    }

    @Test
    fun `nombre de 81 caracteres muestra el error de maximo`() {
        assertEquals("El nombre no puede superar 80 caracteres", oportunidad(nombre = "a".repeat(81))[CampoOportunidad.NOMBRE])
    }

    @Test
    fun `nombre de 3 y de 80 caracteres es valido`() {
        assertTrue(oportunidad(nombre = "abc").isEmpty())
        assertTrue(oportunidad(nombre = "a".repeat(80)).isEmpty())
    }

    // --- Horario: solo se valida si inicio y término son el mismo día ---

    @Test
    fun `horario con hora de inicio invalida el mismo dia devuelve error de inicio`() {
        val errores = oportunidad(fechaInicio = "10/11/2026", fechaTermino = "10/11/2026", horaInicio = "25:00", horaTermino = "18:00")
        assertEquals("La hora de inicio no es válida", errores[CampoOportunidad.HORA_TERMINO])
    }

    @Test
    fun `horario con hora de termino invalida el mismo dia devuelve error de termino`() {
        val errores = oportunidad(fechaInicio = "10/11/2026", fechaTermino = "10/11/2026", horaInicio = "09:00", horaTermino = "9:00")
        assertEquals("La hora de término no es válida", errores[CampoOportunidad.HORA_TERMINO])
    }

    @Test
    fun `horario con termino posterior el mismo dia es valido`() {
        val errores = oportunidad(fechaInicio = "10/11/2026", fechaTermino = "10/11/2026", horaInicio = "09:00", horaTermino = "09:01")
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `horario no se valida cuando el evento dura varios dias`() {
        val errores = oportunidad(fechaInicio = "10/11/2026", fechaTermino = "11/11/2026", horaInicio = "18:00", horaTermino = "09:00")
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `horario con horas basura no se valida si los dias son distintos`() {
        val errores = oportunidad(fechaInicio = "10/11/2026", fechaTermino = "11/11/2026", horaInicio = "xx", horaTermino = "yy")
        assertTrue(errores.isEmpty())
    }

    // ---------------------------------------------------------------------
    // Organización
    // ---------------------------------------------------------------------

    private fun organizacion(
        nombre: String = "Empresa Demo",
        tipo: String = "Empresa",
        rubro: String = "Retail",
        comuna: String = "Santiago"
    ) = ValidacionesFormularios.validarOrganizacion(nombre, tipo, rubro, comuna)

    @Test
    fun `organizacion valida devuelve mapa vacio`() {
        assertTrue(organizacion().isEmpty())
    }

    @Test
    fun `organizacion con nombre corto o largo falla`() {
        assertEquals("El nombre es obligatorio", organizacion(nombre = "")[CampoOrganizacion.NOMBRE])
        assertEquals("El nombre debe tener al menos 3 caracteres", organizacion(nombre = "ab")[CampoOrganizacion.NOMBRE])
        assertEquals("El nombre no puede superar 80 caracteres", organizacion(nombre = "a".repeat(81))[CampoOrganizacion.NOMBRE])
        assertTrue(organizacion(nombre = "a".repeat(80)).isEmpty())
    }

    @Test
    fun `organizacion sin tipo falla solo en TIPO`() {
        assertEquals(setOf(CampoOrganizacion.TIPO), organizacion(tipo = "").keys)
    }

    @Test
    fun `organizacion con rubro vacio o de mas de 50 caracteres falla solo en RUBRO`() {
        assertEquals(setOf(CampoOrganizacion.RUBRO), organizacion(rubro = "").keys)
        assertEquals(setOf(CampoOrganizacion.RUBRO), organizacion(rubro = "a".repeat(51)).keys)
        assertTrue(organizacion(rubro = "a".repeat(50)).isEmpty())
    }

    @Test
    fun `organizacion con comuna vacia o fuera del catalogo falla solo en COMUNA`() {
        assertEquals(setOf(CampoOrganizacion.COMUNA), organizacion(comuna = "").keys)
        assertEquals(setOf(CampoOrganizacion.COMUNA), organizacion(comuna = "Narnia").keys)
    }

    // ---------------------------------------------------------------------
    // Contacto
    // ---------------------------------------------------------------------

    private fun contacto(
        nombre: String = "Ana Pérez",
        cargo: String = "Gerente",
        area: String = "Marketing",
        correo: String = "ana@empresa.cl",
        telefono: String = "+56912345678"
    ) = ValidacionesFormularios.validarContacto(nombre, cargo, area, correo, telefono)

    @Test
    fun `contacto valido devuelve mapa vacio`() {
        assertTrue(contacto().isEmpty())
    }

    @Test
    fun `contacto con nombre vacio corto o de mas de 60 caracteres falla`() {
        assertEquals("El nombre es obligatorio", contacto(nombre = "")[CampoContacto.NOMBRE])
        assertEquals("El nombre debe tener al menos 3 caracteres", contacto(nombre = "ab")[CampoContacto.NOMBRE])
        assertEquals("El nombre no puede superar 60 caracteres", contacto(nombre = "a".repeat(61))[CampoContacto.NOMBRE])
        assertTrue(contacto(nombre = "a".repeat(60)).isEmpty())
    }

    @Test
    fun `contacto con cargo vacio o de mas de 60 caracteres falla solo en CARGO`() {
        assertEquals(setOf(CampoContacto.CARGO), contacto(cargo = "").keys)
        assertEquals(setOf(CampoContacto.CARGO), contacto(cargo = "a".repeat(61)).keys)
        assertTrue(contacto(cargo = "a".repeat(60)).isEmpty())
    }

    @Test
    fun `contacto sin area falla solo en AREA`() {
        assertEquals(setOf(CampoContacto.AREA), contacto(area = "").keys)
    }

    @Test
    fun `contacto con correo invalido falla solo en CORREO`() {
        assertEquals(setOf(CampoContacto.CORREO), contacto(correo = "sin-arroba").keys)
        assertEquals(setOf(CampoContacto.CORREO), contacto(correo = "").keys)
    }

    @Test
    fun `contacto recorta espacios de correo y telefono antes de validar`() {
        assertTrue(contacto(correo = "  ana@empresa.cl  ", telefono = "  +56912345678  ").isEmpty())
    }

    @Test
    fun `contacto con telefono invalido falla solo en TELEFONO`() {
        assertEquals(setOf(CampoContacto.TELEFONO), contacto(telefono = "123").keys)
        assertEquals(setOf(CampoContacto.TELEFONO), contacto(telefono = "").keys)
    }

    // ---------------------------------------------------------------------
    // Seguimiento (con "hoy" fijo)
    // ---------------------------------------------------------------------

    private val hoy = "22/11/2026"

    private fun seguimiento(
        tipoAccion: String = "Llamada",
        resultado: String = "Contacto cordial",
        proximaAccion: String = "Enviar propuesta",
        fechaProximo: String = "25/11/2026"
    ) = ValidacionesFormularios.validarSeguimiento(tipoAccion, resultado, proximaAccion, fechaProximo, hoy)

    @Test
    fun `seguimiento valido devuelve mapa vacio`() {
        assertTrue(seguimiento().isEmpty())
    }

    @Test
    fun `seguimiento sin tipo de accion falla solo en TIPO`() {
        assertEquals(setOf(CampoSeguimiento.TIPO), seguimiento(tipoAccion = "").keys)
    }

    @Test
    fun `seguimiento valida minimo 5 y maximo 300 en resultado`() {
        assertEquals("El resultado es obligatorio", seguimiento(resultado = "")[CampoSeguimiento.RESULTADO])
        assertEquals("El resultado debe tener al menos 5 caracteres", seguimiento(resultado = "abcd")[CampoSeguimiento.RESULTADO])
        assertTrue(seguimiento(resultado = "abcde").isEmpty())
        assertTrue(seguimiento(resultado = "a".repeat(300)).isEmpty())
        assertEquals("El resultado no puede superar 300 caracteres", seguimiento(resultado = "a".repeat(301))[CampoSeguimiento.RESULTADO])
    }

    @Test
    fun `seguimiento valida obligatoria y maximo 120 en proxima accion`() {
        assertEquals(setOf(CampoSeguimiento.PROXIMA_ACCION), seguimiento(proximaAccion = "").keys)
        assertTrue(seguimiento(proximaAccion = "a".repeat(120)).isEmpty())
        assertEquals(setOf(CampoSeguimiento.PROXIMA_ACCION), seguimiento(proximaAccion = "a".repeat(121)).keys)
    }

    @Test
    fun `seguimiento con fecha de ayer falla`() {
        val errores = seguimiento(fechaProximo = "21/11/2026")
        assertEquals(setOf(CampoSeguimiento.FECHA_PROXIMA), errores.keys)
        assertEquals("El próximo seguimiento no puede ser anterior a la fecha de la gestión", errores[CampoSeguimiento.FECHA_PROXIMA])
    }

    @Test
    fun `seguimiento con fecha de hoy y de manana es valido`() {
        assertTrue(seguimiento(fechaProximo = "22/11/2026").isEmpty())
        assertTrue(seguimiento(fechaProximo = "23/11/2026").isEmpty())
    }

    @Test
    fun `seguimiento con fecha vacia o con formato invalido falla`() {
        assertEquals("La fecha del próximo seguimiento es obligatorio", seguimiento(fechaProximo = "")[CampoSeguimiento.FECHA_PROXIMA])
        assertEquals(
            "La fecha del próximo seguimiento debe estar en formato dd/MM/yyyy",
            seguimiento(fechaProximo = "2026-11-25")[CampoSeguimiento.FECHA_PROXIMA]
        )
    }

    @Test
    fun `seguimiento usa la fecha real de hoy cuando no se entrega una`() {
        val errores = ValidacionesFormularios.validarSeguimiento("Llamada", "Contacto cordial", "Enviar propuesta", "01/01/2000")
        assertEquals(setOf(CampoSeguimiento.FECHA_PROXIMA), errores.keys)
        val valido = ValidacionesFormularios.validarSeguimiento("Llamada", "Contacto cordial", "Enviar propuesta", "01/01/2999")
        assertTrue(valido.isEmpty())
    }

    // ---------------------------------------------------------------------
    // Resultado de activación
    // ---------------------------------------------------------------------

    private fun resultado(
        asistentes: String = "100",
        contactos: String = "10",
        inscripciones: String = "5",
        aprendizajes: String = "Funcionó bien el stand"
    ) = ValidacionesFormularios.validarResultado(asistentes, contactos, inscripciones, aprendizajes)

    @Test
    fun `resultado valido devuelve mapa vacio`() {
        assertTrue(resultado().isEmpty())
    }

    @Test
    fun `resultado acepta cero en los contadores`() {
        assertTrue(resultado(asistentes = "0", contactos = "0", inscripciones = "0").isEmpty())
    }

    @Test
    fun `resultado con contadores invalidos falla en cada campo`() {
        val errores = resultado(asistentes = "-1", contactos = "abc", inscripciones = "")
        assertEquals(
            setOf(CampoResultado.ASISTENTES, CampoResultado.CONTACTOS, CampoResultado.INSCRIPCIONES),
            errores.keys
        )
        assertEquals("Los asistentes no puede ser negativo", errores[CampoResultado.ASISTENTES])
        assertEquals("Los contactos generados debe ser un número", errores[CampoResultado.CONTACTOS])
        assertEquals("Las inscripciones es obligatorio", errores[CampoResultado.INSCRIPCIONES])
    }

    @Test
    fun `resultado valida obligatorio y maximo 500 en aprendizajes`() {
        assertEquals("Los aprendizajes es obligatorio", resultado(aprendizajes = "")[CampoResultado.APRENDIZAJES])
        assertTrue(resultado(aprendizajes = "a".repeat(500)).isEmpty())
        assertEquals(
            "Los aprendizajes no puede superar 500 caracteres",
            resultado(aprendizajes = "a".repeat(501))[CampoResultado.APRENDIZAJES]
        )
    }
}
