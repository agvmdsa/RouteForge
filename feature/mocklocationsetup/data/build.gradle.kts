plugins {
    id("routeforge.android.library")
    id("routeforge.koin")
}

android {
    namespace = "com.routeforge.mocklocationsetup.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:mocklocationsetup:domain"))
}
