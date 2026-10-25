package app.reelstack.ui.screens

import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.AppTab
import app.reelstack.ui.AppNavigationRail
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

    @Test fun longShortcutListsNeverScrollTheBrandOrHomeOutOfView() {
        rule.setContent {
            ReelstackTheme {
                Surface {
                    app.reelstack.ui.AppNavigationRail(AppTab.HOME, {}, expanded = true,
                        shortcuts = (1..24).map { "library-$it" to "Bibliotek $it" },
                        modifier = Modifier.height(320.dp))
                }
            }
        }
        // The touch rail merges the brand into its clickable expand/collapse row.
        val brandNode = rule.onNodeWithTag("sidebar-brand", useUnmergedTree = true)
        val brand = brandNode.getUnclippedBoundsInRoot()
        val home = rule.onNodeWithTag("wide-tab-HOME").getUnclippedBoundsInRoot()
        rule.onNodeWithTag("wide-library-library-24").performScrollTo().assertIsDisplayed()
        brandNode.assertIsDisplayed()
        rule.onNodeWithTag("wide-tab-HOME").assertIsDisplayed()
        assertEquals(brand, brandNode.getUnclippedBoundsInRoot())
        assertEquals(home, rule.onNodeWithTag("wide-tab-HOME").getUnclippedBoundsInRoot())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun clippedTvDestinationLeavesNoIconFragmentAndReturnsWithRemoteFocus() {
        lateinit var inputMode: androidx.compose.ui.input.InputModeManager
        var opened: AppTab? = null
        rule.setContent {
            val configuration = android.content.res.Configuration(
                androidx.compose.ui.platform.LocalConfiguration.current).apply {
                uiMode = (uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK.inv()) or
                    android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
            }
            inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides configuration) {
                ReelstackTheme { CompositionLocalProvider(LocalPersonalization provides Personalization()) {
                    AppNavigationRail(AppTab.HOME, { opened = it }, expanded = true,
                        shortcuts = (1..8).map { "library-$it" to "Bibliotek $it" },
                        modifier = Modifier.height(320.dp))
                } }
            }
        }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        val viewport = rule.onNodeWithTag("sidebar-destinations")
        val library = rule.onNodeWithTag("wide-tab-LIBRARY")
        viewport.performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
            scroll(0f, with(rule.density) { 35.dp.toPx() })
        }
        val port = viewport.getUnclippedBoundsInRoot()
        val partial = library.getUnclippedBoundsInRoot()
        assertTrue("The fixture must leave a genuinely partial row", partial.top < port.top && partial.bottom > port.top)
        // Robolectric's native renderer can draw the real Compose tree into a Canvas, but it
        // does not deliver PixelCopy's force-redraw callback used by captureToImage().
        val portPixels = viewport.fetchSemanticsNode().boundsInRoot
        val drawn = rule.runOnIdle {
            val content = rule.activity.findViewById<android.view.View>(android.R.id.content)
            val bitmap = android.graphics.Bitmap.createBitmap(content.width, content.height,
                android.graphics.Bitmap.Config.ARGB_8888)
            content.draw(android.graphics.Canvas(bitmap))
            android.graphics.Bitmap.createBitmap(bitmap, portPixels.left.toInt(), portPixels.top.toInt(),
                portPixels.width.toInt(), portPixels.height.toInt())
        }
        val pixels = drawn.asImageBitmap().toPixelMap()
        val visibleHeight = (((partial.bottom - port.top).value / (port.bottom - port.top).value) * pixels.height)
            .toInt().coerceIn(0, pixels.height)
        val background = pixels[0, 0]
        var fragments = 0
        var fullRowPixels = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val pixel = pixels[x, y]
            if (kotlin.math.abs(pixel.red - background.red) > .02f ||
                kotlin.math.abs(pixel.green - background.green) > .02f ||
                kotlin.math.abs(pixel.blue - background.blue) > .02f) {
                if (y < visibleHeight) fragments++ else fullRowPixels++
            }
        }
        assertTrue("The fixture must really render the following full destinations", fullRowPixels > 20)
        assertEquals("A clipped row must not paint an icon/label fragment", 0, fragments)

        rule.onNodeWithTag("wide-tab-HOME").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionDown) }
        library.assertIsFocused().assertIsDisplayed()
        val revealed = library.getUnclippedBoundsInRoot()
        assertTrue("Remote focus must reveal the whole real row", revealed.top >= port.top && revealed.bottom <= port.bottom)
        library.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(AppTab.LIBRARY, opened)
        library.performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag("wide-tab-HOME").assertIsFocused()
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
        rule.onNodeWithTag("menu-visible-DISCOVER").performScrollTo().performClick()
        rule.onNodeWithTag("compact-tab-DISCOVER").assertExists()
        assertFalse("DISCOVER" in repository.personalization.hiddenMenuItems)
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
        assertEquals(setOf("DISCOVER"), repository.personalization.hiddenMenuItems)
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
    }
}
