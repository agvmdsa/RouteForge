package com.routeforge.buildlogic.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.findByType<LibraryExtension>()?.buildFeatures?.compose = true
            extensions.findByType<ApplicationExtension>()?.buildFeatures?.compose = true

            // Material3's newer surfaces (bottom sheets, etc.) are still marked experimental
            // even though they're the recommended, stable-in-practice API — opt in project-wide
            // instead of scattering @OptIn annotations across every screen that uses one.
            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
                }
            }

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
