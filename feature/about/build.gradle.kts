plugins { alias(libs.plugins.mova.android.feature) }
dependencies {
    implementation(project(":core:security"))
    testImplementation(libs.truth)
}
