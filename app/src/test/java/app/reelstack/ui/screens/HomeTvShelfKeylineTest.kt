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
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A television row scrolls to a keyline: the focused card always lands where the first card started, so no
 * card is ever left sliced by the menu with its caption cut mid-word, and the row never jumps (Shield, 2026-11-07).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class HomeTvShelfKeylineTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val media = List(14) { index -> LibraryMedia(
        id = "movie-$index", title = "Serie $index", subtitle = "Episode", progress = .4f,
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Movie") }

    private fun show() {
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "fixture", userId = "u")),
            recentMovies = media,
            homeSections = setOf(HomeSection.JELLYFIN_MOVIES))
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                CompositionLocalProvider(LocalPersonalization provides Personalization(showHero = false),
                    LocalMotionEnabled provides true) {
                    HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false)
                }
            }
        }
    }

    private fun card(index: Int) = rule.onNode(hasClickAction() and hasCardLabel("Serie $index"))

    /**
     * Every step lands the card in focus on the keyline, from the first press on: the row moves one card
     * at a time under a still focus. A row that let focus walk to the far edge and then moved four cards
     * at once read as the screen jumping (the Shield, 2026-11-07, «Sjå vidare»).
     */
    @Test fun everyStepLandsTheFocusedCardOnTheKeylineAndNoCardIsSlicedAcrossIt() {
        show()
        card(0).requestFocus()
        // The page gutter, where the titles and the first card start.
        val keyline = 24f
        repeat(10) { step ->
            rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionRight) }
            rule.mainClock.advanceTimeBy(1_000)
            val focused = card(step + 1).assertIsFocused().getUnclippedBoundsInRoot()
            assertEquals("after step $step the focused card stands on the keyline", keyline, focused.left.value, 3f)
            (0..step).forEach { other ->
                if (rule.onAllNodes(hasClickAction() and hasCardLabel("Serie $other")).fetchSemanticsNodes().isEmpty()) return@forEach
                val b = card(other).getUnclippedBoundsInRoot()
                val sliced = b.left.value < keyline - 1f && b.right.value > keyline + 1f
                assertTrue("card $other is sliced by the keyline after step $step: ${b.left}..${b.right} vs $keyline", !sliced)
            }
        }
    }

    @Test fun aFocusedCardHasRoomToGrowAboveAndBelow() {
        show()
        card(0).requestFocus()
        val rail = rule.onNodeWithTag("library-rail").getUnclippedBoundsInRoot()
        // A card that is not in focus; the focused one is already grown into its room.
        val card = card(2).getUnclippedBoundsInRoot()
        assertTrue("room above the card for its focus growth: ${card.top - rail.top}", (card.top - rail.top).value >= 14f)
        assertTrue("room below the card for its focus growth: ${rail.bottom - card.bottom}", (rail.bottom - card.bottom).value >= 14f)
    }
}
