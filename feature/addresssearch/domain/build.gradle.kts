plugins {
    id("routeforge.domain.module")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(project(":core:domain"))
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
