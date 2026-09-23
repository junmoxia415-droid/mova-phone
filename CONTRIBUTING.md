# Contribuir a MOVA Phone

Gracias por querer aportar a MOVA Phone (Studio Lexair).

## Flujo de trabajo

1. Crea una rama corta y descriptiva: `feat/sos-countdown`, `fix/dialer-haptics`, `docs/permisos`.
2. Commits en imperativo y en español o inglés, describiendo el *qué* y el *por qué*:
   `feat(sos): cancelar la emergencia desde la notificación`.
3. Antes de abrir la PR ejecuta:

```bash
./gradlew testDebugUnitTest lintDebug :app:assembleDebug
```

4. Abre la PR explicando el cambio, cómo probarlo y qué pantallas del mockup afecta.

## Reglas del proyecto

- **Nativo siempre**: nada de WebView, HTML ni simulaciones. Si una API de Android no lo permite,
  se documenta y se ofrece la mejor alternativa real.
- **Sin mocks en producción**: los datos y las acciones vienen del dispositivo o fallan con un
  mensaje claro para el usuario.
- **Arquitectura**: respeta las capas (`core`, `domain`, `data`, `services`, `feature`). El dominio no
  importa Android; las features no tocan APIs del sistema directamente.
- **Archivos pequeños**: ~200 líneas como referencia; si crece, divide.
- **Errores**: mensajes amables para el usuario, sin stack traces; los detalles técnicos van al log.
- **Sin secretos**: nunca subas tokens, keystores, `.env` ni datos personales.
- **Permisos**: no añadas permisos sin justificarlos en `docs/PERMISOS.md`.
- **Idioma de la interfaz**: español, tono claro y directo.

## Pruebas

- Unitarias: lógica de dominio, formateadores, parser del asistente, mapeos.
- UI: componentes y pantallas con Compose Test.
- Integración: Room en memoria, orquestador SOS, motor de automatizaciones.

## Código de conducta

Respeto y colaboración. No se aceptan contribuciones que comprometan la privacidad del usuario,
añadan rastreadores o dependan de servicios que recojan datos sin consentimiento explícito.
