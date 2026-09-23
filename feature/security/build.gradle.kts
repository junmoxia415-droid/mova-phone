plugins { alias(libs.plugins.mova.android.feature) }

dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":core:security"))
    implementation(project(":core:database"))
    testImplementation(libs.truth)
}
