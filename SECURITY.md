# Política de seguridad · MOVA Phone

## Versiones soportadas

| Versión | Soporte |
|---|---|
| 1.0.x | ✅ |

## Cómo reportar una vulnerabilidad

Escribe a **soporte@studiolexair.com** con el asunto `[SEGURIDAD] MOVA Phone` e incluye:

- descripción del problema y su impacto,
- pasos para reproducirlo,
- versión de la app, versión de Android y dispositivo,
- pruebas de concepto si las tienes.

Responderemos en un máximo de 72 horas. No publiques la vulnerabilidad antes de que exista una corrección.

## Compromisos de diseño

- **Sin secretos en el repositorio ni en el APK.** La firma de release se inyecta desde GitHub Secrets.
- **Sin cuentas ni servidores propios.** No se transmiten datos personales a Internet.
- **PIN cifrado** con Android Keystore (AES/GCM, PBKDF2 con 120 000 iteraciones y sal aleatoria).
- **Permisos mínimos** solicitados en contexto y con explicación.
- **Datos locales** protegidos por el aislamiento de la app; el usuario puede borrarlos en cualquier momento.
- **Registro de eventos** para auditar bloqueos, accesos y emergencias.
- **R8/ProGuard** activo en release: el símbolo `mapping.txt` se publica junto a la release para poder
  leer trazas de fallos sin exponer el código fuente.
- **Sin logs en release**: se eliminan las llamadas de depuración en la compilación de release.

## Alcance

Quedan fuera del alcance las vulnerabilidades del sistema operativo, de la app de telefonía del
fabricante y las limitaciones documentadas en `docs/COMPATIBILIDAD.md`.
