# MOVA Phone

**Más que un teléfono, tu seguridad en tus manos.**

Aplicación de teléfono **nativa para Android** (Kotlin + Jetpack Compose) desarrollada por **Studio Lexair**.
No es una web dentro de una WebView ni un mockup: cada función usa la API real de Android
(Telephony, SmsManager, CallLog, ContactsContract, LocationManager, Keystore, BiometricPrompt…).

> **Desarrollado por Studio Lexair** · Versión 1.0.0 · © 2026

---

## 1. Qué es el proyecto

MOVA Phone reúne en una sola aplicación, en español y con interfaz oscura accesible:

- **Marcador inteligente** con teclado, háptica configurable y búsqueda mientras escribes.
- **Llamadas e historial** completos (todas, perdidas, entrantes, salientes, spam) con acciones reales.
- **Contactos** con favoritos, grupos/etiquetas, contactos privados e importación desde la agenda del sistema.
- **SOS avanzado**: mantener 3 segundos, cuenta regresiva visible, cancelación inmediata y estado por cada paso
  (aviso, SMS a contactos, ubicación, batería, llamada, registro local).
- **Contactos de emergencia** con prioridad reordenable y canales por contacto (llamada / SMS / ubicación).
- **Plantilla de SMS de emergencia** editable con marcadores `{nombre}` `{ubicacion}` `{enlace}` `{bateria}` `{hora}` `{precision}` `{timestamp}`.
- **Ubicación** agnóstica de proveedor (LocationManager del sistema, sin dependencias de mapas propietarias).
- **Centro de seguridad**: PIN con Keystore, bloqueo de la app, biometría, números bloqueados, contactos de
  confianza, modo privado y registro de eventos.
- **Motor de automatizaciones** disparador → condición → acción con historial de ejecuciones.
- **Detección de spam** local (reglas propias + reporte del usuario + coincidencia de números bloqueados).
- **Mensajes**: SMS reales hoy y capa `MessageProvider` preparada para chat por Internet en el futuro.
- **Modo conducción** con interfaz simplificada y comandos de voz del sistema.
- **Asistente MOVA**: órdenes por voz o escritas interpretadas **en el dispositivo**, con confirmación para
  acciones sensibles (llamar, enviar SMS, compartir ubicación, activar SOS).

## 2. Arquitectura

Clean Architecture + MVVM + modularización por feature:

```text
app/                     # Actividad única, grafo de navegación, contenedor de dependencias
build-logic/             # Plugins de convención (mova.android.application / library / library.compose / feature)
core/
  common/                # Resultados de negocio, utilidades (teléfono, formato), capacidades del dispositivo
  designsystem/          # Tema, colores, tipografía, componentes del mockup (Aurora, tarjetas, SOS, etc.)
  database/              # Room: 12 entidades, 7 DAOs, esquema v1
  security/              # Keystore, PIN, biometría, bloqueo de app, eventos, ajustes (DataStore)
  permissions/           # Permisos con justificación, comprobaciones y pantallas de "no disponible"
  navigation/            # Rutas tipadas, navegador y destinos de la barra inferior
  logging/               # Registro y mensajes de error aptos para el usuario (sin stack traces)
domain/                  # Modelos, contratos y casos de uso (sin Android)
  contacts/ calls/ emergency/ automation/
data/                    # Implementaciones: Room, CallLog, ContactsContract, SMS, LocationManager
  contacts/ calls/ messages/ emergency/ location/ automation/
services/                # Componentes que Android crea por sí mismo
  calls/                 # CallScreeningService (filtrado oficial) + receptor de estado
  sms/                   # Recepción de SMS, multiparte y confirmación de envío
  location/              # Servicio en primer plano durante SOS + Worker de historial + arranque
  notifications/         # Canales centralizados y notificaciones (incluida la de emergencia)
feature/                 # Una pantalla (o familia de pantallas) por módulo
  home/ dialer/ calls/ contacts/ favorites/ emergency/ messages/ location/
  automation/ security/ driving/ settings/ about/ smart-assistant/
```

Reglas de dependencia: `feature → domain/data/services → core`. El dominio no conoce Android.
Ninguna pantalla habla con APIs del sistema: siempre pasa por repositorios y casos de uso.
Los archivos se mantienen pequeños (~200 líneas) para que cada responsabilidad sea evidente.

### Inyección de dependencias

`MovaContainer` (módulo `app`) construye la infraestructura una sola vez y la reparte:

- **ViewModels**: por constructor, mediante fábricas explícitas (`container.homeFactory`, etc.).
- **Servicios y receptores** (creados por el sistema): mediante los puentes `CallServiceDependencies`,
  `SmsServiceDependencies`, `LocationServiceDependencies`, `EmergencyServiceDependencies` y
  `AutomationWorkerDependencies`, que la aplicación rellena al arrancar.

Es inyección explícita, sin reflexión: se ve de un vistazo de qué depende cada capa.

## 3. Instalación

```bash
git clone https://github.com/<usuario>/mova-phone.git
cd mova-phone
./gradlew :app:installDebug      # requiere un dispositivo o emulador conectado
```

