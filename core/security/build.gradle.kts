plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    api(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:logging"))
    implementation(libs.biometric.lib)
    implementation(libs.datastore.preferences)
    implementation(libs.appcompat)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
