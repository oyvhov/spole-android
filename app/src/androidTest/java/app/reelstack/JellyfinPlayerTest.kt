package app.reelstack

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.player.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** Only synthetic accounts on isolated test devices. Actual decoding over loopback HTTP. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class JellyfinPlayerTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private class Server(val assets: android.content.res.AssetManager, val hls: Boolean = false, val resumeMs: Long = 0,
        val episode: Boolean = false, val defaultSubtitle: Int = 2, val audioCodec: String = "aac",
        val rejectSubtitles: Boolean = false, val subtitleDelayMs: Long = 0, val pgs: Boolean = false,
        val syncProgress: Boolean = false, val subtitleCodec: String = "srt", val hugeAss: Boolean = false) : AutoCloseable {
        val socket = ServerSocket(0)
        val base = "http://127.0.0.1:${socket.localPort}"
        val pool = Executors.newCachedThreadPool()
        val events = CopyOnWriteArrayList<Pair<String, JsonObject>>()
        val requests = CopyOnWriteArrayList<String>()
        val clientHeaders = CopyOnWriteArrayList<String>()
        @Volatile var rejectVideo = false
        @Volatile var rejectReports = false
        @Volatile var savedPositionTicks = resumeMs * 10_000
        @Volatile var savedPlayed = false
        init { pool.execute { while (!socket.isClosed) runCatching { val client = socket.accept(); pool.execute {
            client.use { conn -> runCatching {
                conn.soTimeout = 5000
                val reader = conn.getInputStream().buffered()
                fun readHeaderLine(): String? {
                    val bytes = java.io.ByteArrayOutputStream()
                    while (true) {
                        val next = reader.read()
                        if (next < 0) return if (bytes.size() == 0) null else bytes.toString("UTF-8").trimEnd('\r')
                        if (next == 10) return bytes.toString("UTF-8").trimEnd('\r')
                        bytes.write(next)
                    }
                }
                val line = readHeaderLine() ?: return@runCatching
                val path = line.split(' ')[1]
                requests += path
                val headers = mutableMapOf<String,String>()
                while (true) { val value = readHeaderLine() ?: break; if (value.isBlank()) break
                    headers[value.substringBefore(':').lowercase()] = value.substringAfter(':').trim() }
                clientHeaders += headers["authorization"].orEmpty()
                // HTTP Content-Length counts UTF-8 bytes, not characters (e.g. the profile's middle dot).
                val chars = ByteArray(headers["content-length"]?.toIntOrNull() ?: 0)
                var count = 0
                while (count < chars.size) { val n = reader.read(chars, count, chars.size-count); if (n < 0) break; count += n }
                val body = if (chars.isEmpty()) JsonObject(emptyMap()) else Json.parseToJsonElement(chars.toString(Charsets.UTF_8)).jsonObject
                var status = 200
                var contentType = "application/json"
                var bytes = when {
                    syncProgress && path.contains("/UserData") -> {
                        events += path to body
                        savedPositionTicks = body.getValue("PlaybackPositionTicks").jsonPrimitive.long
                        savedPlayed = body.getValue("Played").jsonPrimitive.boolean
                        status = 204
                        byteArrayOf()
                    }
                    path == "/Users/Me" || path == "/Users/u1" -> """{"Id":"u1","Name":"Testperson","Policy":{"EnableMediaPlayback":true,"IsAdministrator":false}}""".toByteArray()
                    episode && path.contains("adjacentTo=") -> """{"Items":[
                        {"Id":"film","Type":"Episode","SeriesId":"series","Name":"Første","RunTimeTicks":200000000},
                        {"Id":"next","Type":"Episode","SeriesId":"series","Name":"Neste","RunTimeTicks":200000000}]}""".toByteArray()
                    episode && path.substringBefore('?').endsWith("/Items/film") ->
                        """{"Id":"film","Type":"Episode","SeriesId":"series","Name":"Første","RunTimeTicks":200000000}""".toByteArray()
                    path.contains("/Items/series") -> """{"Id":"series","Type":"Series","Name":"Testserie"}""".toByteArray()
                    path.startsWith("/Shows/") -> """{"Items":[{"Id":"season","Type":"Season","Name":"Sesong 1","LocationType":"Virtual"}],"TotalRecordCount":1}""".toByteArray()
                    path.startsWith("/Items?") -> """{"Items":[{"Id":"film","Type":"Episode","Name":"Ny dag","SeriesName":"Testserie","ParentIndexNumber":1,"IndexNumber":1,"RunTimeTicks":200000000},{"Id":"missing","Type":"Episode","IsMissing":true}],"TotalRecordCount":2}""".toByteArray()
                    syncProgress && path.contains("/Items/") && !path.contains("PlaybackInfo") ->
                        """{"Id":"film","Type":"Movie","Name":"Spole testvideo","RunTimeTicks":200000000,"UserData":{"PlaybackPositionTicks":$savedPositionTicks,"Played":$savedPlayed,"PlayCount":1,"IsFavorite":true}}""".toByteArray()
                    path.contains("/Items/") && !path.contains("PlaybackInfo") -> """{"Id":"film","Type":"Movie","Name":"Spole testvideo","RunTimeTicks":200000000,"UserData":{"PlaybackPositionTicks":${resumeMs*10000}}}""".toByteArray()
                    path.endsWith("PlaybackInfo") -> {
                        events += path to body
                        val direct = !hls && body["EnableDirectPlay"] != JsonPrimitive(false)
                        val source = if (path.contains("/Items/next/")) "next-source" else "source"
                        val sub = (body["SubtitleStreamIndex"] as? JsonPrimitive)?.intOrNull ?: defaultSubtitle
                        """{"PlaySessionId":"session${events.size}","MediaSources":[{"Id":"$source","SupportsDirectPlay":$direct,"TranscodingUrl":"/hls/master.m3u8?api_key=fixture","DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":$sub,"MediaStreams":[{"Index":0,"Type":"Video","Codec":"h264","Profile":"baseline","Width":640,"Height":360,"BitDepth":8,"VideoRangeType":"SDR","AverageFrameRate":24},{"Index":1,"Type":"Audio","Codec":"aac","Channels":2,"SampleRate":48000,"DisplayTitle":"English","Language":"eng"},{"Index":3,"Type":"Audio","Codec":"aac","Channels":2,"SampleRate":48000,"DisplayTitle":"Norsk","Language":"nor"},{"Index":2,"Type":"Subtitle","DisplayTitle":"Norsk tekst","Language":"nor","Codec":"$subtitleCodec","IsTextSubtitleStream":true}]}]}""".toByteArray()
                    }
                    path.startsWith("/Sessions/Playing") -> {
                        events += path to body
                        status = if (rejectReports) 503 else 204
                        if (syncProgress && !rejectReports) savedPositionTicks = body.getValue("PositionTicks").jsonPrimitive.long
                        byteArrayOf()
                    }
                    path.contains("/Subtitles/") -> {
                        if (subtitleDelayMs > 0) Thread.sleep(subtitleDelayMs)
                        contentType="text/vtt"
                        if (rejectSubtitles) { status = 503; byteArrayOf() }
                        else if (hugeAss && path.substringBefore('?').endsWith("/Stream.ass")) {
                            // Heavily typeset ASS can be several MiB: larger than the libass document bound.
                            contentType = "text/x-ssa"
                            buildString {
                                append("[Script Info]\nScriptType: v4.00+\n\n[Events]\n")
                                append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n")
                                while (length < 3 * 1024 * 1024) append("Dialogue: 0,0:00:00.00,0:00:20.00,Default,,0,0,0,,{\\pos(10,10)}Typesetting\n")
                            }.toByteArray()
                        }
                        else assets.open("player/subtitle.vtt").use { it.readBytes() }
                    }
                    else -> {
                        if (rejectVideo) { status=503; byteArrayOf() } else {
                            val name = if (path.startsWith("/hls/")) path.substringAfterLast('/').substringBefore('?')
                                else if (pgs) "pgs.mkv" else if (audioCodec != "aac") "$audioCodec.mkv" else "video.mp4"
                            contentType = when {
                                name.endsWith("m3u8") -> "application/vnd.apple.mpegurl"
                                name.endsWith("ts") -> "video/mp2t"
                                name.endsWith("mkv") -> "video/x-matroska"
                                else -> "video/mp4"
                            }
                            assets.open("player/$name").use { it.readBytes() }
                        }
                    }
                }
                if (path.endsWith("PlaybackInfo") && audioCodec != "aac") {
                    bytes = bytes.toString(Charsets.UTF_8)
                        .replace("\"Codec\":\"aac\",\"Channels\":2", "\"Codec\":\"$audioCodec\",\"Channels\":6")
                        .replace(",{\"Index\":3,\"Type\":\"Audio\",\"Codec\":\"$audioCodec\",\"Channels\":6,\"SampleRate\":48000,\"DisplayTitle\":\"Norsk\",\"Language\":\"nor\"}", "")
                        .toByteArray()
                }
                if (path.endsWith("PlaybackInfo") && pgs) {
                    val response = Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
                    val sources = response.getValue("MediaSources").jsonArray.map { value ->
                        val source = value.jsonObject
                        JsonObject(source + mapOf("Container" to JsonPrimitive("mkv"), "MediaStreams" to JsonArray(
                            source.getValue("MediaStreams").jsonArray.filter { it.jsonObject["Index"] != JsonPrimitive(3) }.map { stream ->
                                if (stream.jsonObject["Type"] == JsonPrimitive("Subtitle")) JsonObject(stream.jsonObject + mapOf(
                                    "Codec" to JsonPrimitive("pgssub"), "IsTextSubtitleStream" to JsonPrimitive(false), "IsExternal" to JsonPrimitive(false)))
                                else stream
                            })))
                    }
                    bytes = JsonObject(response + ("MediaSources" to JsonArray(sources))).toString().toByteArray()
                }
                val total = bytes.size
                val range = headers["range"]?.substringAfter("bytes=")?.substringBefore('-')?.toIntOrNull()
                val rangeHeader = if (status == 200 && range != null && range < total) {
                    bytes = bytes.copyOfRange(range,total); status=206; "Content-Range: bytes $range-${total-1}/$total\r\n"
                } else ""
                val response = "HTTP/1.1 $status OK\r\nContent-Type: $contentType\r\nContent-Length: ${bytes.size}\r\nAccept-Ranges: bytes\r\n${rangeHeader}Connection: close\r\n\r\n"
                conn.getOutputStream().apply { write(response.toByteArray()); write(bytes); flush() }
            } }
        } } } }
        override fun close() { socket.close(); pool.shutdownNow() }
    }

    private fun exercise(hls: Boolean = false, resumeMs: Long = 0, root: String = "film", autoResume: Boolean = true,
        episode: Boolean = false, defaultSubtitle: Int = 2, audioCodec: String = "aac", kind: ServiceKind = ServiceKind.JELLYFIN,
        rejectSubtitles: Boolean = false, subtitleDelayMs: Long = 0, pgs: Boolean = false, syncProgress: Boolean = false,
        subtitleCodec: String = "srt", hugeAss: Boolean = false,
        block: (ActivityScenario<JellyfinPlayerActivity>, Server, ConnectionRepository) -> Unit) {
        Server(instrumentation.context.assets, hls, resumeMs, episode, defaultSubtitle, audioCodec, rejectSubtitles, subtitleDelayMs, pgs, syncProgress,
            subtitleCodec, hugeAss).use { server ->
            val connections = (context.applicationContext as ReelstackApplication).container.connectionRepository
            ServiceKind.entries.forEach(connections::delete)
            connections.save(ServiceConnection(kind,"Test",server.base,"fixture","u1"))
            val preferences = (context.applicationContext as ReelstackApplication).container.preferencesRepository
            val original = preferences.personalization
            preferences.personalization = original.copy(autoResume = autoResume,
                preferredSubtitleLanguage = SubtitleLanguage.SERVER,
                fallbackSubtitleLanguage = SubtitleLanguage.NONE).let {
                if (episode) it.copy(autoPlayNextEpisode = true, showNextEpisode = true,
                    nextEpisodeLeadSeconds = 10, nextEpisodeDelaySeconds = 5) else it
            }
            try {
                ActivityScenario.launch<JellyfinPlayerActivity>(Intent(context,JellyfinPlayerActivity::class.java)
                    .putExtra("jellyfin_item_id",root).putExtra("media_service_kind", kind.name)).use { scenario ->
                    block(scenario,server,connections)
                }
                waitFor { server.events.any { it.first.endsWith("/Stopped") } || server.rejectVideo }
                assertTrue(server.requests.none { it.contains("api_key") || it.contains("token=") })
            } finally {
                preferences.personalization = original
                ServiceKind.entries.forEach(connections::delete)
            }
        }
    }
    private fun waitFor(timeout: Long = 20_000, condition: () -> Boolean) {
        val deadline=SystemClock.elapsedRealtime()+timeout
        while(SystemClock.elapsedRealtime()<deadline) { if(condition()) return; SystemClock.sleep(100) }
        assertTrue("Condition timed out",condition())
    }
    private fun snapshot(s: ActivityScenario<JellyfinPlayerActivity>): PlayerScreenState {
        var value=PlayerScreenState(); s.onActivity { value=it.model.state.value }; return value
    }
    private fun playing(s: ActivityScenario<JellyfinPlayerActivity>) = waitFor { snapshot(s).let { it.playing && it.positionMs > 600 } }

    @Test fun dtsSurroundPlaysDirectlyFromBothServersAndSurvivesSeeking() = localAudio("dts")
    @Test fun eac3SurroundPlaysDirectlyFromBothServersAndSurvivesSeeking() = localAudio("eac3")
    @Test fun trueHdSurroundPlaysDirectlyFromBothServersAndSurvivesSeeking() = localAudio("truehd")
    @Test fun ac3SurroundPlaysDirectlyFromBothServersAndSurvivesSeeking() = localAudio("ac3")

    @Test fun platformAacFailureSwitchesToFfmpegWithoutServerRenegotiation() = localRecovery("aac")
    @Test fun platformEac3FailureSwitchesToFfmpegWithoutServerRenegotiation() = localRecovery("eac3")
    @Test fun localAudioRecoveryPreservesPauseAndSelectedSubtitle() = localRecovery("eac3", paused = true)

    @Test fun temporaryFocusLossDoesNotPauseVisiblePlayback() = exercise { scenario, _, _ ->
        playing(scenario)
        scenario.moveToState(Lifecycle.State.STARTED)
        SystemClock.sleep(400)
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertTrue(snapshot(scenario).playWhenReady)
        playing(scenario)
    }

    @Test fun networkRecoveryKeepsTheServerSessionAndPosition() {
        for (kind in listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN)) {
            exercise(kind = kind) { scenario, server, _ ->
                playing(scenario)
                scenario.onActivity { it.model.seek(8_000) }
                waitFor { snapshot(scenario).let { it.playing && it.positionMs >= 8_000 } }
                val negotiations = server.events.count { it.first.endsWith("PlaybackInfo") }
                val stops = server.events.count { it.first.endsWith("/Stopped") }
                scenario.onActivity {
                    // Model sees the same idle state as after Media3 exhausts source retries.
                    it.model.player.stop()
                    it.model.handlePlaybackError(androidx.media3.common.PlaybackException("synthetic timeout", null,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
                }
                waitFor { snapshot(scenario).let { it.playing && it.positionMs >= 8_000 } }
                assertEquals(negotiations, server.events.count { it.first.endsWith("PlaybackInfo") })
                assertEquals(stops, server.events.count { it.first.endsWith("/Stopped") })
                assertTrue(snapshot(scenario).direct)
            }
        }
    }

    @Test fun malformedContainerRecoversThroughHlsOnceOnBothServers() = containerRecovery(paused = false)
    @Test fun containerRecoveryPreservesPauseAndTracks() = containerRecovery(paused = true)

    private fun containerRecovery(paused: Boolean) {
        for (kind in listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN)) {
            exercise(kind = kind) { scenario, server, _ ->
                playing(scenario)
                scenario.onActivity { it.model.seek(8_000) }
                waitFor { snapshot(scenario).let { it.playing && it.positionMs >= 8_000 } }
                val negotiations = server.events.count { it.first.endsWith("PlaybackInfo") }
                val audio = snapshot(scenario).audioIndex
                val subtitle = snapshot(scenario).subtitleIndex
                var position = 0L
                val error = androidx.media3.common.PlaybackException("synthetic container failure", null,
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
                scenario.onActivity {
                    if (paused) it.model.player.pause()
                    position = it.model.player.currentPosition
                    it.model.player.stop()
                    it.model.handlePlaybackError(error)
                }
                waitFor { snapshot(scenario).let { !it.direct && !it.busy && it.error == null } }
                val request = server.events.last { it.first.endsWith("PlaybackInfo") }.second
                assertEquals(negotiations + 1, server.events.count { it.first.endsWith("PlaybackInfo") })
                assertEquals(JsonPrimitive(false), request["EnableDirectPlay"])
                assertEquals(JsonPrimitive(true), request["AllowVideoStreamCopy"])
                assertEquals(JsonPrimitive(true), request["AllowAudioStreamCopy"])
                assertEquals(JsonPrimitive("source"), request["MediaSourceId"])
                assertEquals(audio, snapshot(scenario).audioIndex)
                assertEquals(subtitle, snapshot(scenario).subtitleIndex)
                assertTrue(snapshot(scenario).fallback.endsWith("REMUX"))
                scenario.onActivity {
                    assertEquals(!paused, it.model.player.playWhenReady)
                    assertTrue(it.model.player.currentPosition >= position - 500)
                    if (paused) it.model.player.play()
                }
                playing(scenario)
                assertTrue(server.requests.any { it.startsWith("/hls/") })
                // A second parser error on the replacement stream must stop, not renegotiate.
                scenario.onActivity { it.model.player.stop(); it.model.handlePlaybackError(error) }
                assertTrue(snapshot(scenario).fallback.endsWith("STOPPED"))
                assertNotNull(snapshot(scenario).error)
                SystemClock.sleep(500)
                assertEquals(negotiations + 1, server.events.count { it.first.endsWith("PlaybackInfo") })
            }
        }
    }

    @Test fun videoStartDoesNotForceADisplayModeChange() = exercise { scenario, _, _ ->
        var mode = 0
        scenario.onActivity { mode = it.window.attributes.preferredDisplayModeId }
        playing(scenario)
        scenario.onActivity { assertEquals(mode, it.window.attributes.preferredDisplayModeId) }
    }

    @Test fun failedSubtitleExtractionDoesNotStopTheFilm() {
        for (kind in listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN)) {
            exercise(kind = kind, rejectSubtitles = true) { scenario, server, _ ->
                playing(scenario)
                waitFor { snapshot(scenario).subtitleUnavailable }
                assertNull(snapshot(scenario).error)
                assertTrue(snapshot(scenario).direct)
                assertEquals(1, server.events.count { it.first.endsWith("PlaybackInfo") })
                assertTrue(server.requests.count { it.contains("/Subtitles/") } <= 2)
            }
        }
    }

    @Test fun backCancelsHangingSubtitleExtractionPromptly() = exercise(subtitleDelayMs = 30_000) { scenario, server, _ ->
        waitFor { server.requests.any { it.contains("/Subtitles/") } }
        val start = SystemClock.elapsedRealtime()
        scenario.onActivity { assertFalse(it.model.back()); it.finish() }
        waitFor(3_000) { scenario.state == Lifecycle.State.DESTROYED }
        assertTrue("Back must not wait for subtitle HTTP timeout", SystemClock.elapsedRealtime() - start < 3_000)
    }

    private fun localRecovery(codec: String, paused: Boolean = false) {
        for (kind in listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN)) {
            exercise(audioCodec = codec, kind = kind, defaultSubtitle = if (paused) 2 else -1) { scenario, server, _ ->
                playing(scenario)
                scenario.onActivity { it.model.seek(8_000) }
                waitFor { snapshot(scenario).let { it.playing && it.positionMs >= 8_000 } }
                val negotiations = server.events.count { it.first.endsWith("PlaybackInfo") }
                val stops = server.events.count { it.first.endsWith("/Stopped") }
                val audio = snapshot(scenario).audioIndex
                val subtitle = snapshot(scenario).subtitleIndex
                var position = 0L
                scenario.onActivity {
                    val model = it.model
                    if (paused) model.player.pause()
                    position = model.player.currentPosition
                    val format = model.player.audioFormat!!
                    // Inject the Pixel-shaped runtime failure, then use the real new renderer,
                    // real media source, decoding and output. No server transcode is available.
                    model.handlePlaybackError(androidx.media3.exoplayer.ExoPlaybackException.createForRenderer(
                        IllegalStateException("synthetic platform audio failure"), "MediaCodecAudioRenderer", 1,
                        format, androidx.media3.common.C.FORMAT_HANDLED, null, false,
                        androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED))
                }
                waitFor { snapshot(scenario).let { it.audioDecoder.startsWith("ffmpeg", true) && !it.busy } }
                if (!paused) waitFor { snapshot(scenario).let { it.playing && it.positionMs >= position + 500 } }
                else scenario.onActivity {
                    assertFalse(it.model.player.playWhenReady)
                    assertEquals(position, it.model.player.currentPosition)
                    it.model.player.play()
                }
                playing(scenario)
                scenario.onActivity { assertTrue(it.model.player.audioDecoderCounters!!.renderedOutputBufferCount > 0) }
                assertTrue(snapshot(scenario).direct)
                assertNull(snapshot(scenario).error)
                assertTrue(snapshot(scenario).fallback.endsWith("LOCAL_FFMPEG"))
                assertEquals(audio, snapshot(scenario).audioIndex)
                assertEquals(subtitle, snapshot(scenario).subtitleIndex)
                assertEquals(negotiations, server.events.count { it.first.endsWith("PlaybackInfo") })
                assertEquals(stops, server.events.count { it.first.endsWith("/Stopped") })
                assertTrue(server.requests.none { it.contains("master.m3u8") })
                scenario.onActivity { it.model.seek(12_000) }
                waitFor { snapshot(scenario).let { it.playing && it.positionMs >= 12_000 } }
            }
        }
    }

    private fun localAudio(codec: String) {
        assertTrue(androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable())
        for (kind in listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN)) {
            exercise(audioCodec = codec, kind = kind, defaultSubtitle = -1) { scenario, server, _ ->
                playing(scenario)
                assertTrue(snapshot(scenario).direct)
                // A language preference may legitimately negotiate a subtitle before starting.
                val negotiations = server.events.count { it.first.endsWith("PlaybackInfo") }
                assertTrue(server.events.filter { it.first.endsWith("PlaybackInfo") }
                    .all { it.second["EnableDirectPlay"] == JsonPrimitive(true) })
                scenario.onActivity { activity ->
                    assertTrue(activity.model.player.audioDecoderCounters!!.renderedOutputBufferCount > 0)
                    activity.model.seek(8_000)
                }
                waitFor { snapshot(scenario).let { it.playing && it.positionMs >= 8_500 } }
                assertTrue(snapshot(scenario).direct)
                assertNull(snapshot(scenario).error)
                assertEquals("", snapshot(scenario).fallback)
                assertEquals(negotiations, server.events.count { it.first.endsWith("PlaybackInfo") })
                assertTrue(server.requests.none { it.contains("master.m3u8") })
                assertTrue(server.events.filter { it.first.startsWith("/Sessions/Playing") }
                    .all { it.second["PlayMethod"] == JsonPrimitive("DirectPlay") })
            }
        }
    }

    @Test fun countdownWaitsForRealEpisodeEndBeforeAdvancing() = exercise(episode = true) { scenario, server, _ ->
        playing(scenario)
        waitFor { snapshot(scenario).nextEpisode != null }
        val durationMs = snapshot(scenario).durationMs
        scenario.onActivity { it.model.seek(10_500) }
        waitFor { snapshot(scenario).positionMs >= 11_000 }
        assertFalse(snapshot(scenario).ended)
        assertNull("Showing the next-episode offer must not cut the current episode short", snapshot(scenario).nextEpisodeCountdown)
        scenario.onActivity { it.model.seek(18_000) }
        var countdownState = PlayerScreenState()
        waitFor { snapshot(scenario).also { countdownState = it }.nextEpisodeCountdown != null }
        assertTrue("Countdown starts only at EOS: $countdownState", countdownState.ended)
        waitFor { snapshot(scenario).itemId == "next" && snapshot(scenario).playing }
        assertNull(snapshot(scenario).error)
        val stopped = server.events.first { it.first.endsWith("/Stopped") }.second
        val stoppedAt = stopped.getValue("PositionTicks").jsonPrimitive.long
        assertTrue("The episode must finish before advancing; stopped at $stoppedAt ticks", stoppedAt >= durationMs * 10_000)
    }

    @Test fun nextButtonUsesTheNextFilesSourceAndStartsOnlyOnce() = exercise(episode = true) { scenario, server, _ ->
        playing(scenario)
        waitFor { snapshot(scenario).nextEpisode != null }
        scenario.onActivity { it.model.playNext(); it.model.playNext() }
        waitFor { snapshot(scenario).itemId == "next" && snapshot(scenario).playing }
        assertNull(snapshot(scenario).error)
        val starts = server.events.filter { it.first == "/Items/next/PlaybackInfo" }
        assertEquals(1, starts.size)
        assertNull(starts.single().second["MediaSourceId"])
        assertTrue(server.requests.any { it.contains("MediaSourceId=next-source") })
    }

    @Test fun localProgressSurvivesFailedReportsAndImmediatelyLeadsTheHomeShelf() = exercise { scenario, server, connections ->
        playing(scenario)
        server.rejectReports = true
        scenario.onActivity { it.model.seek(12_000) }
        waitFor { snapshot(scenario).positionMs >= 12_000 }
        scenario.moveToState(Lifecycle.State.CREATED)
        val container = (context.applicationContext as ReelstackApplication).container
        val account = connections.get(ServiceKind.JELLYFIN)
        val restored = app.reelstack.data.repository.LocalPlaybackStore(context)
        assertTrue(restored.resume(account, PlayableItem("film", "Film", "Movie", durationMs = 20_000)).resumeMs >= 12_000)
        val store = androidx.lifecycle.ViewModelStore()
        instrumentation.runOnMainSync {
            val model = app.reelstack.ui.ReelstackViewModel(container)
            store.put("home", model)
            model.returnedToApp()
            assertEquals("jellyfin-film", model.uiState.value.resume.first().id)
            assertTrue(model.uiState.value.resume.first().progress!! >= .6f)
            store.clear()
        }
    }

    @Test fun failedLiveReportsAreRetriedAsUserDataAfterPlayerClosesOnBothServers() {
        for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            exercise(kind = kind, defaultSubtitle = -1, syncProgress = true) { scenario, server, connections ->
                playing(scenario)
                val container = (context.applicationContext as ReelstackApplication).container
                val scope = app.reelstack.data.repository.PlaybackProgressOutbox.scope("", connections.get(kind))
                // Wait for the start report/read to finish before disconnecting report delivery.
                waitFor { server.events.any { it.first == "/Sessions/Playing" } }
                server.rejectReports = true
                scenario.onActivity { it.model.seek(12_000); if (it.model.player.playWhenReady) it.model.toggle() }
                waitFor { container.playbackProgressOutbox.pending(scope).any { it.positionMs >= 12_000 } }
                assertTrue(server.events.none { it.first.contains("/UserData") })
                scenario.close()
                waitFor(40_000) { server.events.any { it.first.contains("/UserData") } }
                waitFor { container.playbackProgressOutbox.pending(scope).isEmpty() }
                assertTrue(server.savedPositionTicks >= 120_000_000)
                assertFalse(server.savedPlayed)
                val retried = server.events.single { it.first.contains("/UserData") }.second
                assertFalse(retried.containsKey("PlaySessionId"))
                if (kind == ServiceKind.EMBY) assertEquals(JsonPrimitive(true), retried["IsFavorite"])
            }
        }
    }

    @Test fun dismissingNextOfferKeepsCurrentEpisodeAndPreventsAutoplay() = exercise(episode = true) { scenario, _, _ ->
        playing(scenario)
        waitFor { snapshot(scenario).nextEpisode != null }
        scenario.onActivity { it.model.seek(10_500) }
        waitFor { snapshot(scenario).positionMs >= 11_000 }
        scenario.onActivity { it.model.cancelNextEpisode() }
        scenario.onActivity { it.model.seek(19_000) }
        waitFor { snapshot(scenario).ended }
        SystemClock.sleep(5500)
        assertEquals("film", snapshot(scenario).itemId)
        assertNull(snapshot(scenario).nextEpisodeCountdown)
        assertTrue(snapshot(scenario).nextEpisodeDismissed)
    }

    @Test fun bothHigherQualityOptionsAreAcceptedByThePlayer() = exercise { scenario, _, _ ->
        playing(scenario)
        scenario.onActivity { it.model.quality(20_000_000) }
        waitFor { snapshot(scenario).let { it.quality == 20_000_000 && it.playing } }
        scenario.onActivity { it.model.quality(80_000_000) }
        waitFor { snapshot(scenario).let { it.quality == 80_000_000 && it.playing } }
    }

    @Test fun googleTvRemoteControlsRealVideoAndBackReturnsSafely() {
        org.junit.Assume.assumeTrue(context.getSystemService(android.app.UiModeManager::class.java)
            .currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)
        exercise { scenario, server, _ ->
            playing(scenario)
            waitFor { var focused = false; scenario.onActivity { focused = it.hasWindowFocus() }; focused }
            scenario.onActivity { assertTrue("Real decoder must produce video", it.model.player.videoSize.width > 0) }
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            waitFor { !snapshot(scenario).playing }
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            assertFalse(snapshot(scenario).playing)
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            waitFor { snapshot(scenario).playing }
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            // TV Back first dismisses visible controls; the next Back exits playback.
            waitFor {
                val automation = instrumentation.uiAutomation
                automation.clearCache()
                fun hasPause(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
                    node ?: return false
                    if (node.contentDescription?.toString() == context.getString(R.string.player_pause)) return true
                    return (0 until node.childCount).any { hasPause(node.getChild(it)) }
                }
                val root = automation.rootInActiveWindow
                root != null && !hasPause(root)
            }
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.state)
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            waitFor { scenario.state == Lifecycle.State.DESTROYED }
            waitFor { server.events.any { it.first.endsWith("/Stopped") } }
            assertEquals(1, server.events.count { it.first == "/Sessions/Playing" })
        }
    }

    @Test fun playbackHidesBothSystemBarsAndKeepsVideoInTheWholeWindow() = exercise { scenario,_,_ ->
        playing(scenario)
        waitFor {
            var hidden = false
            scenario.onActivity {
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(it.window.decorView)
                hidden = insets != null && !insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars()) &&
                    !insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            }
            hidden
        }
    }
    @Test fun miniPlayerKeepsVideoRunningAndReturnsWithoutRestarting() {
        org.junit.Assume.assumeTrue(context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE))
        exercise { scenario, server, _ ->
            playing(scenario)
            var fullWidth = 0
            waitFor {
                var ready = false
                scenario.onActivity {
                    fullWidth = it.window.decorView.width
                    ready = it.hasWindowFocus() && fullWidth > 0
                }
                ready
            }
            var positionBeforeMini = 0L
            scenario.onActivity {
                positionBeforeMini = it.model.player.currentPosition
                it.openMiniPlayer()
            }
            // The PiP flag arrives before Android finishes resizing the window. Returning
            // immediately can abort the system transition. Observe a real small window and
            // continued video progress before testing expansion back to fullscreen.
            waitFor {
                var active = false
                scenario.onActivity {
                    active = it.isInPictureInPictureMode && !it.hasWindowFocus() &&
                        it.window.decorView.width in 1 until fullWidth && it.model.player.isPlaying &&
                        it.model.player.currentPosition >= positionBeforeMini + 1_500
                }
                active
            }
            scenario.onActivity { context.startActivity(Intent(context, JellyfinPlayerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)) }
            waitFor {
                var full = false
                scenario.onActivity {
                    full = !it.isInPictureInPictureMode && it.hasWindowFocus() &&
                        it.window.decorView.width == fullWidth && it.model.player.isPlaying
                }
                full
            }
            assertEquals(1, server.events.count { it.first == "/Sessions/Playing" })
        }
    }
    @Test fun directVideoRendersSeeksPausesAndSurvivesRotation() = exercise { scenario,server,_ ->
        playing(scenario)
        assertTrue(server.clientHeaders.isNotEmpty())
        assertTrue(server.clientHeaders.all { it.contains("Client=\"Spole\"") })
        scenario.onActivity { assertTrue(it.model.player.videoSize.width > 0); it.model.seek(10_000) }
        waitFor { snapshot(scenario).positionMs >= 10_000 }
        scenario.onActivity { it.model.toggle() }
        waitFor { !snapshot(scenario).playing }
        val position=snapshot(scenario).positionMs
        scenario.recreate()
        assertFalse(snapshot(scenario).playing)
        assertTrue(kotlin.math.abs(snapshot(scenario).positionMs-position)<1500)
        waitFor { server.events.any { it.first.endsWith("/Progress") && it.second["IsPaused"] == JsonPrimitive(true) } }
        assertEquals(1,server.events.count { it.first == "/Sessions/Playing" })
    }
    @Test fun androidBackClosesVideoAndReportsStopped() = exercise { scenario,server,_ ->
        playing(scenario)
        // Decoder readiness precedes the window transition/IME dismissal. Send Back only to
        // the focused video window, not to the keyboard left by a preceding UI test.
        waitFor {
            var ready = false
            scenario.onActivity {
                ready = it.hasWindowFocus() && androidx.core.view.ViewCompat.getRootWindowInsets(it.window.decorView)
                    // Headless instrumentation can have no insets object at all. That means no
                    // IME is obscuring Back; treating it as "not ready" turns this into a timeout.
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
            }
            ready
        }
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        instrumentation.waitForIdleSync()
        val tv = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (tv && scenario.state != Lifecycle.State.DESTROYED) {
            // TV Back dismisses the OSD before the next press leaves the player.
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        }
        waitFor { scenario.state == Lifecycle.State.DESTROYED }
        waitFor { server.events.any { it.first.endsWith("/Stopped") } }
    }
    @Test fun videoTouchShowsControlsAndPauseReceivesTheNextTap() = exercise { scenario,_,_ ->
        org.junit.Assume.assumeFalse(context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)
        val automation = instrumentation.uiAutomation
        val pauseLabel = context.getString(R.string.player_pause)
        fun freshRoot(): android.view.accessibility.AccessibilityNodeInfo? {
            // AnimatedVisibility recreates nodes; do not assert against UiAutomation's old tree.
            if (android.os.Build.VERSION.SDK_INT >= 33) automation.clearCache()
            return automation.rootInActiveWindow
        }
        fun find(description: String, node: android.view.accessibility.AccessibilityNodeInfo? = freshRoot()): android.view.accessibility.AccessibilityNodeInfo? {
            node ?: return null
            if (node.contentDescription?.toString() == description) return node
            for (index in 0 until node.childCount) find(description,node.getChild(index))?.let { return it }
            return null
        }
        fun tap(x: Float,y: Float) {
            val time = SystemClock.uptimeMillis()
            for (action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
                val event = android.view.MotionEvent.obtain(time,SystemClock.uptimeMillis(),action,x,y,0)
                event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                try { assertTrue(automation.injectInputEvent(event,true)) } finally { event.recycle() }
            }
        }
        playing(scenario)
        // Startup may outlast the initial OSD timeout on a busy emulator. Begin from an
        // explicitly visible layer instead of depending on startup/decoder timing.
        if (find(pauseLabel) == null) {
            val screen = android.graphics.Rect()
            scenario.onActivity { it.window.decorView.getGlobalVisibleRect(screen) }
            tap(screen.left + screen.width() * .85f, screen.top + screen.height() * .35f)
        }
        waitFor { find(pauseLabel) != null }
        waitFor(8_000) { find(pauseLabel) == null && find("Tilbake") == null }
        val bounds = android.graphics.Rect()
        scenario.onActivity { it.window.decorView.getGlobalVisibleRect(bounds) }
        tap(bounds.left + bounds.width() * .85f,bounds.top + bounds.height() * .35f)
        waitFor(2_000) { find(pauseLabel) != null }
        find(pauseLabel)!!.getBoundsInScreen(bounds)
        tap(bounds.exactCenterX(),bounds.exactCenterY())
        waitFor { !snapshot(scenario).playing }
        assertTrue(snapshot(scenario).error == null)
    }
    @Test fun onScreenBackReturnsOnlyWhenTransportControlsAreShown() {
        org.junit.Assume.assumeFalse(context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)
        exercise { scenario,server,_ ->
        val automation = instrumentation.uiAutomation
        fun root(): android.view.accessibility.AccessibilityNodeInfo? {
            if (android.os.Build.VERSION.SDK_INT >= 33) automation.clearCache()
            return automation.rootInActiveWindow
        }
        playing(scenario)
        SystemClock.sleep(3_900) // Real auto-hide deadline, not the Compose test clock.
        fun findBack(node: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
            if (node.contentDescription?.toString() == "Tilbake") return node
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                findBack(child)?.let { return it }
            }
            return null
        }
        assertNull(root()?.let(::findBack))
        val window = android.graphics.Rect()
        scenario.onActivity { it.window.decorView.getGlobalVisibleRect(window) }
        val revealTime = SystemClock.uptimeMillis()
        for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
            val event = android.view.MotionEvent.obtain(revealTime, SystemClock.uptimeMillis(), action,
                window.left + window.width() * .75f, window.top + window.height() * .4f, 0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            try { automation.injectInputEvent(event, true) } finally { event.recycle() }
        }
        // UiAutomation's accessibility service connects asynchronously on first use.
        var back: android.view.accessibility.AccessibilityNodeInfo? = null
        waitFor(5_000) {
            back = root()?.let(::findBack)
            back != null
        }
        assertNotNull("Back must be available with the on-screen controls", back)
        val bounds = android.graphics.Rect()
        back!!.getBoundsInScreen(bounds)
        val time = SystemClock.uptimeMillis()
        for (action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
            val event = android.view.MotionEvent.obtain(time,SystemClock.uptimeMillis(),action,
                bounds.exactCenterX(),bounds.exactCenterY(),0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            try { assertTrue(instrumentation.uiAutomation.injectInputEvent(event,true)) } finally { event.recycle() }
        }
        waitFor { scenario.state == Lifecycle.State.DESTROYED }
        waitFor { server.events.any { it.first.endsWith("/Stopped") } }
    }
    }
    @Test fun embeddedPgsRendersAndSwitchesWithoutServerTranscoding() {
        for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            exercise(kind = kind, pgs = true) { scenario, server, _ ->
                var diagnostic = ""
                fun bitmapIsVisible(): Boolean {
                    var visible = false
                    scenario.onActivity { activity ->
                        val player = activity.model.player
                        visible = player.currentCues.cues.any { it.bitmap?.let { bitmap -> bitmap.width == 160 && bitmap.height == 40 } == true }
                        diagnostic = "mode=${activity.model.state.value.mode}, error=${activity.model.state.value.error}, " +
                            "position=${player.currentPosition}, subtitle=${activity.model.state.value.subtitleIndex}, " +
                            "tracks=${player.currentTracks.groups.map { group -> (0 until group.length).map { index ->
                                val format = group.getTrackFormat(index)
                                "${format.sampleMimeType}/${format.codecs}, selected=${group.isTrackSelected(index)}"
                            } }}, cues=${player.currentCues.cues.map { it.bitmap?.let { b -> "${b.width}x${b.height}" } }}"
                    }
                    return visible
                }
                try { waitFor { bitmapIsVisible() && snapshot(scenario).playing } }
                catch (failure: AssertionError) { throw AssertionError(diagnostic, failure) }
                // The later cue must not hide losing the early one during initial track selection.
                scenario.onActivity { assertTrue("The first PGS cue must survive startup", it.model.player.currentPosition < 5_000) }
                assertEquals(PlaybackMode.DIRECT_PLAY, snapshot(scenario).mode)
                assertFalse(snapshot(scenario).subtitleUnavailable)
                val negotiations = server.events.count { it.first.endsWith("PlaybackInfo") }
                scenario.onActivity { it.model.subtitles(-1) }
                waitFor { !bitmapIsVisible() && snapshot(scenario).subtitleIndex == -1 }
                scenario.onActivity { it.model.subtitles(2); it.model.seek(8_000) }
                // The fixture has a new display set after 9 s. Matroska need not replay a display
                // set that began before the video keyframe used by an 8 s seek.
                try { waitFor { bitmapIsVisible() && snapshot(scenario).playing && snapshot(scenario).positionMs >= 8_000 } }
                catch (failure: AssertionError) { throw AssertionError(diagnostic, failure) }
                assertEquals(negotiations, server.events.count { it.first.endsWith("PlaybackInfo") })
                assertFalse(server.requests.any { it.contains("/Subtitles/") || it.startsWith("/hls/") })
            }
        }
    }

    @Test fun hlsVideoSeeksAndSubtitleTrackIsSelected() = exercise(hls=true) { scenario,server,_ ->
        playing(scenario)
        assertFalse(snapshot(scenario).direct)
        // The text track is switched on once its file has arrived beside the video.
        waitFor {
            var shown = false
            scenario.onActivity { activity ->
                shown = activity.model.player.currentTracks.groups.any { it.type==androidx.media3.common.C.TRACK_TYPE_TEXT && it.isSelected } &&
                    activity.model.player.currentCues.cues.isNotEmpty()
            }
            shown
        }
        scenario.onActivity { activity -> activity.model.seek(12_000) }
        waitFor { snapshot(scenario).positionMs>=12_000 && snapshot(scenario).playing }
        assertTrue(server.requests.any { it.contains("Stream.vtt") })
        val sessions = server.events.count { it.first.endsWith("PlaybackInfo") }
        val downloads = server.requests.count { it.contains("/Subtitles/") }
        scenario.onActivity { it.model.subtitles(-1) }
        waitFor { !snapshot(scenario).busy && snapshot(scenario).subtitleIndex == -1 && snapshot(scenario).playing }
        scenario.onActivity { it.model.subtitles(2) }
        waitFor { !snapshot(scenario).busy && snapshot(scenario).subtitleIndex == 2 && snapshot(scenario).playing }
        assertEquals(sessions,server.events.count { it.first.endsWith("PlaybackInfo") })
        assertEquals(downloads, server.requests.count { it.contains("/Subtitles/") })
        assertTrue(snapshot(scenario).positionMs>=12_000)
    }
    @Test fun anAssFileTooLargeForTheRendererFallsBackToTheServersVtt() = exercise(subtitleCodec = "ass", hugeAss = true) { scenario, server, _ ->
        playing(scenario)
        // beta17 always asked for Stream.vtt. A styled original that is too large must not cost the text.
        waitFor {
            var shown = false
            scenario.onActivity { activity ->
                shown = activity.model.player.currentTracks.groups.any { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT && it.isSelected } &&
                    activity.model.player.currentCues.cues.isNotEmpty()
            }
            shown
        }
        assertTrue(server.requests.any { it.substringBefore('?').endsWith("/Subtitles/2/Stream.ass") })
        assertTrue(server.requests.any { it.substringBefore('?').endsWith("/Subtitles/2/Stream.vtt") })
        assertFalse(snapshot(scenario).subtitleUnavailable)
    }
    @Test fun subtitlesOffFetchNothingAndSwitchingOnLoadsTheTextOnce() = exercise(defaultSubtitle = -1) { scenario, server, _ ->
        playing(scenario)
        assertEquals(-1, snapshot(scenario).subtitleIndex)
        SystemClock.sleep(1_000)
        assertFalse(server.requests.any { it.contains("/Subtitles/") })
        val preparations = server.events.count { it.first.endsWith("PlaybackInfo") }
        scenario.onActivity { it.model.subtitles(2) }
        waitFor {
            var visible = false
            scenario.onActivity { visible = it.model.player.currentCues.cues.isNotEmpty() }
            visible
        }
        assertEquals(1, server.requests.count { it.contains("/Subtitles/") })
        assertEquals(preparations, server.events.count { it.first.endsWith("PlaybackInfo") })
        assertTrue(snapshot(scenario).playing)
    }

    /**
     * The TV report: an episode started, stopped for a moment, and went on with the subtitle.
     * Media3 held the video loader back while the selected text file was still being extracted.
     * The text now loads beside the picture, so the picture runs on and the text joins it.
     */
    @Test fun slowSubtitleExtractionNeverStallsThePicture() = exercise(hls = true, subtitleDelayMs = 6_000) { scenario, server, _ ->
        playing(scenario)
        var stalled = false
        val until = SystemClock.elapsedRealtime() + 4_000
        while (SystemClock.elapsedRealtime() < until) {
            if (snapshot(scenario).busy) stalled = true
            SystemClock.sleep(100)
        }
        assertFalse("the picture waited for the subtitle", stalled)
        waitFor(15_000) {
            var visible = false
            scenario.onActivity { visible = it.model.player.currentCues.cues.isNotEmpty() }
            visible
        }
        assertEquals(1, server.requests.count { it.contains("/Subtitles/") })
    }

    @Test fun switchingAudioAndQualityKeepsPosition() = exercise { scenario,server,_ ->
        playing(scenario)
        scenario.onActivity { it.model.seek(7000) }
        waitFor { snapshot(scenario).let { it.positionMs >= 7000 && it.playing && !it.busy } }
        scenario.onActivity { it.model.audio(3) }
        waitFor { snapshot(scenario).audioIndex==3 && snapshot(scenario).playing }
        assertTrue(snapshot(scenario).positionMs>=6500)
        scenario.onActivity { it.model.quality(2_000_000) }
        waitFor { snapshot(scenario).quality==2_000_000 && snapshot(scenario).playing }
        assertTrue(server.events.any { it.second["MaxStreamingBitrate"]==JsonPrimitive(2_000_000) })
    }
    @Test fun backgroundPausesAndDoesNotResumeWithoutUserAction() = exercise { scenario,_,_ ->
        playing(scenario)
        scenario.moveToState(Lifecycle.State.CREATED)
        SystemClock.sleep(500)
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertFalse(snapshot(scenario).playing)
        scenario.onActivity { it.model.toggle() }; playing(scenario)
    }
    @Test fun pausedAndBackgroundPlayersDoNotOverwriteAnotherDevicesProgress() {
        for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            exercise(kind = kind, defaultSubtitle = -1) { scenario, server, _ ->
                playing(scenario)
                scenario.onActivity { it.model.seek(12_000); it.model.toggle() }
                waitFor { server.events.any { (path, body) -> path.endsWith("/Progress") &&
                    body["IsPaused"] == JsonPrimitive(true) &&
                    (body["PositionTicks"] as? JsonPrimitive)?.longOrNull?.let { it >= 120_000_000 } == true } }
                val pausedReports = server.events.count { it.first.endsWith("/Progress") }
                val pausedAt = SystemClock.elapsedRealtime()
                waitFor(13_000) { SystemClock.elapsedRealtime() - pausedAt >= 11_000 }
                val last = server.events.last { it.first.endsWith("/Progress") }
                assertEquals(JsonPrimitive(true), last.second["IsPaused"])
                assertEquals("Paused player must not send periodic reports", pausedReports,
                    server.events.count { it.first.endsWith("/Progress") })
                val before = server.events.size
                val backgroundAt = SystemClock.elapsedRealtime()
                scenario.moveToState(Lifecycle.State.CREATED)
                waitFor(13_000) { SystemClock.elapsedRealtime() - backgroundAt >= 11_000 }
                assertEquals("An already paused background player must not repeat old progress", before, server.events.size)
                scenario.moveToState(Lifecycle.State.RESUMED)
                assertFalse(snapshot(scenario).playWhenReady)
                scenario.onActivity { it.model.toggle() }
                playing(scenario)
            }
        }
    }
    @Test fun accountReplacementStopsOldPlayer() = exercise { scenario,_,connections ->
        playing(scenario)
        connections.delete(ServiceKind.JELLYFIN)
        waitFor { snapshot(scenario).error?.contains("Kontoen er endra") == true }
        assertFalse(snapshot(scenario).playing)
    }
    @Test fun backgroundPlayerStaysRevokedAfterSwitchingAwayAndBack() = exercise { scenario, _, connections ->
        playing(scenario)
        scenario.moveToState(Lifecycle.State.CREATED)
        try {
            connections.activeProfileId = "synthetic-child"
            connections.activeProfileId = ""
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitFor { snapshot(scenario).error?.contains("Kontoen er endra") == true }
            scenario.onActivity { it.model.toggle() }
            assertFalse(snapshot(scenario).playing)
            assertEquals("Spole", snapshot(scenario).title)
            assertEquals("", snapshot(scenario).itemId)
            scenario.onActivity { assertEquals(0, it.model.player.mediaItemCount) }
        } finally { connections.activeProfileId = "" }
    }
    @Test fun resumeRequiresChoiceAndUsesTheSavedPosition() = exercise(resumeMs=8000, autoResume=false) { scenario,server,_ ->
        waitFor { snapshot(scenario).awaitingResume }
        assertFalse(snapshot(scenario).playing)
        assertTrue(server.events.isEmpty())
        scenario.onActivity { it.model.resume(false) }
        playing(scenario)
        assertTrue(snapshot(scenario).positionMs>=8000)
    }
    @Test fun autoResumeStartsTheVideoAtTheSavedPositionWithoutAPrompt() = exercise(resumeMs=8000) { scenario,server,_ ->
        playing(scenario)
        assertFalse(snapshot(scenario).awaitingResume)
        assertTrue(snapshot(scenario).positionMs >= 8000)
        waitFor { server.events.any { it.first == "/Sessions/Playing" } }
    }
    @Test fun askingStillAllowsAnExplicitFreshStart() = exercise(resumeMs=8000, autoResume=false) { scenario,_,_ ->
        waitFor { snapshot(scenario).awaitingResume }
        scenario.onActivity { it.model.resume(true) }
        playing(scenario)
        assertTrue(snapshot(scenario).positionMs < 8000)
    }
    @Test fun seriesSelectsSeasonThenOnlyAnAvailableEpisode() = exercise(root="series") { scenario,_,_ ->
        waitFor(30_000) { snapshot(scenario).choices.isNotEmpty() }
        scenario.onActivity { it.model.choose(it.model.state.value.choices.single()) }
        waitFor(30_000) { snapshot(scenario).choices.singleOrNull()?.type == "Episode" }
        scenario.onActivity { it.model.choose(it.model.state.value.choices.single()) }
        playing(scenario)
        scenario.onActivity { assertTrue(it.model.back()) }
        waitFor { snapshot(scenario).browsing && !snapshot(scenario).busy }
        assertFalse(snapshot(scenario).playing)
    }
    @Test fun interruptedStreamHasWorkingRetry() = exercise { scenario,server,_ ->
        playing(scenario)
        server.rejectVideo=true
        scenario.onActivity { it.model.quality(2_000_000) }
        waitFor(30_000) { snapshot(scenario).error != null }
        assertFalse(snapshot(scenario).playing)
        server.rejectVideo=false
        scenario.onActivity { it.model.retry() }
        playing(scenario)
    }
    @Test fun changingTracksWhilePausedStaysPaused() = exercise { scenario,_,_ ->
        playing(scenario)
        scenario.onActivity { it.model.toggle() }
        waitFor { !snapshot(scenario).playing }
        scenario.onActivity { it.model.subtitles(-1) }
        waitFor { !snapshot(scenario).busy && snapshot(scenario).subtitleIndex == -1 }
        assertFalse(snapshot(scenario).playing)
        scenario.onActivity { it.model.quality(2_000_000) }
        waitFor { !snapshot(scenario).busy && snapshot(scenario).quality == 2_000_000 }
        assertFalse(snapshot(scenario).playing)
        scenario.onActivity { it.model.toggle() }; playing(scenario)
    }
    @Test fun progressWarningClearsWhenServerRecovers() = exercise { scenario,server,_ ->
        playing(scenario)
        server.rejectReports=true
        scenario.onActivity { it.model.toggle() }
        waitFor { snapshot(scenario).warning != null }
        server.rejectReports=false
        scenario.onActivity { it.model.toggle() }
        waitFor { snapshot(scenario).warning == null }
        assertTrue(snapshot(scenario).playing)
    }
}
