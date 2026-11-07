package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home on a television has two calm states: the hero, and the shelves. Going down from the hero
 * moves the whole page once, and every shelf's title then lands on the same line under the top
 * edge; nothing is left half on the screen. Up brings the hero back whole, and Back does the same
 * before it ever leaves the app (seen on the Shield, 2026-11-07).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class HomeTvRowLineTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val episodes = List(6) { index -> LibraryMedia(
        id = "episode-$index", title = "Serie $index", subtitle = "Episode", progress = .4f,
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        heroUrl = "https://example.com/Items/series-$index/Images/Backdrop?maxWidth=1920&quality=90&tag=t",
        season = 1, episode = index + 1, overview = "Ei kort skildring av episoden.",
    ) }
    private val films = List(6) { index -> LibraryMedia(
        id = "film-$index", title = "Film $index", subtitle = "2026",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Movie",
    ) }

    private fun showHome() {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "fixture", userId = "u")),
            resume = episodes,
            recentSeries = episodes,
            recentMovies = films,
            homeSections = setOf(HomeSection.CONTINUE_WATCHING, HomeSection.JELLYFIN_SERIES, HomeSection.JELLYFIN_MOVIES),
        )
        rule.setContent {
            ReelstackTheme {
                val input = androidx.compose.ui.platform.LocalInputModeManager.current
                androidx.compose.runtime.SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
                CompositionLocalProvider(LocalPersonalization provides Personalization(showHero = true)) {
                    HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false)
                }
            }
        }
        rule.waitForIdle()
    }

    private fun titleTop(res: Int) = rule.onNode(hasContentDescription("${rule.activity.getString(res)} · Jellyfin"))
        .getUnclippedBoundsInRoot().top.value

    private fun feedAtEnd(): Boolean {
        val range = rule.onNodeWithTag("home-feed").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
        return range.value() >= range.maxValue() - 1f
    }

    private fun heroTop() = rule.onNodeWithTag("tablet-library-feature").getUnclippedBoundsInRoot().top.value

    private fun press(key: Key) {
        rule.onNode(isFocused()).performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private fun focusHero() {
        rule.onNodeWithTag("tablet-feature-open").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()
        rule.onNodeWithTag("tablet-feature-open").assertIsFocused()
    }

    @Test fun everyShelfTitleLandsOnTheSameLineUnderTheTopEdge() {
        showHome()
        focusHero()
        press(Key.DirectionDown)
        val first = titleTop(R.string.home_continue)
        // Under Apple's 60 pt (30 dp here) top safe area, and high enough that the shelf is the page.
        assertTrue("the first shelf's title stands at $first dp", first in 30f..64f)
        // The layout decides which of the two new-title shelves comes first; each lands on the same line in turn.
        val rest = mutableListOf(R.string.home_new_movies, R.string.home_new_episodes)
        repeat(2) { step ->
            press(Key.DirectionDown)
            val landed = rest.firstOrNull { kotlin.math.abs(titleTop(it) - first) <= 2f }
            // The last shelf has nothing below it to rise on: it stands as high as the page goes.
            assertTrue("shelf ${step + 2} lands where the first did, or the page is at its end",
                landed != null || feedAtEnd())
            rest.remove(landed ?: rest.first())
        }
    }

    @Test fun upFromTheFirstShelfBringsTheHeroBackWhole() {
        showHome()
        focusHero()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        val back = titleTop(R.string.home_continue)
        assertTrue("Up lands the first shelf on the line again, at $back dp", back in 30f..64f)
        press(Key.DirectionUp)
        assertEquals("the hero stands whole at the top", 0f, heroTop(), 1f)
    }

    @Test fun backFromTheShelvesReturnsToTheHeroBeforeLeaving() {
        showHome()
        focusHero()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        val heroes = rule.onAllNodesWithTag("tablet-library-feature").fetchSemanticsNodes()
        // A hero kept composed above the page (the list's cache window) is not placed: its bounds are NaN.
        assertTrue("the shelves are up", heroes.isEmpty() || heroTop().isNaN() || heroTop() < -10f)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertFalse("Back did not leave the activity", rule.activity.isFinishing)
        assertEquals("the hero stands whole at the top", 0f, heroTop(), 1f)
        val hero = rule.onNodeWithTag("tablet-library-feature").getUnclippedBoundsInRoot()
        val focused = rule.onNode(isFocused()).getUnclippedBoundsInRoot()
        assertTrue("the remote is in the hero", focused.top >= hero.top && focused.bottom <= hero.bottom)
    }
}
