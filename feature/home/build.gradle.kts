plugins { alias(libs.plugins.mova.android.feature) }
dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":domain:calls"))
    implementation(project(":domain:emergency"))
    implementation(project(":core:database"))
    testImplementation(libs.truth)
}
