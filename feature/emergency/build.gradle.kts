plugins { alias(libs.plugins.mova.android.feature) }
dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":domain:emergency"))
    implementation(project(":domain:calls"))
    implementation(project(":core:database"))
    implementation(project(":domain:automation"))
    implementation(project(":data:automation"))
    testImplementation(libs.truth)
}
