# Diseño · del mockup a la aplicación

El mockup entregado (16 paneles) define la identidad visual y el flujo. Esto es lo que se implementó:

| Panel del mockup | Pantalla en MOVA Phone | Módulo |
|---|---|---|
| Splash con logotipo | `SplashScreen` | `app` |
| Inicio (hub con accesos grandes) | `HomeRoute` | `feature:home` |
| Marcador con teclado | `DialerRoute` | `feature:dialer` |
| Historial de llamadas con filtros | `CallsRoute` | `feature:calls` |
| Contactos con búsqueda | `ContactsRoute` | `feature:contacts` |
| Detalle de contacto | `ContactDetailRoute` | `feature:contacts` |
| Favoritos | `FavoritesRoute` | `feature:favorites` |
| SOS con pulsación mantenida | `SosRoute` | `feature:emergency` |
| Emergencia activa (progreso por pasos) | `EmergencyActiveScreen` | `feature:emergency` |
| Contactos de emergencia con prioridad | `EmergencyContactsRoute` | `feature:emergency` |
| Mensajes y conversación | `MessagesRoute`, `ConversationRoute`, `TemplatesRoute` | `feature:messages` |
| Ubicación (radar propio) | `LocationRoute` | `feature:location` |
| Automatizaciones | `AutomationRoute`, `AutomationEditorRoute`, `AutomationHistoryRoute` | `feature:automation` |
| Centro de seguridad y bloqueos | `SecurityRoute`, `BlockedNumbersRoute`, `TrustedContactsRoute`, `SecurityEventsRoute` | `feature:security` |
| Modo conducción | `DrivingRoute` | `feature:driving` |
| Ajustes y "Acerca de" | `SettingsRoute`, `SettingsSectionRoute`, `AboutRoute`, `CreditsRoute`, `PrivacyRoute` | `feature:settings`, `feature:about` |
| Asistente (voz) | `SmartAssistantRoute` | `feature:smart-assistant` |

## Identidad visual

- **Fondo**: degradado azul noche profundo con halos cian/azul (`AuroraBackground`).
- **Marca**: logotipo "M" vectorial con brillo cian-azul + nombre MOVA y sufijo "Phone".
- **Códigos de color**: verde = llamadas, rojo = emergencias, violeta = historial y automatizaciones,
  ámbar = favoritos.
- **Formas**: tarjetas redondeadas, botones de acción rápida grandes, tipografía editorial con títulos
  de sección en mayúsculas y etiquetas en tonos apagados.
- **Accesibilidad**: contraste alto disponible en ajustes, texto grande, reducir animaciones y
  objetivos táctiles de al menos 48 dp.

Todo el sistema visual vive en `core:designsystem` (`MovaPalette`, `MovaDimens`, `MovaTheme`,
`BrandConfig`), de modo que Studio Lexair puede reajustar la marca sin tocar las pantallas.

## Pie de marca

Todas las pantallas de información muestran **"Desarrollado por Studio Lexair"**, la versión, el número de
compilación, el año y las tecnologías y licencias utilizadas (Ajustes → Acerca de → Créditos).
