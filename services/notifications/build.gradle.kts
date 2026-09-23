plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    api(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:logging"))
    implementation(project(":domain:automation"))
    implementation(project(":domain:emergency"))
    testImplementation(libs.junit)
}
