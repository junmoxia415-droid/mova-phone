# Seguridad · MOVA Phone

## Modelo de seguridad

| Área | Medida |
|---|---|
| PIN de la app | Derivación PBKDF2 (120 000 iteraciones, sal aleatoria) y almacenamiento cifrado con Android Keystore (AES/GCM, clave `mova_phone_master_key`) |
| Bloqueo de la app | Activación por el usuario, bloqueo automático tras el tiempo configurado (30 s por defecto) |
| Biometría | `BiometricPrompt` con autenticadores fuertes; si el dispositivo no la admite se informa y se usa el PIN |
| Números bloqueados | Rechazo oficial con `CallScreeningService` (requiere rol de filtrado) |
| Spam | Clasificación local: números bloqueados, reglas propias y reportes del usuario |
| Modo privado | Contactos, conversaciones y registros marcados como privados se ocultan |
| Auditoría | Registro local de eventos: bloqueos, spam, intentos de PIN, desbloqueos, SOS, automatizaciones |
| Logs | `MovaLog` centralizado y silenciado en release (R8 elimina `Log.d/i/v`) |

## Datos

- Todo se guarda en el dispositivo: Room (`mova_phone.db`) y DataStore (`mova_settings`).
- Sin cuentas, sin sincronización, sin servidores propios y sin SDK de analítica o publicidad.
- El usuario puede vaciar historiales, registros de eventos y ubicaciones desde los ajustes.

## Secretos y firmas

- Ninguna clave privada, token o contraseña vive en el repositorio.
- La firma de release se inyecta en CI desde GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, etc.).
- Si no hay secretos configurados, el workflow produce un APK de release **firmado con la clave de
  desarrollo** para poder instalarlo en pruebas; está documentado en `docs/FIRMA.md` y no debe publicarse
  en tiendas.

## Acciones sensibles con confirmación

Llamar, enviar SMS, compartir ubicación y activar el SOS requieren una acción explícita del usuario:
pulsación mantenida de 3 segundos para el SOS, y confirmación en el asistente antes de ejecutar.

## Respuesta ante incidentes

Consulta `SECURITY.md` para el canal de reporte. Los fallos de seguridad tienen prioridad sobre
cualquier otra tarea del proyecto.
