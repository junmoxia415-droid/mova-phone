# Auditoría real de MOVA Phone 1.0.1

**Desarrollado por Studio Lexair** · Auditoría hecha sobre el código del repositorio, no sobre la intención.
Leyenda: **✅ real** (funciona con APIs del sistema) · **🟡 parcial** (existe, con límites que se explican) · **❌ no implementado** (documentado y con alternativa).

Fecha: 2026-09-23 · Versión auditada: **1.0.1 (versionCode 2)** · Versión anterior auditada: 1.0.0

---

## 1. Resumen de la auditoría

| Punto | Qué pedía el usuario | Estado antes (1.0.0) | Estado ahora (1.0.1) |
|---|---|---|---|
| a | Auditoría honesta | No existía | **Este documento**, sección 3 y 4 |
| b | Que se pidieran permisos al instalar/usar | ❌ ninguna pantalla pedía permisos | ✅ asistente de primer arranque + petición **en contexto** en cada función |
| c | Llamar desde MOVA, no desde el marcador del sistema | ❌ `placeCall()` caía a `ACTION_DIAL` sin permiso | ✅ `TelecomManager.placeCall()`; sin permiso se pide y se reintenta la llamada |
| d | Contactos del teléfono aparecen solos | ❌ sólo con botón "Importar" manual | ✅ sincronización automática al conceder el permiso |
| e | Enviar mensaje justo después de añadir el contacto | ❌ fallaba por falta de `SEND_SMS` | ✅ permiso en contexto + confirmación de envío real + botón *Reintentar* |
| f | Palomitas de estado y ver el estado al tocar | ❌ texto "enviado" bajo cada burbuja | ✅ palomitas + ficha de detalle por mensaje |
| g | Sección de personas que me han escrito | ❌ no existía | ✅ pestaña **"Te han escrito"** en Mensajes |
| h | El botón Inicio funciona tras añadir un contacto | ❌ `replaceWith()` destruía el grafo | ✅ `popUpTo(findStartDestination())`; Inicio y barra inferior siempre operativos |

---

## 2. Qué es real y cómo comprobarlo en el teléfono

| Función | Implementación real | Cómo comprobarlo |
|---|---|---|
| Marcador | `TelecomManager.placeCall` con `CALL_PHONE` | Marca un número: la llamada sale sin abrir el marcador del sistema |
| Historial | `CallLog.Calls` (lectura) + Room | Concede permiso: aparecen tus llamadas perdidas/entrantes/salientes reales |
| Contactos | `ContactsContract` (lectura) + Room | Concede permiso: tu agenda del teléfono se importa sola |
| SMS | `SmsManager` + `Telephony.Sms` + confirmaciones | Envía un SMS: palomita ✓ al salir y ✓✓ al entregarse |
| SMS entrantes | `SmsReceiver` + notificación + automatización | Recibe un SMS: aparece en Mensajes y notifica |
| SOS | Orquestador con SMS, ubicación, batería, llamada y registro | Mantén el botón SOS 3 s y mira el estado de cada paso |
| Ubicación | `LocationManager` del sistema | Pide posición y compártela por SMS |
| Bloqueo de números | `CallScreeningService` con rechazo real | Bloquea un número y llama desde él |
| Spam | Clasificador local + reglas + reporte del usuario | Registro de eventos de seguridad |
| PIN / biometría | PBKDF2 + Keystore + `BiometricPrompt` | Ajustes → Seguridad → bloqueo de la app |
| Automatizaciones | Disparador + condición + acción + historial | Crea una regla: "batería baja" → "enviar SMS" |
| Asistente | Comandos de texto y voz del sistema (`RecognizerIntent`) | Di/pulsa un comando y confirma la acción sensible |
| Ajustes | DataStore (se guardan de verdad) | Cambia el tema oscuro y reabre la app |

---

## 3. Fase por fase (según el plan de 9 fases del proyecto)

### FASE 1 — Fundación ✅
- Proyecto Android nativo: Kotlin 2.0.21 + Compose (BOM 2024.12.01), AGP 8.7.3, JDK 17, `minSdk 26`, `targetSdk 35`.
- 37 módulos Gradle (`app`, `core:*`, `domain:*`, `data:*`, `services:*`, `feature:*`), 175 archivos Kotlin, ~15.6k líneas.
- Design System propio (`core:designsystem`): paleta Aurora, tipografía, espaciados, componentes reutilizables.
- Navegación central con rutas tipadas (`core:navigation`).
- Room 2.6.1 con 12 entidades y DAOs; DataStore para ajustes.
- Seguridad base (`core:security`): Keystore AES-GCM, PIN, biometría, bloqueo de app, registro de eventos.
- CI (`android.yml`) y release automática (`release.yml`); README y 9 documentos en `docs/`.
- **Sin limitaciones**: es la base sobre la que se apoya todo lo demás.

