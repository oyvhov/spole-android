package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home on a television looks like Home in Stova: a large hero, and one shelf at the bottom of the
 * screen — «Sjå vidare» — with the rest of the rows further down.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class HomeTvHeroLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val episodes = List(6) { index -> LibraryMedia(
        id = "episode-$index", title = "Serie $index", subtitle = "Episode", progress = .4f,
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        heroUrl = "https://example.com/Items/series-$index/Images/Thumb?maxWidth=1920&quality=90&tag=t",
        season = 1, episode = index + 1,
        overview = "Ein galen vitskapsmann viser fram ein robot som skal fange rotter, og så går alt gale i kloakken under byen.",
    ) }

    private fun showHome(fontScale: Float = 1f, topInset: androidx.compose.ui.unit.Dp = 0.dp) {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "fixture", userId = "u")),
            resume = episodes,
            recentSeries = episodes,
            homeSections = setOf(HomeSection.CONTINUE_WATCHING, HomeSection.JELLYFIN_SERIES),
        )
        rule.setContent {
            ReelstackTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    val input = androidx.compose.ui.platform.LocalInputModeManager.current
                    androidx.compose.runtime.SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
                    CompositionLocalProvider(LocalPersonalization provides Personalization(showHero = true)) {
                        HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false, topInset = topInset)
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun theHeroReachesDownToAWholeFirstShelfAtTheBottomOfTheScreen() {
        showHome()
        val screen = rule.onRoot().getBoundsInRoot()
        val hero = rule.onNodeWithTag("tablet-library-feature").getBoundsInRoot()
        // Unclipped: a shelf running off the bottom of the screen must not read as ending at the edge.
        val shelf = rule.onNodeWithTag("continue-watching-JELLYFIN").getUnclippedBoundsInRoot()
        assertTrue("the hero is larger than the old 44 %: ${hero.height}", hero.height > screen.height * .44f)
        assertTrue("the whole shelf is above the faded edge: ${shelf.bottom} of ${screen.bottom}",
            shelf.bottom <= screen.bottom - TV_SHELF_BOTTOM + 1.dp)
        assertTrue("and it stands at the bottom, not half-way up: ${screen.bottom - shelf.bottom}",
            screen.bottom - shelf.bottom < 48.dp)
        assertTrue("nothing but that shelf is on screen under the hero: ${shelf.top - hero.bottom}",
            shelf.top - hero.bottom < 8.dp)
        val picture = rule.onNodeWithTag("feature-artwork", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("the picture runs on behind the shelf to the bottom: ${picture.bottom}", picture.bottom >= screen.bottom - 1.dp)
    }

    // With «Toppmeny» the picture runs on behind the menu line, and the hero had the line's height added to
    // it: the first shelf stood half under the bottom of the screen (the owner's TV, 2026-11-02). The words
    // make room instead, and the shelf stands whole as it does with the side menu.
    @Test fun withTheMenuLineOnTopTheShelfStillStandsWholeAndTheWordsStayBelowTheLine() {
        val line = 54.dp
        showHome(topInset = line)
        val screen = rule.onRoot().getBoundsInRoot()
        val shelf = rule.onNodeWithTag("continue-watching-JELLYFIN").getUnclippedBoundsInRoot()
        assertTrue("the whole shelf is above the faded edge: ${shelf.bottom} of ${screen.bottom}",
            shelf.bottom <= screen.bottom - TV_SHELF_BOTTOM + 1.dp)
        assertTrue("and it stands at the bottom: ${screen.bottom - shelf.bottom}", screen.bottom - shelf.bottom < 48.dp)
        val caption = rule.onNodeWithTag("feature-caption").getBoundsInRoot()
        assertTrue("the words begin below the line: ${caption.top}", caption.top >= line)
        val picture = rule.onNodeWithTag("feature-artwork", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("the picture still starts at the top, behind the line: ${picture.top}", picture.top <= 0.dp)
    }

    // Eilev's Home on the Shield ended with the refresh button half hidden in the faded edge: the remote
    // reached it, but the page could not go all the way down.
    @Test fun thePageGoesAllTheWayDownToAWholeLastButton() {
        showHome(topInset = 54.dp)
        // With the remote, as on the TV: Down from the hero until the last button has focus.
        rule.onNodeWithTag("tablet-feature-open").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        repeat(8) {
            rule.onNode(androidx.compose.ui.test.isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("feed-refresh").assertIsFocused()
        val screen = rule.onRoot().getBoundsInRoot()
        val button = rule.onNodeWithTag("feed-refresh").getUnclippedBoundsInRoot()
        assertTrue("the last button is whole above the faded edge: ${button.bottom} of ${screen.bottom}",
            button.bottom <= screen.bottom - TV_SHELF_BOTTOM - 8.dp + 1.dp)
    }

    @Test fun largeTextKeepsTheShelfOnScreenByShrinkingTheHero() {
        showHome(fontScale = 1.5f)
        val screen = rule.onRoot().getBoundsInRoot()
        val shelf = rule.onNodeWithTag("continue-watching-JELLYFIN").getBoundsInRoot()
        val hero = rule.onNodeWithTag("tablet-library-feature").getBoundsInRoot()
        assertTrue("the shelf still begins on screen: ${shelf.top} of ${screen.bottom}", shelf.top < screen.bottom)
        assertTrue("the hero never drops below the old 44 %: ${hero.height}", hero.height >= screen.height * .44f - 1.dp)
    }
}
