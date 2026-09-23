# Compatibilidad y funciones no disponibles

MOVA Phone se instala en **Android 8.0 (API 26) o superior** y está preparada para **Android 15 (API 35)**.

## Comportamiento por versión

| Función | Requisito | Comportamiento |
|---|---|---|
| Notificaciones | Android 13+ pide `POST_NOTIFICATIONS` | Se solicita; si se deniega, la emergencia se gestiona dentro de la app |
| Filtrado de llamadas | Android 7+ con rol concedido | Sin rol, el bloqueo actúa sobre el historial y los avisos, no sobre la llamada entrante |
| Ubicación en segundo plano | Android 10+ | Se pide sólo si activas el historial; si se deniega, se registra en primer plano |
| Servicio en primer plano de ubicación | Android 14+ exige tipo y justificación | Se declara `foregroundServiceType="location"` y se muestra notificación |
| Biometría | Hardware + datos registrados | Si no existe, el ajuste se desactiva y se informa (nunca se simula) |
| Grabación de llamadas | API pública desde Android 10, restringida por fabricante en Android 11+ | Si no está permitida, la app muestra "no disponible" en lugar de fingirla |
| Detección de conducción | Sin `ActivityRecognition` de Google Play Services | Se usa el modo conducción manual + comando de voz; no se inventan detecciones |

## Qué se decidió no usar y por qué

- **Google Play Services** para ubicación: evita dependencias propietarias y mantiene la app ligera y
  funcional sin servicios de Google. Se usa `LocationManager` del sistema.
- **SDK de mapas propietario**: el módulo de ubicación es agnóstico del proveedor; hoy muestra coordenadas,
  un radar propio y enlaces de mapa, y está preparado para integrar Mapas/OSM más adelante.
- **Room destructivo**: no se usa `fallbackToDestructiveMigration`; el esquema se versiona para no perder datos.
- **Inyección por reflexión (Hilt)**: se prefirió un contenedor explícito, más ligero y fácil de auditar.

## Dispositivos sin telefonía

En tablets o dispositivos sin módulo telefónico, la app funciona en modo consulta: marcador, contactos,
seguridad, automatizaciones e historial muestran un aviso claro de "llamadas no disponibles en este
dispositivo" en lugar de fallar.
