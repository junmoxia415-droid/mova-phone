plugins { alias(libs.plugins.mova.android.library.compose) }
dependencies {
    api(project(":core:common"))
    api(libs.navigation.compose)
    implementation(project(":core:designsystem"))
}
