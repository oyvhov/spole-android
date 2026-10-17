@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.common.text.CueGroup
import androidx.media3.datasource.AssetDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.player.*
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.*
import org.junit.Test

class BitmapSidecarPlaybackTest {
    @Test fun dvdMksRendersSeeksAndRestoresWithoutOpeningUnselectedPgs() = exercise("dvd", SPOLE_MKS_DVD_MIME)
    @Test fun pgsMksRendersSeeksAndRestoresWithoutOpeningUnselectedDvd() = exercise("pgs", SPOLE_MKS_PGS_MIME)

    private fun exercise(name: String, mime: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val cues = CopyOnWriteArrayList<CueGroup>()
        val errors = CopyOnWriteArrayList<PlaybackException>()
        val opened = CopyOnWriteArrayList<String>()
        val other = if (name == "dvd") "pgs" else "dvd"
        val id = "spole-subtitle-42"
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext,
                PlaybackRenderersFactory(instrumentation.targetContext, LocalAudioFallback())).build()
            player.addListener(object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) { cues += cueGroup }
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            val item = MediaItem.Builder().setUri("asset:///player/video.mp4").setSubtitleConfigurations(listOf(
                MediaItem.SubtitleConfiguration.Builder(Uri.parse("asset:///player/$name.mks"))
                    .setId(id).setMimeType(mime).setLanguage("nor").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build(),
                MediaItem.SubtitleConfiguration.Builder(Uri.parse("asset:///player/$other.mks"))
                    .setId("spole-subtitle-43").setMimeType(if (other == "dvd") SPOLE_MKS_DVD_MIME else SPOLE_MKS_PGS_MIME)
                    .setLanguage("eng").build(),
            )).build()
            val factory = DataSource.Factory {
                val source = AssetDataSource(instrumentation.context)
                object : DataSource by source {
                    override fun open(dataSpec: DataSpec): Long {
                        opened += dataSpec.uri.path.orEmpty()
                        return source.open(dataSpec)
                    }
                }
            }
            player.setMediaSource(playbackMediaSource(factory, item)); player.prepare(); player.play()
        }
        fun await(label: String, condition: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 15_000
            while (!condition() && errors.isEmpty() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
            assertTrue("$label; errors=${errors.map { it.errorCodeName + ": " + it.cause?.message }}",
                errors.isEmpty() && condition())
        }
        fun hasBitmap() = cues.any { it.cues.any { cue -> cue.bitmap != null } }
        try {
            await("The selected MKS renders a bitmap", ::hasBitmap)
            instrumentation.runOnMainSync {
                val text = player.currentTracks.groups.single { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
                assertEquals(id, text.getTrackFormat(0).id.orEmpty().substringAfterLast(':'))
                // Media3 canonicalises the advertised ISO 639-2 "nor" to BCP 47 "no".
                assertEquals("no", text.getTrackFormat(0).language)
                player.pause()
            }
            assertTrue(opened.any { it.endsWith("/$name.mks") })
            assertFalse("Lazy selection must not fetch the other language", opened.any { it.endsWith("/$other.mks") })
            cues.clear()
            instrumentation.runOnMainSync { player.seekTo(8_000) }
            await("An active bitmap returns at the new paused position") {
                // TextRenderer first clears at the seek position, then emits the active cue
                // with its original start time. Require that order, not a new cue timestamp.
                val reset = cues.indexOfFirst { it.presentationTimeUs >= 7_500_000 && it.cues.isEmpty() }
                reset >= 0 && cues.drop(reset + 1).any { it.cues.any { cue -> cue.bitmap != null } }
            }
            instrumentation.runOnMainSync {
                assertEquals(8_000L, player.currentPosition)
                assertFalse(player.playWhenReady)
            }
            instrumentation.runOnMainSync {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
            }
            await("Off clears the bitmap") { cues.lastOrNull()?.cues?.isEmpty() == true }
            cues.clear()
            instrumentation.runOnMainSync {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
            }
            await("The selected bitmap returns after re-enabling", ::hasBitmap)
            assertFalse(opened.any { it.endsWith("/$other.mks") })
        } finally { instrumentation.runOnMainSync { player.release() } }
    }
}
