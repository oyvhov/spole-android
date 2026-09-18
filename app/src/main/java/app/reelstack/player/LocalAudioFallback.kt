package app.reelstack.player

import android.content.Context
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

internal fun canRetryAudioLocally(errorCode: Int, rendererName: String?, mime: String?,
    encrypted: Boolean, softwareSupported: Boolean, alreadyTried: Boolean): Boolean =
    rendererName == "MediaCodecAudioRenderer" && mime?.startsWith("audio/") == true &&
        !encrypted && softwareSupported && !alreadyTried && errorCode in setOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        )

/** Main-thread writes, playback-thread reads. Never remember failures across different titles. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class LocalAudioFallback {
    @Volatile private var softwareMimes: Set<String> = emptySet()

    fun reset() { softwareMimes = emptySet() }

    fun usesSoftware(format: Format): Boolean =
        format.cryptoType == C.CRYPTO_TYPE_NONE && format.sampleMimeType in softwareMimes

    fun tryEnable(error: PlaybackException): Boolean {
        val rendererError = error as? ExoPlaybackException ?: return false
        if (rendererError.type != ExoPlaybackException.TYPE_RENDERER) return false
        val format = rendererError.rendererFormat ?: return false
        val mime = format.sampleMimeType ?: return false
        if (!canRetryAudioLocally(error.errorCode, rendererError.rendererName, mime,
                format.cryptoType != C.CRYPTO_TYPE_NONE, FfmpegLibrary.supportsFormat(mime),
                mime in softwareMimes)) return false
        softwareMimes = softwareMimes + mime
        return true
    }
}

/** Keep hardware and passthrough first. Only a failed audio format moves to FFmpeg. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class PlaybackRenderersFactory(context: Context, private val fallback: LocalAudioFallback) :
    DefaultRenderersFactory(context) {
    init { setEnableDecoderFallback(true) }

    override fun buildAudioRenderers(context: Context, extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector, enableDecoderFallback: Boolean, audioSink: AudioSink,
        eventHandler: Handler, eventListener: AudioRendererEventListener, out: ArrayList<Renderer>) {
        out.add(object : MediaCodecAudioRenderer(context, codecAdapterFactory, mediaCodecSelector,
            enableDecoderFallback, eventHandler, eventListener, audioSink) {
            override fun supportsFormat(selector: MediaCodecSelector, format: Format): Int =
                if (fallback.usesSoftware(format)) RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE)
                else super.supportsFormat(selector, format)
        })
        out.add(FfmpegAudioRenderer(eventHandler, eventListener, audioSink))
    }
}
