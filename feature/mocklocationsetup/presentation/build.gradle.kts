plugins {
    id("routeforge.android.feature")
}

android {
    namespace = "com.routeforge.mocklocationsetup.presentation"
}

dependencies {
    implementation(project(":feature:mocklocationsetup:domain"))
    implementation(project(":core:design-system"))
}
