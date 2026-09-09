package com.routeforge.buildlogic.convention

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.intVersion(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

internal val jdkVersion = JavaVersion.VERSION_21

internal fun Project.configureKotlinJvmToolchain() {
    extensions.getByType(KotlinProjectExtension::class.java).jvmToolchain(21)
}
