plugins {
    alias(libs.plugins.mova.android.library.compose)
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.material3)
    api(libs.compose.material.icons.extended)
    implementation(project(":core:common"))
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.compose.ui.test.manifest)
}
