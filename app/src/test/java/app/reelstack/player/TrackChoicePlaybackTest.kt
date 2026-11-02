package app.reelstack.player

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SubtitleLanguage
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import app.reelstack.data.repository.RememberedSubtitle
import app.reelstack.data.repository.RememberedTrack
import app.reelstack.data.repository.TrackChoices
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the player asks the server for when the series remembers how it was being watched.
 *
 * The first answer from the server says which tracks this file has; only then can the remembered
 * description be matched against them. When the match differs from what the server chose, the
 * stream has to be negotiated again with the matched tracks, because a transcode is built for one
 * audio track and one subtitle.
 */
class TrackChoicePlaybackTest {
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://media.example/jellyfin", "synthetic-token", "u1")
    private val episode = PlayableItem("ep2", "Serie", "Episode", seriesId = "show", durationMs = 20_000)

    private class Fixture : JsonHttpTransport {
        val payloads = mutableListOf<JsonObject>()
        val body = """{"PlaySessionId":"session","MediaSources":[{"Id":"source","SupportsDirectPlay":true,
            "DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":4,"MediaStreams":[
            {"Index":0,"Type":"Video","Codec":"h264"},
            {"Index":1,"Type":"Audio","Codec":"eac3","Language":"eng","Channels":6,"DisplayTitle":"English"},
            {"Index":2,"Type":"Audio","Codec":"eac3","Language":"jpn","Channels":2,"DisplayTitle":"Japanese"},
            {"Index":3,"Type":"Subtitle","Codec":"subrip","Language":"eng","DisplayTitle":"English","IsTextSubtitleStream":true},
            {"Index":4,"Type":"Subtitle","Codec":"subrip","Language":"nor","DisplayTitle":"Norwegian","IsTextSubtitleStream":true},
            {"Index":5,"Type":"Subtitle","Codec":"subrip","Language":"nor","DisplayTitle":"Norwegian Forced","IsForced":true,"IsTextSubtitleStream":true},
            {"Index":6,"Type":"Subtitle","Codec":"subrip","Language":"eng","Title":"SDH","DisplayTitle":"English SDH","IsHearingImpaired":true,"IsTextSubtitleStream":true}]}]}"""
        override fun get(url: String, headers: Map<String, String>) = HttpResponse(200, "{}")
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            payloads += Json.parseToJsonElement(jsonBody).jsonObject
            return HttpResponse(200, body)
        }
    }

    private fun prepare(
        fixture: Fixture, remembered: TrackChoices? = null, audio: Int? = null, subtitle: Int? = null,
    ) = MediaPlaybackClient(fixture, "device").prepare(connection, "u1", episode, 4_000_000, audio, subtitle,
        PlaybackCompatibility.DIRECT, null, SubtitleLanguage.SERVER, SubtitleLanguage.NONE, remembered)

    private fun track(language: String?, forced: Boolean = false, sdh: Boolean = false, title: String? = null,
        codec: String? = null, channels: Int = 0, position: Int = -1) =
        RememberedTrack(language, title, forced, sdh, codec, channels, position)

    private fun JsonObject.number(key: String) = this[key]?.jsonPrimitive?.int

    @Test fun withNothingRememberedTheServersChoiceStandsAndNothingIsAskedTwice() {
        val fixture = Fixture()
        val plan = prepare(fixture)
        assertEquals(1, plan.audioIndex)
        assertEquals(4, plan.subtitleIndex)
        assertEquals(1, fixture.payloads.size)
        // The tracks now carry what is needed to describe them later.
        val japanese = plan.audio.single { it.index == 2 }
        assertEquals("jpn", japanese.language)
        assertEquals("eac3", japanese.codec)
        assertEquals(2, japanese.channels)
        assertEquals(true, plan.subtitles.single { it.index == 5 }.forced)
        assertEquals(true, plan.subtitles.single { it.index == 6 }.hearingImpaired)
        assertEquals("SDH", plan.subtitles.single { it.index == 6 }.title)
        assertNull(plan.audio.single { it.index == 1 }.title)
    }

    @Test fun aRememberedAudioLanguageIsNegotiatedAndPlayed() {
        val fixture = Fixture()
        val plan = prepare(fixture, TrackChoices(audio = track("jpn")))
        assertEquals(2, plan.audioIndex)
        // The subtitle was not remembered, so the server's choice stays.
        assertEquals(4, plan.subtitleIndex)
        assertEquals(2, fixture.payloads.size)
        assertEquals(2, fixture.payloads.last().number("AudioStreamIndex"))
        assertEquals(4, fixture.payloads.last().number("SubtitleStreamIndex"))
    }

    @Test fun aRememberedAudioTrackThatIsTheServersChoiceCostsNoSecondRequest() {
        val fixture = Fixture()
        val plan = prepare(fixture, TrackChoices(audio = track("eng")))
        assertEquals(1, plan.audioIndex)
        assertEquals(1, fixture.payloads.size)
    }

    @Test fun subtitlesRememberedAsOffStayOffAgainstTheServersDefault() {
        val fixture = Fixture()
        val plan = prepare(fixture, TrackChoices(subtitle = RememberedSubtitle.Off))
        assertEquals(-1, plan.subtitleIndex)
        assertNull(plan.subtitleUrl)
        assertEquals(2, fixture.payloads.size)
        assertEquals(-1, fixture.payloads.last().number("SubtitleStreamIndex"))
    }

    @Test fun aRememberedSubtitleIsMatchedOnLanguageAndForcedAndSdh() {
        val forced = prepare(Fixture(), TrackChoices(subtitle = RememberedSubtitle.Track(track("nor", forced = true))))
        assertEquals(5, forced.subtitleIndex)
        val full = prepare(Fixture(), TrackChoices(subtitle = RememberedSubtitle.Track(track("nor"))))
        assertEquals(4, full.subtitleIndex)
        val sdh = prepare(Fixture(), TrackChoices(subtitle = RememberedSubtitle.Track(track("eng", sdh = true))))
        assertEquals(6, sdh.subtitleIndex)
        val plain = prepare(Fixture(), TrackChoices(subtitle = RememberedSubtitle.Track(track("eng"))))
        assertEquals(3, plain.subtitleIndex)
    }

    @Test fun aRememberedLanguageTheFileDoesNotHaveFallsBackToTheDefaultNotToOff() {
        val fixture = Fixture()
        val plan = prepare(fixture, TrackChoices(audio = track("fra"), subtitle = RememberedSubtitle.Track(track("fra"))))
        assertEquals(1, plan.audioIndex)
        assertEquals(4, plan.subtitleIndex)
        assertEquals(1, fixture.payloads.size)
    }

    @Test fun whatTheTitlePageChoseOutranksWhatTheSeriesRemembers() {
        val fixture = Fixture()
        val remembered = TrackChoices(audio = track("jpn"), subtitle = RememberedSubtitle.Off)
        val plan = prepare(fixture, remembered, audio = 1, subtitle = 3)
        assertEquals(1, plan.audioIndex)
        assertEquals(3, plan.subtitleIndex)
        assertEquals(1, fixture.payloads.size)
        // Only one of the two was asked for explicitly: the other still follows the series.
        val half = prepare(Fixture(), remembered, audio = 1)
        assertEquals(1, half.audioIndex)
        assertEquals(-1, half.subtitleIndex)
        val other = prepare(Fixture(), remembered, subtitle = 3)
        assertEquals(2, other.audioIndex)
        assertEquals(3, other.subtitleIndex)
    }

    @Test fun anExplicitOffFromTheTitlePageIsAnExplicitChoiceToo() {
        val plan = prepare(Fixture(), TrackChoices(subtitle = RememberedSubtitle.Track(track("nor"))), subtitle = -1)
        assertEquals(-1, plan.subtitleIndex)
    }
}
