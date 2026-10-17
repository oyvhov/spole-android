package app.reelstack.player

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.SubtitleStyle
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tool row's 48 dp rule, for the one menu that replaced it: audio, subtitles, chapters, speed,
 * delays, quality, frame, statistics and mini player must each stay a full-size target.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class PlayerOptionsMenuTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyRouteInTheOptionsMenuIsAFullSizeTarget() {
        val state = PlayerScreenState(busy = false, audio = listOf(PlaybackTrack(1, "English", "eng")),
            subtitles = listOf(PlaybackTrack(2, "Norsk", "nor", isText = true)), audioIndex = 1, subtitleIndex = 2,
            chapters = listOf(PlaybackChapter("Opning", 0)))
        rule.setContent { ReelstackTheme {
            PlayerOptionsMenu(PlayerMenu.HOME, state, PlaybackAdjustments(), offline = false, isTelevision = true,
                fillVideo = false, style = SubtitleStyle.CLEAN, onPage = {}, onDismiss = {}, onAudio = {}, onSubtitle = {},
                onQuality = {}, onSeek = {}, onAdjust = {}, onStyle = {}, onFrameMode = {}, onStats = {},
                onRotate = {}, onMiniPlayer = {})
        } }
        val routes = listOf("player-option-audio", "player-option-subtitles", "player-option-chapters", "player-option-speed",
            "player-option-audio_delay", "player-option-subtitle_delay", "player-option-quality", "player-frame-mode",
            "player-mini", "player-stats")
        for (tag in routes) {
            val bounds = rule.onNodeWithTag(tag).assertExists().getUnclippedBoundsInRoot()
            val height = bounds.bottom - bounds.top
            assertTrue("$tag is $height high", height >= 48.dp)
        }
    }
}
