plugins { alias(libs.plugins.mova.android.feature) }
dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":domain:calls"))
    testImplementation(libs.truth)
}
