plugins { alias(libs.plugins.mova.android.feature) }

dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":core:database"))
    implementation(project(":core:permissions"))
    implementation(project(":data:location"))
    testImplementation(libs.truth)
}
