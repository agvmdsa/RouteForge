plugins {
    id("routeforge.android.library")
    id("routeforge.koin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.routeforge.routing.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":feature:routing:domain"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)

    implementation(files("libs/brouter-1.7.10-ro.jar"))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
