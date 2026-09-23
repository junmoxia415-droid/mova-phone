plugins { alias(libs.plugins.mova.android.feature) }
dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":domain:calls"))
    implementation(project(":core:security"))
    testImplementation(libs.truth)
}
