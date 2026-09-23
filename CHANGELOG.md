# Changelog · MOVA Phone

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
[Versionado semántico](https://semver.org/lang/es/).

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
