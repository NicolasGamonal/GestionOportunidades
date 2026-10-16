package duoc.example.gestionoportunidades.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import duoc.example.gestionoportunidades.ui.components.TopBar
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleOportunidadScreen
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadesScreen
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionScreen
import duoc.example.gestionoportunidades.ui.organizaciones.OrganizacionesScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import duoc.example.gestionoportunidades.ui.oportunidades.RegistroOportunidadScreen
import duoc.example.gestionoportunidades.ui.seguimiento.SeguimientoViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.HistorialSeguimientoScreen
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoScreen
import duoc.example.gestionoportunidades.ui.viewmodel.OportunidadViewModel

@Composable
fun NavegacionPrincipal() {
    val navController = rememberNavController()
    // Obtenemos la ruta actual para saber qué título mostrar en el TopBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    // MOCK: Variable temporal para probar el menú según rol.
    // Tu compañero conectará esto con Auth y Room en las sesiones de PC EXTRA.
    val esSupervisor = true

    // Lista de ítems para el menú inferior
    val itemsMenu = mutableListOf(
        Triple(Ruta.Dashboard, Icons.Filled.Dashboard, "Inicio"),
        Triple(Ruta.Oportunidades, Icons.Filled.Assessment, "Oportunidades"),
        Triple(Ruta.Organizaciones, Icons.Filled.Business, "Organizaciones")
    )

    // Si es supervisor, agregamos su panel exclusivo al menú[cite: 1]
    if (esSupervisor) {
        itemsMenu.add(Triple(Ruta.PanelSupervisor, Icons.Filled.SupervisorAccount, "Equipo"))
    }

    // No mostramos TopBar ni menú inferior en el Login
    val mostrarBarras = rutaActual != Ruta.Login.ruta
    val seguimientoViewModel: SeguimientoViewModel = viewModel()
    val oportunidadViewModel: OportunidadViewModel = viewModel()

    Scaffold(
        topBar = {
            if (mostrarBarras) {
                // Buscamos el título correspondiente a la ruta actual
                val titulo = itemsMenu.find { it.first.ruta == rutaActual }?.third ?: "Gestión"
                TopBar(
                    titulo = titulo,
                    mostrarBotonVolver = rutaActual != Ruta.Dashboard.ruta,
                    onVolverClick = { navController.popBackStack() }
                )
            }
        },
        bottomBar = {
            if (mostrarBarras) {
                NavigationBar {
                    itemsMenu.forEach { (ruta, icono, etiqueta) ->
                        NavigationBarItem(
                            icon = { Icon(icono, contentDescription = etiqueta) },
                            label = { Text(etiqueta) },
                            selected = rutaActual == ruta.ruta,
                            onClick = {
                                navController.navigate(ruta.ruta) {
                                    popUpTo(Ruta.Dashboard.ruta) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Ruta.Dashboard.ruta,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Ruta.Login.ruta) {
                Text("Pantalla de Login (Compañero)")
            }
            composable(Ruta.Dashboard.ruta) {
                Text("Dashboard (Compañero)")
            }
            composable(Ruta.Oportunidades.ruta) {
                // Aquí llamamos a tu pantalla real en lugar del texto
                OportunidadesScreen (
                    onVerDetalle = { navController.navigate("detalle_prueba") }
                )
            }
            composable(Ruta.Organizaciones.ruta) {
                OrganizacionesScreen(
                    onNuevaOrganizacion = { navController.navigate("formulario_organizacion") }
                )
            }
            composable(Ruta.PanelSupervisor.ruta) {
                Text("Panel de Supervisor (Compañero)")
            }
            composable("formulario_organizacion") {
                FormularioOrganizacionScreen(onVolver = { navController.popBackStack() })
            }
            composable("detalle_prueba") {
                // Objeto alineado EXACTAMENTE al nuevo modelo de datos
                val oportunidadPrueba = duoc.example.gestionoportunidades.data.Oportunidad(
                    id = 1,
                    nombre = "Feria Empresarial Sur",
                    tipoEvento = "Feria",
                    fechaDeteccion = "2026-10-10",
                    fechaInicio = "2026-10-20 09:00", // Importante para el calendario
                    fechaTermino = "2026-10-20 18:00",
                    comuna = "Puente Alto",
                    publicoEsperado = 500,
                    prioridad = "Alta",
                    estado = "Identificada",
                    costoReferencial = 150000,
                    observaciones = "Requiere confirmación de espacio físico para la activación.",
                    idOrganizacion = 1,
                    idResponsable = 1
                )

                DetalleOportunidadScreen (
                    oportunidad = oportunidadPrueba,
                    onVolver = { navController.navigate(Ruta.Oportunidades.ruta) },
                    onVerHistorial = { navController.navigate("historial_seguimiento") }
                )
            }
            composable("historial_seguimiento") {
                HistorialSeguimientoScreen(
                    viewModel = seguimientoViewModel,
                    onNavigateToNuevoSeguimiento = { navController.navigate("formulario_seguimiento") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("formulario_seguimiento") {
                FormularioSeguimientoScreen(
                    viewModel = seguimientoViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // ... otras pantallas ...

            composable(route = "registro") {
                RegistroOportunidadScreen(
                    viewModel = oportunidadViewModel,
                    // NUEVO: Le decimos al navController que vuelva atrás
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}