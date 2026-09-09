plugins {
    id("routeforge.android.application")
    id("routeforge.compose")
    id("routeforge.koin")
}

android {
    namespace = "com.routeforge.app"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(project(":core:design-system"))
    implementation(project(":feature:mocklocationsetup:data"))
    implementation(project(":feature:mocklocationsetup:presentation"))
}
