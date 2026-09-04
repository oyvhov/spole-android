package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidatorTest {
    @Test
    fun addsHttpsWhenSchemeIsMissing() {
        assertEquals("https://media.example.com", EndpointValidator.normalizeBaseUrl("media.example.com/"))
    }

    @Test
    fun preservesPortAndPath() {
        assertEquals(
            "http://192.168.1.20:8096/jellyfin",
            EndpointValidator.normalizeBaseUrl("http://192.168.1.20:8096/jellyfin/"),
        )
    }

    @Test
    fun resolvesApiPathWithoutDoubleSlash() {
        assertEquals(
            "https://media.example.com/api/v3/system/status",
            EndpointValidator.resolve("https://media.example.com/", "/api/v3/system/status"),
        )
    }

    @Test
    fun reportsCleartextConnections() {
        assertTrue(EndpointValidator.isCleartext("http://192.168.1.20:8096"))
        assertFalse(EndpointValidator.isCleartext("https://media.example.com"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCredentialsInsideUrl() {
        EndpointValidator.normalizeBaseUrl("https://user:password@media.example.com")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCleartextPublicHosts() {
        EndpointValidator.normalizeBaseUrl("http://media.example.com")
    }

    @Test
    fun permitsCleartextPrivateLanHosts() {
        assertEquals(
            "http://172.20.0.5:8096",
            EndpointValidator.normalizeBaseUrl("http://172.20.0.5:8096"),
        )
    }
}
