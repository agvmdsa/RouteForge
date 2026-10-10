plugins {
    id("routeforge.android.library")
    id("routeforge.koin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.routeforge.addresssearch.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":feature:addresssearch:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
