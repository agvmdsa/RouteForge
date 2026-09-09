package com.routeforge.buildlogic.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.findByType<LibraryExtension>()?.buildFeatures?.compose = true
            extensions.findByType<ApplicationExtension>()?.buildFeatures?.compose = true

            val composeBom = libs.findLibrary("compose-bom").get()
            dependencies.add("implementation", dependencies.platform(composeBom))
            dependencies.add("androidTestImplementation", dependencies.platform(composeBom))

            dependencies.add("implementation", libs.findLibrary("compose-ui").get())
            dependencies.add("implementation", libs.findLibrary("compose-ui-graphics").get())
            dependencies.add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
            dependencies.add("implementation", libs.findLibrary("compose-material3").get())
            dependencies.add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
            dependencies.add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())

            dependencies.add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
            dependencies.add("androidTestImplementation", libs.findLibrary("junit4").get())
            dependencies.add("androidTestImplementation", libs.findLibrary("androidx-test-ext-junit").get())
            dependencies.add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
        }
    }
}
