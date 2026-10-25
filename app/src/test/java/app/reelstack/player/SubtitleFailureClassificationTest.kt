package app.reelstack.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class SubtitleFailureClassificationTest {
    /** Media3 calls a malformed sidecar a malformed container, which used to repackage the whole film. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun anUnreadableSubtitleFileIsASubtitleFailureNotABrokenFilm() {
        val subtitle = androidx.media3.exoplayer.ExoPlaybackException.createForSource(
            SubtitleSourceException("Invalid SUP segment"), PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
        assertTrue(playbackFailureIsSubtitle(subtitle))
        val film = androidx.media3.exoplayer.ExoPlaybackException.createForSource(
            androidx.media3.common.ParserException.createForMalformedContainer("broken cluster", null),
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
        assertFalse(playbackFailureIsSubtitle(film))
    }
}
