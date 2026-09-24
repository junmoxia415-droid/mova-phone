import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Aplicación para el reloj (Wear OS) de MOVA Phone.
 *
 * Se conecta al teléfono por Bluetooth de bajo consumo con el protocolo propio de MOVA:
 * sin Google Play Services, sin cuentas y sin nube. Muestra llamadas, mensajes y batería
 * del teléfono, y permite contestar, colgar, responder con un mensaje corto y lanzar el SOS
 * (siempre con confirmación en la muñeca).
 */
android {
    namespace = "com.studiolexair.movaphone.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.studiolexair.movaphone.wear"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2"
    }

    // El reloj también se firma: en el 1.1 el APK salía sin firma y no se podía instalar.
    // Se usa la misma clave que el teléfono (llega por variables de entorno; si no está,
    // se firma con la clave de depuración para que el APK siempre sea instalable).
    signingConfigs {
        create("releaseFromEnv") {
            val keystoreFile = rootProject.file(System.getenv("MOVA_KEYSTORE_PATH") ?: "mova-release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("MOVA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MOVA_KEY_ALIAS")
                keyPassword = System.getenv("MOVA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val keystoreFile = rootProject.file(System.getenv("MOVA_KEYSTORE_PATH") ?: "mova-release.jks")
            signingConfig = if (keystoreFile.exists()) {
                signingConfigs.getByName("releaseFromEnv")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)
}
