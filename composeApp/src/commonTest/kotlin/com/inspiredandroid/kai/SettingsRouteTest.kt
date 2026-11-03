package com.inspiredandroid.kai

import com.inspiredandroid.kai.ui.settings.SettingsTab
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
 * built ("Route settings could not find any NavType for argument tab of type SettingsTab?").
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
    fun tabNameRoundTrips() {
        val route = Settings.of(SettingsTab.Services)
        assertEquals("Services", route.tab)
        assertEquals(SettingsTab.Services, Settings.tabOf(route))
    }

    @Test
    fun everyTabRoundTrips() {
        for (tab in SettingsTab.entries) {
            assertEquals(tab, Settings.tabOf(Settings.of(tab)))
        }
    }

    @Test
    fun nullTabPointsAtTheDefaultTab() {
        assertEquals(Settings(null), Settings.of(null))
        assertNull(Settings.tabOf(Settings()))
    }

    @Test
    fun unknownTabNameFallsBackToTheDefaultTab() {
        assertNull(Settings.tabOf(Settings("NotATab")))
    }
}
