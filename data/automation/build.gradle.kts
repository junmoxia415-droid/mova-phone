plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    api(project(":domain:automation"))
    implementation(project(":core:database"))
    implementation(project(":core:logging"))
    implementation(project(":core:permissions"))
    implementation(project(":core:security"))
    implementation(project(":domain:contacts"))
    implementation(project(":domain:emergency"))
    implementation(project(":data:messages"))
    implementation(project(":data:calls"))
    implementation(project(":data:emergency"))
    implementation(project(":data:location"))
    implementation(libs.work.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
