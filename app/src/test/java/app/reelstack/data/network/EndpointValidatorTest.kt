package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidatorTest {
    @Test fun normalizationDoesNotDependOnDeviceLanguage() {
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            assertEquals("https://jellyfin.example.com", EndpointValidator.normalizeBaseUrl("HTTPS://JELLYFIN.EXAMPLE.COM"))
        } finally {
            java.util.Locale.setDefault(previous)
        }
    }

    @Test fun invalidAddressesHaveActionableMessages() {
        val cases = mapOf(
            " " to "Skriv inn tenaradressa først",
            "https://media .example.com" to "Tenaradressa inneheld mellomrom. Fjern dei og prøv igjen.",
            "https://media.example.com:99999" to "Portnummeret må vere mellom 1 og 65535",
            "ftp://media.example.com" to "Berre HTTP- og HTTPS-adresser er støtta",
        )
        cases.forEach { (input, expected) ->
            assertEquals(expected, runCatching { EndpointValidator.normalizeBaseUrl(input) }.exceptionOrNull()?.message)
        }
    }

    @Test fun apiResolutionNormalizesHostWithoutChangingPathCase() {
        assertEquals("https://emby.example.com/Media/Users/AuthenticateByName",
            EndpointValidator.resolve("HTTPS://EMBY.EXAMPLE.COM/Media/", "/Users/AuthenticateByName"))
    }

    @Test fun normalizesUppercaseSchemeAndHostButPreservesCaseSensitivePath() {
        assertEquals("https://emby.example.com/MediaServer", EndpointValidator.normalizeBaseUrl(" HTTPS://EMBY.Example.COM/MediaServer/ "))
        assertEquals("https://jellyfin.example.com", EndpointValidator.normalizeBaseUrl("Jellyfin.Example.COM"))
        assertEquals("http://localhost:8096", EndpointValidator.normalizeBaseUrl("HTTP://LOCALHOST:8096"))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsBlankAddress() {
        EndpointValidator.normalizeBaseUrl(" ")
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidPort() {
        EndpointValidator.normalizeBaseUrl("https://media.example.com:99999")
    }

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
