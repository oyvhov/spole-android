package app.reelstack

import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.common.text.CueGroup
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.player.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/** Real native bitmap output. An ASS→VTT fallback cannot pass the drawing/layer assertions. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class NativeAssPlaybackTest {
    @Test fun embeddedDvdBitmapRendersWithoutServerBurning() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val cues = CopyOnWriteArrayList<CueGroup>()
        val errors = CopyOnWriteArrayList<PlaybackException>()
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext,
                PlaybackRenderersFactory(instrumentation.targetContext, LocalAudioFallback())).build()
            player.addListener(object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) { cues += cueGroup }
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            player.setMediaSource(playbackMediaSource(androidx.media3.datasource.DataSource.Factory {
                androidx.media3.datasource.AssetDataSource(instrumentation.context)
            }, MediaItem.fromUri("asset:///player/dvd.mkv")))
            player.prepare(); player.play()
        }
        try {
            val deadline = SystemClock.elapsedRealtime() + 15_000
            while (cues.none { it.cues.any { cue -> cue.bitmap != null } } && errors.isEmpty() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
            assertTrue("DVD/VobSub must produce a local bitmap; errors=${errors.map { it.errorCodeName }}",
                errors.isEmpty() && cues.any { it.cues.any { cue -> cue.bitmap != null } })
        } finally { instrumentation.runOnMainSync { player.release() } }
    }

    @Test fun anEmptyAssDocumentCannotPreventTheVideoFromEnding() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val errors = CopyOnWriteArrayList<PlaybackException>()
        val ended = java.util.concurrent.atomic.AtomicBoolean()
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext,
                PlaybackRenderersFactory(instrumentation.targetContext, LocalAudioFallback())).build()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) ended.set(true) }
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            val item = MediaItem.Builder().setUri("asset:///player/video.mp4").setSubtitleConfigurations(listOf(
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse("asset:///player/empty.ass"))
                    .setId("empty-ass").setMimeType(MimeTypes.TEXT_SSA).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build())).build()
            player.setMediaSource(playbackMediaSource(androidx.media3.datasource.DataSource.Factory {
                androidx.media3.datasource.AssetDataSource(instrumentation.context)
            }, item), 19_000)
            player.prepare(); player.play()
        }
        try {
            val deadline = SystemClock.elapsedRealtime() + 15_000
            while (!ended.get() && errors.isEmpty() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
            assertTrue("Empty ASS must let video end; errors=${errors.map { it.errorCodeName }}", errors.isEmpty() && ended.get())
        } finally { instrumentation.runOnMainSync { player.release() } }
    }

    /** A line that runs past the picture (a watermark, another cut's file) must not hold the end hostage. */
    @Test fun anAssLineBeyondTheVideoCannotKeepItPlaying() = endsDespite("late.ass", MimeTypes.TEXT_SSA)
    @Test fun aVttLineBeyondTheVideoCannotKeepItPlaying() = endsDespite("late.vtt", MimeTypes.TEXT_VTT)

    private fun endsDespite(asset: String, mime: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val errors = CopyOnWriteArrayList<PlaybackException>()
        val ended = java.util.concurrent.atomic.AtomicBoolean()
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext,
                PlaybackRenderersFactory(instrumentation.targetContext, LocalAudioFallback())).build()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) ended.set(true) }
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            val item = MediaItem.Builder().setUri("asset:///player/video.mp4").setSubtitleConfigurations(listOf(
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse("asset:///player/$asset"))
                    .setId("late").setMimeType(mime).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build())).build()
            player.setMediaSource(playbackMediaSource(androidx.media3.datasource.DataSource.Factory {
                androidx.media3.datasource.AssetDataSource(instrumentation.context)
            }, item), 17_000)
            player.prepare(); player.play()
        }
        try {
            // Three seconds of picture remain; allow for a slow emulator, not for the line's nine hours.
            val deadline = SystemClock.elapsedRealtime() + 12_000
            while (!ended.get() && errors.isEmpty() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
            var position = 0L
            instrumentation.runOnMainSync { position = player.currentPosition }
            assertTrue("$asset: ended=${ended.get()} position=$position errors=${errors.map { it.errorCodeName }}",
                errors.isEmpty() && ended.get())
        } finally { instrumentation.runOnMainSync { player.release() } }
    }

    @Test fun styledAssRendersLocallyAndSurvivesSeekOffOnAndDelay() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val cues = CopyOnWriteArrayList<CueGroup>()
        val errors = CopyOnWriteArrayList<PlaybackException>()
        lateinit var player: ExoPlayer
        val adjustments = PlaybackAdjustmentController()
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext,
                PlaybackRenderersFactory(instrumentation.targetContext, LocalAudioFallback(), adjustments)).build()
            player.addListener(object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) { cues += cueGroup }
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            val item = MediaItem.Builder().setUri("asset:///player/video.mp4").setSubtitleConfigurations(listOf(
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse("asset:///player/styled.ass"))
                    .setId("ass-test").setMimeType(MimeTypes.TEXT_SSA).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build())).build()
            player.setMediaSource(playbackMediaSource(androidx.media3.datasource.DataSource.Factory {
                androidx.media3.datasource.AssetDataSource(instrumentation.context)
            }, item))
            player.prepare(); player.play()
        }
        fun await(description: String, test: () -> Boolean) {
            val end = SystemClock.elapsedRealtime() + 20_000
            while (!test() && errors.isEmpty() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(25)
            assertTrue("$description; errors=${errors.map { it.errorCodeName }}", errors.isEmpty() && test())
        }
        try {
            await("ASS drawings and styled text produce positioned bitmap regions") { cues.any { it.cues.count { cue -> cue.bitmap != null } >= 3 } }
            val rendered = cues.last { it.cues.size >= 2 }.cues
            assertTrue(rendered.any { it.position < .4f && it.line < .4f })
            assertTrue(rendered.any { it.position >= .45f && it.line >= .5f })
            cues.clear()
            instrumentation.runOnMainSync { player.pause(); player.seekTo(8_000) }
            await("An active ASS event returns after seek") { cues.any { it.presentationTimeUs >= 7_500_000 && it.cues.any { c -> c.bitmap != null } } }
            instrumentation.runOnMainSync {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
            }
            await("Turning subtitles off clears output") { cues.lastOrNull()?.cues?.isEmpty() == true }
            cues.clear()
            instrumentation.runOnMainSync {
                adjustments.apply(player, PlaybackAdjustments(speed = 1.25f, subtitleDelayMs = -500))
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
            }
            await("Turning ASS back on renders the current event at the adjusted clock") { cues.any { it.presentationTimeUs >= 8_400_000 && it.cues.any { c -> c.bitmap != null } } }
            instrumentation.runOnMainSync { assertEquals(1.25f, player.playbackParameters.speed) }
        } finally { instrumentation.runOnMainSync { player.release() } }
    }

    @Test fun supBitmapSidecarRendersAndSeeksToAnAlreadyActiveCue() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val cues = CopyOnWriteArrayList<CueGroup>()
        val errors = CopyOnWriteArrayList<PlaybackException>()
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext,
                PlaybackRenderersFactory(instrumentation.targetContext, LocalAudioFallback())).build()
            player.addListener(object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) { cues += cueGroup }
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            val item = MediaItem.Builder().setUri("asset:///player/video.mp4").setSubtitleConfigurations(listOf(
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse("asset:///player/synthetic.sup"))
                    .setId("sup-test").setMimeType(SPOLE_SUP_MIME).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build())).build()
            player.setMediaSource(playbackMediaSource(androidx.media3.datasource.DataSource.Factory {
                androidx.media3.datasource.AssetDataSource(instrumentation.context)
            }, item)); player.prepare(); player.play()
        }
        fun awaitBitmap() {
            val deadline = SystemClock.elapsedRealtime() + 15_000
            while (cues.none { it.cues.any { c -> c.bitmap != null } } && errors.isEmpty() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
            assertTrue("SUP must render without server burning; errors=${errors.map { it.errorCodeName }}",
                errors.isEmpty() && cues.any { it.cues.any { c -> c.bitmap != null } })
        }
        try {
            awaitBitmap()
            instrumentation.runOnMainSync { player.pause() }
            cues.clear()
            instrumentation.runOnMainSync { player.seekTo(8_000) }
            awaitBitmap()
        } finally { instrumentation.runOnMainSync { player.release() } }
    }

    @Test fun embeddedMatroskaAssKeepsStylesAndAnimatedText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val cues = CopyOnWriteArrayList<CueGroup>()
        val errors = CopyOnWriteArrayList<PlaybackException>()
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext,
                PlaybackRenderersFactory(instrumentation.targetContext, LocalAudioFallback())).build()
            player.addListener(object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) { cues += cueGroup }
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            player.setMediaSource(playbackMediaSource(androidx.media3.datasource.DataSource.Factory {
                androidx.media3.datasource.AssetDataSource(instrumentation.context)
            }, MediaItem.fromUri("asset:///player/styled.mkv")))
            player.prepare(); player.play()
        }
        try {
            val deadline = SystemClock.elapsedRealtime() + 15_000
            while (cues.count { it.cues.count { c -> c.bitmap != null } >= 3 } < 4 && errors.isEmpty() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
            assertTrue("Embedded ASS styles and moving text must render; errors=${errors.map { it.errorCodeName }}",
                errors.isEmpty() && cues.count { it.cues.count { c -> c.bitmap != null } >= 3 } >= 4)
            val textPositions = cues.flatMap { it.cues }.filter { it.line in .30f.. .48f }.map { it.position }.distinct()
            assertTrue("The ASS move animation changes its position", textPositions.size >= 2)
        } finally { instrumentation.runOnMainSync { player.release() } }
    }
}
