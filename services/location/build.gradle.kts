plugins { alias(libs.plugins.mova.android.library) }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:logging"))
    implementation(project(":core:database"))
    implementation(project(":data:location"))
    implementation(project(":data:automation"))
    implementation(project(":services:notifications"))
    implementation(libs.work.runtime.ktx)
    testImplementation(libs.junit)
}
