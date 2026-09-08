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

/** Only synthetic accounts on emulator-5562. Actual AVC/AAC/HLS/VTT decoding over loopback HTTP. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class JellyfinPlayerTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private class Server(val assets: android.content.res.AssetManager, val hls: Boolean = false, val resumeMs: Long = 0) : AutoCloseable {
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
                val reader = conn.getInputStream().bufferedReader()
                val line = reader.readLine() ?: return@runCatching
                val path = line.split(' ')[1]
                requests += path
                val headers = mutableMapOf<String,String>()
                while (true) { val value = reader.readLine() ?: break; if (value.isBlank()) break
                    headers[value.substringBefore(':').lowercase()] = value.substringAfter(':').trim() }
                clientHeaders += headers["authorization"].orEmpty()
                val chars = CharArray(headers["content-length"]?.toIntOrNull() ?: 0)
                var count = 0
                while (count < chars.size) { val n = reader.read(chars, count, chars.size-count); if (n < 0) break; count += n }
                val body = if (chars.isEmpty()) JsonObject(emptyMap()) else Json.parseToJsonElement(String(chars)).jsonObject
                var status = 200
                var contentType = "application/json"
                var bytes = when {
                    path == "/Users/Me" -> """{"Id":"u1","Name":"Testperson","Policy":{"EnableMediaPlayback":true,"IsAdministrator":false}}""".toByteArray()
                    path.endsWith("/Items/series") -> """{"Id":"series","Type":"Series","Name":"Testserie"}""".toByteArray()
                    path.startsWith("/Shows/") -> """{"Items":[{"Id":"season","Type":"Season","Name":"Sesong 1","LocationType":"Virtual"}],"TotalRecordCount":1}""".toByteArray()
                    path.startsWith("/Items?") -> """{"Items":[{"Id":"film","Type":"Episode","Name":"Ny dag","SeriesName":"Testserie","ParentIndexNumber":1,"IndexNumber":1,"RunTimeTicks":200000000},{"Id":"missing","Type":"Episode","IsMissing":true}],"TotalRecordCount":2}""".toByteArray()
                    path.contains("/Items/") && !path.contains("PlaybackInfo") -> """{"Id":"film","Type":"Movie","Name":"Spole testvideo","RunTimeTicks":200000000,"UserData":{"PlaybackPositionTicks":${resumeMs*10000}}}""".toByteArray()
                    path.endsWith("PlaybackInfo") -> {
                        events += path to body
                        val direct = !hls && body["EnableDirectPlay"] != JsonPrimitive(false)
                        val sub = (body["SubtitleStreamIndex"] as? JsonPrimitive)?.intOrNull ?: 2
                        """{"PlaySessionId":"session${events.size}","MediaSources":[{"Id":"source","SupportsDirectPlay":$direct,"TranscodingUrl":"/hls/master.m3u8?api_key=fixture","DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":$sub,"MediaStreams":[{"Index":1,"Type":"Audio","DisplayTitle":"English","Language":"eng"},{"Index":3,"Type":"Audio","DisplayTitle":"Norsk","Language":"nor"},{"Index":2,"Type":"Subtitle","DisplayTitle":"Norsk tekst","Language":"nor","Codec":"srt","IsTextSubtitleStream":true}]}]}""".toByteArray()
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

    private fun exercise(hls: Boolean = false, resumeMs: Long = 0, root: String = "film", block: (ActivityScenario<JellyfinPlayerActivity>, Server, ConnectionRepository) -> Unit) {
        Server(instrumentation.context.assets, hls, resumeMs).use { server ->
            val connections = (context.applicationContext as ReelstackApplication).container.connectionRepository
            ServiceKind.entries.forEach(connections::delete)
            connections.save(ServiceConnection(ServiceKind.JELLYFIN,"Test",server.base,"fixture","u1"))
            try {
                ActivityScenario.launch<JellyfinPlayerActivity>(Intent(context,JellyfinPlayerActivity::class.java).putExtra("jellyfin_item_id",root)).use { scenario ->
                    block(scenario,server,connections)
                }
                waitFor { server.events.any { it.first.endsWith("/Stopped") } || server.rejectVideo }
                assertTrue(server.requests.none { it.contains("api_key") || it.contains("token=") })
            } finally { ServiceKind.entries.forEach(connections::delete) }
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
        waitFor(8_000) { find("Set på pause") == null && find("Tilbake") != null }
        val bounds = android.graphics.Rect()
        scenario.onActivity { it.window.decorView.getGlobalVisibleRect(bounds) }
        tap(bounds.left + bounds.width() * .85f,bounds.top + bounds.height() * .35f)
        waitFor(2_000) { find("Set på pause") != null }
        find("Set på pause")!!.getBoundsInScreen(bounds)
        tap(bounds.exactCenterX(),bounds.exactCenterY())
        waitFor { !snapshot(scenario).playing }
        assertTrue(snapshot(scenario).error == null)
    }
    @Test fun onScreenBackWorksAfterTransportControlsHide() = exercise { scenario,server,_ ->
        val automation = instrumentation.uiAutomation
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
        // UiAutomation's accessibility service connects asynchronously on first use.
        var back: android.view.accessibility.AccessibilityNodeInfo? = null
        waitFor(5_000) {
            back = automation.rootInActiveWindow?.let(::findBack)
            back != null
        }
        assertNotNull("Back must remain available while video plays", back)
        val bounds = android.graphics.Rect()
        back!!.getBoundsInScreen(bounds)
        val time = SystemClock.uptimeMillis()
        for (action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
            val event = android.view.MotionEvent.obtain(time,SystemClock.uptimeMillis(),action,
                bounds.exactCenterX(),bounds.exactCenterY(),0)
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
    @Test fun resumeRequiresChoiceAndUsesTheSavedPosition() = exercise(resumeMs=8000) { scenario,server,_ ->
        waitFor { snapshot(scenario).awaitingResume }
        assertFalse(snapshot(scenario).playing)
        assertTrue(server.events.isEmpty())
        scenario.onActivity { it.model.resume(false) }
        playing(scenario)
        assertTrue(snapshot(scenario).positionMs>=8000)
    }
    @Test fun seriesSelectsSeasonThenOnlyAnAvailableEpisode() = exercise(root="series") { scenario,_,_ ->
        waitFor { snapshot(scenario).choices.isNotEmpty() }
        scenario.onActivity { it.model.choose(it.model.state.value.choices.single()) }
        waitFor { snapshot(scenario).choices.singleOrNull()?.type == "Episode" }
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
