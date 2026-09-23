plugins {
    alias(libs.plugins.mova.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    defaultConfig {
        // Permite que "MOVA Phone" administre llamadas/SMS sólo en la versión debug si es necesario.
        manifestPlaceholders["movaRoleDebug"] = "false"
    }
    buildTypes {
        getByName("release") {
            // Firma: usa el keystore de release si existe (CI por secrets).
            // Si no existe, cae al keystore de debug para producir un APK instalable
            // (ver docs/FIRMA.md). Nunca hay secretos en el repositorio.
            val keystorePath = System.getenv("MOVA_KEYSTORE_PATH") ?: "mova-release.jks"
            val keystoreFile = rootProject.file(keystorePath)
            signingConfig = if (keystoreFile.exists()) {
                signingConfigs.create("releaseFromEnv").apply {
                    storeFile = keystoreFile
                    storePassword = System.getenv("MOVA_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("MOVA_KEY_ALIAS")
                    keyPassword = System.getenv("MOVA_KEY_PASSWORD")
                }
            } else {
                // Fallback documentado: APK instalable firmado con la clave de desarrollo.
                // El keystore real llega por GitHub Secrets en CI (ver docs/FIRMA.md).
                signingConfigs.getByName("debug")
            }
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:database"))
    implementation(project(":core:security"))
    implementation(project(":core:permissions"))
    implementation(project(":core:logging"))

    implementation(project(":domain:contacts"))
    implementation(project(":domain:calls"))
    implementation(project(":domain:emergency"))
    implementation(project(":domain:automation"))

    implementation(project(":data:contacts"))
    implementation(project(":data:calls"))
    implementation(project(":data:messages"))
    implementation(project(":data:emergency"))
    implementation(project(":data:location"))
    implementation(project(":data:automation"))

    implementation(project(":services:calls"))
    implementation(project(":services:sms"))
    implementation(project(":services:location"))
    implementation(project(":services:notifications"))

    implementation(project(":feature:home"))
    implementation(project(":feature:dialer"))
    implementation(project(":feature:calls"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:favorites"))
    implementation(project(":feature:emergency"))
    implementation(project(":feature:messages"))
    implementation(project(":feature:location"))
    implementation(project(":feature:automation"))
    implementation(project(":feature:security"))
    implementation(project(":feature:driving"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:about"))
    implementation(project(":feature:smart-assistant"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.work.runtime.ktx)
    implementation(libs.biometric.lib)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
