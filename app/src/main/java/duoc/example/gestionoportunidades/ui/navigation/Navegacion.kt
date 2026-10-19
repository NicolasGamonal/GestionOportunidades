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
import duoc.example.gestionoportunidades.ui.organizaciones.DetalleOrganizacionScreen
import duoc.example.gestionoportunidades.ui.organizaciones.DetalleOrganizacionViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioContactoScreen
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioContactoViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionScreen
import duoc.example.gestionoportunidades.ui.organizaciones.FormularioOrganizacionViewModel
import duoc.example.gestionoportunidades.ui.organizaciones.OrganizacionesScreen
import duoc.example.gestionoportunidades.ui.organizaciones.OrganizacionesViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoScreen
import duoc.example.gestionoportunidades.ui.seguimiento.FormularioSeguimientoViewModel
import duoc.example.gestionoportunidades.ui.seguimiento.HistorialSeguimientoScreen
import duoc.example.gestionoportunidades.ui.seguimiento.HistorialSeguimientoViewModel
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorScreen
import duoc.example.gestionoportunidades.ui.supervisor.SupervisorViewModel

// NavegacionPrincipal: el "mapa" de toda la app.
//
// ¿Qué hace?
// Define TODAS las rutas de navegación, el menú inferior (bottom bar),
// la barra superior (TopBar) y las reglas de acceso por rol.
// Es el único archivo que sabe cómo se conectan las pantallas entre sí.
//
// ¿Por qué es un solo archivo y no uno por pantalla?
// Porque Navigation Compose requiere un NavHost central que registre todas
// las rutas. Si se dividiera en varios archivos, habría que combinarlos
// con extensores de NavGraphBuilder, lo cual agrega complejidad innecesaria
// para un proyecto de 12 pantallas.
//
// Estructura del archivo:
// 1. NavegacionPrincipal() → el Composable raíz que arma Scaffold + NavHost.
// 2. irAPestana() → función de extensión de NavController para navegar al menú inferior.
//
// Reglas de navegación:
// - La app SIEMPRE arranca en Login (startDestination = Ruta.Login.ruta).
// - Después del login, se navega al Dashboard y se borra Login de la pila
//   (popUpTo inclusive = true): el usuario no puede "volver" al login.
// - El menú inferior tiene 3 pestañas para ejecutivos y 4 para supervisores.
// - El panel del supervisor está BLOQUEADO para ejecutivos (redirección automática).
// - Las pantallas "hijas" (Detalle, Formulario) muestran botón "Volver" en el TopBar.
// - Las pantallas del menú (Dashboard, Oportunidades, Organizaciones) NO muestran "Volver".
//
// ¿Por qué Josue es el dueño de este archivo?
// Porque es el único que puede editarlo (regla del plan). Si el compañero
// necesita agregar una ruta, se lo pide a Josue. Esto evita conflictos de merge.

