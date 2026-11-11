# Gestión Móvil de Oportunidades y Activaciones Corporativas

## Descripción
Aplicación Android para la gestión de oportunidades comerciales de activación corporativa, dirigida a una agencia especializada en convenios y beneficios (clínicas dentales, servicios automotrices y veterinarios). Permite identificar, organizar, priorizar y seguir eventos corporativos de forma 100% local.

## Integrantes
- **Josué** — Pantallas, tema, navegación, animaciones, cámara, calendario
- **Nicolas** — Room, validaciones, roles, login, dashboard, panel de supervisor, datos ficticios

## Tecnologías
- Kotlin + Jetpack Compose + Material Design 3
- Arquitectura MVVM (ViewModel + StateFlow + Repository)
- Room (persistencia local)
- KSP (compilación de Room)
- Coil (carga de imágenes)
- Navigation Compose

## Funcionalidades
- Login con 4 cuentas ficticias (3 ejecutivos, 1 supervisor)
- Dashboard con alertas según rol (eventos próximos, gestiones atrasadas)
- Perfil del usuario (nombre, rol, área y zona) con cierre de sesión
- CRUD de oportunidades con ficha completa (descripción, fuente, horario, región y comuna, público objetivo, potencial comercial, afinidad, condiciones, requisitos y espacios disponibles)
- Búsqueda y filtros de oportunidades: estado, prioridad, tipo de evento, región, comuna, responsable (supervisor) y rango de fechas
- Regiones y comunas de Chile (16 regiones, 346 comunas) en desplegables encadenados; la región se deriva de la comuna
- Registro de organizaciones y contactos (con medio de contacto preferente) e historial de gestiones por organización
- Seguimiento con historial y registro de nuevas gestiones
- Recordatorios locales: notificación el día del próximo seguimiento (AlarmManager, sin internet)
- Dashboard con alertas: gestiones atrasadas, seguimientos pendientes, oportunidades sin seguimiento y sin contacto, y eventos próximos
- Resultado de la activación (asistentes, contactos, inscripciones, evaluación y aprendizajes) para oportunidades confirmadas
- Panel de supervisor: asignación, reasignación, confirmar y descartar, más indicadores del equipo (oportunidades por estado y conversión)
- Cámara con vista previa y evidencia fotográfica asociada
- Integración con calendario del dispositivo
- Animaciones (AnimatedVisibility, animateColorAsState)

## Decisiones de diseño
- **Región derivada de la comuna:** en Room solo se guarda la comuna. La región depende de ella (comuna → región), por lo que guardarla duplicaría información y rompería la 3FN. Se obtiene con `Regiones.regionDe(comuna)`.
- **Horario del evento:** la hora de inicio y término se guarda dentro de los timestamps `fechaInicio` y `fechaTermino` (no se agregan columnas nuevas).
- **Conversión (HU-25):** porcentaje de oportunidades Confirmadas sobre el total.
- **Recordatorios (HU-21):** alarma inexacta (`AlarmManager.set`) a las 09:00 del día del próximo seguimiento. En Android 13+ la app pide el permiso de notificaciones al abrir el formulario de seguimiento. Las alarmas no sobreviven a un reinicio del teléfono.
- **Base de datos:** versión 5 con `fallbackToDestructiveMigration()`; al actualizar se recrea la base y se vuelven a cargar los datos de ejemplo.
- **Fuera de alcance:** HU-27 (rol Marketing) y HU-28 (mapa de eventos, requiere internet y contradice el funcionamiento sin conexión de HU-29).

## Cuentas de prueba
| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| ejecutivo1 | 123456 | Ejecutivo Comercial |
| ejecutivo2 | 123456 | Ejecutivo Comercial |
| ejecutivo3 | 123456 | Ejecutivo Comercial |
| supervisor1 | admin123 | Supervisor Comercial |

## Instrucciones de ejecución
1. Clonar el repositorio
2. Abrir en Android Studio (Hedgehog o superior)
3. Sync con Gradle
4. Crear emulador (recomendado: Pixel 8, API 34+)
5. Ejecutar la app
6. Iniciar sesión con cualquiera de las 4 cuentas de prueba

## Arquitectura

Estructura de carpetas:
- ui/ → Composables, ViewModels, UiState
- data/ → Room (entidades, DAOs, AppDatabase), Repositorios
- model/ → Entidades de datos
- util/ → Validaciones.kt, Permisos.kt, Regiones.kt (regiones y comunas), Recordatorios.kt (notificaciones locales)

Flujo de datos:
Compose UI → ViewModel (StateFlow) → Repository → DAO → Room

## Modelo de datos (3FN)

```mermaid
erDiagram
    USUARIO {
        Long idUsuario PK
        String username
        String contrasena
        String nombreCompleto
        String rol
        String area
        String zona
    }

    ORGANIZACION {
        Long idOrganizacion PK
        String nombre
        String tipo
        String rubro
        String comuna
    }

    CONTACTO {
        Long idContacto PK
        String nombre
        String cargo
        String area
        String correo
        String telefono
        String medioPreferente
        Long idOrganizacion FK
    }

    OPORTUNIDAD {
        Long idOportunidad PK
        String nombre
        String tipoEvento
        Long fechaDeteccion
        Long fechaInicio
        Long fechaTermino
        String comuna
        Int publicoEsperado
        String prioridad
        String estado
        Double costoReferencial
        String observaciones
        Long idOrganizacion FK
        Long idResponsable FK
        String descripcion
        String fuenteInformacion
        String potencialComercial
        String afinidad
        String publicoObjetivo
        String condicionesParticipacion
        String requisitos
        Int espaciosDisponibles
    }

    SEGUIMIENTO {
        Long idSeguimiento PK
        String tipoAccion
        Long fechaHora
        String resultado
        String proximaAccion
        Long fechaProximo
        Long idOportunidad FK
        Long idUsuario FK
    }

    EVIDENCIA {
        Long idEvidencia PK 
        String rutaArchivo
        Long fecha
        Long idOportunidad FK
    }

    RESULTADO_ACTIVACION {
        Long idResultado PK
        Long idOportunidad FK
        Int asistentes
        Int contactosGenerados
        Int inscripciones
        Int evaluacion
        String aprendizajes
        Long fechaRegistro
        Long idUsuario
    }

    USUARIO ||--o{ OPORTUNIDAD : "responsable de"
    ORGANIZACION ||--o{ OPORTUNIDAD : "tiene"
    ORGANIZACION ||--o{ CONTACTO : "posee"
    OPORTUNIDAD ||--o{ SEGUIMIENTO : "tiene"
    OPORTUNIDAD ||--o{ EVIDENCIA : "asocia"
    OPORTUNIDAD ||--o| RESULTADO_ACTIVACION : "tiene"
    USUARIO ||--o{ SEGUIMIENTO : "registra"
    CONTACTO |o--o{ SEGUIMIENTO : "relacionado"