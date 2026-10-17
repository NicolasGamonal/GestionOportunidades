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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import duoc.example.gestionoportunidades.ui.AppViewModelProvider
import duoc.example.gestionoportunidades.ui.alertas.DashboardScreen
import duoc.example.gestionoportunidades.ui.alertas.DashboardViewModel
import duoc.example.gestionoportunidades.ui.auth.AuthViewModel
import duoc.example.gestionoportunidades.ui.auth.LoginScreen
import duoc.example.gestionoportunidades.ui.components.TopBar
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleOportunidadScreen
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadesScreen
import duoc.example.gestionoportunidades.ui.oportunidades.RegistroOportunidadScreen
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionScreen
import duoc.example.gestionoportunidades.ui.organizaciones.OrganizacionesScreen
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoScreen
import duoc.example.gestionoportunidades.ui.seguimiento.HistorialSeguimientoScreen
import duoc.example.gestionoportunidades.ui.seguimiento.SeguimientoViewModel
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorScreen
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorViewModel
import duoc.example.gestionoportunidades.ui.viewmodel.OportunidadViewModel

// Pantallas que ya dibujan su propia barra superior: ocultamos la global para no duplicarla
private val rutasConBarraPropia = setOf("historial_seguimiento", "formulario_seguimiento")

@Composable
fun NavegacionPrincipal() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    // Sesión: el rol viene del usuario que inició sesión
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val esSupervisor = authState.usuarioLogueado?.rol == "SUPERVISOR"

    // Ítems del menú inferior según el rol
    val itemsMenu = mutableListOf(
        Triple(Ruta.Dashboard, Icons.Filled.Dashboard, "Inicio"),
        Triple(Ruta.Oportunidades, Icons.Filled.Assessment, "Oportunidades"),
        Triple(Ruta.Organizaciones, Icons.Filled.Business, "Organizaciones")
    )
    if (esSupervisor) {
        itemsMenu.add(Triple(Ruta.PanelSupervisor, Icons.Filled.SupervisorAccount, "Equipo"))
    }

    val enLogin = rutaActual == Ruta.Login.ruta
    val mostrarTopBar = !enLogin && rutaActual !in rutasConBarraPropia
    val esPantallaDelMenu = itemsMenu.any { it.first.ruta == rutaActual }

    // ViewModels compartidos entre pantallas (los de J2 a J4 se migrarán a la fábrica)
    val seguimientoViewModel: SeguimientoViewModel = viewModel()
    val oportunidadViewModel: OportunidadViewModel = viewModel()

    Scaffold(
        topBar = {
            if (mostrarTopBar) {
                val titulo = itemsMenu.find { it.first.ruta == rutaActual }?.third ?: "Gestión"
                TopBar(
                    titulo = titulo,
                    mostrarBotonVolver = !esPantallaDelMenu,
                    onVolverClick = { navController.popBackStack() },
                    onCerrarSesion = {
                        authViewModel.cerrarSesion()
                        navController.navigate(Ruta.Login.ruta) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!enLogin) {
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
            startDestination = Ruta.Login.ruta,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Ruta.Login.ruta) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginExitoso = { _, _ ->
                        navController.navigate(Ruta.Dashboard.ruta) {
                            popUpTo(Ruta.Login.ruta) { inclusive = true }
                        }
                    }
                )
            }

            composable(Ruta.Dashboard.ruta) {
                val vm: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DashboardScreen(
                    viewModel = vm,
                    onVerOportunidad = { navController.navigate(Ruta.Oportunidades.ruta) } // provisional hasta J2
                )
            }

            composable(Ruta.Oportunidades.ruta) {
                OportunidadesScreen(
                    onVerDetalle = { navController.navigate("detalle_prueba") }
                )
            }

            composable(Ruta.Organizaciones.ruta) {
                OrganizacionesScreen(
                    onNuevaOrganizacion = { navController.navigate("formulario_organizacion") }
                )
            }

            composable(Ruta.PanelSupervisor.ruta) {
                if (esSupervisor) {
                    val vm: SupervisorViewModel = viewModel(factory = AppViewModelProvider.Factory)
                    SupervisorScreen(
                        viewModel = vm,
                        onVerOportunidad = { navController.navigate(Ruta.Oportunidades.ruta) } // provisional hasta J2
                    )
                } else {
                    // Bloqueo de ruta: un ejecutivo no puede ver el panel del supervisor
                    LaunchedEffect(Unit) {
                        navController.navigate(Ruta.Dashboard.ruta) {
                            popUpTo(Ruta.PanelSupervisor.ruta) { inclusive = true }
                        }
                    }
                }
            }

            composable("formulario_organizacion") {
                FormularioOrganizacionScreen(onVolver = { navController.popBackStack() })
            }

            composable("detalle_prueba") {
                val oportunidadPrueba = duoc.example.gestionoportunidades.data.Oportunidad(
                    id = 1,
                    nombre = "Feria Empresarial Sur",
                    tipoEvento = "Feria",
                    fechaDeteccion = "2026-10-10",
                    fechaInicio = "2026-10-20 09:00",
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

                DetalleOportunidadScreen(
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

            composable("registro") {
                RegistroOportunidadScreen(
                    viewModel = oportunidadViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}