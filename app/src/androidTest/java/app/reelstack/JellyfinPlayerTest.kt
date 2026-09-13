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
        val episode: Boolean = false) : AutoCloseable {
        val socket = ServerSocket(0)
        val base = "http://127.0.0.1:${socket.localPort}"
        val pool = Executors.newCachedThreadPool()
        val events = CopyOnWriteArrayList<Pair<String, JsonObject>>()
        val requests = CopyOnWriteArrayList<String>()
        val clientHeaders = CopyOnWriteArrayList<String>()
        @Volatile var rejectVideo = false
        @Volatile var rejectReports = false
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
                    path == "/Users/Me" -> """{"Id":"u1","Name":"Testperson","Policy":{"EnableMediaPlayback":true,"IsAdministrator":false}}""".toByteArray()
                    episode && path.contains("adjacentTo=") -> """{"Items":[
                        {"Id":"film","Type":"Episode","SeriesId":"series","Name":"Første","RunTimeTicks":200000000},
                        {"Id":"next","Type":"Episode","SeriesId":"series","Name":"Neste","RunTimeTicks":200000000}]}""".toByteArray()
                    episode && path.substringBefore('?').endsWith("/Items/film") ->
                        """{"Id":"film","Type":"Episode","SeriesId":"series","Name":"Første","RunTimeTicks":200000000}""".toByteArray()
                    path.contains("/Items/series") -> """{"Id":"series","Type":"Series","Name":"Testserie"}""".toByteArray()
                    path.startsWith("/Shows/") -> """{"Items":[{"Id":"season","Type":"Season","Name":"Sesong 1","LocationType":"Virtual"}],"TotalRecordCount":1}""".toByteArray()
                    path.startsWith("/Items?") -> """{"Items":[{"Id":"film","Type":"Episode","Name":"Ny dag","SeriesName":"Testserie","ParentIndexNumber":1,"IndexNumber":1,"RunTimeTicks":200000000},{"Id":"missing","Type":"Episode","IsMissing":true}],"TotalRecordCount":2}""".toByteArray()
                    path.contains("/Items/") && !path.contains("PlaybackInfo") -> """{"Id":"film","Type":"Movie","Name":"Spole testvideo","RunTimeTicks":200000000,"UserData":{"PlaybackPositionTicks":${resumeMs*10000}}}""".toByteArray()
                    path.endsWith("PlaybackInfo") -> {
                        events += path to body
                        val direct = !hls && body["EnableDirectPlay"] != JsonPrimitive(false)
                        val sub = (body["SubtitleStreamIndex"] as? JsonPrimitive)?.intOrNull ?: 2
                        """{"PlaySessionId":"session${events.size}","MediaSources":[{"Id":"source","SupportsDirectPlay":$direct,"TranscodingUrl":"/hls/master.m3u8?api_key=fixture","DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":$sub,"MediaStreams":[{"Index":0,"Type":"Video","Codec":"h264","Profile":"baseline","Width":640,"Height":360,"BitDepth":8,"VideoRangeType":"SDR","AverageFrameRate":24},{"Index":1,"Type":"Audio","Codec":"aac","Channels":2,"SampleRate":48000,"DisplayTitle":"English","Language":"eng"},{"Index":3,"Type":"Audio","Codec":"aac","Channels":2,"SampleRate":48000,"DisplayTitle":"Norsk","Language":"nor"},{"Index":2,"Type":"Subtitle","DisplayTitle":"Norsk tekst","Language":"nor","Codec":"srt","IsTextSubtitleStream":true}]}]}""".toByteArray()
                    }
                    path.startsWith("/Sessions/Playing") -> { events += path to body; status = if (rejectReports) 503 else 204; byteArrayOf() }
                    path.contains("/Subtitles/") -> { contentType="text/vtt"; assets.open("player/subtitle.vtt").use { it.readBytes() } }
                    else -> {
                        if (rejectVideo) { status=503; byteArrayOf() } else {
                            val name = if (path.startsWith("/hls/")) path.substringAfterLast('/').substringBefore('?') else "video.mp4"
                            contentType = if(name.endsWith("m3u8")) "application/vnd.apple.mpegurl" else if(name.endsWith("ts")) "video/mp2t" else "video/mp4"
                            assets.open("player/$name").use { it.readBytes() }
                        }
                    }
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
        episode: Boolean = false, block: (ActivityScenario<JellyfinPlayerActivity>, Server, ConnectionRepository) -> Unit) {
        Server(instrumentation.context.assets, hls, resumeMs, episode).use { server ->
            val connections = (context.applicationContext as ReelstackApplication).container.connectionRepository
            ServiceKind.entries.forEach(connections::delete)
            connections.save(ServiceConnection(ServiceKind.JELLYFIN,"Test",server.base,"fixture","u1"))
            val preferences = (context.applicationContext as ReelstackApplication).container.preferencesRepository
            val original = preferences.personalization
            preferences.personalization = original.copy(autoResume = autoResume).let {
                if (episode) it.copy(autoPlayNextEpisode = true, showNextEpisode = true,
                    nextEpisodeLeadSeconds = 10, nextEpisodeDelaySeconds = 5) else it
            }
            try {
                ActivityScenario.launch<JellyfinPlayerActivity>(Intent(context,JellyfinPlayerActivity::class.java).putExtra("jellyfin_item_id",root)).use { scenario ->
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

    @Test fun countdownAdvancesFromRealVideoBeforeTheCurrentEpisodeEnds() = exercise(episode = true) { scenario, server, _ ->
        playing(scenario)
        waitFor { snapshot(scenario).nextEpisode != null }
        scenario.onActivity { it.model.seek(10_500) }
        waitFor { snapshot(scenario).nextEpisodeCountdown != null }
        assertFalse(snapshot(scenario).ended)
        waitFor { snapshot(scenario).itemId == "next" }
        val stopped = server.events.first { it.first.endsWith("/Stopped") }.second
        val stoppedAt = stopped.getValue("PositionTicks").jsonPrimitive.long
        assertTrue("Episode must change before its 20-second end; stopped at $stoppedAt ticks", stoppedAt < 200_000_000L)
    }

    @Test fun cancellingEarlyCountdownKeepsCurrentEpisode() = exercise(episode = true) { scenario, _, _ ->
        playing(scenario)
        waitFor { snapshot(scenario).nextEpisode != null }
        scenario.onActivity { it.model.seek(10_500) }
        waitFor { snapshot(scenario).nextEpisodeCountdown != null }
        scenario.onActivity { it.model.cancelNextEpisode() }
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
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == false
            }
            ready
        }
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        waitFor { scenario.state == Lifecycle.State.DESTROYED }
        waitFor { server.events.any { it.first.endsWith("/Stopped") } }
    }
    @Test fun videoTouchShowsControlsAndPauseReceivesTheNextTap() = exercise { scenario,_,_ ->
        val automation = instrumentation.uiAutomation
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
        waitFor { find("Set på pause") != null }
        waitFor(8_000) { find("Set på pause") == null && find("Tilbake") == null }
        val bounds = android.graphics.Rect()
        scenario.onActivity { it.window.decorView.getGlobalVisibleRect(bounds) }
        tap(bounds.left + bounds.width() * .85f,bounds.top + bounds.height() * .35f)
        waitFor(2_000) { find("Set på pause") != null }
        find("Set på pause")!!.getBoundsInScreen(bounds)
        tap(bounds.exactCenterX(),bounds.exactCenterY())
        waitFor { !snapshot(scenario).playing }
        assertTrue(snapshot(scenario).error == null)
    }
    @Test fun onScreenBackReturnsOnlyWhenTransportControlsAreShown() = exercise { scenario,server,_ ->
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
    @Test fun hlsVideoSeeksAndSubtitleTrackIsSelected() = exercise(hls=true) { scenario,server,_ ->
        playing(scenario)
        assertFalse(snapshot(scenario).direct)
        scenario.onActivity { activity ->
            assertTrue(activity.model.player.currentTracks.groups.any { it.type==androidx.media3.common.C.TRACK_TYPE_TEXT && it.isSelected })
            assertTrue(activity.model.player.currentCues.cues.isNotEmpty())
            activity.model.seek(12_000)
        }
        waitFor { snapshot(scenario).positionMs>=12_000 && snapshot(scenario).playing }
        assertTrue(server.requests.any { it.contains("Stream.vtt") })
        val sessions = server.events.count { it.first.endsWith("PlaybackInfo") }
        scenario.onActivity { it.model.subtitles(-1) }
        waitFor { !snapshot(scenario).busy && snapshot(scenario).subtitleIndex == -1 && snapshot(scenario).playing }
        scenario.onActivity { it.model.subtitles(2) }
        waitFor { !snapshot(scenario).busy && snapshot(scenario).subtitleIndex == 2 && snapshot(scenario).playing }
        assertEquals(sessions,server.events.count { it.first.endsWith("PlaybackInfo") })
        assertTrue(snapshot(scenario).positionMs>=12_000)
    }
    @Test fun switchingAudioAndQualityKeepsPosition() = exercise { scenario,server,_ ->
        playing(scenario)
        scenario.onActivity { it.model.seek(7000) }
        waitFor { snapshot(scenario).positionMs>=7000 }
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
    @Test fun accountReplacementStopsOldPlayer() = exercise { scenario,_,connections ->
        playing(scenario)
        connections.delete(ServiceKind.JELLYFIN)
        waitFor { snapshot(scenario).error?.contains("Kontoen er endra") == true }
        assertFalse(snapshot(scenario).playing)
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
