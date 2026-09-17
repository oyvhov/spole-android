package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DevicePlaybackProfileTest {
    private val capable = DevicePlaybackCapabilities(
        listOf(VideoPlaybackCapability("h264", 3840, 2160, profiles = listOf("main", "high"), level = 51),
            VideoPlaybackCapability("hevc", 3840, 2160, 10, listOf("main", "main 10"), 153, listOf("SDR", "HDR10", "HLG")),
            VideoPlaybackCapability("av1", 3840, 2160, 10)),
        listOf(AudioPlaybackCapability("aac", 8), AudioPlaybackCapability("truehd", 8, true), AudioPlaybackCapability("ac3", 6, true)),
    )
    private fun conditions(profile: JsonObject, codec: String) = profile.objects("CodecProfiles").first { it.str("Codec") == codec }.objects("Conditions")
    @Test fun capableTvAdvertises4kHevcAv1AndSeparateAudioChannelLimits() {
        val profile = devicePlaybackProfile(120_000_000, capable)
        assertEquals("h264,hevc,av1", profile.objects("DirectPlayProfiles").first().str("VideoCodec"))
        assertEquals("3840", conditions(profile, "hevc").first { it.str("Property") == "Width" }.str("Value"))
        assertEquals("8", conditions(profile, "truehd").single().str("Value"))
        assertEquals("6", conditions(profile, "ac3").single().str("Value"))
        assertEquals("6", profile.objects("TranscodingProfiles").single().str("MaxAudioChannels"))
        assertEquals("Streaming", profile.objects("TranscodingProfiles").single().str("Context"))
    }
    @Test fun soundTheSetCanPassThroughIsAskedForBeforeAacSoSurroundSurvivesTheTv() {
        val profile = devicePlaybackProfile(120_000_000, capable)
        // AAC 5.1 is decoded by the television and leaves it as whatever its own output carries,
        // which over ARC is two channels; AC3 passes through the set untouched.
        assertEquals("ac3,aac", profile.objects("TranscodingProfiles").single().str("AudioCodec"))
        // FFmpeg's AC-3 encoder stops at 5.1 even though the set will pass 8 channels through.
        assertEquals("6", profile.objects("TranscodingProfiles").single().str("MaxAudioChannels"))
    }
    @Test fun aDeviceThatDecodesAc3WithoutPassingItThroughStillGetsAac() {
        val decodeOnly = capable.copy(audio = listOf(AudioPlaybackCapability("aac", 8), AudioPlaybackCapability("ac3", 6, passthrough = false)))
        val profile = devicePlaybackProfile(120_000_000, decodeOnly)
        assertEquals("aac", profile.objects("TranscodingProfiles").single().str("AudioCodec"))
        assertEquals("8", profile.objects("TranscodingProfiles").single().str("MaxAudioChannels"))
    }
    @Test fun aPhoneWithNoSurroundOutputIsUnchanged() {
        val profile = devicePlaybackProfile(4_000_000, DevicePlaybackCapabilities.CONSERVATIVE)
        assertEquals("aac", profile.objects("TranscodingProfiles").single().str("AudioCodec"))
        assertEquals("2", profile.objects("TranscodingProfiles").single().str("MaxAudioChannels"))
    }
    @Test fun eac3IsOfferedWhenTheSetPassesItAndAc3IsNotAdvertisedAtAll() {
        val eac3Only = capable.copy(audio = listOf(AudioPlaybackCapability("aac", 8), AudioPlaybackCapability("eac3", 8, passthrough = true)))
        assertEquals(listOf("eac3", "aac"), transcodeAudioCodecs(eac3Only))
        assertEquals(6, transcodeAudioChannels(eac3Only))
    }
    @Test fun sdrDisplayDoesNotAdvertiseHdrOrDolbyVisionFromTenBitDecoderAlone() {
        val profile = devicePlaybackProfile(40_000_000, capable.copy(video = listOf(VideoPlaybackCapability("hevc", 3840, 2160, 10))))
        assertEquals("SDR", conditions(profile, "hevc").first { it.str("Property") == "VideoRangeType" }.str("Value"))
    }
    @Test fun unsupportedAudioAndVideoCodecsAreNotAdvertised() {
        val profile = devicePlaybackProfile(4_000_000, DevicePlaybackCapabilities.CONSERVATIVE)
        assertEquals("h264", profile.objects("DirectPlayProfiles").first().str("VideoCodec"))
        assertEquals("aac,mp3", profile.objects("DirectPlayProfiles").first().str("AudioCodec"))
        assertEquals(2, DevicePlaybackCapabilities.CONSERVATIVE.maxAudioChannels)
    }
    @Test fun hlsCanCopyHevcButNeverClaimsAv1InMpegTs() {
        val profile = devicePlaybackProfile(80_000_000, capable)
        assertEquals("h264,hevc", profile.objects("TranscodingProfiles").single().str("VideoCodec"))
        val ts = profile.objects("DirectPlayProfiles").first { it.str("Container") == "ts,mpegts" }
        assertEquals("h264,hevc", ts.str("VideoCodec"))
        assertEquals("aac,ac3", ts.str("AudioCodec"))
        assertFalse(ts.str("AudioCodec").contains("truehd"))
    }
    @Test fun unmeteredEthernetAndWifiDoNotUseTheOldMobileCeiling() {
        assertEquals(120_000_000, automaticPlaybackBitrate(true))
        assertEquals(4_000_000, automaticPlaybackBitrate(false))
    }
    private class Server : JsonHttpTransport {
        val calls = mutableListOf<JsonObject>()
        var imageSubtitle = false
        var direct = true
        override fun get(url: String, headers: Map<String,String>) = error("Unexpected read")
        override fun post(url: String, headers: Map<String,String>, jsonBody: String): HttpResponse {
            val request = Json.parseToJsonElement(jsonBody).jsonObject; calls += request
            return HttpResponse(200, """{"PlaySessionId":"session","MediaSources":[{"Id":"source","SupportsDirectPlay":$direct,
                "TranscodingUrl":"/Videos/item/master.m3u8","DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":${if(imageSubtitle) 2 else -1},
                "MediaStreams":[{"Type":"Audio","Index":1},{"Type":"Audio","Index":3},{"Type":"Subtitle","Index":2,"Codec":"${if(imageSubtitle) "pgssub" else "srt"}"}]}]}""")
        }
    }
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture", "user")
    private val item = PlayableItem("item", "Fixture", "Movie")
    @Test fun explicitAudioAndTextChoicesCanRemainDirectAndRetainIndices() {
        val server = Server()
        val plan = JellyfinPlaybackClient(server, "device", { capable }).prepare(connection, "user", item, 80_000_000, audio=3, subtitle=2)
        assertTrue(plan.direct); assertEquals(3, plan.audioIndex); assertEquals(2, plan.subtitleIndex)
        assertEquals(JsonPrimitive(true), server.calls.single()["EnableDirectPlay"])
        assertEquals(JsonPrimitive(8), server.calls.single()["MaxAudioChannels"])
    }
    @Test fun audioRouteRejectionKeepsSupportedVideoAndConvertsOnlyAudio() {
        val server = Server()
        val plan = JellyfinPlaybackClient(server, "device", { capable }, { _, _ -> false }).prepare(connection, "user", item, 20_000_000, audio=3)
        assertFalse(plan.direct); assertEquals(2, server.calls.size)
        val retry = server.calls.last()
        assertEquals(JsonPrimitive(false), retry["EnableDirectPlay"])
        assertEquals(JsonPrimitive(true), retry["EnableDirectStream"])
        assertEquals(JsonPrimitive(true), retry["AllowVideoStreamCopy"])
        assertEquals(JsonPrimitive(false), retry["AllowAudioStreamCopy"])
        assertEquals(JsonPrimitive(20_000_000), retry["MaxStreamingBitrate"])
        assertEquals(JsonPrimitive(3), retry["AudioStreamIndex"])
        assertEquals(JsonPrimitive("source"), retry["MediaSourceId"])
        assertEquals(JsonPrimitive(8), retry["MaxAudioChannels"])
    }
    @Test fun imageSubtitlesCannotLoopWhenServerStillClaimsDirectPlay() {
        val server = Server().apply { imageSubtitle=true }
        val plan = JellyfinPlaybackClient(server,"device",{capable}).prepare(connection,"user",item,80_000_000)
        assertFalse(plan.direct); assertEquals(2, server.calls.size)
        assertEquals(JsonPrimitive(2), server.calls.last()["SubtitleStreamIndex"])
    }
    @Test fun changedAudioRouteIsReadAgainForTheNextNegotiation() {
        val server=Server(); var detected=capable
        val client=JellyfinPlaybackClient(server,"device",{detected})
        client.prepare(connection,"user",item,80_000_000)
        detected=DevicePlaybackCapabilities.CONSERVATIVE
        client.prepare(connection,"user",item,80_000_000)
        assertEquals(JsonPrimitive(8),server.calls.first()["MaxAudioChannels"])
        assertEquals(JsonPrimitive(2),server.calls.last()["MaxAudioChannels"])
    }
    @Test fun detectionFailureUsesSafeProfileInsteadOfFailingPlayback() {
        val server=Server()
        JellyfinPlaybackClient(server,"device",{error("device query failed")}).prepare(connection,"user",item,4_000_000)
        assertEquals(JsonPrimitive(2),server.calls.single()["MaxAudioChannels"])
    }
    @Test fun hlsCannotCopyVideoThatFailsTheActualDecoderCheck() {
        val server=Server().apply { direct=false }
        JellyfinPlaybackClient(server,"device",{capable},videoSupported={false}).prepare(connection,"user",item,80_000_000)
        assertEquals(2,server.calls.size)
        assertEquals(JsonPrimitive(false),server.calls.last()["AllowVideoStreamCopy"])
    }
    @Test fun hlsCanPreserveSupportedVideoWhenOnlyAudioNeedsConversion() {
        val server=Server().apply { direct=false }
        JellyfinPlaybackClient(server,"device",{capable},sourceSupported={_,_->false},videoSupported={true}).prepare(connection,"user",item,80_000_000)
        assertEquals(1,server.calls.size)
        assertEquals(JsonPrimitive(true),server.calls.single()["AllowVideoStreamCopy"])
    }
}
