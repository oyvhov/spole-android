package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.AppTab
import app.reelstack.ui.CompactTouchNavigation
import app.reelstack.ui.components.TvMenuSettings
import app.reelstack.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w892dp-h412dp-land-xhdpi", application = app.reelstack.SheetTestApplication::class)
class NavigationSettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun capture(name: String) {
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val file = java.io.File("build/reports/navigation-ui/$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun visibilitySwitchUpdatesLandscapeMenuAndPersistsWithoutLosingSettings() {
        val repository = AppPreferencesRepository(rule.activity)
        var options by mutableStateOf(Personalization())
        var selected by mutableStateOf(AppTab.HOME)
        rule.setContent {
            ReelstackTheme {
                CompositionLocalProvider(LocalPersonalization provides options,
                    LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    Surface { Row(Modifier.fillMaxSize()) {
                        CompactTouchNavigation(selected, { selected = it })
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            TvMenuSettings(options) { options = it; repository.personalization = it }
                        }
                    } }
                }
            }
        }
        rule.onNodeWithTag("menu-visible-DISCOVER").performScrollTo().performClick()
        rule.onNodeWithTag("compact-tab-DISCOVER").assertDoesNotExist()
        assertTrue("DISCOVER" in repository.personalization.hiddenMenuItems)
        rule.onNodeWithTag("menu-visible-SETTINGS").performScrollTo().assertIsNotEnabled().assertIsOn()
        rule.onNodeWithTag("compact-tab-SETTINGS").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(AppTab.SETTINGS, selected) }
        capture("landscape-font2-protected-settings")
        rule.onNodeWithTag("menu-visible-DISCOVER").performScrollTo().performClick()
        rule.onNodeWithTag("compact-tab-DISCOVER").assertExists()
        assertFalse("DISCOVER" in repository.personalization.hiddenMenuItems)
        capture("landscape-font2-menu-editor")
    }

    @Test fun requiredDestinationsSurviveStalePreferencesAndAttemptsToHide() {
        val stale = Personalization(startInLibrary = true, hiddenMenuItems = DEFAULT_MENU.toSet())
        assertEquals(listOf("HOME", "LIBRARY", "SETTINGS"), stale.visibleMenu())
        val saved = stale.withMenuVisible("SETTINGS", false)
        assertFalse("SETTINGS" in saved.hiddenMenuItems)
        assertFalse("HOME" in saved.hiddenMenuItems)
        assertFalse("LIBRARY" in saved.hiddenMenuItems)
        val repository = AppPreferencesRepository(rule.activity)
        repository.personalization = stale
        assertEquals(setOf("DISCOVER", "ACTIVITY"), repository.personalization.hiddenMenuItems)
    }

    @Test
    @Config(qualifiers = "w412dp-h892dp-port-xhdpi")
    fun portraitMenuAlsoUpdatesImmediately() {
        var options by mutableStateOf(Personalization())
        var selected by mutableStateOf(AppTab.HOME)
        rule.setContent {
            ReelstackTheme {
                CompositionLocalProvider(LocalPersonalization provides options) {
                    Surface { Column(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            TvMenuSettings(options) { options = it }
                        }
                        app.reelstack.ui.ReelstackBottomBar(selected, { selected = it })
                    } }
                }
            }
        }
        val discover = rule.activity.getString(app.reelstack.R.string.nav_discover)
        val settings = rule.activity.getString(app.reelstack.R.string.nav_settings)
        rule.onNodeWithTag("menu-visible-DISCOVER").performScrollTo().performClick()
        rule.onNode(hasContentDescription(discover) and hasAnyAncestor(hasTestTag("bottom-navigation"))).assertDoesNotExist()
        rule.onNode(hasContentDescription(settings) and hasAnyAncestor(hasTestTag("bottom-navigation"))).performClick()
        rule.runOnIdle { assertEquals(AppTab.SETTINGS, selected) }
        capture("portrait-menu-editor")
    }
}
