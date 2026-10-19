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

private val oportunidadDeEjemplo = Oportunidad(
    idOportunidad = 1,
    nombre = "Feria del Emprendedor",
    tipoEvento = "Feria empresarial",
    fechaDeteccion = 1_760_000_000_000,
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

@Composable
private fun MuestraDeComponentes() {
    GestionOportunidadesTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Estados.todos.forEach { StatusChip(it) }
                CampoFormulario(valor = "Feria Sur", onValorChange = {}, etiqueta = "Nombre", error = null)
                CampoFormulario(valor = "", onValorChange = {}, etiqueta = "Comuna", error = "La comuna es obligatoria")
                TarjetaOportunidad(oportunidad = oportunidadDeEjemplo, onClick = {})
                MensajeConfirmacion(visible = true, mensaje = "Oportunidad creada")
            }
        }
    }
}

@Preview(name = "Claro", showBackground = true, heightDp = 950)
@Composable
private fun PreviewClaro() = MuestraDeComponentes()

@Preview(name = "Oscuro", showBackground = true, heightDp = 950, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PreviewOscuro() = MuestraDeComponentes()