package app.reelstack.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.SeriesBrowse
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The episode row of a series page on a television.
 *
 * It opens with the next episode as the first tile, so a remote's first Down lands on the thing
 * the reader is most likely to play, and a change of season fills a row that kept its height
 * instead of emptying it, which used to make the cast row below jump up and down again.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "b+nn-w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvEpisodeRowTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun episode(prefix: String, number: Int, played: Boolean = false, season: Int = 1) = LibraryMedia(
        id = "jellyfin-$prefix$number", remoteId = "$prefix$number", title = "Serie", subtitle = "S0$season E$number · Del $number",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        season = season, episode = number, played = played,
    )

    private fun season(number: Int, titled: Boolean) = LibraryMedia(
        id = "jellyfin-s$number", remoteId = "s$number", title = if (titled) "Sesong $number" else "", subtitle = "",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Season", episode = number,
    )

    /** Ten episodes of one season, the first [watched] of them seen; the server says the next one is up. */
    private fun browse(watched: Int, prefix: String = "e", seasonNumber: Int = 1, titled: Boolean = false,
        nextUp: LibraryMedia? = null): SeriesBrowse {
        val episodes = (1..10).map { episode(prefix, it, played = it <= watched, season = seasonNumber) }
        return SeriesBrowse(
            seriesId = "series", openedFor = "series",
            seasons = if (titled) listOf(season(1, true), season(2, true)) else listOf(season(1, false)),
            selectedSeasonId = "s$seasonNumber", episodes = episodes,
            nextUp = nextUp ?: episodes.getOrNull(watched),
        )
    }

    private fun show(state: androidx.compose.runtime.MutableState<SeriesBrowse>) {
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            // The shimmer never stops, and a test that waits for idle would wait for ever.
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                ReelstackTheme {
                    Column(Modifier.fillMaxSize().padding(start = 48.dp)) {
                        // Stands for the buttons above the row. It sits over the second tile, so the
                        // tile straight below it is not the one that has to get focus.
                        Box(Modifier.padding(start = 400.dp).size(48.dp).testTag("above").focusable())
                        SeriesEpisodes(state.value, "series", onSeason = {})
                        // Stands for the cast row, which is what used to jump.
                        Box(Modifier.height(40.dp).testTag("below"))
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private val tiles = SemanticsMatcher("an episode tile") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.let { it.startsWith("episode-") && it != "episode-row" } == true
    }

    /** The tiles that can be seen in the row right now, left to right. */
    private fun visibleTiles(): List<String> {
        val row = rule.onNodeWithTag("episode-row").fetchSemanticsNode().boundsInRoot
        return rule.onAllNodes(tiles).fetchSemanticsNodes()
            .filter { it.layoutInfo.isPlaced && it.boundsInRoot.right > row.left + 1f && it.boundsInRoot.left < row.right - 1f }
            .sortedBy { it.boundsInRoot.left }
            .map { it.config[SemanticsProperties.TestTag] }
    }

    private fun assertSitsFirst(tag: String) {
        val row = rule.onNodeWithTag("episode-row").getUnclippedBoundsInRoot()
        val tile = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        // The row has 4 dp of padding before its first tile; the focus ring needs it.
        assertEquals("$tag should be the first tile in the row", 4f, (tile.left - row.left).value, .6f)
    }

    private fun change(state: androidx.compose.runtime.MutableState<SeriesBrowse>, next: SeriesBrowse) {
        rule.runOnIdle { state.value = next }
        rule.waitForIdle()
    }

    @Test fun theRowOpensWithTheNextEpisodeAsItsFirstTile() {
        show(mutableStateOf(browse(watched = 6)))
        assertEquals(listOf("episode-e7", "episode-e8", "episode-e9", "episode-e10"), visibleTiles())
        assertSitsFirst("episode-e7")
    }

    @Test fun theLastEpisodeCanSitFirstToo() {
        show(mutableStateOf(browse(watched = 9)))
        assertEquals(listOf("episode-e10"), visibleTiles())
        assertSitsFirst("episode-e10")
    }

    @Test fun aSeasonWithoutTheNextEpisodeStartsAtItsFirstOne() {
        // The series is at season one, episode seven; this is a different season.
        val elsewhere = episode("e", 7, season = 1).copy(id = "jellyfin-elsewhere", remoteId = "elsewhere")
        show(mutableStateOf(browse(watched = 0, prefix = "f", seasonNumber = 2, nextUp = elsewhere)))
        assertEquals("episode-f1", visibleTiles().first())
        assertSitsFirst("episode-f1")
    }

    @Test fun downFromTheButtonsAboveLandsOnTheNextEpisode() {
        show(mutableStateOf(browse(watched = 6)))
        rule.onNodeWithTag("above").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.onNodeWithTag("above").performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        rule.onNodeWithTag("episode-e7").assertIsFocused()
    }

    @Test fun downFromTheSeasonTabsLandsOnTheNextEpisode() {
        show(mutableStateOf(browse(watched = 6, titled = true)))
        rule.onNodeWithTag("season-s1").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.onNodeWithTag("season-s1").performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        rule.onNodeWithTag("episode-e7").assertIsFocused()
    }

    @Test fun aReloadWhileFocusIsInTheRowNeverScrollsIt() {
        val state = mutableStateOf(browse(watched = 6))
        show(state)
        rule.onNodeWithTag("episode-e8").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        val before = visibleTiles()
        // The same season arrives again with a different episode marked as next, as it does when
        // an episode finishes: new list, new next-up, focus still on the eighth tile.
        change(state, browse(watched = 8))
        assertEquals(before, visibleTiles())
        rule.onNodeWithTag("episode-e8").assertIsFocused()
    }

    @Test fun aReloadWithFocusElsewhereFollowsTheNewNextEpisode() {
        val state = mutableStateOf(browse(watched = 6))
        show(state)
        rule.onNodeWithTag("above").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        change(state, browse(watched = 8))
        assertEquals("episode-e9", visibleTiles().first())
        assertSitsFirst("episode-e9")
    }

    @Test fun aSeasonSwitchKeepsTheRowHeightAndTheContentBelowStaysPut() {
        val state = mutableStateOf(browse(watched = 6, titled = true))
        show(state)
        val rowHeight = rule.onNodeWithTag("episode-row").getUnclippedBoundsInRoot().height
        val belowTop = rule.onNodeWithTag("below").getUnclippedBoundsInRoot().top
        val loading = ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.detail_loading_episodes)

        // Season two was chosen; season one's episodes are kept only until it answers.
        change(state, state.value.copy(selectedSeasonId = "s2", episodesStale = true, loading = true))
        rule.onNodeWithContentDescription(loading).assertExists()
        assertTrue("No tile of the old season may stay on screen", visibleTiles().isEmpty())
        assertEquals(rowHeight.value, rule.onNodeWithTag("episode-row").getUnclippedBoundsInRoot().height.value, .01f)
        assertEquals(belowTop.value, rule.onNodeWithTag("below").getUnclippedBoundsInRoot().top.value, .01f)

        // And when it answers, the row opens on that season's own next episode, with the page still in place.
        val second = browse(watched = 2, prefix = "f", seasonNumber = 2, titled = true)
        change(state, second)
        rule.onNodeWithContentDescription(loading).assertDoesNotExist()
        assertEquals("episode-f3", visibleTiles().first())
        assertSitsFirst("episode-f3")
        assertEquals(rowHeight.value, rule.onNodeWithTag("episode-row").getUnclippedBoundsInRoot().height.value, .01f)
        assertEquals(belowTop.value, rule.onNodeWithTag("below").getUnclippedBoundsInRoot().top.value, .01f)
    }

    @Test fun theFirstLoadOfASeasonShowsPlaceholderTilesRatherThanASpinnerLine() {
        show(mutableStateOf(SeriesBrowse(seriesId = "series", openedFor = "series", seasons = listOf(season(1, true), season(2, true)),
            selectedSeasonId = "s1", loading = true)))
        val loading = ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.detail_loading_episodes)
        rule.onNodeWithContentDescription(loading).assertExists()
        // A row of tiles is far taller than the one line a spinner takes.
        assertTrue(rule.onNodeWithTag("episode-row").getUnclippedBoundsInRoot().height > 120.dp)
    }
}
