# Estado de las 9 fases

| Fase | Contenido | Estado |
|---|---|---|
| 1 · Fundación | Proyecto Android, Kotlin, Compose, arquitectura modular, navegación, design system, Room, seguridad, CI, README | ✅ |
| 2 · Teléfono | Marcador, llamadas, historial con filtros, contactos, favoritos, grupos, detección de spam | ✅ |
| 3 · SOS | SOS con 3 s, contactos de emergencia con prioridad, plantilla de SMS editable, registro y estado por paso | ✅ |
| 4 · Seguridad | PIN con Keystore, biometría, bloqueo de app, números bloqueados, contactos de confianza, modo privado, eventos | ✅ |
| 5 · Automatizaciones | Motor disparador → condición → acción, plantillas, historial, worker periódico | ✅ |
| 6 · Mensajes | SMS reales, conversaciones, plantillas rápidas, abstracción `MessageProvider` | ✅ |
| 7 · Asistente | Interpretación local de órdenes, voz del sistema, confirmación de acciones sensibles, modo conducción | ✅ |
| 8 · Optimización | R8/ProGuard, batería, permisos mínimos, mensajes amables, accesibilidad, rendimiento | ✅ |
| 9 · Release | Firma por secretos, APK/AAB, `mapping.txt`, checksums y GitHub Release automática | ✅ |

## Verificación

- `./gradlew :app:assembleDebug` → APK de depuración.
- `./gradlew testDebugUnitTest` → pruebas unitarias.
- `./gradlew :app:assembleRelease :app:bundleRelease` → artefactos de release.
- Workflow `release.yml` → publica APK, AAB, `mapping.txt` y `checksums.txt` en la release de GitHub.
