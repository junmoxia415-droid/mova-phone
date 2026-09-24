plugins { alias(libs.plugins.mova.android.feature) }

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(project(":services:calls"))
    testImplementation(libs.truth)
}
