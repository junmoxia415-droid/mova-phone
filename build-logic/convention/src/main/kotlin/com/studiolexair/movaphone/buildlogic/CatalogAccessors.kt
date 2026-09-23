package com.studiolexair.movaphone.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Acceso al catálogo de versiones (gradle/libs.versions.toml) desde los plugins de convención.
 * Mantiene una única fuente de verdad de dependencias para los 35 módulos.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
