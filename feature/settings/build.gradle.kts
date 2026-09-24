plugins { alias(libs.plugins.mova.android.feature) }

dependencies {
    implementation(project(":core:security"))
    implementation(project(":services:wear"))
    implementation(project(":domain:automation"))
    implementation(project(":core:database"))
    testImplementation(libs.truth)
}
