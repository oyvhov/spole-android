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

    /**
     * The private-address check used to be a prefix test on the hostname, so any registered name
     * that merely started like a private address unlocked cleartext HTTP over the public internet.
     */
    @Test fun publicNamesThatLookPrivateDoNotUnlockCleartextHttp() {
        listOf(
            "http://fcbarcelona.com",        // starts with "fc", the IPv6 ULA prefix
            "http://fdependencies.example",  // starts with "fd"
            "http://192.168.1.5.nip.io",     // wildcard resolver, public DNS
            "http://10.example.com",
            "http://127.0.0.1.nip.io",
            "http://172.16.example.com",
            "http://fe80.example.com",
        ).forEach { address ->
            assertEquals(
                "Vanleg HTTP er berre tillate for localhost eller private lokalnettadresser",
                runCatching { EndpointValidator.normalizeBaseUrl(address) }.exceptionOrNull()?.message,
            )
        }
    }

    @Test fun realPrivateAddressesStillAllowCleartextHttp() {
        listOf(
            "http://192.168.1.5:8096",
            "http://10.0.0.8:8096",
            "http://172.16.0.1:8096",
            "http://172.31.255.254:8096",
            "http://127.0.0.1:8096",
            "http://localhost:8096",
            "http://tunet.local:8096",
            "http://[fd00::1]:8096",
            "http://[::1]:8096",
        ).forEach { address ->
            assertTrue(address, runCatching { EndpointValidator.normalizeBaseUrl(address) }.isSuccess)
        }
    }

    @Test fun addressesOutsideThePrivateRangesStayHttpsOnly() {
        listOf("http://172.15.0.1", "http://172.32.0.1", "http://11.0.0.1", "http://192.169.0.1", "http://256.1.1.1")
            .forEach { address ->
                assertTrue(address, runCatching { EndpointValidator.normalizeBaseUrl(address) }.isFailure)
            }
        assertTrue(runCatching { EndpointValidator.normalizeBaseUrl("https://172.15.0.1") }.isSuccess)
    }

}
