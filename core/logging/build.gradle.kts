plugins { alias(libs.plugins.mova.android.library) }
dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
