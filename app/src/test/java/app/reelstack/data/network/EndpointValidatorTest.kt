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

    /**
     * Which rule rejected the address, not how it was worded — the wording lives in three
     * languages now, and pinning one of them here would only ever test that one.
     */
    @Test fun invalidAddressesHaveActionableMessages() {
        val cases = mapOf(
            " " to app.reelstack.R.string.endpoint_blank,
            "https://media .example.com" to app.reelstack.R.string.endpoint_whitespace,
            "https://media.example.com:99999" to app.reelstack.R.string.endpoint_port,
            "ftp://media.example.com" to app.reelstack.R.string.endpoint_scheme,
        )
        cases.forEach { (input, expected) ->
            val failure = runCatching { EndpointValidator.normalizeBaseUrl(input) }.exceptionOrNull()
            assertEquals(input, expected, failure?.localizedFailure()?.resId)
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
    fun completeRequestUrlsKeepTheirPathAndQueryAfterValidation() {
        val url = "https://media.example.com/Items/An%20episode/Images/Primary?quality=90"
        assertEquals(url, EndpointValidator.validateRequestUrl(url))
    }

    @Test
    fun sharedJsonTransportRefusesAnUnsafeCompleteUrlBeforeOpeningANetworkRequest() {
        val failure = runCatching {
            HttpTransport().get("http://media.example.com/Items/1", emptyMap())
        }.exceptionOrNull()
        assertEquals(app.reelstack.R.string.endpoint_cleartext, failure?.localizedFailure()?.resId)
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
                app.reelstack.R.string.endpoint_cleartext,
                runCatching { EndpointValidator.normalizeBaseUrl(address) }.exceptionOrNull()?.localizedFailure()?.resId,
            )
        }
    }

    @Test fun realPrivateAddressesStillAllowCleartextHttp() {
        listOf(
            "http://192.168.1.5:8096",
            "http://10.0.0.8:8096",
            "http://100.64.0.8:8096",
            "http://100.127.255.254:8096",
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
        listOf("http://100.63.255.255", "http://100.128.0.1", "http://172.15.0.1", "http://172.32.0.1", "http://11.0.0.1", "http://192.169.0.1", "http://256.1.1.1")
            .forEach { address ->
                assertTrue(address, runCatching { EndpointValidator.normalizeBaseUrl(address) }.isFailure)
            }
        assertTrue(runCatching { EndpointValidator.normalizeBaseUrl("https://172.15.0.1") }.isSuccess)
    }

}
