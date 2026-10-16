package app.reelstack.player

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * «Neste episode» in the corner, as the owner chose it: no picture, the name and where it is, and
 * a calm countdown that fills «Spel neste» from the left instead of counting in numbers.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class NextEpisodeCardTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val next = PlayableItem("next", "Innbrotet", "Episode", season = 2, episode = 3)

    private fun card(countdown: Int?) = rule.setContent { ReelstackTheme {
        PlayerScreen(PlayerScreenState(busy = false, playing = true, positionMs = 1_740_000, durationMs = 1_800_000,
            nextEpisodeLeadSeconds = 60, nextEpisode = next, nextEpisodeCountdown = countdown, nextEpisodeCountdownTotalSeconds = 12),
            null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
    } }

    @Test fun theCornerCardNamesTheEpisodeWithoutAPicture() {
        card(countdown = 6)
        rule.onNodeWithTag("player-next-episode").assertIsDisplayed()
        rule.onNodeWithTag("player-next-artwork").assertDoesNotExist()
        rule.onNodeWithTag("player-next-play").assert(hasText(rule.activity.getString(R.string.next_episode_play)))
        rule.onNodeWithTag("player-next-cancel").assert(hasText(rule.activity.getString(R.string.next_episode_hide)))
        // No seconds written out; the button itself shows how far the countdown has come.
        rule.onAllNodes(hasText(rule.activity.getString(R.string.next_episode_countdown, 6)), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun theCountdownFillsTheButtonFromTheLeft() {
        card(countdown = 6)
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        val button = rule.onNodeWithTag("player-next-play").fetchSemanticsNode().boundsInRoot
        val fill = rule.onNodeWithTag("player-next-progress", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // Six of twelve seconds gone: half the button.
        assertEquals(button.left, fill.left, 1f)
        assertEquals(.5f, fill.width / button.width, .06f)
    }
}
