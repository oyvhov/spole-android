@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.BaseRenderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.text.TextOutput
import io.github.peerless2012.ass.Ass
import io.github.peerless2012.ass.AssRender
import io.github.peerless2012.ass.AssTexType
import io.github.peerless2012.ass.AssTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal data class SubtitleFont(val name: String, val bytes: ByteArray)
internal class PlaybackSubtitleResources {
    @Volatile var fonts: List<SubtitleFont> = emptyList()
    @Volatile var width: Int = 1920
    @Volatile var height: Int = 1080
}

/** Raw ASS packets go to libass on a dedicated thread. No video effects, HDR conversion or UI work. */
internal class NativeAssRenderer(output: TextOutput, outputLooper: Looper,
    private val adjustments: PlaybackAdjustmentController, resources: PlaybackSubtitleResources) : BaseRenderer(C.TRACK_TYPE_TEXT) {
    private val worker = AssWorker(output, outputLooper, resources)
    private val input = DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL)
    private var currentFormat: Format? = null
    private var period: MediaSource.MediaPeriodId? = null
    private var documentFormat: Format? = null
    private var document: ByteArray? = null
    private var ended = false
    private var nextFrameUs = Long.MIN_VALUE
    private var queuedBytes = 0

    override fun getName() = "SpoleLibassRenderer"
    override fun supportsFormat(format: Format): Int = RendererCapabilities.create(
        if (format.sampleMimeType == MimeTypes.TEXT_SSA && format.cryptoType == C.CRYPTO_TYPE_NONE) C.FORMAT_HANDLED else C.FORMAT_UNSUPPORTED_TYPE)
    override fun isReady() = true // Optional subtitles never stall the picture.
    override fun isEnded() = ended && worker.pendingPackets.get() == 0 && worker.renderedMs >= worker.endMs

    override fun onStreamChanged(formats: Array<out Format>, startPositionUs: Long, offsetUs: Long, mediaPeriodId: MediaSource.MediaPeriodId) {
        currentFormat = formats.firstOrNull()
        if (period != mediaPeriodId || documentFormat?.id != currentFormat?.id) { document = null; documentFormat = currentFormat }
        period = mediaPeriodId
        resetWorker()
        ended = false; queuedBytes = 0; nextFrameUs = Long.MIN_VALUE
    }
    override fun onPositionReset(positionUs: Long, joining: Boolean, sampleStreamIsResetToKeyFrame: Boolean) {
        resetWorker()
        ended = false; nextFrameUs = Long.MIN_VALUE; queuedBytes = 0
    }
    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        if (queuedBytes >= 8 * 1024 * 1024) { ended = true; worker.reset(null); queuedBytes = 0 }
        // Bound read-ahead per render and total subtitle data. A malformed file must stay optional.
        repeat(16) {
            if (ended || queuedBytes >= 8 * 1024 * 1024 || worker.pendingPackets.get() >= 32) return@repeat
            input.clear()
            val holder = formatHolder
            when (readSource(holder, input, 0)) {
                C.RESULT_FORMAT_READ -> { currentFormat = holder.format; resetWorker() }
                C.RESULT_BUFFER_READ -> {
                    if (input.isEndOfStream) { ended = true; worker.finishInput() }
                    else {
                        input.flip()
                        input.data?.let { data ->
                            if (data.remaining() <= 2 * 1024 * 1024) {
                                val bytes = ByteArray(data.remaining()); data.get(bytes); queuedBytes += bytes.size
                                if (currentFormat?.initializationData.orEmpty().size >= 2) {
                                    worker.packet(bytes, (input.timeUs - streamOffsetUs) / 1000)
                                } else if (document?.contentEquals(bytes) != true) {
                                    // A progressive sidecar may already be consumed when re-enabled.
                                    // Keep one bounded document for this media period, and replay it
                                    // after seek/off/on without borrowing another title's subtitles.
                                    document = bytes
                                    resetWorker()
                                }
                            }
                        }
                    }
                }
                else -> return@repeat
            }
        }
        // Paused players still need a fresh frame after track/offset/font changes. Using the
        // media position here would suppress every later frame while the clock stood still.
        if (elapsedRealtimeUs >= nextFrameUs) {
            nextFrameUs = elapsedRealtimeUs + 33_333
            worker.frame(subtitleRenderPosition(positionUs - streamOffsetUs, adjustments.current.subtitleDelayMs) / 1000)
        }
    }
    private fun resetWorker() { worker.reset(currentFormat); document?.let { worker.packet(it, 0) } }
    override fun onDisabled() { worker.reset(null); currentFormat = null }
    override fun onReset() { worker.reset(null) }
    override fun onRelease() { document = null; documentFormat = null; period = null; worker.close() }
}

