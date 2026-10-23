package com.routeforge.buildlogic.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

private const val DEFAULT_VERSION_NAME = "1.0"
private const val DEFAULT_VERSION_CODE = 1

private const val KEYSTORE_FILE = "ROUTEFORGE_KEYSTORE_FILE"
private const val KEYSTORE_PASSWORD = "ROUTEFORGE_KEYSTORE_PASSWORD"
private const val KEY_ALIAS = "ROUTEFORGE_KEY_ALIAS"
private const val KEY_PASSWORD = "ROUTEFORGE_KEY_PASSWORD"
private val SIGNING_VARIABLES = listOf(KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)

private class ReleaseSigning(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("routeforge.ktlint")

            // Without the properties the app keeps its development version, so a local build is the same as before.
            val releaseVersionName =
                providers
                    .gradleProperty("routeforge.versionName")
                    .orNull
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_VERSION_NAME
            val releaseVersionCode = versionCodeFrom(providers.gradleProperty("routeforge.versionCode").orNull)
            val releaseSigning = releaseSigningFromEnvironment()

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.intVersion("androidCompileSdk")

                defaultConfig {
                    applicationId = "com.routeforge.app"
                    minSdk = libs.intVersion("androidMinSdk")
                    targetSdk = libs.intVersion("androidTargetSdk")
                    versionCode = releaseVersionCode
                    versionName = releaseVersionName
                }

                // Only the release build is signed with the release key, and only when all four values are present;
                // otherwise it stays unsigned, as it always was. Minification is not changed here.
                if (releaseSigning != null) {
                    // The Action overloads of the container are unusable with the DSL's projected types, so configure the
                    // object that create() returns.
                    val releaseConfig = signingConfigs.create("release")
                    releaseConfig.storeFile = file(releaseSigning.storeFile)
                    releaseConfig.storePassword = releaseSigning.storePassword
                    releaseConfig.keyAlias = releaseSigning.keyAlias
                    releaseConfig.keyPassword = releaseSigning.keyPassword
                    buildTypes.getByName("release").signingConfig = releaseConfig
                }

                compileOptions {
                    sourceCompatibility = jdkVersion
                    targetCompatibility = jdkVersion
                }

                testOptions {
                    unitTests.isReturnDefaultValues = true
                }
            }

            dependencies.add("testImplementation", libs.findLibrary("junit-jupiter").get())
            dependencies.add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }

    private fun versionCodeFrom(value: String?): Int {
        if (value == null) return DEFAULT_VERSION_CODE
        return value.trim().toIntOrNull()?.takeIf { it >= 1 }
            ?: throw GradleException("The Gradle property routeforge.versionCode must be a positive whole number, but it is '$value'")
    }

    /**
     * The release key from the environment: nothing when none of the four variables is set, the key when all four are, and a
     * failed build, naming the missing ones, when only some are (a half-configured release must never become an unsigned one).
     */
    private fun Project.releaseSigningFromEnvironment(): ReleaseSigning? {
        val values = SIGNING_VARIABLES.associateWith { providers.environmentVariable(it).orNull?.takeIf { value -> value.isNotEmpty() } }
        val missing = values.filterValues { it == null }.keys
        return when {
            missing.size == SIGNING_VARIABLES.size -> null
            missing.isNotEmpty() ->
                throw GradleException("Release signing is partly configured; missing: ${missing.joinToString(", ")}")
            else ->
                ReleaseSigning(
                    storeFile = values.getValue(KEYSTORE_FILE).orEmpty(),
                    storePassword = values.getValue(KEYSTORE_PASSWORD).orEmpty(),
                    keyAlias = values.getValue(KEY_ALIAS).orEmpty(),
                    keyPassword = values.getValue(KEY_PASSWORD).orEmpty(),
                )
        }
    }
}
