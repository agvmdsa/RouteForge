plugins {
    id("routeforge.android.library")
    id("routeforge.compose")
}

android {
    namespace = "com.routeforge.designsystem"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.osmdroid.android)
    implementation(libs.compose.material.icons.extended)
}
