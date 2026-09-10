package app.reelstack.player

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel as P
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.Display
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.exoplayer.audio.AudioCapabilities
import kotlinx.serialization.json.*

/** Re-read output capabilities at negotiation time: a receiver/headset can change between plays. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AndroidPlaybackCapabilities(private val context: Context) {
    private val attributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()
    private val codecs get() = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filterNot { it.isEncoder }
    private fun decoders(mime: String) = runCatching { codecs.filter { info -> info.supportedTypes.any { it.equals(mime, true) } }
        .map { it to it.getCapabilitiesForType(mime) } }.getOrDefault(emptyList())
    private fun hdrTypes() = runCatching { context.getSystemService(DisplayManager::class.java)
        .getDisplay(Display.DEFAULT_DISPLAY)?.hdrCapabilities?.supportedHdrTypes?.toSet().orEmpty() }.getOrDefault(emptySet())
    private fun audioRoute() = AudioCapabilities.getCapabilities(context, attributes, null, emptyList())

    fun snapshot(): DevicePlaybackCapabilities {
        val hdr = hdrTypes()
        val videos = VIDEO_MIMES.mapNotNull { (codec, mime) ->
            val found = decoders(mime)
            val best = found.mapNotNull { (info, caps) ->
                val video = caps.videoCapabilities ?: return@mapNotNull null
                val software = if (Build.VERSION.SDK_INT >= 29) info.isSoftwareOnly else
                    info.name.startsWith("OMX.google.") || info.name.startsWith("c2.android.")
                val size = listOf(7680 to 4320, 3840 to 2160, 2560 to 1440, 1920 to 1080, 1280 to 720, 720 to 480)
                    .firstOrNull { (!software || it.first <= 1920) && video.isSizeSupported(it.first, it.second) } ?: return@mapNotNull null
                Triple(caps, size.first, size.second)
            }.maxByOrNull { it.second.toLong() * it.third } ?: return@mapNotNull null
            val profiles = best.first.profileLevels.map { it.profile }.toSet()
            val tenBit = when (codec) {
                "hevc" -> profiles.any { it in setOf(P.HEVCProfileMain10, P.HEVCProfileMain10HDR10, P.HEVCProfileMain10HDR10Plus) }
                "av1" -> profiles.any { it in setOf(P.AV1ProfileMain10, P.AV1ProfileMain10HDR10, P.AV1ProfileMain10HDR10Plus) }
                "vp9" -> profiles.any { it in setOf(P.VP9Profile2, P.VP9Profile2HDR, P.VP9Profile2HDR10Plus) }
                else -> false // AVC High10 is not a portable hardware path.
            }
            val allowedProfiles = when(codec) {
                "h264" -> buildList {
                    add("baseline"); add("constrained baseline")
                    if (profiles.any { it in setOf(P.AVCProfileMain, P.AVCProfileHigh, P.AVCProfileConstrainedHigh) }) add("main")
                    if (profiles.any { it in setOf(P.AVCProfileHigh, P.AVCProfileConstrainedHigh) }) { add("high"); add("constrained high") }
                }
                "hevc" -> listOfNotNull("main", "main 10".takeIf { tenBit })
                "vp9" -> listOfNotNull("Profile 0", "Profile 2".takeIf { tenBit })
                else -> emptyList()
            }
            VideoPlaybackCapability(codec, best.second, best.third, if(tenBit) 10 else 8, allowedProfiles,
                best.first.profileLevels.mapNotNull { levelNumber(codec, it.level) }.maxOrNull(), buildList {
                    add("SDR")
                    if (tenBit && Display.HdrCapabilities.HDR_TYPE_HDR10 in hdr) add("HDR10")
                    if (tenBit && Display.HdrCapabilities.HDR_TYPE_HLG in hdr) add("HLG")
                    val hdrPlus = profiles.any { it in setOf(P.HEVCProfileMain10HDR10Plus, P.AV1ProfileMain10HDR10Plus, P.VP9Profile2HDR10Plus) }
                    if (tenBit && hdrPlus && Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS in hdr) add("HDR10Plus")
                    // Dolby Vision needs profile/container-specific validation; do not infer it from HEVC Main10.
                })
        }
        val route = runCatching { audioRoute() }.getOrNull()
        val audio = AUDIO_MIMES.mapNotNull { (codec, mime) ->
            val decoded = decoders(mime).maxOfOrNull { it.second.audioCapabilities?.maxInputChannelCount ?: 0 }?.coerceAtMost(8) ?: 0
            val passthroughMimes = if(codec in setOf("dts", "dca")) listOf(mime, "audio/vnd.dts.hd") else listOf(mime)
            val passed = if (route == null) 0 else (8 downTo 2).firstOrNull { count -> runCatching {
                passthroughMimes.any { route.isPassthroughPlaybackSupported(Format.Builder().setSampleMimeType(it).setChannelCount(count).setSampleRate(48_000).build(), attributes) }
            }.getOrDefault(false) } ?: 0
            val channels = maxOf(decoded, passed)
            if (channels > 0) AudioPlaybackCapability(codec, channels, passed > 0) else null
        }
        return DevicePlaybackCapabilities(videos.ifEmpty { DevicePlaybackCapabilities.CONSERVATIVE.video },
            audio.ifEmpty { DevicePlaybackCapabilities.CONSERVATIVE.audio })
    }

    /** Guard the actual source too: width/height maxima alone do not prove 4K60 or a codec profile. */
    fun canDecodeVideo(source: JsonObject): Boolean = runCatching {
        val snapshot = snapshot()
        val streams = source.objects("MediaStreams")
        val video = streams.firstOrNull { it.str("Type") == "Video" } ?: return false
        val codec = video.str("Codec").lowercase()
        val advertised = snapshot.video.firstOrNull { it.codec == codec } ?: return false
        val range = video.str("VideoRangeType").ifBlank { if(video.str("VideoRange").equals("HDR", true)) "UnknownHDR" else "SDR" }
        if (range !in advertised.ranges || video.num("BitDepth")?.let { it > advertised.bitDepth } == true) return false
        val width = video.num("Width")?.toInt() ?: return false
        val height = video.num("Height")?.toInt() ?: return false
        if (width > advertised.width || height > advertised.height) return false
        val mime = VIDEO_MIMES.getValue(codec)
        val format = MediaFormat.createVideoFormat(mime, width, height)
        val profile = platformProfile(codec, video.str("Profile"), video.num("BitDepth")?.toInt() ?: 8) ?: return false
        format.setInteger(MediaFormat.KEY_PROFILE, profile)
        video.num("Level")?.toInt()?.let { level ->
            (0..27).map { 1 shl it }.firstOrNull { levelNumber(codec, it) == level }
                ?.let { format.setInteger(MediaFormat.KEY_LEVEL, it) }
        }
        val rate = (video["AverageFrameRate"] as? JsonPrimitive)?.floatOrNull
            ?: (video["RealFrameRate"] as? JsonPrimitive)?.floatOrNull
        if (rate != null && rate > 0) format.setFloat(MediaFormat.KEY_FRAME_RATE, rate)
        video.num("BitRate")?.takeIf { it in 1..Int.MAX_VALUE }?.let { format.setInteger(MediaFormat.KEY_BIT_RATE, it.toInt()) }
        decoders(mime).any { (_, caps) -> runCatching { caps.isFormatSupported(format) }.getOrDefault(false) }
    }.getOrDefault(false)

    fun canDirectPlay(source: JsonObject, audioIndex: Int?): Boolean = runCatching {
        if (!canDecodeVideo(source)) return false
        val audios = source.objects("MediaStreams").filter { it.str("Type") == "Audio" }
        if (audios.isEmpty()) return true
        val audio = audios.firstOrNull { it.num("Index")?.toInt() == audioIndex } ?: return false
        val audioMime = if (audio.str("Codec").lowercase() in setOf("dts", "dca") && audio.str("Profile").contains("HD", true))
            "audio/vnd.dts.hd" else AUDIO_MIMES[audio.str("Codec").lowercase()] ?: return false
        val channels = audio.num("Channels")?.toInt() ?: return false
        val sampleRate = audio.num("SampleRate")?.toInt() ?: 48_000
        val media3 = Format.Builder().setSampleMimeType(audioMime).setChannelCount(channels).setSampleRate(sampleRate).build()
        if (audioRoute().isPassthroughPlaybackSupported(media3, attributes)) return true
        val audioFormat = MediaFormat.createAudioFormat(audioMime, sampleRate, channels)
        decoders(audioMime).any { (_, caps) -> runCatching { caps.isFormatSupported(audioFormat) }.getOrDefault(false) }
    }.getOrDefault(false)

    companion object {
        private val VIDEO_MIMES = linkedMapOf("h264" to "video/avc", "hevc" to "video/hevc", "av1" to "video/av01", "vp9" to "video/x-vnd.on2.vp9")
        private val AUDIO_MIMES = linkedMapOf("aac" to "audio/mp4a-latm", "mp3" to "audio/mpeg", "ac3" to "audio/ac3",
            "eac3" to "audio/eac3", "dts" to "audio/vnd.dts", "dca" to "audio/vnd.dts", "truehd" to "audio/true-hd",
            "flac" to "audio/flac", "opus" to "audio/opus", "vorbis" to "audio/vorbis")
        internal fun platformProfile(codec: String, name: String, depth: Int): Int? = when(codec) {
            "h264" -> when(name.lowercase()) { "", "baseline", "constrained baseline" -> P.AVCProfileBaseline
                "main" -> P.AVCProfileMain; "high", "constrained high" -> P.AVCProfileHigh; else -> null }
            "hevc" -> when(name.lowercase()) { "", "main" -> if(depth > 8) P.HEVCProfileMain10 else P.HEVCProfileMain
                "main 10" -> P.HEVCProfileMain10; else -> null }
            "av1" -> if (name.lowercase() in setOf("", "main", "main 8", "main 10")) { if(depth > 8) P.AV1ProfileMain10 else P.AV1ProfileMain8 } else null
            "vp9" -> when(name.lowercase()) { "", "profile 0" -> if(depth > 8) P.VP9Profile2 else P.VP9Profile0; "profile 2" -> P.VP9Profile2; else -> null }
            else -> null
        }
        private fun levelNumber(codec: String, level: Int): Int? {
            val levels = when(codec) {
                "h264" -> listOf(P.AVCLevel1, P.AVCLevel1b, P.AVCLevel11, P.AVCLevel12, P.AVCLevel13, P.AVCLevel2, P.AVCLevel21, P.AVCLevel22, P.AVCLevel3, P.AVCLevel31, P.AVCLevel32, P.AVCLevel4, P.AVCLevel41, P.AVCLevel42, P.AVCLevel5, P.AVCLevel51, P.AVCLevel52, P.AVCLevel6, P.AVCLevel61, P.AVCLevel62).zip(listOf(10,9,11,12,13,20,21,22,30,31,32,40,41,42,50,51,52,60,61,62)).toMap()
                "hevc" -> listOf(30,60,63,90,93,120,123,150,153,156,180,183,186).flatMapIndexed { index, value -> listOf((1 shl (index*2)) to value, (2 shl (index*2)) to value) }.toMap()
                else -> emptyMap()
            }
            return levels[level]
        }
    }
}