Requisitos: **JDK 17**, Android SDK con **API 35** (build-tools 35.0.0). `minSdk` 26, `targetSdk` 35.

## 4. Compilación

```bash
./gradlew :app:assembleDebug                 # APK de depuración  → app/build/outputs/apk/debug/
./gradlew :app:assembleRelease               # APK de release (R8) → app/build/outputs/apk/release/
./gradlew :app:bundleRelease                 # AAB para Google Play → app/build/outputs/bundle/release/
./gradlew testDebugUnitTest                  # pruebas unitarias de todos los módulos
./gradlew lintDebug                          # análisis estático
```

## 5. Permisos

Los permisos se solicitan **en contexto**, con justificación y con alternativa cuando el usuario deniega.
El detalle completo está en [docs/PERMISOS.md](docs/PERMISOS.md).

| Permiso | Para qué |
|---|---|
| `CALL_PHONE` | Marcar llamadas desde el marcador, contactos, historial y SOS |
| `READ_PHONE_STATE` | Detectar llamadas entrantes y su estado real |
| `READ_CALL_LOG` | Mostrar el historial del dispositivo y las llamadas perdidas |
| `READ_CONTACTS` | Importar la agenda del sistema (sólo lectura) |
| `SEND_SMS`, `RECEIVE_SMS`, `READ_SMS` | Mensajes y SMS de emergencia reales |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Ubicación para el protocolo SOS |
| `POST_NOTIFICATIONS` | Avisar de emergencias, mensajes y automatizaciones |
| `VIBRATE`, `WAKE_LOCK` | Avisos durante una emergencia |
| `USE_BIOMETRIC` | Desbloqueo con huella o rostro |

El filtrado de llamadas usa `CallScreeningService` y **sólo actúa si el usuario concede el rol** de filtrado.
La grabación de llamadas se ofrece únicamente cuando la API pública del sistema lo permite
(en el resto de casos la app informa "no disponible" en vez de simularlo).

## 6. Configuración y variables de entorno

Copia `.env.example` a `.env` **sólo en tu entorno local** (nunca se sube al repositorio):

```env
GITHUB_TOKEN=
GITHUB_REPOSITORY=
MOVA_KEYSTORE_PATH=
MOVA_KEYSTORE_PASSWORD=
MOVA_KEY_ALIAS=
MOVA_KEY_PASSWORD=
```

La firma de release se toma de **GitHub Secrets**; no hay ninguna clave privada en el repositorio.
Ver [docs/FIRMA.md](docs/FIRMA.md).

## 7. GitHub Actions (CI/CD)

- **`.github/workflows/android.yml`** — checkout → JDK 17 → Android SDK → caché de Gradle → compilación →
  pruebas → lint → APK → AAB → publicación de artefactos. Los trabajos de **debug** y **release** están separados.
- **`.github/workflows/release.yml`** — al publicar un tag `v*` o al lanzarlo a mano: genera APK de release,
  AAB, `mapping.txt` y `checksums.txt` (SHA-256), y crea la **GitHub Release** con esos archivos adjuntos.

## 8. Generación de APK y release

```bash
# local
./gradlew :app:assembleRelease

# release automática en GitHub
git tag v1.0.0 && git push origin v1.0.0     # el workflow adjunta APK + AAB + mapping + checksums
```

## 9. Seguridad

- Sin cuentas, sin servidores propios, sin publicidad ni analítica.
- PIN cifrado con Android Keystore (PBKDF2, 120 000 iteraciones) — nunca en texto plano.
- Bloqueo de la app con timeout configurable y biometría opcional.
- Registro de eventos local (bloqueos, accesos, SOS, automatizaciones).
- Ningún secreto en el código ni en el APK. Ver [SECURITY.md](SECURITY.md).

## 10. Documentación técnica

| Documento | Contenido |
|---|---|
| [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md) | Capas, módulos y decisiones técnicas |
| [docs/DISENO.md](docs/DISENO.md) | Diseño del mockup y cómo se refleja en la app |
| [docs/PERMISOS.md](docs/PERMISOS.md) | Cada permiso, su justificación y su alternativa |
| [docs/SEGURIDAD.md](docs/SEGURIDAD.md) | Modelo de seguridad y privacidad |
| [docs/PRIVACIDAD.md](docs/PRIVACIDAD.md) | Qué datos se guardan, dónde y cómo borrarlos |
| [docs/COMPATIBILIDAD.md](docs/COMPATIBILIDAD.md) | Versiones de Android y funciones no disponibles |
| [docs/FIRMA.md](docs/FIRMA.md) | Firma de release con secretos |
| [docs/PRUEBAS.md](docs/PRUEBAS.md) | Estrategia y ejecución de pruebas |
| [docs/FASES.md](docs/FASES.md) | Estado de las 9 fases del proyecto |

## 11. Contribución

Lee [CONTRIBUTING.md](CONTRIBUTING.md). Resumen: ramas cortas, commits descriptivos, pruebas antes de la PR,
respetar la arquitectura modular y el límite de ~200 líneas por archivo.

## 12. Licencia

MIT — ver [LICENSE](LICENSE).

---

**Desarrollado por Studio Lexair** · © 2026 · MOVA Phone 1.0.0
