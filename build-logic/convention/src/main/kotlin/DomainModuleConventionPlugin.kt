package com.routeforge.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

class DomainModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("routeforge.ktlint")

            configureKotlinJvmToolchain()

            dependencies.add("testImplementation", libs.findLibrary("junit-jupiter").get())
            dependencies.add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
