plugins {
    id("routeforge.android.library")
    id("routeforge.koin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.routeforge.coredata"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
}