@Composable
fun NavegacionPrincipal() {
    // NavController: el "controlador" que maneja la pila de navegación.
    // rememberNavController(): se crea una sola vez y sobrevive a recomposiciones.
    val navController = rememberNavController()

    // Obtiene la entrada actual de la pila (la pantalla que se está viendo ahora).
    // currentBackStackEntryAsState(): convierte la back stack entry en un State
    // que se actualiza cada vez que cambia la pantalla (reactivo).
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // Ruta de la pantalla actual (ej: "dashboard", "oportunidades", "detalle_oportunidad/{id}").
    // Se usa para: decidir el título del TopBar, si mostrar "Volver", y qué ítem del menú está activo.
    val rutaActual = navBackStackEntry?.destination?.route

    // --- SESIÓN Y ROL ---
    // AuthViewModel se crea con el factory de AppViewModelProvider (que inyecta AppContainer).
    // Se necesita aquí (no solo en LoginScreen) para:
    // 1. Saber si el usuario es supervisor (para mostrar/ocultar la pestaña "Equipo").
    // 2. Permitir cerrar sesión desde cualquier pantalla (botón en el TopBar).
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    // ¿El usuario logueado es supervisor? Se usa para:
    // - Agregar la pestaña "Equipo" al menú inferior.
    // - Permitir/bloquear el acceso al PanelSupervisor.
    val esSupervisor = authState.usuarioLogueado?.rol == "SUPERVISOR"

    // --- MENÚ INFERIOR (BOTTOM BAR) ---
    // Se construye dinámicamente según el rol:
    // - EJECUTIVO: 3 pestañas (Inicio, Oportunidades, Organizaciones)
    // - SUPERVISOR: 4 pestañas (Inicio, Oportunidades, Organizaciones, Equipo)
    //
    // Cada ítem es un Triple: (Ruta, Icono, Etiqueta)
    // - Ruta: la ruta de navegación (String)
    // - Icono: el ícono Material que se muestra en el menú
    // - Etiqueta: el texto debajo del ícono
    val itemsMenu = mutableListOf(
        Triple(Ruta.Dashboard, Icons.Filled.Dashboard, "Inicio"),
        Triple(Ruta.Oportunidades, Icons.Filled.Assessment, "Oportunidades"),
        Triple(Ruta.Organizaciones, Icons.Filled.Business, "Organizaciones")
    )
    // Solo el supervisor ve la pestaña "Equipo" (panel de supervisor)
    if (esSupervisor) {
        itemsMenu.add(Triple(Ruta.PanelSupervisor, Icons.Filled.SupervisorAccount, "Equipo"))
    }

    // --- CONDICIONES VISUALES ---
    // ¿Estamos en la pantalla de Login? (no se muestra TopBar ni BottomBar)
    val enLogin = rutaActual == Ruta.Login.ruta

    // ¿Se muestra el TopBar? (siempre, excepto en Login que tiene su propio diseño)
    val mostrarTopBar = !enLogin

    // ¿La pantalla actual es una de las pestañas del menú?
    // Si SÍ → no se muestra botón "Volver" (son pantallas "raíz").
    // Si NO → se muestra botón "Volver" (son pantallas "hijas": Detalle, Formulario, etc.)
    val esPantallaDelMenu = itemsMenu.any { it.first.ruta == rutaActual }

    // --- SCAFFOLD: estructura base de la pantalla ---
    // Scaffold de Material 3: organiza TopBar + contenido + BottomBar.
    // El padding que devuelve se aplica al NavHost para que el contenido
    // no se solape con la barra superior ni inferior.
    Scaffold(
        // --- BARRA SUPERIOR ---
        topBar = {
            if (mostrarTopBar) {
                // Extraer el argumento "id" de la ruta actual (si existe).
                // Se usa para decidir si el formulario es "Crear" o "Editar":
                // - id > 0 → editando (se muestra "Editar oportunidad")
                // - id = -1 (default) → creando (se muestra "Nueva oportunidad")
                val idArgumento = navBackStackEntry?.arguments?.getLong("id") ?: -1L
                val editando = idArgumento > 0

                // Calcular el título del TopBar según la ruta actual.
                // Primero busca en las pestañas del menú (Dashboard, Oportunidades, etc.).
                // Si no es una pestaña, usa un when para las pantallas hijas.
                val titulo = itemsMenu.find { it.first.ruta == rutaActual }?.third ?: when (rutaActual) {
                    Ruta.DetalleOportunidad.ruta -> "Detalle"
                    Ruta.FormularioOportunidad.ruta -> if (editando) "Editar oportunidad" else "Nueva oportunidad"
                    Ruta.DetalleOrganizacion.ruta -> "Organización"
                    Ruta.FormularioOrganizacion.ruta -> if (editando) "Editar organización" else "Nueva organización"
                    Ruta.FormularioContacto.ruta -> if (editando) "Editar contacto" else "Nuevo contacto"
                    Ruta.HistorialSeguimiento.ruta -> "Historial de gestión"
                    Ruta.FormularioSeguimiento.ruta -> "Nueva gestión"
                    else -> "Gestión"
                }

                // TopBar reutilizable (ui/components/):
                // - titulo: el texto calculado arriba
                // - mostrarBotonVolver: solo en pantallas hijas (no en las del menú)
                // - onVolverClick: regresa a la pantalla anterior (popBackStack)
                // - onCerrarSesion: borra la sesión y navega al Login (borrando toda la pila)
                TopBar(
                    titulo = titulo,
                    mostrarBotonVolver = !esPantallaDelMenu,
                    onVolverClick = { navController.popBackStack() },
                    onCerrarSesion = {
                        // 1. Borrar la sesión global (SesionManager)
                        authViewModel.cerrarSesion()
                        // 2. Navegar al Login y BORRAR TODA la pila de navegación
                        //    (popUpTo inclusive = true: elimina Login y todo lo que haya encima).
                        //    Esto impide que el usuario presione "Volver" y regrese al Dashboard.
                        navController.navigate(Ruta.Login.ruta) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        },
        // --- BARRA INFERIOR (MENÚ DE PESTAÑAS) ---
        // Solo se muestra si NO estamos en Login.
        bottomBar = {
            if (!enLogin) {
                NavigationBar {
                    // Un NavigationBarItem por cada pestaña del menú
                    itemsMenu.forEach { (ruta, icono, etiqueta) ->
                        NavigationBarItem(
                            icon = { Icon(icono, contentDescription = etiqueta) },
                            label = { Text(etiqueta) },
                            // selected: true si la ruta actual coincide con esta pestaña
                            // (resalta visualmente la pestaña activa)
                            selected = rutaActual == ruta.ruta,
                            onClick = { navController.irAPestana(ruta.ruta) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // --- NAVHOST: el "mapa" de todas las rutas ---
        // NavHost: contenedor que muestra la pantalla correspondiente a la ruta actual.
        // startDestination: la primera pantalla que se muestra al abrir la app (Login).
        // modifier = Modifier.padding(paddingValues): respeta el espacio ocupado
        // por el TopBar y el BottomBar (el contenido no se solapa).
        NavHost(
            navController = navController,
            startDestination = Ruta.Login.ruta,
            modifier = Modifier.padding(paddingValues)
        ) {
            // ================================================================
            // RUTA: LOGIN
            // ================================================================
            // Pantalla de inicio de sesión. No tiene TopBar ni BottomBar.
            // Al login exitoso: navega al Dashboard y borra Login de la pila.
            composable(Ruta.Login.ruta) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginExitoso = { _, _ ->
                        // Navega al Dashboard y elimina Login de la pila
                        // (el usuario no puede "volver" al login con el botón de atrás)
                        navController.navigate(Ruta.Dashboard.ruta) {
                            popUpTo(Ruta.Login.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // ================================================================
            // RUTA: DASHBOARD (pestaña "Inicio")
            // ================================================================
            // Pantalla principal post-login. Muestra alertas según rol.
            composable(Ruta.Dashboard.ruta) {
                val vm: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DashboardScreen(
                    viewModel = vm,
                    // Al tocar una tarjeta del dashboard → navegar al detalle de esa oportunidad
                    onVerOportunidad = { id ->
                        navController.navigate(Ruta.DetalleOportunidad.crear(id))
                    }
                )
            }

            // ================================================================
            // RUTA: OPORTUNIDADES (pestaña "Oportunidades")
            // ================================================================
            // Lista de oportunidades con búsqueda y filtros.
            composable(Ruta.Oportunidades.ruta) {
                val vm: OportunidadesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                OportunidadesScreen(
                    viewModel = vm,
                    // Al tocar una tarjeta → navegar al detalle
                    onVerDetalle = { id ->
                        navController.navigate(Ruta.DetalleOportunidad.crear(id))
                    },
                    // Al presionar el botón "+" → navegar al formulario de creación
                    onNueva = { navController.navigate(Ruta.FormularioOportunidad.crear()) }
                )
            }

            // ================================================================
            // RUTA: DETALLE OPORTUNIDAD (pantalla hija)
            // ================================================================
            // Detalle completo de una oportunidad (con cámara, calendario, historial).
            // Recibe un argumento: id (Long) → cuál oportunidad mostrar.
            composable(
                route = Ruta.DetalleOportunidad.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                val vm: DetalleOportunidadViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DetalleOportunidadScreen(
                    viewModel = vm,
                    onVolver = { navController.popBackStack() },
                    // Al presionar "Ver historial" → navegar al historial de gestiones
                    onVerHistorial = { id ->
                        navController.navigate(Ruta.HistorialSeguimiento.crear(id))
                    },
                    // Al presionar "Editar" → navegar al formulario con el id (modo edición)
                    onEditar = { id ->
                        navController.navigate(Ruta.FormularioOportunidad.editar(id))
                    }
                )
            }

            // ================================================================
            // RUTA: FORMULARIO OPORTUNIDAD (pantalla hija)
            // ================================================================
            // Crear o editar una oportunidad.
            // Argumento opcional: id (default = -1).
            // - id = -1 → modo CREAR (se muestra "Nueva oportunidad")
            // - id > 0 → modo EDITAR (se muestra "Editar oportunidad", carga los datos)
            composable(
                route = Ruta.FormularioOportunidad.ruta,
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L  // -1 = crear nuevo
                })
            ) {
                val vm: FormularioOportunidadViewModel =
                    viewModel(factory = AppViewModelProvider.Factory)
                FormularioOportunidadScreen(
                    viewModel = vm,
                    // Al guardar exitosamente → volver a la pantalla anterior (detalle o lista)
                    onGuardado = { navController.popBackStack() },
                    // Al cancelar → volver sin guardar
                    onCancelar = { navController.popBackStack() },
                    // Al eliminar → volver a la lista de oportunidades (pestaña)
                    onEliminado = { navController.irAPestana(Ruta.Oportunidades.ruta) }
                )
            }

            // ================================================================
            // RUTA: ORGANIZACIONES (pestaña "Organizaciones")
            // ================================================================
            // Lista de organizaciones con búsqueda.
            composable(Ruta.Organizaciones.ruta) {
                val vm: OrganizacionesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                OrganizacionesScreen(
                    viewModel = vm,
                    // Al tocar una tarjeta → navegar al detalle de la organización
                    onVerDetalle = { id -> navController.navigate(Ruta.DetalleOrganizacion.crear(id)) },
                    // Al presionar "+" → navegar al formulario de creación
                    onNueva = { navController.navigate(Ruta.FormularioOrganizacion.crear()) }
                )
            }

            // ================================================================
            // RUTA: DETALLE ORGANIZACIÓN (pantalla hija)
            // ================================================================
            // Detalle de una organización: datos + lista de contactos.
            composable(
                route = Ruta.DetalleOrganizacion.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                val vm: DetalleOrganizacionViewModel = viewModel(factory = AppViewModelProvider.Factory)
                DetalleOrganizacionScreen (
                    viewModel = vm,
                    onVolver = { navController.popBackStack() },
                    // Al presionar "Editar" → formulario de organización (modo edición)
                    onEditar = { id -> navController.navigate(Ruta.FormularioOrganizacion.editar(id)) },
                    // Al presionar "Nuevo contacto" → formulario de contacto (modo creación)
                    onNuevoContacto = { idOrg -> navController.navigate(Ruta.FormularioContacto.crear(idOrg)) },
                    // Al tocar un contacto existente → formulario de contacto (modo edición)
                    onEditarContacto = { idOrg, idContacto ->
                        navController.navigate(Ruta.FormularioContacto.editar(idOrg, idContacto))
                    }
                )
            }

            // ================================================================
            // RUTA: FORMULARIO ORGANIZACIÓN (pantalla hija)
            // ================================================================
            // Crear o editar una organización.
            // Argumento opcional: id (default = -1 → crear).
            composable(
                route = Ruta.FormularioOrganizacion.ruta,
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) {
                val vm: FormularioOrganizacionViewModel = viewModel(factory = AppViewModelProvider.Factory)
                FormularioOrganizacionScreen(
                    viewModel = vm,
                    onGuardado = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() },
                    // Al eliminar → volver a la lista de organizaciones (pestaña)
                    onEliminado = { navController.irAPestana(Ruta.Organizaciones.ruta) }
                )
            }

            // ================================================================
            // RUTA: FORMULARIO CONTACTO (pantalla hija)
            // ================================================================
            // Crear o editar un contacto.
            // Argumentos:
            // - idOrganizacion (obligatorio): a qué organización pertenece.
            // - id (opcional, default = -1): -1 = crear, >0 = editar.
            composable(
                route = Ruta.FormularioContacto.ruta,
                arguments = listOf(
                    navArgument("idOrganizacion") { type = NavType.LongType },
                    navArgument("id") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                val vm: FormularioContactoViewModel = viewModel(factory = AppViewModelProvider.Factory)
                FormularioContactoScreen (
                    viewModel = vm,
                    onGuardado = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() },
                    // Al eliminar → volver (al detalle de la organización)
                    onEliminado = { navController.popBackStack() }
                )
            }

            // ================================================================
            // RUTA: PANEL SUPERVISOR (pestaña "Equipo" - solo supervisor)
            // ================================================================
            // Consulta global de oportunidades, asignación, confirmación y descarte.
            //
            // BLOQUEO DE RUTA: si un ejecutivo intenta acceder (por manipulación
            // de la URL o bug), se redirige automáticamente al Dashboard.
            // Esto es una capa de seguridad adicional a Permisos.kt.
            composable(Ruta.PanelSupervisor.ruta) {
                if (esSupervisor) {
                    // Supervisor: muestra el panel normalmente
                    val vm: SupervisorViewModel = viewModel(factory = AppViewModelProvider.Factory)
                    SupervisorScreen(
                        viewModel = vm,
                        // Al tocar una oportunidad → navegar al detalle
                        onVerOportunidad = { id ->
                            navController.navigate(Ruta.DetalleOportunidad.crear(id))
                        }
                    )
                } else {
                    // BLOQUEO: un ejecutivo no puede ver el panel del supervisor.
                    // LaunchedEffect(Unit): se ejecuta una vez al componer esta pantalla.
                    // Navega al Dashboard y borra PanelSupervisor de la pila
                    // (el usuario no puede "volver" al panel bloqueado).
                    LaunchedEffect(Unit) {
                        navController.navigate(Ruta.Dashboard.ruta) {
                            popUpTo(Ruta.PanelSupervisor.ruta) { inclusive = true }
                        }
                    }
                }
            }

            // ================================================================
            // RUTA: HISTORIAL SEGUIMIENTO (pantalla hija)
            // ================================================================
            // Historial de gestiones de una oportunidad (timeline).
            // Recibe: idOportunidad → de cuál oportunidad ver el historial.
            composable(
                route = Ruta.HistorialSeguimiento.ruta,
                arguments = listOf(navArgument("idOportunidad") { type = NavType.LongType })
            ) {
                val vm: HistorialSeguimientoViewModel = viewModel(factory = AppViewModelProvider.Factory)
                HistorialSeguimientoScreen(
                    viewModel = vm,
                    onVolver = { navController.popBackStack() },
                    // Al presionar "Nueva gestión" → formulario de seguimiento
                    onNuevaGestion = { id -> navController.navigate(Ruta.FormularioSeguimiento.crear(id)) }
                )
            }

            // ================================================================
            // RUTA: FORMULARIO SEGUIMIENTO (pantalla hija)
            // ================================================================
            // Registrar una nueva gestión (llamada, email, reunión).
            // Recibe: idOportunidad → a cuál oportunidad se asocia la gestión.
            composable(
                route = Ruta.FormularioSeguimiento.ruta,
                arguments = listOf(navArgument("idOportunidad") { type = NavType.LongType })
            ) {
                val vm: FormularioSeguimientoViewModel = viewModel(factory = AppViewModelProvider.Factory)
                FormularioSeguimientoScreen(
                    viewModel = vm,
                    onGuardado = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() }
                )
            }
        }
    }
}

// Función de extensión de NavController: navegar a una pestaña del menú inferior.
//
// ¿Por qué no usar navController.navigate(ruta) directamente?
// Porque al navegar a una pestaña del menú, se quiere:
// 1. BORRAR las pantallas hijas de la pila (si estaba en Detalle → Oportunidades,
//    al presionar "Organizaciones" no se quiere que "Volver" regrese al Detalle).
// 2. NO duplicar la pestaña si ya está activa (launchSingleTop).
// 3. MANTENER el Dashboard como "base" de la pila (popUpTo Dashboard).
//
// ¿Cómo funciona?
// - popUpTo(Ruta.Dashboard.ruta): elimina todo lo que esté ENCIMA del Dashboard
//   en la pila (pero no el Dashboard mismo). Así el Dashboard siempre queda como
//   pantalla "raíz" y el botón "Volver" desde cualquier pestaña regresa al Dashboard.
// - launchSingleTop = true: si la pestaña ya está activa, no se crea una nueva
//   entrada en la pila (evita duplicados).
//
// Ejemplo de pila de navegación:
//   Dashboard → Oportunidades → Detalle(1) → Formulario
//   Usuario presiona pestaña "Organizaciones":
//     popUpTo(Dashboard) borra: Oportunidades, Detalle(1), Formulario
//     navigate(Organizaciones) agrega: Organizaciones
//   Pila resultante: Dashboard → Organizaciones
//   "Volver" desde Organizaciones → Dashboard (no a Detalle, que ya se borró)
private fun NavController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(Ruta.Dashboard.ruta)
        launchSingleTop = true
    }
}