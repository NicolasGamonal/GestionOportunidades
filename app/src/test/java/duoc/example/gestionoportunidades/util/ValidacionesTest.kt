package duoc.example.gestionoportunidades.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test

class ValidacionesTest {

    // --- validarObligatorio ---

    @Test
    fun `obligatorio con texto vacio devuelve error`() {
        assertEquals("El nombre es obligatorio", Validaciones.validarObligatorio("", "El nombre"))
    }

    @Test
    fun `obligatorio con solo espacios devuelve error`() {
        assertEquals("El nombre es obligatorio", Validaciones.validarObligatorio("   ", "El nombre"))
    }

    @Test
    fun `obligatorio con texto valido devuelve null`() {
        assertNull(Validaciones.validarObligatorio("Feria", "El nombre"))
    }

    // --- validarSeleccion ---

    @Test
    fun `seleccion null devuelve el mensaje recibido`() {
        assertEquals("Selecciona una organización", Validaciones.validarSeleccion(null, "Selecciona una organización"))
    }

    @Test
    fun `seleccion con valor devuelve null aunque sea cero o vacio`() {
        assertNull(Validaciones.validarSeleccion(5L, "x"))
        assertNull(Validaciones.validarSeleccion(0L, "x"))
        assertNull(Validaciones.validarSeleccion("", "x"))
    }

    // --- validarCredenciales ---

    @Test
    fun `credenciales con usuario o clave en blanco devuelven error`() {
        val mensaje = "Por favor, completa todos los campos"
        assertEquals(mensaje, Validaciones.validarCredenciales("", "clave"))
        assertEquals(mensaje, Validaciones.validarCredenciales("usuario", ""))
        assertEquals(mensaje, Validaciones.validarCredenciales("  ", "  "))
    }

    @Test
    fun `credenciales completas devuelven null`() {
        assertNull(Validaciones.validarCredenciales("ejecutivo1", "1234"))
    }

    // --- Longitud (limites exactos) ---

    @Test
    fun `longitud minima falla en min menos 1 y pasa en min y min mas 1`() {
        assertEquals("El nombre debe tener al menos 3 caracteres", Validaciones.validarLongitudMin("ab", 3, "El nombre"))
        assertNull(Validaciones.validarLongitudMin("abc", 3, "El nombre"))
        assertNull(Validaciones.validarLongitudMin("abcd", 3, "El nombre"))
    }

    @Test
    fun `longitud minima con texto vacio devuelve error`() {
        assertEquals("El nombre debe tener al menos 3 caracteres", Validaciones.validarLongitudMin("", 3, "El nombre"))
    }

    @Test
    fun `longitud maxima pasa en max menos 1 y max y falla en max mas 1`() {
        assertNull(Validaciones.validarLongitudMax("abcd", 5, "El nombre"))
        assertNull(Validaciones.validarLongitudMax("abcde", 5, "El nombre"))
        assertEquals("El nombre no puede superar 5 caracteres", Validaciones.validarLongitudMax("abcdef", 5, "El nombre"))
    }

    // --- Correo ---

    @Test
    fun `correo en blanco es obligatorio`() {
        assertEquals("El correo es obligatorio", Validaciones.validarCorreo(""))
        assertEquals("El correo es obligatorio", Validaciones.validarCorreo("   "))
    }

    @Test
    fun `correos validos devuelven null`() {
        assertNull(Validaciones.validarCorreo("ana@empresa.cl"))
        assertNull(Validaciones.validarCorreo("ana.perez@sub.empresa.com"))
        assertNull(Validaciones.validarCorreo("ana+ventas@empresa.cl"))
        assertNull(Validaciones.validarCorreo("a_b-c@x.io"))
    }

    @Test
    fun `correos invalidos devuelven Correo invalido`() {
        val invalidos = listOf(
            "sin-arroba.com",        // sin @
            "ana@empresa",           // sin punto en el dominio
            "ana@empresa.c",         // TLD de 1 letra
            "ana @empresa.cl",       // espacio en el medio
            "@empresa.cl",           // sin parte local
            "ana@.cl",               // dominio vacío
            "ana@empresa.cl ",       // espacio final (la regla no recorta)
            "ñandú@empresa.cl"       // caracter no permitido
        )
        invalidos.forEach { assertEquals("Debería ser inválido: '$it'", "Correo inválido", Validaciones.validarCorreo(it)) }
    }

    // --- Telefono ---

    @Test
    fun `telefono en blanco es obligatorio`() {
        assertEquals("El teléfono es obligatorio", Validaciones.validarTelefono(""))
        assertEquals("El teléfono es obligatorio", Validaciones.validarTelefono("  "))
    }

