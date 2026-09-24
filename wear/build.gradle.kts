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
        versionCode = 3
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
