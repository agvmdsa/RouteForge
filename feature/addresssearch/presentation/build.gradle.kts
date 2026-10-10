plugins {
    id("routeforge.android.feature")
}

android {
    namespace = "com.routeforge.addresssearch.presentation"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":feature:addresssearch:domain"))
    implementation(project(":core:design-system"))
    implementation(libs.compose.material.icons.extended)
}
