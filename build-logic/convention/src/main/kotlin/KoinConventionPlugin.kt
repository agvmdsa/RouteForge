package com.routeforge.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies.add("implementation", libs.findLibrary("koin-android").get())
            dependencies.add("implementation", libs.findLibrary("koin-androidx-compose").get())
        }
    }
}
