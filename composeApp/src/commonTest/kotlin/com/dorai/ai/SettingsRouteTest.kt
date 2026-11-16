package com.dorai.ai

import com.dorai.ai.ui.settings.SettingsCategory
import kotlinx.serialization.descriptors.SerialKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Guards the route argument types of the typed destinations.
 *
 * Typed routes only support a fixed set of NavTypes. An enum NavType exists on Android only, so
 * an enum route argument makes the app crash on desktop, iOS and wasm while the route pattern is
 * built ("Route settings could not find any NavType for argument category of type
 * SettingsCategory?").
 */
class SettingsRouteTest {

    @Test
    fun settingsRouteArgumentIsAPrimitiveString() {
        val element = Settings.serializer().descriptor.getElementDescriptor(0)
        assertNotEquals(SerialKind.ENUM, element.kind)
        // `toInternalType()` strips the nullable marker before matching the built-in NavTypes.
        assertEquals("kotlin.String", element.serialName.replace("?", ""))
    }

    @Test
    fun categoryIdRoundTrips() {
        val route = Settings.of(SettingsCategory.Providers)
        assertEquals("providers", route.category)
        assertEquals(SettingsCategory.Providers, Settings.categoryOf(route))
    }

    @Test
    fun everyCategoryRoundTrips() {
        for (category in SettingsCategory.entries) {
            assertEquals(category, Settings.categoryOf(Settings.of(category)))
        }
    }

    @Test
    fun nullCategoryPointsAtTheDefaultCategory() {
        assertEquals(Settings(null), Settings.of(null))
        assertNull(Settings.categoryOf(Settings()))
    }

    @Test
    fun unknownCategoryIdFallsBackToTheDefaultCategory() {
        assertNull(Settings.categoryOf(Settings("NotACategory")))
    }
}
