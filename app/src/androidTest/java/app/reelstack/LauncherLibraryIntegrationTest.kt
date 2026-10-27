package app.reelstack

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import java.io.File
import java.net.ServerSocket
import java.net.SocketException
import java.util.Collections
import kotlin.concurrent.thread
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource

/** End-to-end launcher handoff against a local fixture, only on the isolated test AVD. */
class LauncherLibraryIntegrationTest {
    private lateinit var server: ServerSocket
    private val requests = Collections.synchronizedList(mutableListOf<String>())

    @get:Rule(order = 0)
    val fixture = object : ExternalResource() {
        override fun before() {
            server = ServerSocket(0)
            thread(isDaemon = true) {
                while (!server.isClosed) {
                    val socket = try { server.accept() } catch (_: SocketException) { break }
                    thread(isDaemon = true) {
                        runCatching { socket.use {
                            it.soTimeout = 3_000
                            val reader = it.getInputStream().bufferedReader()
                            val path = reader.readLine().split(' ').getOrNull(1).orEmpty()
                            requests += path
                            while (!reader.readLine().isNullOrEmpty()) { }
                            val body = when {
                                path.substringBefore('?').endsWith("/Views") ->
                                    """{"Items":[{"Id":"library-42","Name":"Eventyrfilmar","Type":"CollectionFolder","CollectionType":"movies"}]}"""
                                path.contains("ParentId=library-42") && path.contains("Items") ->
                                    """{"Items":[{"Id":"fixture-film","Name":"Eksempelfilm","Type":"Movie"}],"TotalRecordCount":1}"""
                                path.contains("Sessions") -> "[]"
                                else -> """{"Items":[],"TotalRecordCount":0}"""
                            }
                            val bytes = body.toByteArray(Charsets.UTF_8)
                            it.getOutputStream().write(
                                "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray() + bytes,
                            )
                        } }
                    }
                }
            }
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val container = (instrumentation.targetContext.applicationContext as ReelstackApplication).container
            instrumentation.runOnMainSync {
                container.connectionRepository.activeProfileId = ""
                ServiceKind.entries.forEach(container.connectionRepository::delete)
                container.connectionRepository.save(ServiceConnection(ServiceKind.JELLYFIN, "Fixture",
                    "http://127.0.0.1:${server.localPort}", "fixture-token", userId = "fixture-user"))
                container.preferencesRepository.onboardingCompleted = true
            }
        }

        override fun after() {
            server.close()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val container = (instrumentation.targetContext.applicationContext as ReelstackApplication).container
            instrumentation.runOnMainSync { ServiceKind.entries.forEach(container.connectionRepository::delete) }
        }
    }

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    @Test fun libraryLinkOpensTheRequestedViewAndCapturesTheScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("spole://library?kind=jellyfin&user=fixture-user&library=library-42"))
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var screenshot: Bitmap? = null
        var visibleSince = 0L
        rule.waitUntil(timeoutMillis = 30_000) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (!hasVisibleLabel(root, "Eventyrfilmar") || !hasVisibleLabel(root, "Eksempelfilm") || hasVisibleLabel(root, "Spole")) {
                visibleSince = 0L
                false
            }
            else {
                val candidate = instrumentation.uiAutomation.takeScreenshot()
                if (!hasVisiblePage(candidate)) { visibleSince = 0L; candidate.recycle(); false }
                else if (visibleSince == 0L) { visibleSince = SystemClock.uptimeMillis(); candidate.recycle(); false }
                else if (SystemClock.uptimeMillis() - visibleSince >= 600) { screenshot = candidate; true }
                else { candidate.recycle(); false }
            }
        }
        val capturedPaths = synchronized(requests) { requests.toList() }
        assertTrue(capturedPaths.any { it.substringBefore('?').endsWith("/Users/fixture-user/Views") })
        assertTrue(capturedPaths.any { it.contains("ParentId=library-42") })
        File(context.filesDir, "launcher-library-fixture.png").outputStream().use {
            checkNotNull(screenshot).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun hasVisibleLabel(node: AccessibilityNodeInfo?, label: String): Boolean {
        if (node == null) return false
        if (node.isVisibleToUser && (node.text?.toString() == label || node.contentDescription?.toString() == label)) return true
        return (0 until node.childCount).any { hasVisibleLabel(node.getChild(it), label) }
    }

    /** The startup cover can expose underlying semantics before its fade has finished. */
    private fun hasVisiblePage(bitmap: Bitmap): Boolean {
        var bright = 0
        for (y in bitmap.height / 10 until bitmap.height / 3 step 8) {
            for (x in bitmap.width / 20 until bitmap.width * 19 / 20 step 8) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.red(pixel) > 150 && Color.green(pixel) > 150 && Color.blue(pixel) > 150 && ++bright >= 30) return true
            }
        }
        return false
    }
}