    @Test
    fun `telefono acepta entre 8 y 15 digitos`() {
        assertEquals("Teléfono inválido", Validaciones.validarTelefono("1234567"))       // 7
        assertNull(Validaciones.validarTelefono("12345678"))                              // 8
        assertNull(Validaciones.validarTelefono("123456789012345"))                       // 15
        assertEquals("Teléfono inválido", Validaciones.validarTelefono("1234567890123456")) // 16
    }

    @Test
    fun `telefono acepta mas inicial y separadores`() {
        assertNull(Validaciones.validarTelefono("+56912345678"))
        assertNull(Validaciones.validarTelefono("+56 9 1234-5678"))
        assertNull(Validaciones.validarTelefono("9 1234 5678"))
    }

    @Test
    fun `telefono con letras o mas mal ubicado es invalido`() {
        assertEquals("Teléfono inválido", Validaciones.validarTelefono("abc12345678"))
        assertEquals("Teléfono inválido", Validaciones.validarTelefono("56+912345678"))
        assertEquals("Teléfono inválido", Validaciones.validarTelefono("+"))
        assertEquals("Teléfono inválido", Validaciones.validarTelefono("12345678x"))
    }

    // --- Numeros ---

    @Test
    fun `numero no negativo en blanco es obligatorio`() {
        assertEquals("El costo es obligatorio", Validaciones.validarNumeroNoNegativo("", "El costo"))
    }

    @Test
    fun `numero no negativo con texto no numerico devuelve error`() {
        assertEquals("El costo debe ser un número", Validaciones.validarNumeroNoNegativo("abc", "El costo"))
        assertEquals("El costo debe ser un número", Validaciones.validarNumeroNoNegativo("1,5", "El costo"))
    }

    @Test
    fun `numero no negativo rechaza negativos y acepta cero y positivos`() {
        assertEquals("El costo no puede ser negativo", Validaciones.validarNumeroNoNegativo("-1", "El costo"))
        assertEquals("El costo no puede ser negativo", Validaciones.validarNumeroNoNegativo("-0.1", "El costo"))
        assertNull(Validaciones.validarNumeroNoNegativo("0", "El costo"))
        assertNull(Validaciones.validarNumeroNoNegativo("0.1", "El costo"))
        assertNull(Validaciones.validarNumeroNoNegativo("150000", "El costo"))
    }

    @Test
    fun `numero positivo en blanco es obligatorio`() {
        assertEquals("El público es obligatorio", Validaciones.validarNumeroPositivo(" ", "El público"))
    }

    @Test
    fun `numero positivo con texto no numerico devuelve error`() {
        assertEquals("El público debe ser un número", Validaciones.validarNumeroPositivo("abc", "El público"))
    }

    @Test
    fun `numero positivo rechaza cero y negativos y acepta decimales positivos`() {
        assertEquals("El público debe ser mayor que 0", Validaciones.validarNumeroPositivo("0", "El público"))
        assertEquals("El público debe ser mayor que 0", Validaciones.validarNumeroPositivo("-1", "El público"))
        assertNull(Validaciones.validarNumeroPositivo("0.1", "El público"))
        assertNull(Validaciones.validarNumeroPositivo("500", "El público"))
    }

    // --- Fechas ---

    @Test
    fun `fecha en blanco es obligatoria`() {
        assertEquals("La fecha es obligatorio", Validaciones.validarFecha("", "La fecha"))
    }

    @Test
    fun `fecha con formato dd MM yyyy valida devuelve null`() {
        assertNull(Validaciones.validarFecha("22/11/2026", "La fecha"))
        assertNull(Validaciones.validarFecha("29/02/2028", "La fecha")) // año bisiesto
    }

    @Test
    fun `fecha con otro formato o inexistente devuelve error de formato`() {
        val mensaje = "La fecha debe estar en formato dd/MM/yyyy"
        assertEquals(mensaje, Validaciones.validarFecha("2026-11-22", "La fecha"))
        assertEquals(mensaje, Validaciones.validarFecha("22-11-2026", "La fecha"))
        assertEquals(mensaje, Validaciones.validarFecha("32/01/2026", "La fecha"))
        assertEquals(mensaje, Validaciones.validarFecha("10/13/2026", "La fecha"))
        assertEquals(mensaje, Validaciones.validarFecha("texto", "La fecha"))
    }

    // DEFECTO 4 (no estaba en el plan): DateTimeFormatter.ofPattern("dd/MM/yyyy") usa el modo SMART,
    // que "corrige" en silencio el día inexistente (31/02/2026 se lee como 28/02/2026), así que
    // validarFecha los da por válidos. Arreglo: construir el formato con
    // DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT).
    // Se deja @Ignore hasta que el equipo decida corregir el código; quitar @Ignore tras el arreglo.
    @Ignore("Defecto 4: validarFecha acepta fechas inexistentes (modo SMART del formateador)")
    @Test
    fun `fecha inexistente como 31 de febrero deberia ser invalida`() {
        val mensaje = "La fecha debe estar en formato dd/MM/yyyy"
        assertEquals(mensaje, Validaciones.validarFecha("31/02/2026", "La fecha"))
        assertEquals(mensaje, Validaciones.validarFecha("30/02/2026", "La fecha"))
        assertEquals(mensaje, Validaciones.validarFecha("29/02/2027", "La fecha")) // 2027 no es bisiesto
        assertEquals(mensaje, Validaciones.validarFecha("31/04/2026", "La fecha")) // abril tiene 30 días
    }

