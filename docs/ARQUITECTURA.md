# Arquitectura · MOVA Phone

## Capas

```text
feature/*   →  UI Compose + ViewModel (estado de pantalla)
domain/*    →  modelos, contratos (repositorios/puertos) y casos de uso, sin Android
data/*      →  implementaciones reales: Room, CallLog, ContactsContract, SMS, LocationManager
services/*  →  componentes creados por el sistema (CallScreeningService, receptores, Worker, FGS)
core/*      →  infraestructura transversal reutilizable
app         →  actividad única, navegación, contenedor de dependencias, configuración
```

Regla de oro: **las flechas apuntan hacia dentro**. Una feature depende de contratos del dominio,
nunca de implementaciones; una implementación de datos nunca importa nada de `feature`.

## Módulos y responsabilidad

| Módulo | Responsabilidad |
|---|---|
| `core:common` | `MovaResult`/`MovaError`, utilidades de teléfono y formato, capacidades del dispositivo |
| `core:designsystem` | Tema, colores, tipografía, dimensiones, componentes del mockup |
| `core:database` | Room: entidades, DAOs y versión de esquema (sin `fallbackToDestructiveMigration`) |
| `core:security` | Keystore, PIN, biometría, bloqueo de la app, eventos, ajustes (DataStore) |
| `core:permissions` | Catálogo de permisos con justificación, comprobador y componentes de UI |
| `core:navigation` | Rutas tipadas, `MovaNavigator` y destinos de la barra inferior |
| `core:logging` | `MovaLog` y `UserFacingErrors` (mensajes para el usuario) |
| `domain:*` | Casos de uso por área: contactos, llamadas, emergencias, automatizaciones |
| `data:*` | Repositorios y fuentes de datos reales |
| `services:*` | Servicios, receptores y workers del sistema |
| `feature:*` | Una pantalla o familia de pantallas por módulo |

## Decisiones técnicas destacadas

1. **Flujo unidireccional** — UI → intención → ViewModel → caso de uso → repositorio → Flow → UI.
2. **Errores como datos** — `MovaResult` transporta un mensaje ya listo para el usuario; los detalles
   técnicos se registran en el log, nunca se muestran como stack trace.
3. **Inyección explícita** — `MovaContainer` + fábricas de ViewModel y puentes para componentes del
   sistema. Sin reflexión, sin magia y con el grafo de dependencias legible.
4. **Un único grafo de navegación** en `app` con rutas tipadas en `core:navigation`; las features
   piden intenciones de negocio (`navigator.toSos()`) y no manipulan rutas a mano.
5. **Room con migraciones explícitas** — el esquema se versiona; no se destruyen datos del usuario.
6. **Servicios mínimos** — un único servicio en primer plano, y sólo durante una emergencia activa;
   el resto de tareas periódicas usan WorkManager (30 minutos) para cuidar la batería.
7. **`CallScreeningService` en lugar de bloqueos por fuerza bruta** — es la vía oficial de Android;
   la app no intenta eludir restricciones del sistema.
8. **Branding centralizado** en `BrandConfig`: cambiar nombre, eslogan o desarrollador no toca pantallas.

## Hilos y concurrencia

- Toda la E/S (Room, SMS, ubicación) ocurre en `Dispatchers.IO` mediante repositorios y `Flow`.
- La UI consume `StateFlow` con `collectAsStateWithLifecycle`.
- Las acciones sensibles del SOS se orquestan con `CoroutineScope` propio y comunican su estado paso a paso.

## Rendimiento

- Room con índices en las columnas de búsqueda y orden (número normalizado, favoritos, fechas).
- Lazy lists en todas las pantallas con listas potencialmente largas.
- R8 + `shrinkResources` en release.
- Sin dependencias pesadas: no se incluye ningún SDK de mapas ni de analítica.

## Compatibilidad

`minSdk 26` (Android 8.0) y `targetSdk 35` (Android 15). Las funciones que dependen de versiones
superiores se activan por comprobación en tiempo de ejecución y, si no existen, se informa con
claridad. Detalle en [COMPATIBILIDAD.md](COMPATIBILIDAD.md).
