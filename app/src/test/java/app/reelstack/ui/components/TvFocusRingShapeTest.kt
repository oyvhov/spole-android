package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.CastMember
import app.reelstack.data.model.DiscoverPerson
import app.reelstack.data.model.PersonPage
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.ui.PersonSheet
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.MyRequestsScreen
import app.reelstack.ui.ringAt
import app.reelstack.ui.screens.PersonPortrait
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A focus ring follows the shape you see: the circle of a face, not the box around face and name;
 * and around words it leaves room, so it never runs through a letter.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvFocusRingShapeTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun keyboard(content: @Composable () -> Unit) = rule.setContent {
        val input = LocalInputModeManager.current
        SideEffect { input.requestInputMode(InputMode.Keyboard) }
        ReelstackTheme { Box(Modifier.background(Color.Black).padding(40.dp)) { content() } }
    }

    private fun drawn(): android.graphics.Bitmap = rule.runOnIdle { app.reelstack.ui.drawnContent(rule.activity) }

    private fun android.graphics.Bitmap.ringAt(xDp: Float, yDp: Float, origin: Rect): Boolean =
        ringAt(xDp, yDp, origin, rule.density.density)

    /** A ring along the top edge of a focused control, halfway across: the shape's own edge. */
    private fun ringOnTopEdge(tag: String) {
        rule.onNodeWithTag(tag).requestFocus()
        rule.waitForIdle()
        val bounds = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val d = rule.density.density
        assertTrue("$tag has no ring on its edge", drawn().ringAt(bounds.width / 2 / d, 1f, bounds))
    }

    private fun gaps(tag: String, text: String): Pair<Float, Float> {
        val outer = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val words = rule.onNode(hasText(text, substring = true) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val d = rule.density.density
        return (words.left - outer.left) / d to (outer.right - words.right) / d
    }

    @Test fun aCastRingSitsOnTheFace() {
        keyboard { CastRail(listOf(CastMember("Ana de Armas", "Rolla", seerrPersonId = 5)), source = ServiceKind.SEERR) }
        rule.onNode(hasClickAction()).requestFocus()
        rule.waitForIdle()
        val card = rule.onNode(hasClickAction()).fetchSemanticsNode().boundsInRoot
        val image = drawn()
        // 104 dp wide, a 84 dp face 10 dp in from each side and 6 dp from the top.
        assertFalse("a box is drawn around face and name", image.ringAt(20f, 1f, card))
        assertTrue("the face has no ring", image.ringAt(52f, 7f, card))
    }

    @Test fun aPersonRingSitsOnTheFace() {
        keyboard { PersonPortrait(DiscoverPerson(9, "Margaret Qualley", null), onClick = {}) }
        rule.onNodeWithTag("person-9").requestFocus()
        rule.waitForIdle()
        val card = rule.onNodeWithTag("person-9").fetchSemanticsNode().boundsInRoot
        val image = drawn()
        assertFalse("a box is drawn around face and name", image.ringAt(16f, 1f, card))
        assertTrue("the face has no ring", image.ringAt(52f, 1f, card))
    }

    @Test fun aTabRingLeavesRoomAroundItsWords() {
        keyboard {
            MyRequestsScreen(ReelstackUiState(trackedRequests = listOf(TrackedRequest("r", 9, "movie", "Coraline", null, setOf()))),
                PaddingValues(), onOpen = {}, onBack = {}, onLoadOlder = {})
        }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-requests-tab-ALL").assertIsFocused() }.isSuccess }
        val (left, right) = gaps("my-requests-tab-ALL", "1")
        assertTrue("the ring runs ${left} dp left and ${right} dp right of the words", left >= 10f && right >= 10f)
    }

    @Test fun readMoreLeavesRoomAroundItsWords() {
        val page = PersonPage(DiscoverPerson(9, "Margaret Qualley", null), department = "Acting",
            biography = "Margaret Qualley is an American actress. ".repeat(12))
        keyboard { PersonSheet(page, onTitle = {}) }
        rule.waitForIdle()
        val link = rule.onNodeWithTag("person-read-more").fetchSemanticsNode().boundsInRoot
        val name = rule.onNodeWithText("Margaret Qualley").fetchSemanticsNode().boundsInRoot
        val d = rule.density.density
        // The words stay on the column's edge; the ring stands outside them.
        assertTrue("the ring starts ${(name.left - link.left) / d} dp left of the column", (name.left - link.left) / d >= 10f)
    }

    @Test fun theHeroButtonsHaveRingsOnTheirPills() {
        val film = app.reelstack.data.model.LibraryMedia(id = "jf-1", title = "Film", subtitle = "2026", progress = .4f,
            artworkRes = app.reelstack.R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")
        keyboard { TabletLibraryFeature(film, onOpen = {}, candidates = listOf(film)) }
        ringOnTopEdge("tablet-feature-play")
        ringOnTopEdge("tablet-feature-open")
    }

    @Test fun aTrailerLinkHasARingOnItsPill() {
        keyboard { TrailerPreview("https://www.youtube.com/watch?v=abcdefghijk", "Film") }
        ringOnTopEdge("detail-trailer")
    }
}
