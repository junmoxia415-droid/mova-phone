plugins { alias(libs.plugins.mova.android.library) }
dependencies {
    api(project(":domain:emergency"))
    implementation(project(":domain:calls"))
    implementation(project(":core:database"))
    implementation(project(":core:logging"))
    implementation(project(":core:permissions"))
    implementation(project(":data:location"))
    implementation(project(":data:messages"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
