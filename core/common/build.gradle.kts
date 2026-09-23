plugins {
    alias(libs.plugins.mova.android.library)
}

dependencies {
    api(libs.core.ktx)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.process)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
