plugins {
    id("routeforge.android.feature")
}

android {
    namespace = "com.routeforge.simulation.presentation"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:simulation:domain"))
    implementation(project(":core:design-system"))
    implementation(libs.compose.material.icons.extended)
}
