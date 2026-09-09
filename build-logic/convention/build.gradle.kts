plugins {
    `kotlin-dsl`
}

group = "com.routeforge.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "routeforge.android.application"
            implementationClass = "com.routeforge.buildlogic.convention.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "routeforge.android.library"
            implementationClass = "com.routeforge.buildlogic.convention.AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "routeforge.android.feature"
            implementationClass = "com.routeforge.buildlogic.convention.AndroidFeatureConventionPlugin"
        }
        register("domainModule") {
            id = "routeforge.domain.module"
            implementationClass = "com.routeforge.buildlogic.convention.DomainModuleConventionPlugin"
        }
        register("compose") {
            id = "routeforge.compose"
            implementationClass = "com.routeforge.buildlogic.convention.ComposeConventionPlugin"
        }
        register("koin") {
            id = "routeforge.koin"
            implementationClass = "com.routeforge.buildlogic.convention.KoinConventionPlugin"
        }
        register("ktlint") {
            id = "routeforge.ktlint"
            implementationClass = "com.routeforge.buildlogic.convention.KtlintConventionPlugin"
        }
    }
}
