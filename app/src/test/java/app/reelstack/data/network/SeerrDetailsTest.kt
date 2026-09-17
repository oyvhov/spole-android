package app.reelstack.data.network

import app.reelstack.localization.LocalizedText
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SeerrDetailsTest {
    private val connection = ServiceConnection(
        kind = ServiceKind.SEERR,
        name = "Seerr",
        baseUrl = "https://media.example.com/seerr",
        token = "test-api-key",
    )

    @Test
    fun prefersRequestedOverviewWithoutFetchingEnglish() {
        val transport = DetailsTransport(HttpResponse(200, """{"name":"Silo","overview":"Livet under bakken."}"""))

        val details = SeerrServiceClient(transport).details(connection, "tv", 125988, language = "nn-NO")

        assertEquals("Livet under bakken.", details.overview)
        assertEquals(listOf("${connection.baseUrl}/api/v1/tv/125988?language=nn-NO"), transport.urls)
    }

    @Test
    fun defaultsToNorwegianThenUsesEnglishForMissingNullEmptyAndWhitespaceOverviews() {
        listOf("", "\"overview\":null,", "\"overview\":\"\",", "\"overview\":\"  \\n \",").forEach { overview ->
            val primary = """{${overview}"name":"Silo","tagline":"Ei løynd verd.","firstAirDate":"2023-05-05",
                "posterPath":"/silo-nb.jpg","genres":[{"name":"Drama"}],"status":"Returning Series",
                "numberOfSeasons":2,"numberOfEpisodes":20,"networks":[{"name":"Apple TV+"}],"mediaInfo":{"status":4}}"""
            val transport = DetailsTransport(
                HttpResponse(200, primary),
                HttpResponse(200, """{"name":"English title","overview":"People live in a giant underground silo.",
                    "posterPath":"/english.jpg","tagline":"English tagline","genres":[{"name":"English genre"}],
                    "status":"Ended","numberOfSeasons":99,"mediaInfo":{"status":5}}"""),
            )

            val details = SeerrServiceClient(transport).details(connection, "tv", 125988)

            assertEquals("People live in a giant underground silo.", details.overview)
            assertEquals(ServicePayloadParser.mediaDetails(primary).copy(overview = details.overview), details)
            assertEquals(
                listOf("${connection.baseUrl}/api/v1/tv/125988?language=nb", "${connection.baseUrl}/api/v1/tv/125988?language=en"),
                transport.urls,
            )
            assertTrue(transport.headers.all { it == mapOf("X-Api-Key" to "test-api-key") })
        }
    }

    @Test
    fun movieFallbackUsesSameEndpointAndSessionHeaders() {
        val transport = DetailsTransport(
            HttpResponse(200, """{"title":"Film","overview":""}"""),
            HttpResponse(200, """{"title":"Movie","overview":"An English synopsis."}"""),
        )
        val session = connection.copy(token = "connect.sid=test-session", sessionCookie = true)

        val details = SeerrServiceClient(transport).details(session, "movie", 101)

        assertEquals("An English synopsis.", details.overview)
        assertEquals("Film", details.title)
        assertTrue(transport.urls.all { it.startsWith("${connection.baseUrl}/api/v1/movie/101?language=") })
        assertTrue(transport.headers.all { it["Cookie"] == "connect.sid=test-session" && "X-Api-Key" !in it })
        assertTrue(transport.urls.none { it.contains("test-session") })
    }

    @Test
    fun keepsPrimaryDetailsWhenEnglishIsUnavailableOrInvalid() {
        val primary = HttpResponse(200, """{"name":"Silo","overview":"","numberOfSeasons":2,"mediaInfo":{"status":4}}""")
        listOf(
            HttpResponse(404, "{}"), HttpResponse(503, "{}"), HttpResponse(401, "{}"),
            HttpResponse(200, "invalid json"), HttpResponse(200, "null"),
            HttpResponse(200, """{"overview":null}"""), HttpResponse(200, """{"overview":"  "}"""),
        ).forEach { fallback ->
            val transport = DetailsTransport(primary, fallback)

            assertEquals(
                ServicePayloadParser.mediaDetails(primary.body),
                SeerrServiceClient(transport).details(connection, "tv", 125988),
            )
            assertEquals(2, transport.urls.size)
        }
    }

    @Test
    fun keepsPrimaryDetailsWhenFallbackTimesOut() {
        val primary = HttpResponse(200, """{"name":"Silo","numberOfEpisodes":20}""")
        val transport = DetailsTransport(primary) // An exhausted response queue simulates a transport failure.

        assertEquals(
            ServicePayloadParser.mediaDetails(primary.body),
            SeerrServiceClient(transport).details(connection, "tv", 125988),
        )
        assertEquals(2, transport.urls.size)
    }

    @Test
    fun doesNotRepeatEnglishRequestsForEnglishLocales() {
        listOf("en", "en-US", "EN-gb").forEach { language ->
            val transport = DetailsTransport(HttpResponse(200, """{"name":"Silo"}"""))

            assertEquals(null, SeerrServiceClient(transport).details(connection, "tv", 125988, language).overview)
            assertEquals(1, transport.urls.size)
        }
    }

    @Test
    fun propagatesPrimaryHttpFailureWithoutFallback() {
        val transport = DetailsTransport(HttpResponse(401, "{}"))

        val result = runCatching { SeerrServiceClient(transport).details(connection, "tv", 125988) }

        assertTrue(result.isFailure)
        assertEquals(1, transport.urls.size)
    }

    @Test
    fun parsesSeriesCountsNetworksAndNynorskStatus() {
        val details = ServicePayloadParser.mediaDetails(
            """{"name":"Silo","firstAirDate":"2023-05-05","episodeRunTime":[49],"status":"Returning Series",
                "numberOfSeasons":2,"numberOfEpisodes":20,
                "networks":[{"name":"Apple TV+"},{"name":" Apple TV+ "},{"name":" "},{},{"name":"Network B"}]}""",
        )

        assertEquals(
            listOf(
                LocalizedText.raw("2023"),
                LocalizedText(R.string.media_minutes, 49),
                LocalizedText(R.string.production_returning),
                LocalizedText.plural(R.plurals.media_seasons, 2),
                LocalizedText.plural(R.plurals.media_episodes, 20),
                LocalizedText.raw("Apple TV+ · Network B"),
            ),
            details.facts,
        )
    }

    @Test
    fun parsesSingularCountsAndDocumentedSeasonAlias() {
        // The singular form is the resource's job now; the parser only says "one".
        assertEquals(
            listOf(
                LocalizedText.plural(R.plurals.media_seasons, 1),
                LocalizedText.plural(R.plurals.media_episodes, 1),
            ),
            ServicePayloadParser.mediaDetails("""{"name":"Pilot","numberOfSeason":1,"numberOfEpisodes":1}""").facts,
        )
    }

    @Test
    fun omitsMissingAndNonpositiveCountsWithoutInventingFacts() {
        listOf("{}", """{"numberOfSeasons":0,"numberOfEpisodes":-1,"networks":null,"status":" "}""").forEach {
            assertEquals(emptyList<LocalizedText>(), ServicePayloadParser.mediaDetails(it).facts)
        }
        assertEquals(
            listOf(LocalizedText(R.string.production_released)),
            ServicePayloadParser.mediaDetails("""{"title":"Film","status":"Released","numberOfSeasons":2,"numberOfEpisodes":20}""").facts,
        )
    }

    @Test
    fun normalizesKnownProductionStatusesAndPreservesUnknownValues() {
        // Which sentence, not which words — so the mapping is checked once and holds in every
        // language. An unknown status is not ours to translate and passes through as it came.
        mapOf(
            "Returning Series" to LocalizedText(R.string.production_returning),
            "Ended" to LocalizedText(R.string.production_ended),
            "Canceled" to LocalizedText(R.string.production_canceled),
            "Cancelled" to LocalizedText(R.string.production_canceled),
            "In Production" to LocalizedText(R.string.production_in_production),
            "Post Production" to LocalizedText(R.string.production_post),
            "Planned" to LocalizedText(R.string.production_planned),
            "Pilot" to LocalizedText(R.string.production_pilot),
            "Released" to LocalizedText(R.string.production_released),
            "Rumored" to LocalizedText(R.string.production_rumoured),
            "  RETURNING SERIES  " to LocalizedText(R.string.production_returning),
            "Ukjend status" to LocalizedText.raw("Ukjend status"),
        ).forEach { (raw, expected) ->
            assertEquals(listOf(expected), ServicePayloadParser.mediaDetails("""{"name":"Silo","status":"$raw"}""").facts)
        }
    }

    @Test
    fun blankLowercaseOverviewAllowsExistingUppercaseFallback() {
        assertEquals(
            "Ei skildring.",
            ServicePayloadParser.mediaDetails("""{"name":"Silo","overview":" ","Overview":"Ei skildring."}""").overview,
        )
    }

    private class DetailsTransport(vararg responses: HttpResponse) : JsonHttpTransport {
        private val responses = responses.toMutableList()
        val urls = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()

        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            this.headers += headers
            if (responses.isEmpty()) throw IOException("Test transport timeout")
            return responses.removeAt(0)
        }

        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            error("Details enrichment must only use GET")
    }
}