/** Native allocations and rendering are serial. Old generations can never repaint after a seek. */
private class AssWorker(private val output: TextOutput, outputLooper: Looper, private val resources: PlaybackSubtitleResources) : AutoCloseable {
    private val thread = HandlerThread("Spole-libass", android.os.Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
    private val handler = Handler(thread.looper)
    private val ui = Handler(outputLooper)
    private val generation = AtomicInteger()
    private val rendering = AtomicBoolean()
    val pendingPackets = AtomicInteger()
    private var ass: Ass? = null
    private var renderer: AssRender? = null
    private var track: AssTrack? = null
    private var embedded = false
    private var lastFonts: List<SubtitleFont> = emptyList()
    @Volatile var endMs = Long.MAX_VALUE
        private set
    @Volatile var renderedMs = Long.MIN_VALUE
        private set

    fun reset(format: Format?) {
        val ticket = generation.incrementAndGet()
        endMs = if (format == null) 0 else Long.MAX_VALUE
        renderedMs = Long.MIN_VALUE
        publish(ticket, emptyList(), 0)
        handler.post {
            releaseNative()
            if (format == null || ticket != generation.get()) return@post
            runCatching {
                ass = Ass()
                // Android does not install desktop fontconfig directories. Register a bounded
                // system fallback explicitly; movie attachments retain their own family names.
                listOf("Roboto-Regular.ttf", "NotoSans-Regular.ttf").firstNotNullOfOrNull { name ->
                    runCatching { java.io.File("/system/fonts", name).takeIf { it.length() in 1..2 * 1024 * 1024 }
                        ?.let { SubtitleFont(name, it.readBytes()) } }.getOrNull()
                }?.let { ass!!.addFont(it.name, it.bytes) }
                lastFonts = resources.fonts
                lastFonts.forEach { ass!!.addFont(it.name, it.bytes) }
                renderer = ass!!.createRender().apply { setCacheLimit(2_000, 24) }
                track = ass!!.createTrack()
                embedded = format.initializationData.size >= 2
                if (embedded) {
                    track!!.readBuffer(format.initializationData[1])
                    // Media3 puts Start,End before the original Matroska Layer/Style fields.
                    track!!.readBuffer(("\n[Events]\n" + format.initializationData[0].toString(Charsets.UTF_8) + "\n").toByteArray())
                }
                renderer!!.setTrack(track)
            }.onFailure { releaseNative(); endMs = 0; renderedMs = 0 }
        }
    }
    fun packet(bytes: ByteArray, timeMs: Long) {
        val ticket = generation.get(); pendingPackets.incrementAndGet()
        handler.post {
            try {
                if (ticket != generation.get()) return@post
                val data = if (embedded) assMatroskaDialogue(bytes, timeMs) ?: return@post else bytes
                runCatching { track?.readBuffer(data) }
                endMs = if (embedded) {
                    val duration = bytes.toString(Charsets.UTF_8).substringAfter(',').substringBefore(',').let(::assTimeMs) ?: 0
                    maxOf(if (endMs == Long.MAX_VALUE) 0 else endMs, timeMs + duration)
                } else assDocumentEndMs(data)
            } finally { pendingPackets.decrementAndGet() }
        }
    }
    fun finishInput() {
        val ticket = generation.get()
        handler.post {
            // Empty/unsupported documents are optional, too. No event must not mean an infinite end.
            if (ticket == generation.get() && endMs == Long.MAX_VALUE) endMs = 0
        }
    }
    fun frame(timeMs: Long) {
        if (!rendering.compareAndSet(false, true)) return
        val ticket = generation.get()
        handler.post {
            try {
                if (ticket != generation.get()) return@post
                // A native failure or an empty frame cannot hold the video's EOS hostage.
                renderedMs = timeMs
                val fonts = resources.fonts
                if (fonts !== lastFonts) {
                    fonts.forEach { ass?.addFont(it.name, it.bytes) }; lastFonts = fonts
                    renderer?.release()
                    renderer = ass?.createRender()?.apply { setCacheLimit(2_000, 24); setTrack(track) }
                }
                val render = renderer ?: return@post
                val width = resources.width.coerceIn(1, 8192)
                val height = resources.height.coerceIn(1, 8192)
                val scale = minOf(1.0, kotlin.math.sqrt(921_600.0 / (width.toLong() * height)))
                val renderWidth = (width * scale).toInt().coerceAtLeast(2)
                val renderHeight = (height * scale).toInt().coerceAtLeast(2)
                render.setStorageSize(width, height); render.setFrameSize(renderWidth, renderHeight)
                val frame = render.renderFrame(timeMs, AssTexType.BITMAP_RGBA) ?: return@post
                if (frame.changed == 0) return@post
                val cues = frame.images.orEmpty().mapNotNull { tex -> tex.bitmap?.let { bitmap ->
                    Cue.Builder().setBitmap(bitmap).setPosition(tex.x.toFloat() / renderWidth).setPositionAnchor(Cue.ANCHOR_TYPE_START)
                        .setLine(tex.y.toFloat() / renderHeight, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_START)
                        .setSize(bitmap.width.toFloat() / renderWidth).setBitmapHeight(bitmap.height.toFloat() / renderHeight).build()
                } }
                publish(ticket, cues, timeMs)
            } catch (_: RuntimeException) { publish(ticket, emptyList(), timeMs) }
            // Up to 30 bitmap sets a second: a low-memory TV loses this frame's text, not the app.
            catch (_: OutOfMemoryError) { publish(ticket, emptyList(), timeMs) }
            finally { rendering.set(false) }
        }
    }
    private fun publish(ticket: Int, cues: List<Cue>, timeMs: Long) { ui.post { if (ticket == generation.get()) output.onCues(CueGroup(cues, timeMs * 1000)) } }
    private fun releaseNative() { renderer?.release(); renderer = null; track?.release(); track = null; ass?.release(); ass = null; lastFonts = emptyList() }
    override fun close() { generation.incrementAndGet(); handler.post { releaseNative(); thread.quitSafely() } }
}
