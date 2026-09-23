plugins {
    alias(libs.plugins.mova.android.library)
    alias(libs.plugins.ksp)
}

android {
    // Los esquemas de Room se versionan en el repositorio para poder auditar migraciones.
    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
}

dependencies {
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
