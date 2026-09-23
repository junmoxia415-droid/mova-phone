plugins { alias(libs.plugins.mova.android.library) }
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:logging"))
    implementation(project(":core:permissions"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
