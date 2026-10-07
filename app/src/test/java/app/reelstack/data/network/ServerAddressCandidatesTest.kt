package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAddressCandidatesTest {
    @Test
    fun bareLanAddressTriesTheDefaultsOverHttpFirst() {
        assertEquals(
            listOf("http://192.168.1.20:8096", "https://192.168.1.20:8920", "http://192.168.1.20", "https://192.168.1.20"),
            serverAddressCandidates("192.168.1.20"),
        )
    }

    @Test
    fun typedPortIsKeptAndHttpComesFirstOnTheLan() {
        assertEquals(
            listOf("http://192.168.1.20:8097", "https://192.168.1.20:8097"),
            serverAddressCandidates(" 192.168.1.20:8097/ "),
        )
    }

    @Test
    fun typedSchemeAndPortAreUsedAsTheyAre() {
        assertEquals(listOf("http://10.0.0.4:8097"), serverAddressCandidates("http://10.0.0.4:8097"))
        assertEquals(
            listOf("https://media.example.org", "https://media.example.org:8920"),
            serverAddressCandidates("https://media.example.org"),
        )
    }

    @Test
    fun publicNamesAreOnlyTriedOverHttps() {
        val candidates = serverAddressCandidates("media.example.org")
        assertEquals(listOf("https://media.example.org", "https://media.example.org:8920"), candidates)
        assertTrue(serverAddressCandidates("http://media.example.org").isEmpty())
    }

    @Test
    fun mdnsNameAndBasePathCountAsLocal() {
        assertEquals("http://nas.local:8096/jellyfin", serverAddressCandidates("nas.local/jellyfin").first())
    }

    @Test
    fun nonsenseGivesNothingToTry() {
        listOf("", "   ", "ftp://10.0.0.4", "two words", "http://").forEach {
            assertTrue(it, serverAddressCandidates(it).isEmpty())
        }
    }
}
