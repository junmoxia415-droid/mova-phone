plugins { alias(libs.plugins.mova.android.library) }
dependencies {
    api(project(":domain:calls"))
    implementation(project(":core:database"))
    implementation(project(":core:logging"))
    implementation(project(":core:permissions"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
