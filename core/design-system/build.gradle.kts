plugins {
    id("routeforge.android.library")
    id("routeforge.compose")
}

android {
    namespace = "com.routeforge.designsystem"
}

dependencies {
    implementation(libs.osmdroid.android)
    implementation(libs.compose.material.icons.extended)
}
