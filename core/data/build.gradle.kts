plugins {
    id("routeforge.android.library")
    id("routeforge.koin")
}

android {
    namespace = "com.routeforge.coredata"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
}