### FASE 2 — Teléfono ✅ (con una limitación honesta)
- ✅ Marcador con teclado, háptica, contacto sugerido, `ACTION_CALL`/TelecomManager.
- ✅ Llamadas entrantes: receptor de `PHONE_STATE`, notificación y automatización; identificación del número.
- ✅ Llamadas salientes y perdidas: historial real del sistema, filtros, agrupar, borrar, guardar como contacto.
- ✅ Favoritos, búsqueda, números frecuentes.
- ✅ Bloqueo e identificación: `CallScreeningService` (rechaza llamadas bloqueadas/spam **si el usuario concede el rol de filtrado**).
- 🟡 **Limitación**: MOVA Phone no sustituye la pantalla de llamada del sistema (no implementa `InCallService` ni pide el rol de teléfono). El proyecto lo dice explícitamente: *"El objetivo no es construir una aplicación de teléfono tradicional"*. La llamada se inicia **desde MOVA**, y la pantalla durante la llamada es la del sistema.

### FASE 3 — SOS ✅
- Contactos de emergencia con prioridad y canales por contacto.
- Protocolo real: aviso + SMS (plantilla con marcadores `{nombre}` `{ubicacion}` `{enlace}` `{bateria}` `{hora}`), ubicación, nivel de batería, llamada y registro local.
- Cuenta regresiva con cancelación inmediata y estado individual de cada paso, incluidos los pasos que fallan.
- 🟡 **Limitación**: el envío de la ubicación en segundo plano con la pantalla apagada depende de `ACCESS_BACKGROUND_LOCATION`, permiso que Android concede sólo por separado desde Ajustes (no todos los fabricantes lo permiten).

### FASE 4 — Seguridad ✅
- PIN PBKDF2 con sal aleatoria cifrada en Keystore; nunca se guarda el PIN.
- Bloqueo de la app con tiempo de gracia y biometría (`BiometricPrompt`).
- Modo privado, contactos de confianza, números bloqueados, registro de eventos con severidad.
- 🟡 **Limitación**: la biométrica necesita hardware y huella/rostro registrados; si no existe, la app lo dice y no simula nada.

### FASE 5 — Automatizaciones ✅
- Motor real: disparador → condiciones → acciones, con historial de ejecuciones en Room.
- 12 disparadores, 7 condiciones y 11 acciones disponibles en el editor.
- Disparadores que **se lanzan de verdad** hoy: llamada entrante, SMS recibido, batería baja (aviso del sistema + revisión cada 30 min con WorkManager), hora del día, cargador conectado, Wi-Fi conectado, Bluetooth conectado, modo conducción, SOS activado y desbloqueo de la app.

🟡 **Matiz de Bluetooth**: en Android 12 o superior el sistema exige el permiso `BLUETOOTH_CONNECT` para leer qué
dispositivo se ha conectado; si el fabricante no lo concede, ese disparador concreto no recibe el evento
(el resto sigue funcionando). El aviso de batería se registra en tiempo de ejecución porque
`ACTION_BATTERY_CHANGED` es un broadcast *sticky* que Android no entrega a los receptores del manifiesto.
- 🟡 **Limitación**: `LOCATION_ENTER` / `LOCATION_EXIT` se pueden configurar y guardar, pero **no** se disparan solos: requieren geovallas del sistema (`GeofencingClient`), que exige Google Play Services, dependencia que el proyecto prohíbe. Alternativa documentada: usar la ubicación puntual y el historial.

### FASE 6 — Mensajes ✅ (rediseñada en 1.0.1)
- SMS reales con `SmsManager`, troceado de mensajes largos y **confirmación de envío y de entrega** por `PendingIntent`.
- Estados reales por mensaje: `SENDING · SENT · DELIVERED · READ · FAILED · RECEIVED`, visibles como **palomita** (reloj, ✓, ✓✓, ✓✓ marcada, ⚠).
- Al **tocar** un mensaje se abre una ficha con el estado, su explicación, la hora, el transporte usado, copiar texto, llamar, reintentar el envío y borrar.
- Pestaña **"Te han escrito"**: todas las personas que han enviado un mensaje, con último texto, fecha, total y mensajes sin leer; un toque abre la conversación y marca como leído.
- Plantillas de mensajes rápidos y de emergencia; compartir ubicación en un toque.
- 🟡 **Limitación**: la mensajería por Internet no existe todavía (por diseño: el proyecto dice *"no montar un backend enorme todavía"*). El contrato `MessageProvider` está preparado para añadirla sin tocar la interfaz.

