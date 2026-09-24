plugins { alias(libs.plugins.mova.android.feature) }

dependencies {
    implementation(project(":domain:contacts"))
    implementation(project(":domain:calls"))
    implementation(project(":domain:emergency"))
    implementation(project(":data:messages"))
    implementation(project(":data:location"))
    implementation(project(":core:database"))
    implementation(project(":core:logging"))
    implementation(project(":core:security"))
    implementation(project(":domain:automation"))
    // Modelo de lenguaje en el dispositivo (MediaPipe LLM Inference)
    implementation(libs.mediapipe.genai)
    testImplementation(libs.truth)
}
