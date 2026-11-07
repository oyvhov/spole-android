package app.reelstack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.SeriesBrowse
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Shield, 2026-11-07: season 13 was scrolled so its first tab was cut, and the episode captions sat 26 px from the bottom. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "b+nn-w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvSeriesSafeAreaTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun episode(number: Int) = LibraryMedia(
        id = "jellyfin-e$number", remoteId = "e$number", title = "Serie", subtitle = "S13 E$number · En ganske lang tittel som tar tre linjer i kortet $number",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode", season = 13, episode = number)

    private fun season(number: Int) = LibraryMedia(
        id = "jellyfin-s$number", remoteId = "s$number", title = "Sesong $number", subtitle = "",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Season", episode = number)

    private fun show() {
        val episodes = (1..10).map(::episode)
        val browse = SeriesBrowse(seriesId = "series", openedFor = "series", seasons = (1..16).map(::season),
            selectedSeasonId = "s1", episodes = episodes, nextUp = episodes.first())
        val details = ContentDetails("series", "Serie", "Jellyfin", "2020", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, mediaType = "Series")
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            CompositionLocalProvider(LocalMotionEnabled provides false) { ReelstackTheme {
                app.reelstack.ui.components.TvCinematicDetails(details, rememberScrollState(),
                    heading = { Box(Modifier.height(220.dp)) }) {
                    SeriesEpisodes(browse, "series", onSeason = {})
                }
            } }
        }
        rule.waitForIdle()
    }

    @Test fun episodeCaptionsStayInsideTheSafeArea() {
        show()
        rule.onNodeWithTag("episode-e1").requestFocus()
        rule.waitForIdle()
        val screen = rule.onNodeWithTag("tv-cinematic-detail").getUnclippedBoundsInRoot().bottom.value
        val tile = rule.onNodeWithTag("episode-e1").getUnclippedBoundsInRoot()
        // The tile keeps 6 dp of its own padding under its caption.
        val captionBottom = tile.bottom.value - 6f
        assertTrue("the caption ends ${screen - captionBottom} dp above the bottom", screen - captionBottom >= 28f)
        rule.onNodeWithTag("season-s1").assertIsDisplayed()
    }

    @Test fun noSeasonTabIsCutAtEitherEdge() {
        show()
        rule.onNodeWithTag("season-s1").requestFocus()
        repeat(12) { step ->
            rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionRight) }
            rule.waitForIdle()
            rule.mainClock.advanceTimeBy(1_000)
            val bounds = rule.onNodeWithTag("season-strip").getUnclippedBoundsInRoot()
            (1..16).forEach { n ->
                if (rule.onAllNodesWithTag("season-s$n").fetchSemanticsNodes().isEmpty()) return@forEach
                val tab = rule.onNodeWithTag("season-s$n").getUnclippedBoundsInRoot()
                val cutAtStart = tab.left.value < bounds.left.value - 1f && tab.right.value > bounds.left.value + 1f
                assertTrue("season $n is cut at the start after step $step: ${tab.left}..${tab.right} in ${bounds.left}", !cutAtStart)
            }
        }
    }
}
