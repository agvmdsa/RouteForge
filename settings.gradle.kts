pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RouteForge"

include(":app")
include(":core:domain")
include(":core:presentation")
include(":core:design-system")
include(":core:data")
include(":feature:mocklocationsetup:domain")
include(":feature:mocklocationsetup:data")
include(":feature:mocklocationsetup:presentation")
include(":feature:routing:domain")
include(":feature:routing:data")
include(":feature:routing:presentation")
