# Permisos · MOVA Phone

MOVA Phone pide lo mínimo, en el momento en que se necesita y explicando para qué.
Si el usuario deniega, la app **sigue funcionando** con la función afectada marcada como no disponible.

## Cómo se piden (flujo real desde la 1.0.1)

1. **Primer arranque**: tras la pantalla de marca, MOVA Phone muestra el asistente de permisos
   (`ui/PermissionsScreen.kt`) con los permisos **uno por uno**, para qué sirve cada uno y el
   estado actual. Hay un botón para concederlos todos y otro para volver a pedir los que falten.
2. **En contexto**: cada función pide su permiso cuando el usuario la usa y, al concederlo,
   **retoma la acción** que estaba haciendo:
   - Marcador → `CALL_PHONE`; la llamada se reintenta automáticamente.
   - Historial → `READ_CALL_LOG` + `READ_PHONE_STATE`; el historial se sincroniza al instante.
   - Contactos → `READ_CONTACTS`; la agenda del teléfono se importa sola.
   - Mensajes → `READ_SMS` + `RECEIVE_SMS` + `SEND_SMS`; el mensaje escrito se envía al concederlo.
   - Ubicación → `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`.
   - SOS → `SEND_SMS` + ubicación + `CALL_PHONE`.
3. **Revisión posterior**: Más → **Permisos**, o Ajustes → *Permisos de la aplicación*.
4. Si el usuario marcó "no volver a preguntar", la pantalla ofrece **Abrir ajustes del sistema**.

| Permiso | Cuándo se pide | Para qué se usa | Si se deniega |
|---|---|---|---|
| `CALL_PHONE` | Al pulsar llamar | Marcar desde marcador, contactos, historial, conducción y SOS | Se ofrece abrir el marcador del sistema |
| `READ_PHONE_STATE` | Al activar el filtrado o el registro de llamadas | Detectar llamada entrante/estado real | No hay detección de estado |
| `READ_CALL_LOG` | Al abrir historial por primera vez | Mostrar historial del dispositivo y perdidas reales | Se muestra sólo el registro propio de MOVA |
| `READ_CONTACTS` | Al importar la agenda | Traer contactos del sistema (sólo lectura) | Se usan sólo los contactos propios |
| `SEND_SMS` | Al enviar el primer SMS o al activar SMS de SOS | Mensajes rápidos y aviso de emergencia | El SOS continúa con llamada y ubicación |
| `RECEIVE_SMS`, `READ_SMS` | Al abrir Mensajes | Recibir y mostrar conversaciones | Mensajes en modo lectura limitada |
| `ACCESS_FINE_LOCATION` | Al usar SOS o Ubicación | Posición precisa para la alerta | Se intenta posición aproximada o se informa |
| `ACCESS_COARSE_LOCATION` | Igual que la anterior | Posición estimada suficiente | Sólo última ubicación conocida |
| `ACCESS_BACKGROUND_LOCATION` | Sólo si activas historial en segundo plano | Registrar ubicación durante una emergencia con pantalla apagada | Historial sólo en primer plano |
| `POST_NOTIFICATIONS` | Tras el primer arranque | Avisos de emergencia, mensajes y automatizaciones | La emergencia se ve dentro de la app |
| `VIBRATE` | Uso interno | Avisar con vibración durante una emergencia | Sólo aviso visual y sonoro |
| `WAKE_LOCK` | Uso interno durante el SOS | Mantener el proceso vivo mientras dura la emergencia | El SOS puede interrumpirse en pantalla apagada |
| `USE_BIOMETRIC` | Al activar biometría en Seguridad | Desbloquear la app con huella o rostro | Desbloqueo con PIN |

## Roles del sistema

- **Filtrado de llamadas** (`CallScreeningService`): opcional. Se solicita desde el centro de seguridad;
  sólo con el rol concedido MOVA Phone puede rechazar llamadas de números bloqueados o spam.
- **App de teléfono**: no se solicita. MOVA Phone actúa como marcador mediante intents `tel:`.

## Lo que MOVA Phone NO hace

- No pide `MANAGE_EXTERNAL_STORAGE`, `QUERY_ALL_PACKAGES`, acceso a la lista completa de apps ni permisos
  de accesibilidad.
- No accede a la cámara, al micrófono ni al almacenamiento: la voz la transcribe el sistema mediante
  `RecognizerIntent` y el resultado se interpreta en el dispositivo.
- No graba llamadas salvo que la API pública lo permita; en ese caso lo indica claramente.
