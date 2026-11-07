package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.FocusStyle
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Down from a card goes to the row below, whatever the captions next to it look like.
 *
 * The focused card used to grow as a whole, caption and all, and the grown card was what the remote measured
 * from. Its neighbour, whose episode name ran to two lines, then reached lower than the focused card and so
 * counted as "below" it: Down moved focus one card to the right and the row jumped (the Shield, 2026-11-07).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class HomeTvDownNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val episodes = List(6) { index -> LibraryMedia(
        id = "episode-$index", title = "Serie $index", progress = .4f,
        // Every other episode name runs to a second line under its card.
        subtitle = if (index % 2 == 1) "S01E0${index + 1} · Det er bare å legge gulv og flytte inn, og så litt til etter det"
            else "S01E0${index + 1} · Kort",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        season = 1, episode = index + 1,
    ) }
    private val films = List(6) { index -> LibraryMedia(
        id = "film-$index", title = "Film $index", subtitle = "2026",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Movie",
    ) }

    private fun show(resume: List<LibraryMedia> = episodes, recent: List<LibraryMedia> = films) {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "fixture", userId = "u")),
            resume = resume,
            recentMovies = recent,
            homeSections = setOf(HomeSection.CONTINUE_WATCHING, HomeSection.JELLYFIN_MOVIES),
        )
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                CompositionLocalProvider(
                    LocalPersonalization provides Personalization(showHero = false, focusStyle = FocusStyle.LIFT),
                    LocalMotionEnabled provides true) {
                    HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false)
                }
            }
        }
        rule.waitForIdle()
    }

    /**
     * The root of it: the remote measures from the card's own bounds, so those must not grow with focus.
     * Only the picture inside comes forward; on the Shield the row below was further away than here and
     * the grown card's neighbour won.
     */
    @Test fun theCardTheRemoteMeasuresFromDoesNotGrowWithFocus() {
        show()
        val card = rule.onNodeWithTag("resume-card-episode-0")
        val before = card.getUnclippedBoundsInRoot()
        card.requestFocus()
        rule.mainClock.advanceTimeBy(1_000)
        card.assertIsFocused()
        val after = card.getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals("top", before.top.value, after.top.value, .5f)
        org.junit.Assert.assertEquals("bottom", before.bottom.value, after.bottom.value, .5f)
        org.junit.Assert.assertEquals("left", before.left.value, after.left.value, .5f)
    }

    /**
     * A row is as tall as its tallest card in view. When a card with a two-line episode name came into view or
     * left it, the whole row grew or shrank and everything under it jumped (the Shield, 2026-11-07). On a
     * television every card's caption takes the same room.
     */
    @Test fun everyCardInARowIsTheSameHeightWhateverItsCaption() {
        show()
        val short = rule.onNodeWithTag("resume-card-episode-0").getUnclippedBoundsInRoot()
        val long = rule.onNodeWithTag("resume-card-episode-1").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals("a card with a long episode name is as tall as one with a short",
            (short.bottom - short.top).value, (long.bottom - long.top).value, .5f)
    }

    /**
     * Focus that comes into a row from above or below leaves the row where it is: the row moves sideways only
     * for Left and Right. Coming down from the second card of a row that fits whole, the row below slid one card
     * sideways before the page went down (the Shield, 2026-11-07).
     */
    @Test fun comingDownIntoARowDoesNotSlideItSideways() {
        // Three episodes fit whole, so the second stays where it stands; many films do not fit.
        show(resume = episodes.take(3), recent = List(14) { index -> films[0].copy(id = "film-$index", title = "Film $index") })
        rule.onNodeWithTag("resume-card-episode-1").requestFocus()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("resume-card-episode-1").assertIsFocused()
        org.junit.Assert.assertTrue("the second episode stands right of the keyline",
            rule.onNodeWithTag("resume-card-episode-1").getUnclippedBoundsInRoot().left.value > 100f)
        val films = rule.onAllNodes(hasTestTag("library-rail")).fetchSemanticsNodes()
        org.junit.Assert.assertTrue("the films row is on the page", films.isNotEmpty())
        val before = rule.onAllNodes(hasText("Film", substring = true) and hasClickAction()).fetchSemanticsNodes()
            .associate { it.id to it.boundsInRoot.left }
        rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
        rule.mainClock.advanceTimeBy(1_000)
        val focused = rule.onNode(isFocused()).fetchSemanticsNode()
        org.junit.Assert.assertTrue("focus went down into the films", rule.onAllNodes(isFocused() and hasText("Film", substring = true)).fetchSemanticsNodes().isNotEmpty())
        val leftBefore = before[focused.id]
        org.junit.Assert.assertNotNull("the focused film was in view before the move", leftBefore)
        org.junit.Assert.assertEquals("the films row did not slide sideways (px)", leftBefore!!, focused.boundsInRoot.left, 2f)
    }

    @Test fun downFromACardBesideATallerCaptionGoesToTheRowBelow() {
        show()
        listOf(0, 2).forEach { index ->
            rule.onNodeWithTag("resume-card-episode-$index").requestFocus()
            rule.mainClock.advanceTimeBy(1_000)
            rule.onNodeWithTag("resume-card-episode-$index").assertIsFocused()
            rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
            rule.mainClock.advanceTimeBy(1_000)
            val focused = rule.onNode(isFocused()).fetchSemanticsNode()
            val key = androidx.compose.ui.semantics.SemanticsProperties.TestTag
            val tag = if (key in focused.config) focused.config[key] else ""
            assertTrue("Down from card $index went to «$tag», not to the films below", !tag.startsWith("resume-card"))
        }
    }
}
