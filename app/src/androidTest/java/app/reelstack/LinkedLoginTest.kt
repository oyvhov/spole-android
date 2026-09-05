package app.reelstack

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.*
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread
import org.junit.Assert.*
import org.junit.Test

/** Loopback fixtures, never real media accounts. Run only on the isolated test AVD. */
class LinkedLoginTest {
    private fun exercise(rejectSeerr: Boolean = false, mismatchedId: Boolean = false, primary: ServiceKind = ServiceKind.JELLYFIN) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val server = ServerSocket(0)
        val serverThread = thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: SocketException) { break }
                socket.use {
                    it.soTimeout = 3000
                    val reader = it.getInputStream().bufferedReader()
                    val request = reader.readLine().orEmpty()
                    var length = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
                    }
                    repeat(length) { reader.read() }
                    val path = request.split(' ').getOrNull(1).orEmpty()
                    var status = 200
                    var cookie = ""
                    val body = when {
                        path.endsWith("Users/AuthenticateByName") -> """{"AccessToken":"jellyfin-personal","User":{"Id":"media-user"}}"""
                        path.endsWith("Users/Me") -> """{"Id":"media-user","Name":"Test User","Policy":{"IsAdministrator":false}}"""
                        path.endsWith("Users/media-user") -> """{"Id":"media-user","Name":"Test User","Policy":{"IsAdministrator":false}}"""
                        path.endsWith("api/v1/status") -> "{}"
                        path.endsWith("api/v1/auth/jellyfin") -> {
                            if (rejectSeerr) { status = 401; "{}" }
                            else { cookie = "Set-Cookie: connect.sid=fixture-session; HttpOnly\r\n"; """{"id":7}""" }
                        }
                        path.endsWith("api/v1/auth/me") -> """{"id":7,"displayName":"Test User","jellyfinUserId":"${if (mismatchedId) "other-user" else "media-user"}","permissions":32}"""
                        else -> { status = 404; "{}" }
                    }
                    val bytes = body.toByteArray()
                    it.getOutputStream().write("HTTP/1.1 $status OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\n${cookie}Connection: close\r\n\r\n".toByteArray() + bytes)
                }
            }
        }
        val container = AppContainer(instrumentation.targetContext)
        ServiceKind.entries.forEach { container.connectionRepository.delete(it) }
        val store = ViewModelStore()
        lateinit var model: ReelstackViewModel
        try {
            instrumentation.runOnMainSync {
                model = ReelstackViewModel(container)
                store.put("test", model)
                model.openSheet(AppSheet.ConnectionEditor(primary))
                model.updateConnectionAuthMode(ConnectionAuthMode.ACCOUNT)
                model.updateConnectionUrl("http://127.0.0.1:${server.localPort}/${primary.name.lowercase()}")
                model.updateConnectionUsername("fixture-user")
                model.updateConnectionPassword("fixture-password")
                if (primary != ServiceKind.EMBY) model.updateCompanionLogin(true,
                    "http://127.0.0.1:${server.localPort}/${if (primary == ServiceKind.SEERR) "jellyfin" else "seerr"}")
                model.testAndSaveConnection()
            }
            val end = System.currentTimeMillis() + 15000
            while (model.connectionDraft.value?.saving == true && System.currentTimeMillis() < end) Thread.sleep(25)
            assertFalse("Login must finish", model.connectionDraft.value?.saving == true)
            if (primary == ServiceKind.EMBY) {
                assertNull(model.connectionDraft.value)
                assertEquals("jellyfin-personal", container.connectionRepository.get(ServiceKind.EMBY).token)
                assertEquals("media-user", container.connectionRepository.get(ServiceKind.EMBY).userId)
                assertEquals("Test User", model.uiState.value.accounts[ServiceKind.EMBY]?.displayName)
            } else if (rejectSeerr || mismatchedId) {
                assertNotNull(model.connectionDraft.value?.error)
                assertEquals("", model.connectionDraft.value?.password)
                assertEquals("", container.connectionRepository.get(ServiceKind.SEERR).token)
                assertEquals("", container.connectionRepository.get(ServiceKind.JELLYFIN).token)
            } else {
                assertNull(model.connectionDraft.value)
                assertEquals("jellyfin-personal", container.connectionRepository.get(ServiceKind.JELLYFIN).token)
                assertEquals("connect.sid=fixture-session", container.connectionRepository.get(ServiceKind.SEERR).token)
                assertTrue(container.connectionRepository.get(ServiceKind.SEERR).sessionCookie)
                assertFalse(container.connectionRepository.get(ServiceKind.JELLYFIN).sessionCookie)
                instrumentation.runOnMainSync { model.removeConnection(ServiceKind.SEERR) }
                assertEquals("", container.connectionRepository.get(ServiceKind.SEERR).token)
                assertEquals("jellyfin-personal", container.connectionRepository.get(ServiceKind.JELLYFIN).token)
                assertTrue(model.uiState.value.trackedRequests.isEmpty())
            }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            server.close()
            serverThread.join(3000)
            ServiceKind.entries.forEach { container.connectionRepository.delete(it) }
        }
    }
    @Test fun oneFormCreatesDistinctPersonalSessionsWithoutAdminAccess() = exercise()
    @Test fun seerrFirstAlsoConnectsJellyfin() = exercise(primary = ServiceKind.SEERR)
    @Test fun secondLoginFailureDoesNotSaveHalfALogin() = exercise(rejectSeerr = true)
    @Test fun sameDisplayNameDoesNotLinkDifferentAccounts() = exercise(mismatchedId = true)
    @Test fun embyLoginVerifiesTheReturnedUserIdWithoutCallingUsersMe() = exercise(primary = ServiceKind.EMBY)
}
