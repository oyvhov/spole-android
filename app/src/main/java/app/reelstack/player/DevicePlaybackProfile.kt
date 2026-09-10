package app.reelstack.player

import kotlinx.serialization.json.*

/** A snapshot of decoders and the current audio/display route, never a device-name allowlist. */
data class VideoPlaybackCapability(
    val codec: String, val width: Int, val height: Int, val bitDepth: Int = 8,
    val profiles: List<String> = emptyList(), val level: Int? = null,
    val ranges: List<String> = listOf("SDR"),
)
data class AudioPlaybackCapability(val codec: String, val channels: Int, val passthrough: Boolean = false)
data class DevicePlaybackCapabilities(
    val video: List<VideoPlaybackCapability>, val audio: List<AudioPlaybackCapability>,
) {
    val maxAudioChannels get() = audio.maxOfOrNull { it.channels }?.coerceIn(2, 8) ?: 2
    companion object {
        val CONSERVATIVE = DevicePlaybackCapabilities(
            listOf(VideoPlaybackCapability("h264", 1920, 1080, profiles = listOf("baseline", "constrained baseline", "main", "high"), level = 42)),
            listOf(AudioPlaybackCapability("aac", 2), AudioPlaybackCapability("mp3", 2)),
        )
    }
}

fun automaticPlaybackBitrate(unmetered: Boolean): Int = if (unmetered) 120_000_000 else 4_000_000

fun devicePlaybackProfile(bitrate: Int, capabilities: DevicePlaybackCapabilities): JsonObject = buildJsonObject {
    put("Name", "Spole Android · detected capabilities")
    put("MaxStreamingBitrate", bitrate); put("MaxStaticBitrate", bitrate)
    putJsonArray("DirectPlayProfiles") {
        for ((containers, codecs, audioCodecs) in listOf(
            Triple("mp4,m4v,mkv,mov", capabilities.video.map { it.codec }, capabilities.audio.map { it.codec }),
            Triple("webm", capabilities.video.map { it.codec }.filter { it in setOf("av1", "vp9") }, capabilities.audio.map { it.codec }.filter { it in setOf("opus", "vorbis") }),
            Triple("ts,mpegts", capabilities.video.map { it.codec }.filter { it in setOf("h264", "hevc") }, capabilities.audio.map { it.codec }.filter { it in setOf("aac", "mp3", "ac3", "eac3", "dts", "dca") }))) {
            if (codecs.isNotEmpty()) add(buildJsonObject {
                put("Type", "Video"); put("Container", containers); put("VideoCodec", codecs.joinToString(","))
                put("AudioCodec", audioCodecs.joinToString(",").ifBlank { "none" })
            })
        }
    }
    // HLS MPEG-TS can copy AVC/HEVC while converting only unsupported audio.
    // AV1/VP9 stay available for direct playback; TS fallback uses AVC.
    putJsonArray("TranscodingProfiles") { add(buildJsonObject {
        put("Type", "Video"); put("Container", "ts"); put("Protocol", "hls")
        put("VideoCodec", listOf("h264", "hevc").filter { codec -> capabilities.video.any { it.codec == codec } }.joinToString(",").ifBlank { "h264" })
        put("AudioCodec", "aac")
        put("MaxAudioChannels", (capabilities.audio.firstOrNull { it.codec == "aac" }?.channels ?: 2).toString())
        put("MinSegments", 2); put("SegmentLength", 3); put("CopyTimestamps", false)
        put("EnableSubtitlesInManifest", false)
    }) }
    putJsonArray("CodecProfiles") {
        capabilities.video.forEach { video -> add(buildJsonObject {
            put("Type", "Video"); put("Codec", video.codec)
            putJsonArray("Conditions") {
                add(condition("Width", video.width.toString()))
                add(condition("Height", video.height.toString()))
                add(condition("VideoBitDepth", video.bitDepth.toString()))
                video.level?.let { add(condition("VideoLevel", it.toString())) }
                if (video.profiles.isNotEmpty()) add(condition("VideoProfile", video.profiles.joinToString("|"), "EqualsAny"))
                add(condition("VideoRangeType", video.ranges.joinToString("|"), "EqualsAny"))
            }
        }) }
        capabilities.audio.forEach { audio -> add(buildJsonObject {
            put("Type", "VideoAudio"); put("Codec", audio.codec)
            putJsonArray("Conditions") { add(condition("AudioChannels", audio.channels.toString())) }
        }) }
    }
    putJsonArray("SubtitleProfiles") {
        add(buildJsonObject { put("Format", "vtt"); put("Method", "External") })
        for (format in listOf("pgssub", "dvdsub", "dvbsub")) add(buildJsonObject { put("Format", format); put("Method", "Encode") })
    }
}

private fun condition(property: String, value: String, comparison: String = "LessThanEqual") = buildJsonObject {
    put("Condition", comparison); put("Property", property); put("Value", value); put("IsRequired", false)
}