    // --- Reglas entre fechas ---

    @Test
    fun `termino despues de inicio acepta mismo dia y posterior`() {
        assertNull(Validaciones.validarFechaTerminoDespuesInicio("10/11/2026", "10/11/2026"))
        assertNull(Validaciones.validarFechaTerminoDespuesInicio("10/11/2026", "11/11/2026"))
    }

    @Test
    fun `termino anterior a inicio devuelve error`() {
        assertEquals(
            "La fecha de término no puede ser anterior a la de inicio",
            Validaciones.validarFechaTerminoDespuesInicio("10/11/2026", "09/11/2026")
        )
    }

    @Test
    fun `termino despues de inicio con campos vacios o invalidos devuelve null`() {
        assertNull(Validaciones.validarFechaTerminoDespuesInicio("", "09/11/2026"))
        assertNull(Validaciones.validarFechaTerminoDespuesInicio("10/11/2026", ""))
        assertNull(Validaciones.validarFechaTerminoDespuesInicio("xx", "09/11/2026"))
    }

    @Test
    fun `deteccion antes de inicio acepta igual y anterior`() {
        assertNull(Validaciones.validarDeteccionAntesInicio("10/11/2026", "10/11/2026"))
        assertNull(Validaciones.validarDeteccionAntesInicio("01/11/2026", "10/11/2026"))
    }

    @Test
    fun `deteccion posterior a inicio devuelve error`() {
        assertEquals(
            "La fecha de detección no puede ser posterior al inicio",
            Validaciones.validarDeteccionAntesInicio("11/11/2026", "10/11/2026")
        )
    }

    @Test
    fun `deteccion antes de inicio con campos vacios o invalidos devuelve null`() {
        assertNull(Validaciones.validarDeteccionAntesInicio("", "10/11/2026"))
        assertNull(Validaciones.validarDeteccionAntesInicio("11/11/2026", ""))
        assertNull(Validaciones.validarDeteccionAntesInicio("xx", "10/11/2026"))
    }

    @Test
    fun `proximo seguimiento acepta igual y posterior a la gestion`() {
        assertNull(Validaciones.validarProximoSeguimiento("22/11/2026", "22/11/2026"))
        assertNull(Validaciones.validarProximoSeguimiento("22/11/2026", "23/11/2026"))
    }

    @Test
    fun `proximo seguimiento anterior a la gestion devuelve error`() {
        assertEquals(
            "El próximo seguimiento no puede ser anterior a la fecha de la gestión",
            Validaciones.validarProximoSeguimiento("22/11/2026", "21/11/2026")
        )
    }

    @Test
    fun `proximo seguimiento con campos vacios o invalidos devuelve null`() {
        assertNull(Validaciones.validarProximoSeguimiento("", "21/11/2026"))
        assertNull(Validaciones.validarProximoSeguimiento("22/11/2026", ""))
        assertNull(Validaciones.validarProximoSeguimiento("22/11/2026", "xx"))
    }

    // --- Filtros de entrada ---

    @Test
    fun `filtrarSoloDigitos elimina letras y simbolos`() {
        assertEquals("123", Validaciones.filtrarSoloDigitos("a1b2-c3 ", 7))
        assertEquals("", Validaciones.filtrarSoloDigitos("abc", 7))
    }

    @Test
    fun `filtrarSoloDigitos trunca al maximo indicado`() {
        assertEquals("1234567", Validaciones.filtrarSoloDigitos("12345678901", 7))
        assertEquals("1234567890", Validaciones.filtrarSoloDigitos("123456789012", 10))
        assertEquals("1234", Validaciones.filtrarSoloDigitos("1234", 7))
    }

    @Test
    fun `filtrarCorreo elimina espacios`() {
        assertEquals("ana@empresa.cl", Validaciones.filtrarCorreo(" ana @ empresa.cl "))
    }

    @Test
    fun `filtrarTelefono deja digitos mas y espacios y trunca a 16`() {
        assertEquals("+56 9 12345678 ", Validaciones.filtrarTelefono("+56 9 1234-5678 abc"))
        assertEquals("1234567890123456", Validaciones.filtrarTelefono("12345678901234567890"))
        assertEquals("", Validaciones.filtrarTelefono("abc-()"))
    }
}
