package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class ProfileImageHeadersTest {
    private val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://media.example/seerr", "connect.sid=secret", sessionCookie = true)

    @Test fun sameServiceImageUsesThePersonalCookie() {
        assertEquals("connect.sid=secret", profileImageHeaders(connection, "https://media.example/seerr/avatar/a", "device")["Cookie"])
    }
    @Test fun externalAvatarNeverReceivesCredentials() {
        assertTrue(profileImageHeaders(connection, "https://gravatar.example/avatar/a", "device").isEmpty())
        assertTrue(profileImageHeaders(connection, "https://media.example.evil/seerr/a", "device").isEmpty())
        assertTrue(profileImageHeaders(connection, "http://media.example/seerr/a", "device").isEmpty())
        assertTrue(profileImageHeaders(connection, "https://media.example/another-service/a", "device").isEmpty())
        assertTrue(profileImageHeaders(connection, "https://media.example/seerr-other/a", "device").isEmpty())
    }
    @Test fun jellyfinUsesItsOwnAuthorizationNotASeerrCookie() {
        val headers = profileImageHeaders(connection.copy(kind = ServiceKind.JELLYFIN), "https://media.example/seerr/Users/1/Images/Primary", "device")
        assertTrue(headers.getValue("Authorization").startsWith("MediaBrowser"))
        assertFalse(headers.containsKey("Cookie"))
    }
}
