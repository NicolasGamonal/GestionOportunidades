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
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import duoc.example.gestionoportunidades.ui.AppViewModelProvider
import duoc.example.gestionoportunidades.ui.alertas.DashboardScreen
import duoc.example.gestionoportunidades.ui.alertas.DashboardViewModel
import duoc.example.gestionoportunidades.ui.auth.AuthViewModel
import duoc.example.gestionoportunidades.ui.auth.LoginScreen
import duoc.example.gestionoportunidades.ui.components.TopBar
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleOportunidadScreen
import duoc.example.gestionoportunidades.ui.oportunidades.DetalleOportunidadViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.FormularioOportunidadScreen
import duoc.example.gestionoportunidades.ui.oportunidades.FormularioOportunidadViewModel
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadesScreen
import duoc.example.gestionoportunidades.ui.oportunidades.OportunidadesViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionScreen
import duoc.example.gestionoportunidades.ui.organizaciones.OrganizacionesScreen
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoScreen
import duoc.example.gestionoportunidades.ui.seguimiento.HistorialSeguimientoScreen
import duoc.example.gestionoportunidades.ui.seguimiento.SeguimientoViewModel
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorScreen
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorViewModel

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

    Scaffold(
        topBar = {
            if (mostrarTopBar) {
                val titulo = itemsMenu.find { it.first.ruta == rutaActual }?.third ?: when (rutaActual) {
                    Ruta.DetalleOportunidad.ruta -> "Detalle"
                    Ruta.FormularioOportunidad.ruta ->
                        if ((navBackStackEntry?.arguments?.getLong("id") ?: -1L) > 0) "Editar oportunidad"
                        else "Nueva oportunidad"
                    else -> "Gestión"
                }
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
                                navController.irAPestana(ruta.ruta)
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
                    onVerOportunidad = { id -> navController.navigate(Ruta.DetalleOportunidad.crear(id)) }
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
                        onVerOportunidad = { id -> navController.navigate(Ruta.DetalleOportunidad.crear(id)) }
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


            composable(Ruta.Oportunidades.ruta) {
                val vm: OportunidadesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                OportunidadesScreen(
                    viewModel = vm,
                    onVerDetalle = { id -> navController.navigate(Ruta.DetalleOportunidad.crear(id)) },
                    onNueva = { navController.navigate(Ruta.FormularioOportunidad.crear()) }
                )
            }

            composable(
                route = Ruta.DetalleOportunidad.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                val vm: DetalleOportunidadViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DetalleOportunidadScreen(
                    viewModel = vm,
                    onVolver = { navController.popBackStack() },
                    onVerHistorial = { navController.navigate("historial_seguimiento") },
                    onEditar = { id -> navController.navigate(Ruta.FormularioOportunidad.editar(id)) }
                )
            }
            composable(
                route = Ruta.FormularioOportunidad.ruta,
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) {
                val vm: FormularioOportunidadViewModel = viewModel(factory = AppViewModelProvider.Factory)
                FormularioOportunidadScreen (
                    viewModel = vm,
                    onGuardado = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() }
                )
            }
        }

    }
}
private fun NavController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(Ruta.Dashboard.ruta)
        launchSingleTop = true
    }
}