package com.studiolexair.movaphone.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configuración compartida de compilación Kotlin/Android para todos los módulos.
 * Evita duplicar bloques android{} en cada módulo y garantiza coherencia.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = ProjectConfig.COMPILE_SDK

        defaultConfig {
            minSdk = ProjectConfig.MIN_SDK
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        lint {
            abortOnError = false
            checkDependencies = false
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-Xjvm-default=all", "-opt-in=kotlin.RequiresOptIn")
        }
    }
}

/** Namespace derivado de la ruta del módulo: :core:common -> com.studiolexair.movaphone.core.common */
internal fun libraryNamespace(path: String): String =
    ProjectConfig.APPLICATION_NAMESPACE + path.replace(':', '.').replace("-", "")
