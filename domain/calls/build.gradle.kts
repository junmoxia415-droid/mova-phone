plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    api(project(":core:common"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
