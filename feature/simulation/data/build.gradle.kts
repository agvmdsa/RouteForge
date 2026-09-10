plugins {
    id("routeforge.android.library")
    id("routeforge.koin")
}

android {
    namespace = "com.routeforge.simulation.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:simulation:domain"))

    implementation(libs.kotlinx.coroutines.core)
}
