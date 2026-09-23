# Privacidad · MOVA Phone

**Principio: lo que ocurre en tu teléfono, se queda en tu teléfono.**

## Qué datos se guardan y dónde

| Dato | Dónde | Cómo borrarlo |
|---|---|---|
| Contactos propios, favoritos y grupos | Base de datos local (Room) | Ficha del contacto → eliminar; o borrar los datos de la app |
| Historial de llamadas propio | Room | Historial → eliminar, o desactivar en Ajustes → Privacidad |
| Mensajes SMS gestionados por la app | Room | Conversación → eliminar |
| Contactos de emergencia y plantillas | Room | Pantallas de emergencia y plantillas |
| Ubicaciones (si activas el historial) | Room | Ajustes → Privacidad → ubicación, o Historial → borrar |
| Eventos de seguridad | Room | Seguridad → Registro de eventos → vaciar |
| Ajustes y preferencias | DataStore | Ajustes, o borrar los datos de la app |

## Qué NO se recoge

- No hay cuentas de usuario, ni identificadores de publicidad, ni analítica de terceros.
- No se envían contactos, mensajes ni ubicaciones a servidores de Studio Lexair: **no existen**.
- No se accede a la cámara, al micrófono ni a archivos personales.

## Cuándo se usa la ubicación

- Sólo al abrir la pantalla Ubicación, al compartirla explícitamente o durante un SOS activo.
- El historial de ubicación está **desactivado por defecto**; si se activa, se guarda únicamente en local.

## Qué ocurre durante el SOS

1. Confirmación por pulsación mantenida de 3 segundos con cuenta regresiva y opción de cancelar.
2. Se registra la emergencia en local y se muestra el progreso paso a paso.
3. Si está configurado: se envía un SMS a los contactos de emergencia con la plantilla elegida
   (puede incluir nombre, coordenadas, enlace de mapas, batería, hora y precisión).
4. Si está configurado: se inicia la llamada al primer contacto de emergencia.
5. Se publica una notificación persistente con la acción **Cancelar emergencia**.
6. Todo queda en el registro local: qué se hizo, cuándo y con qué resultado.

## Permisos

Cada permiso se solicita en contexto, con su explicación, y puede denegarse sin que la app deje de
funcionar. Ver [PERMISOS.md](PERMISOS.md).

## Contacto

**Studio Lexair** · soporte@studiolexair.com
