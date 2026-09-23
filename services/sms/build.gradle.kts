plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(project(":core:database"))
    implementation(project(":data:messages"))
    implementation(project(":services:notifications"))
    implementation(project(":domain:automation"))
    implementation(project(":domain:contacts"))
    testImplementation(libs.junit)
}
