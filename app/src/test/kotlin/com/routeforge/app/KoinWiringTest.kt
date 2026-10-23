package com.routeforge.app

import android.content.Context
import android.content.pm.PackageManager
import com.routeforge.addresssearch.presentation.SearchSourceContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.dsl.module
import org.koin.test.verify.verify
import java.io.File
import kotlin.reflect.KClass

// Types that a class asks for but that no registration provides, because something else supplies them: each one is named
// here with who supplies it. The check never reports them, so this list is reviewed whenever it changes.
private val suppliedFromOutside: List<KClass<*>> =
    listOf(
        Context::class, // androidContext(): the application context, read by classes registered with it
        PackageManager::class, // androidContext().packageManager, given to SetupDeepLinkIntentFactory
        File::class, // the folders of BundledRegionCatalog and BrouterRoutingEngine come from androidContext().filesDir
        Function1::class, // BundledRegionCatalog.openAsset, built from androidContext().assets
        Class.forName("io.ktor.client.engine.HttpClientEngine").kotlin, // the engine is chosen where HttpClient is created
        SearchSourceContext::class, // parametersOf(...) of AddressSearchViewModel, from the navigation route
        Boolean::class, // parametersOf(isPickerMode) of FavoritesViewModel, from the screen that opens it
    )

internal class MissingDependency

internal class NeedsMissingDependency(
    val dependency: MissingDependency,
)

class KoinWiringTest {
    @Test
    fun `all registrations of the app are wired`() {
        module { includes(*routeForgeModules.toTypedArray()) }.verify(extraTypes = suppliedFromOutside)
    }

    @Test
    fun `a missing registration is reported with the class and the type`() {
        val brokenModule = module { factory { NeedsMissingDependency(get()) } }

        val failure = assertThrows<Exception> { brokenModule.verify() }

        val message = failure.message.orEmpty()
        assertTrue(message.contains(NeedsMissingDependency::class.qualifiedName.orEmpty()), message)
        assertTrue(message.contains(MissingDependency::class.qualifiedName.orEmpty()), message)
    }
}
