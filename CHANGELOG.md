# Changelog · MOVA Phone

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
[Versionado semántico](https://semver.org/lang/es/).

## [1.0.1] — 2026-09-23

### Corregido (auditoría del APK instalado)
- **Permisos**: ahora se piden de verdad. Asistente de permisos en el primer arranque
  (uno por uno, con su explicación) y petición **en contexto** en marcador, historial,
  contactos, mensajes, ubicación y SOS. Antes ninguna pantalla los solicitaba.
- **Llamadas desde MOVA**: `placeCall()` usa `TelecomManager` y **no** abre el marcador del
  sistema. Si falta `CALL_PHONE` se pide el permiso y la llamada se reintenta sola.
- **Contactos del teléfono**: se sincronizan automáticamente al conceder el permiso (antes
  sólo con el botón "Importar") y al reimportar se actualizan nombre, foto y favorito.
- **Enviar mensaje justo después de añadir un contacto**: el envío pide el permiso de SMS,
  confirma el envío real y ofrece *Reintentar* si el sistema lo rechaza.
- **Botón Inicio**: `replaceWith()` ya no destruye el grafo de navegación
  (`popUpTo(findStartDestination())`), así que la barra inferior sigue funcionando después
  de guardar un contacto.
- **Historial de llamadas**: pide `READ_CALL_LOG` y `READ_PHONE_STATE` en la propia pantalla
  y se sincroniza en cuanto se conceden.

### Añadido
- **Palomitas de estado** en cada burbuja del chat (enviando · enviado ✓ · entregado ✓✓ ·
  leído · fallo) en lugar del texto "enviado" bajo el mensaje.
- **Ficha de mensaje al tocar**: estado real con su explicación, hora, transporte, copiar,
  llamar, reintentar el envío y borrar.
- **Sección "Te han escrito"** en Mensajes: todas las personas que han escrito al usuario,
  con último mensaje, fecha, número de mensajes y cuántos quedan sin leer.
- **Confirmación de entrega real** de los SMS (`PendingIntent` de envío y de entrega por
  mensaje, con receptor propio) además del de envío.
- **Sección de permisos** en Ajustes y en el hub "Más", más "Acerca de" y "Créditos".
- **Disparadores de automatización** que faltaban y ya se lanzan de verdad: cargador
  conectado, Wi-Fi conectado, Bluetooth conectado, modo conducción, SOS activado y
  desbloqueo de la app.
- **Disponibilidad de grabación de llamadas** informada en Ajustes → Llamadas
  (requisito 35: si el sistema no lo permite, se dice con claridad).
- Auditoría completa del proyecto en `docs/AUDITORIA.md` (qué está implementado de verdad
  y qué no, fase por fase).
- Pruebas unitarias de la agrupación "Te han escrito".

### Cambiado
- Versión `1.0.1` (`versionCode 2`): se puede actualizar por encima de la 1.0.0 sin perder datos.

## [1.0.0] — 2026

### Añadido

- Marcador nativo con teclado, háptica configurable, búsqueda mientras se escribe y sugerencias.
- Llamadas e historial con filtros (todas, perdidas, entrantes, salientes, spam) y acciones
  de guardar, bloquear, reportar spam y eliminar.
- Contactos con favoritos, contactos privados y grupos, más importación desde la agenda del sistema.
- Contactos de emergencia con prioridad reordenable y canales por contacto.
- Protocolo **SOS** con confirmación de 3 segundos, cuenta regresiva, cancelación,
  estado por paso (SMS, ubicación, llamada, batería, registro) y notificación persistente.
- Plantilla de SMS de emergencia editable con marcadores.
- Módulo de ubicación agnóstico de proveedor (LocationManager) con historial opcional.
- Centro de seguridad: PIN con Keystore, bloqueo de la app, biometría, números bloqueados,
  contactos de confianza, modo privado y registro de eventos.
- Motor de automatizaciones disparador → condición → acción con plantillas e historial.
- Detección de spam local y filtrado de llamadas con `CallScreeningService` (rol opcional).
- Mensajes SMS reales con capa `MessageProvider` preparada para chat por Internet.
- Modo conducción con interfaz simplificada y comandos de voz.
- Asistente MOVA con interpretación local y confirmación de acciones sensibles.
- Sistema de configuración completo (15 secciones) sobre DataStore, con tema claro/oscuro.
- Modo privado, accesibilidad (texto grande, alto contraste, reducir animaciones) y avisos claros
  cuando una función no está disponible en el dispositivo.
- CI/CD: workflow de Android (debug y release separados) y workflow de release con APK, AAB,
  `mapping.txt` y `checksums.txt`.

### Seguridad

- PIN cifrado con Android Keystore, eventos de seguridad locales y sin secretos en el repositorio.
- R8 activo en release y `mapping.txt` publicado para poder interpretar fallos.
