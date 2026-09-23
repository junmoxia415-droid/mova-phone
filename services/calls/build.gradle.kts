plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(project(":core:database"))
    implementation(project(":domain:calls"))
    implementation(project(":domain:automation"))
    implementation(project(":domain:contacts"))
    implementation(project(":services:notifications"))
    testImplementation(libs.junit)
}
