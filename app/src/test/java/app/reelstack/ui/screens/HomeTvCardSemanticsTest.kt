package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A card on a television is one stop for an accessibility service.
 *
 * With any accessibility service on, Compose rebuilds, sorts and compares the whole semantics tree about every 100 ms
 * while the page moves. Each card brought ten nodes to it, and that work took about 38 % of the main thread while
 * Home scrolled, on the Shield (Button Mapper) and on Loftet (only Stova's home button) alike (2026-11-07).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class HomeTvCardSemanticsTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun show() {
        val episodes = List(6) { i -> LibraryMedia(id = "ep-$i", title = "Serie $i", progress = .4f, subtitle = "S01E0${i + 1} · Namn",
            artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode", season = 1, episode = i + 1) }
        val films = List(6) { i -> LibraryMedia(id = "film-$i", title = "Film $i", subtitle = "2026",
            artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Movie") }
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "fixture", userId = "u")),
            resume = episodes, recentMovies = films, homeSections = setOf(HomeSection.CONTINUE_WATCHING, HomeSection.JELLYFIN_MOVIES))
        rule.setContent { ReelstackTheme { CompositionLocalProvider(
            LocalPersonalization provides Personalization(showHero = false, focusStyle = FocusStyle.LIFT)) {
            HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false) } } }
        rule.waitForIdle()
    }

    /**
     * The card clears what lies under it, so an accessibility service, which skips cleared nodes, sees the card and
     * not the ten nodes of artwork, progress and caption it is drawn with. (A test's unmerged tree still lists them.)
     */
    @Test fun aResumeCardIsOneStopThatSaysWhatItIs() {
        show()
        val card = rule.onNodeWithTag("resume-card-ep-0").fetchSemanticsNode()
        assertTrue("the card clears the nodes under it", card.config.isClearingSemantics)
        val label = card.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()
        assertTrue(label, "Serie 0" in label)
        assertEquals("the caption is not read a second time", null, card.config.getOrNull(SemanticsProperties.Text))
        assertNotNull(card.config.getOrNull(SemanticsActions.OnClick))
    }

    @Test fun aFilmCardIsOneStopThatSaysWhatItIs() {
        show()
        val card = rule.onNode(hasCardLabel("Film 0") and hasClickAction()).fetchSemanticsNode()
        assertTrue("the card clears the nodes under it", card.config.isClearingSemantics)
        assertEquals("the caption is not read a second time", null, card.config.getOrNull(SemanticsProperties.Text))
    }
}
