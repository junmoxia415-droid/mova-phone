# Firma de release

## Nunca en el repositorio

MOVA Phone **no** guarda ninguna clave privada en Git. `.gitignore` bloquea `*.jks`, `*.keystore`,
`keystore.properties` y `.env`.

## 1. Crear el keystore (una sola vez, en local)

```bash
keytool -genkeypair -v \
  -keystore mova-release.jks \
  -alias mova \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Studio Lexair, OU=MOVA Phone, O=Studio Lexair, L=Madrid, C=ES"
```

Guarda el archivo y las contraseñas en un gestor de secretos.

## 2. Configurar GitHub Secrets

| Secreto | Contenido |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Keystore codificado: `base64 -w0 mova-release.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `RELEASE_KEY_ALIAS` | Alias (`mova`) |
| `RELEASE_KEY_PASSWORD` | Contraseña de la clave |

El workflow de release los descifra en un archivo temporal, firma la compilación y **borra el archivo**
al terminar. Nunca se imprimen en los logs.

## 3. Compilar en local con firma real

```bash
export MOVA_KEYSTORE_PATH=/ruta/segura/mova-release.jks
export MOVA_KEYSTORE_PASSWORD=...
export MOVA_KEY_ALIAS=mova
export MOVA_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

## 4. Sin secretos configurados

Si no existen los secretos, `app/build.gradle.kts` usa la **clave de depuración** para que la
compilación de release produzca un APK instalable en pruebas. Ese APK:

- sirve para revisar R8, el tamaño y el funcionamiento general,
- **no** debe publicarse en Google Play ni distribuirse como versión final.

Cuando configures los secretos, el mismo comando produce el APK firmado con tu clave y la release de
GitHub quedará lista para distribuir.

## 5. Verificación de la firma

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```
