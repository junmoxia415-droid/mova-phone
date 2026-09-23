plugins { alias(libs.plugins.mova.android.feature) }

dependencies {
    implementation(project(":domain:emergency"))
    implementation(project(":domain:contacts"))
    implementation(project(":data:messages"))
    implementation(project(":data:location"))
    implementation(project(":core:database"))
    testImplementation(libs.truth)
}
