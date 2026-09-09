package com.routeforge.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("routeforge.android.library")
            pluginManager.apply("routeforge.compose")
            pluginManager.apply("routeforge.koin")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies.add("implementation", libs.findLibrary("androidx-core-ktx").get())
            dependencies.add("implementation", libs.findLibrary("androidx-activity-compose").get())
            dependencies.add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            dependencies.add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            dependencies.add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            dependencies.add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
            dependencies.add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            dependencies.add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
