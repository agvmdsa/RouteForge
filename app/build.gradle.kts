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
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:design-system"))
    implementation(project(":feature:mocklocationsetup:data"))
    implementation(project(":feature:mocklocationsetup:presentation"))
    implementation(project(":feature:routing:domain"))
    implementation(project(":feature:routing:data"))
    implementation(project(":feature:routing:presentation"))
    implementation(project(":feature:simulation:data"))
    implementation(project(":feature:simulation:presentation"))
}
