plugins {
    id("routeforge.android.library")
}

android {
    namespace = "com.routeforge.coredata"
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
}
