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
include(":core:design-system")
include(":core:data")
include(":feature:addresssearch:domain")
include(":feature:addresssearch:data")
include(":feature:addresssearch:presentation")
include(":feature:mocklocationsetup:domain")
include(":feature:mocklocationsetup:data")
include(":feature:mocklocationsetup:presentation")
include(":feature:routing:domain")
include(":feature:routing:data")
include(":feature:routing:presentation")
include(":feature:simulation:domain")
include(":feature:simulation:data")
include(":feature:simulation:presentation")
