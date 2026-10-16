package app.reelstack.data.network

import app.reelstack.data.model.*
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class RequestOptionsTest {
    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "cookie", userId = "7", sessionCookie = true)

    private class Fake(private val bodies: Map<String, String>) : JsonHttpTransport {
        var posted: String? = null
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            val key = bodies.keys.filter { url.contains(it) }.maxByOrNull { it.length } ?: return HttpResponse(404, "{}")
            return HttpResponse(200, bodies.getValue(key))
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse { posted = jsonBody; return HttpResponse(201, "{}") }
    }

    private val radarr = mapOf(
        "service/radarr/0" to """{"server":{"id":0,"is4k":false,"activeProfileId":4,"activeDirectory":"/mnt/w/Filmar 10"},
            "profiles":[{"id":4,"name":"HD-1080p"},{"id":5,"name":"Ultra-HD"}],
            "rootFolders":[{"id":1,"path":"/mnt/w/Filmar 10","freeSpace":4815000000000},{"id":2,"path":"/data/film","freeSpace":null}]}""",
        "service/radarr" to """[{"id":0,"name":"Radarr","is4k":false,"isDefault":true},{"id":1,"name":"Radarr 4K","is4k":true,"isDefault":true}]""",
    )

    @Test fun optionsComeFromTheDefaultServerWithFreeSpace() {
        val options = SeerrDiscoverClient(Fake(radarr)).requestOptions(seerr, "movie", is4k = false)!!
        assertEquals(0, options.serverId)
        assertEquals(listOf("HD-1080p", "Ultra-HD"), options.profiles.map { it.name })
        assertEquals(4, options.defaultProfileId)
        assertEquals("Filmar 10", options.folders.first().name)
        assertEquals(4815000000000L, options.folders.first().freeBytes)
        assertNull(options.folders.last().freeBytes)
    }

    @Test fun freeSpaceIsSiWithOneDecimal() {
        assertEquals("4,8 TB", formatFreeSpace(4_815_000_000_000, Locale.forLanguageTag("nn-NO")))
        assertEquals("950,0 GB", formatFreeSpace(950_000_000_000, Locale.forLanguageTag("nn-NO")))
        assertEquals("4.8 TB", formatFreeSpace(4_815_000_000_000, Locale.ENGLISH))
    }

    @Test fun permissionsGateTheChoices() {
        fun account(bits: Long) = ServiceAccount(ServiceKind.SEERR, "7", "Ø", permissions = bits)
        assertTrue(account(8192).canChooseRequestOptions())
        assertTrue(account(16).canChooseRequestOptions())
        assertFalse(account(32).canChooseRequestOptions())
        assertTrue(account(2048).canRequest4k("movie"))
        assertFalse(account(2048).canRequest4k("tv"))
        assertTrue(account(1024).canRequest4k("tv"))
    }

    @Test fun thePostCarriesOnlyTheShownChoices() {
        val fake = Fake(mapOf("auth/me" to """{"id":7,"displayName":"Ø","permissions":8224}"""))
        SeerrServiceClient(fake).request(seerr, "movie", 11, "7", choice = RequestChoice(serverId = 0, profileId = 5, rootFolder = "/data/film"))
        val body = Json.parseToJsonElement(fake.posted!!).jsonObject
        assertEquals("5", body.getValue("profileId").jsonPrimitive.content)
        assertEquals("/data/film", body.getValue("rootFolder").jsonPrimitive.content)
        assertNull(body["is4k"])
    }

    @Test fun fourKSeriesChecksFourKSeasons() {
        val details = """{"name":"S","seasons":[{"seasonNumber":1}],"mediaInfo":{"status":1,"status4k":1,
            "seasons":[{"seasonNumber":1,"status":5,"status4k":1}]}}"""
        val fake = Fake(mapOf("auth/me" to """{"id":7,"displayName":"Ø","permissions":4128}""", "tv/11" to details))
        SeerrServiceClient(fake).request(seerr, "tv", 11, "7", seasons = setOf(1), choice = RequestChoice(is4k = true))
        assertEquals("true", Json.parseToJsonElement(fake.posted!!).jsonObject.getValue("is4k").jsonPrimitive.content)
    }
}
