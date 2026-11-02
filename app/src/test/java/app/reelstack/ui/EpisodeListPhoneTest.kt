package app.reelstack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
 * The same promise as on a television, for the vertical list a phone or tablet shows: choosing
 * another season keeps the height of the episode area, so what sits under it does not jump.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "b+nn-w411dp-h891dp-xxhdpi", application = app.reelstack.SheetTestApplication::class)
class EpisodeListPhoneTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun episode(prefix: String, number: Int, season: Int) = LibraryMedia(
        id = "jellyfin-$prefix$number", remoteId = "$prefix$number", title = "Serie", subtitle = "S0$season E$number · Del $number",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        season = season, episode = number,
    )

    private fun browse(prefix: String, season: Int) = SeriesBrowse(
        seriesId = "series", openedFor = "series",
        seasons = listOf(
            LibraryMedia(id = "jellyfin-s1", remoteId = "s1", title = "Sesong 1", subtitle = "", artworkRes = R.drawable.media_placeholder,
                source = ServiceKind.JELLYFIN, mediaType = "Season", episode = 1),
            LibraryMedia(id = "jellyfin-s2", remoteId = "s2", title = "Sesong 2", subtitle = "", artworkRes = R.drawable.media_placeholder,
                source = ServiceKind.JELLYFIN, mediaType = "Season", episode = 2),
        ),
        selectedSeasonId = "s$season", episodes = (1..4).map { episode(prefix, it, season) },
    )

    private fun show(state: MutableState<SeriesBrowse>) {
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                ReelstackTheme {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        SeriesEpisodes(state.value, "series", onSeason = {})
                        Box(Modifier.height(40.dp).testTag("below"))
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun aSeasonSwitchKeepsTheHeightOfTheListAndTheContentBelowStaysPut() {
        val state = mutableStateOf(browse("e", 1))
        show(state)
        rule.onNodeWithTag("episode-e1", useUnmergedTree = true).assertExists()
        val listHeight = rule.onNodeWithTag("episode-row").getUnclippedBoundsInRoot().height
        val belowTop = rule.onNodeWithTag("below").getUnclippedBoundsInRoot().top
        assertTrue("Four episode cards are a tall list: $listHeight", listHeight > 400.dp)
        val loading = ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.detail_loading_episodes)

        rule.runOnIdle { state.value = state.value.copy(selectedSeasonId = "s2", episodesStale = true, loading = true) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription(loading).assertExists()
        rule.onNodeWithTag("episode-e1", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(listHeight.value, rule.onNodeWithTag("episode-row").getUnclippedBoundsInRoot().height.value, .01f)
        assertEquals(belowTop.value, rule.onNodeWithTag("below").getUnclippedBoundsInRoot().top.value, .01f)

        rule.runOnIdle { state.value = browse("f", 2) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription(loading).assertDoesNotExist()
        rule.onNodeWithTag("episode-f1", useUnmergedTree = true).assertExists()
        assertEquals(belowTop.value, rule.onNodeWithTag("below").getUnclippedBoundsInRoot().top.value, .01f)
    }
}
