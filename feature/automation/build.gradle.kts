plugins { alias(libs.plugins.mova.android.feature) }

dependencies {
    implementation(project(":domain:automation"))
    implementation(project(":domain:contacts"))
    implementation(project(":core:database"))
    implementation(project(":data:automation"))
    testImplementation(libs.truth)
}
