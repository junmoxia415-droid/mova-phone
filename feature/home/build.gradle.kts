plugins { alias(libs.plugins.mova.android.feature) }
dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":domain:calls"))
    implementation(project(":domain:emergency"))
    implementation(project(":core:database"))
    implementation(project(":core:security"))
    testImplementation(libs.truth)
}
