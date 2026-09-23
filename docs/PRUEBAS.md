# Pruebas

## Estrategia

| Nivel | Qué cubre | Dónde |
|---|---|---|
| Unitarias | Dominio, formateadores, parser del asistente, orquestación SOS, motor de automatizaciones, mapeos | `*/src/test/` |
| UI (Compose) | Componentes y pantallas clave: botón SOS con mantenimiento, filas de listas, estados vacíos | `*/src/androidTest/` |
| Integración | Room en memoria (DAOs), flujo de emergencia completo, automatización disparador→acción | `*/src/test/` con Robolectric/AndroidX Test |

## Ejecutar

```bash
./gradlew testDebugUnitTest               # todas las pruebas unitarias
./gradlew connectedDebugAndroidTest        # pruebas de instrumentación (dispositivo/emulador)
./gradlew lintDebug                        # análisis estático
```

## Informes

- Unitarias: `módulo/build/reports/tests/testDebugUnitTest/index.html`
- UI: `app/build/reports/androidTests/connected/`
- Lint: `módulo/build/reports/lint-results-debug.html`

## Reglas

1. Una prueba nunca depende de datos reales del dispositivo: se usan dobles para las fuentes del sistema.
2. Los casos de uso se prueban con flujos reales (no se simulan llamadas ni SMS en producción; en las
   pruebas se sustituyen por implementaciones controladas del puerto correspondiente).
3. Toda corrección de un fallo llega con su prueba.
