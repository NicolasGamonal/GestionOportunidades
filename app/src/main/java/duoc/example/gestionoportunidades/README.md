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
- CRUD de oportunidades con búsqueda y filtros (estado, prioridad)
- Registro de organizaciones y contactos
- Seguimiento con historial y registro de nuevas gestiones
- Panel de supervisor: asignación, reasignación, confirmar y descartar
- Cámara con vista previa y evidencia fotográfica asociada
- Integración con calendario del dispositivo
- Animaciones (AnimatedVisibility, animateColorAsState)

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
- util/ → Validaciones.kt, Permisos.kt

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
        Long id PK
        String rutaArchivo
        Long fecha
        Long idOportunidad FK
    }

    USUARIO ||--o{ OPORTUNIDAD : "responsable de"
    ORGANIZACION ||--o{ OPORTUNIDAD : "tiene"
    ORGANIZACION ||--o{ CONTACTO : "posee"
    OPORTUNIDAD ||--o{ SEGUIMIENTO : "tiene"
    OPORTUNIDAD ||--o{ EVIDENCIA : "asocia"
    USUARIO ||--o{ SEGUIMIENTO : "registra"   