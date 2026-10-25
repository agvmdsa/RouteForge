plugins {
    id("routeforge.domain.module")
    id("java-test-fixtures")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