### FASE 7 — Smart ✅ / 🟡
- ✅ Asistente con comandos de texto y **voz del sistema** (`RecognizerIntent`), con confirmación obligatoria antes de acciones sensibles (llamar, SMS, SOS).
- ✅ Modo conducción con interfaz simplificada para uso en el coche.
- 🟡 **Limitación**: no hay modelo de IA en la nube (es intérprete de comandos local). "Funciones inteligentes" como resumen de llamadas o responder con sugerencias se apoyan en reglas locales, no en un modelo remoto.
- ❌ **No implementado**: *wearables* (Wear OS) y *widgets* de pantalla de inicio. Son líneas de la visión futura del proyecto, no de las fases 1-9.

### FASE 8 — Optimización ✅
- Batería: sin servicios permanentes; `WorkManager` para lo periódico y receptores ligeros con `goAsync()`.
- Accesibilidad: etiquetas de contenido, tamaños táctiles de 48 dp, modo texto grande y alto contraste, tamaños de fuente del sistema respetados.
- Rendimiento: R8 + `shrinkResources`; el APK de release pesa **2,2 MB**.
- Manejo de errores: cada fallo se informa en la pantalla (nunca se muestra un éxito falso) y se registra en el log interno.
- Tests: 7 módulos con pruebas unitarias (números, formateos, clasificador de spam, filtros de historial, plantilla de emergencia, validador de reglas, parser de comandos y agrupación de "Te han escrito"). `lintDebug` y `testDebugUnitTest` pasan en CI.

### FASE 9 — Release ✅
- Versión 1.0.1 (`versionCode 2`), firma de release con keystore propio (algoritmo RSA 4096) subido como *secreto* de GitHub: **el keystore y sus contraseñas no están en el repositorio**.
- APK + AAB + `mapping.txt` + `checksums.txt` publicados automáticamente en la *release* de GitHub mediante Actions.
- Documentación completa en `docs/` y este informe de auditoría.

---

## 4. Lo que NO está implementado (y por qué)

| Función del documento | Estado | Motivo real / alternativa |
|---|---|---|
| Sustituir la app de teléfono (rol de marcador, pantalla de llamada propia) | ❌ | Fuera del objetivo declarado del proyecto; la llamada se inicia desde MOVA y la pantalla en curso es la del sistema |
| Grabación de llamadas | ❌ | Android 10+ bloquea a las apps de terceros grabar llamadas. La app comprueba la disponibilidad y muestra: *"Esta función no está disponible en este dispositivo o versión de Android."* (requisito 35) |
| Mensajería por Internet / chat propio | ❌ | Requiere servidor; el proyecto pide no montar un backend grande en V1. Contrato preparado |
| Geovallas (llegada/salida de un lugar) | 🟡 | Necesitan Google Play Services, prohibido por el proyecto |
| Modelo de IA generativo | ❌ | No se añaden servicios de pago ni claves de API en el APK; el asistente es local |
| Wearables (Wear OS) y widgets | ❌ | Líneas de la visión futura, no de las fases entregadas |
| Sincronización en la nube | ❌ | El proyecto es *offline first*; los datos no salen del teléfono |

---

## 5. Seguridad y privacidad (verificado)

- Sin tokens ni secretos en el repositorio: se comprobó el historial completo de Git y no aparece ningún token.
- El APK no contiene credenciales: los secretos viven en GitHub Secrets y en el entorno de compilación local.
- Datos sólo en el dispositivo (Room + DataStore); sin cuentas, sin telemetría, sin publicidad.
- Permisos mínimos y en contexto; cada permiso explica para qué se usa y la app funciona sin los opcionales.
- Cifrado con Keystore del sistema para el PIN y los datos sensibles.

---

## 6. Cómo se ha verificado esta auditoría

1. Compilación local completa: `:app:assembleDebug`, `:app:assembleRelease`, `:app:bundleRelease` → **BUILD SUCCESSFUL**.
2. Pruebas unitarias: `testDebugUnitTest` → correctas.
3. Lint de Android: `lintDebug` → correcto.
4. Comprobación de componentes declarados: receptores de SMS (entrante, enviado y entregado), servicio de filtrado de llamadas, servicio de ubicación de emergencia, receptor de arranque y receptores de estado del sistema aparecen en los manifiestos.
5. Búsqueda de patrones prohibidos en el código: sin `Toast` de relleno, sin datos falsos, sin llamadas simuladas.
6. Verificación del APK publicado en la release: SHA-256 y firma comprobados con `apksigner`.

---

*Desarrollado por Studio Lexair · © 2026*
