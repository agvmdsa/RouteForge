package com.routeforge.simulation.domain.model

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.net.URLClassLoader

class ExecutionModeInitializationTest {
    @Test
    fun `the default execution mode exists even when a subclass is the first thing the JVM loads`() {
        val classes = ExecutionMode::class.java.protectionDomain.codeSource.location
        val stdlib = Unit::class.java.protectionDomain.codeSource.location

        // A class loader of its own, so that ExecutionMode$Once is the first class of the hierarchy to be initialized,
        // which is what decides whether the companion's DEFAULT reads Once before Once exists.
        URLClassLoader(arrayOf(classes, stdlib), ClassLoader.getPlatformClassLoader()).use { loader ->
            Class.forName("com.routeforge.simulation.domain.model.ExecutionMode\$Once", true, loader)
            val executionMode = Class.forName("com.routeforge.simulation.domain.model.ExecutionMode", true, loader)
            val companion = executionMode.getField("Companion").get(null)

            assertNotNull(companion.javaClass.getMethod("getDEFAULT").invoke(companion))
        }
    }
}
