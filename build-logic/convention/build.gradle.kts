plugins {
    `kotlin-dsl`
}

group = "com.studiolexair.movaphone.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mova.android.application"
            implementationClass = "com.studiolexair.movaphone.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "mova.android.library"
            implementationClass = "com.studiolexair.movaphone.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "mova.android.library.compose"
            implementationClass = "com.studiolexair.movaphone.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "mova.android.feature"
            implementationClass = "com.studiolexair.movaphone.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
