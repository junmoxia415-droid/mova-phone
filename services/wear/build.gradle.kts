plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(libs.core.ktx)
}
