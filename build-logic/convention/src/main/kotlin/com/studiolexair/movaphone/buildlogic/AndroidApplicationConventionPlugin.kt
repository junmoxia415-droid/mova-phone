package com.studiolexair.movaphone.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                namespace = ProjectConfig.APPLICATION_NAMESPACE

                defaultConfig {
                    applicationId = ProjectConfig.APPLICATION_ID
                    targetSdk = ProjectConfig.TARGET_SDK
                    versionCode = ProjectConfig.VERSION_CODE
                    versionName = ProjectConfig.VERSION_NAME
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables { useSupportLibrary = true }
                }

                buildFeatures {
                    buildConfig = true
                }

                buildTypes {
                    getByName("debug") {
                        applicationIdSuffix = ".debug"
                        isMinifyEnabled = false
                        isShrinkResources = false
                    }
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                packaging {
                    resources {
                        excludes += setOf(
                            "/META-INF/{AL2.0,LGPL2.1}",
                            "/META-INF/DEPENDENCIES",
                            "META-INF/LICENSE*",
                            "META-INF/*.kotlin_module"
                        )
                    }
                }
            }
        }
    }
}
