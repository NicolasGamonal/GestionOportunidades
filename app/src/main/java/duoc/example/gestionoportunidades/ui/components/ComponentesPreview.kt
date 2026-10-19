package duoc.example.gestionoportunidades.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import duoc.example.gestionoportunidades.model.Oportunidad
import duoc.example.gestionoportunidades.ui.theme.GestionOportunidadesTheme
import duoc.example.gestionoportunidades.util.Estados

// Archivo de Previews de Compose para los componentes reutilizables.
//
// ¿Para qué sirve?
// Permite ver los componentes (StatusChip, CampoFormulario, TarjetaOportunidad,
// MensajeConfirmacion) directamente en Android Studio (pestaña "Design" / Preview)
// SIN necesidad de ejecutar la app en un emulador.
//
// ¿Por qué es importante para el proyecto?
// El plan dice que el compañero trabaja "sin emulador, con Compose Preview".
// Este archivo es la herramienta que le permite verificar que los componentes
// se ven bien (colores, tipografía, espaciados) sin necesitar un dispositivo.
//
// ¿Por qué está en ui/components/ y no en una carpeta de tests?
// Porque @Preview no es un test: es una herramienta de diseño visual.
// No verifica lógica, solo muestra cómo se ve el componente con datos de ejemplo.
//
// ¿Por qué los previews son private?
// Porque no se usan desde ninguna otra pantalla. Solo Android Studio los invoca
// internamente para renderizar la vista previa. Si fueran públicos, se podrían
// llamar accidentalmente desde la app (y no deberían estar en el flujo de navegación).

// Dato de ejemplo fijo para los previews.
// No viene de Room ni de un ViewModel: es un objeto hardcodeado con valores
// representativos (una oportunidad "Identificada" de prioridad "Alta").
// Se usa para que TarjetaOportunidad y StatusChip tengan algo que mostrar.
private val oportunidadDeEjemplo = Oportunidad(
    idOportunidad = 1,
    nombre = "Feria del Emprendedor",
    tipoEvento = "Feria empresarial",
    fechaDeteccion = 1_760_000_000_000,   // Timestamp fijo (no depende de la fecha actual)
    fechaInicio = 1_762_000_000_000,
    fechaTermino = 1_762_040_000_000,
    comuna = "Providencia",
    publicoEsperado = 800,
    prioridad = "Alta",
    estado = "Identificada",
    costoReferencial = 250000.0,
    observaciones = "Ejemplo",
    idOrganizacion = 1,
    idResponsable = 1
)

// Composable privado que agrupa todos los componentes en una sola columna.
// Se usa como "lienzo" para los 2 previews (claro y oscuro).
//
// ¿Por qué no hacer 2 composables separados (uno por tema)?
// Porque el contenido es idéntico: solo cambia el tema (claro/oscuro).
// Al compartir el mismo composable, cualquier cambio en los componentes
// se refleja automáticamente en ambos previews.
@Composable
private fun MuestraDeComponentes() {
    // GestionOportunidadesTheme: aplica el tema de la app (colores, tipografía, formas).
    // En el preview claro usa los colores claros; en el oscuro (UI_MODE_NIGHT_YES)
    // usa los colores oscuros. El tema lo maneja internamente.
    GestionOportunidadesTheme {
        // Surface: aplica el color de fondo del tema (background).
        // Sin Surface, el fondo sería transparente en el preview.
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. StatusChip: muestra un chip por cada estado del flujo.
                //    Estados.todos = ["Identificada", "Contacto realizado", "Propuesta enviada",
                //                     "En espera de respuesta", "Confirmada", "Descartada"]
                //    Permite verificar que cada estado tiene su color semántico correcto.
                Estados.todos.forEach { StatusChip(it) }

                // 2. CampoFormulario sin error (caso normal)
                CampoFormulario(valor = "Feria Sur", onValorChange = {}, etiqueta = "Nombre", error = null)

                // 3. CampoFormulario con error (caso de validación fallida)
                //    Muestra: borde rojo + ícono de error + supportingText debajo.
                CampoFormulario(valor = "", onValorChange = {}, etiqueta = "Comuna", error = "La comuna es obligatoria")

                // 4. TarjetaOportunidad: la tarjeta completa de la lista.
                //    Muestra: nombre, estado (chip), prioridad, comuna, fecha.
                TarjetaOportunidad(oportunidad = oportunidadDeEjemplo, onClick = {})

                // 5. MensajeConfirmacion: el snackbar/toast de éxito.
                //    Se muestra cuando visible = true (como después de guardar una oportunidad).
                MensajeConfirmacion(visible = true, mensaje = "Oportunidad creada")
            }
        }
    }
}

// Preview en modo CLARO (tema por defecto).
// heightDp = 950: altura suficiente para ver todos los componentes sin scroll.
// showBackground = true: muestra el fondo del sistema (no transparente).
@Preview(name = "Claro", showBackground = true, heightDp = 950)
@Composable
private fun PreviewClaro() = MuestraDeComponentes()

// Preview en modo OSCURO (UI_MODE_NIGHT_YES fuerza el tema oscuro).
// Permite verificar que los colores semánticos funcionan bien en ambos temas
// (ej: que el chip "Confirmada" se lea bien sobre fondo oscuro).
@Preview(name = "Oscuro", showBackground = true, heightDp = 950, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PreviewOscuro() = MuestraDeComponentes()