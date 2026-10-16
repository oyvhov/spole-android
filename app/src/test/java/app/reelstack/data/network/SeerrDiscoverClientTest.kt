package app.reelstack.data.network

import app.reelstack.data.model.CastMember
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.peopleFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeerrDiscoverClientTest {
    private class Fake(private val bodies: Map<String, String>) : JsonHttpTransport {
        val asked = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            asked += url
            val key = bodies.keys.filter { url.contains(it) }.maxByOrNull { it.length } ?: return HttpResponse(404, "{}")
            return HttpResponse(200, bodies.getValue(key))
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(405, "{}")
    }

    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "cookie", userId = "7", sessionCookie = true)
    private val trending = """{"page":1,"totalPages":3,"results":[
        {"id":11,"mediaType":"movie","title":"Love of Your Life","releaseDate":"2026-10-07","posterPath":"/p.jpg","backdropPath":"/b.jpg","mediaInfo":{"status":2}},
        {"id":12,"mediaType":"person","name":"Not a title"}]}"""

    @Test fun aRowKeepsTitlesWithTheirBackdrop() {
        val fake = Fake(mapOf("discover/trending" to trending))
        val page = SeerrDiscoverClient(fake).row(seerr, DiscoverRowKind.TRENDING)
        assertEquals(listOf(11), page.items.map { it.remoteId })
        assertEquals("https://image.tmdb.org/t/p/w1280/b.jpg", page.items.single().backdropUrl)
        assertTrue(page.hasMore)
        assertTrue(fake.asked.single().startsWith("https://seerr.example/api/v1/discover/trending?page=1"))
    }

    @Test fun genresComeWithABackdrop() {
        val genres = SeerrDiscoverClient(Fake(mapOf("genreslider/movie" to
            """[{"id":27,"name":"Grøssar","backdrops":["/g.jpg"]},{"id":35,"name":"Komedie","backdrops":[]}]"""))).genres(seerr, "movie")
        assertEquals(listOf("Grøssar", "Komedie"), genres.map { it.name })
        assertEquals("https://image.tmdb.org/t/p/w780/g.jpg", genres.first().backdropUrl)
        assertNull(genres.last().backdropUrl)
    }

    @Test fun castKeepsTheSeerrPersonId() {
        val details = ServicePayloadParser.mediaDetails(
            """{"title":"X","credits":{"cast":[{"id":501,"name":"Margaret Qualley","character":"Maya","profilePath":"/m.jpg"}]}}""")
        assertEquals(501, details.cast.single().seerrPersonId)
        assertEquals("https://image.tmdb.org/t/p/w185/m.jpg", details.cast.single().portraitUrl)
    }

    @Test fun peopleAreTakenTwoPerTitleWithoutRepeats() {
        fun cast(vararg ids: Int) = ids.map { CastMember("P$it", seerrPersonId = it) }
        assertEquals(listOf(1, 2, 3), peopleFrom(listOf(cast(1, 2, 9), cast(2, 3)), perTitle = 2).map { it.id })
    }

    @Test fun similarTitlesAreAskedForTheRightType() {
        val fake = Fake(mapOf("tv/77/similar" to """{"results":[{"id":5,"name":"Liknande","firstAirDate":"2024-01-01"}]}"""))
        val items = SeerrDiscoverClient(fake).similar(seerr, "tv", 77)
        assertEquals(listOf("tv"), items.map { it.mediaType })
        assertTrue(fake.asked.single().contains("api/v1/tv/77/similar"))
    }

    @Test fun searchKeepsPeopleWithTheirPicture() {
        val people = ServicePayloadParser.searchPeople("""{"results":[
            {"id":501,"mediaType":"person","name":"Margaret Qualley","profilePath":"/m.jpg","knownForDepartment":"Acting"},
            {"id":11,"mediaType":"movie","title":"X"}]}""")
        assertEquals(listOf(501), people.map { it.id })
        assertEquals("https://image.tmdb.org/t/p/w185/m.jpg", people.single().portraitUrl)
        assertEquals("Acting", people.single().role)
    }

    @Test fun aPersonHasCreditsWithoutRepeats() {
        val client = SeerrDiscoverClient(Fake(mapOf(
            "person/501/combined_credits" to """{"cast":[{"id":11,"mediaType":"movie","title":"A","releaseDate":"2024-01-01"}],
                "crew":[{"id":11,"mediaType":"movie","title":"A","releaseDate":"2024-01-01"},{"id":12,"mediaType":"tv","name":"B","firstAirDate":"2025-02-02"}]}""",
            "person/501" to """{"id":501,"name":"Margaret Qualley","biography":"Bio","birthday":"1994-10-23","knownForDepartment":"Acting","profilePath":"/m.jpg"}""")))
        assertEquals("Margaret Qualley", client.person(seerr, 501).name)
        assertEquals(listOf(12, 11), client.personCredits(seerr, 501).map { it.remoteId })
    }

    @Test fun aSeerrTitleKeepsItsYouTubeTrailer() {
        val details = ServicePayloadParser.mediaDetails("""{"title":"X","relatedVideos":[
            {"type":"Teaser","site":"YouTube","url":"https://www.youtube.com/watch?v=a"},
            {"type":"Trailer","site":"YouTube","url":"https://www.youtube.com/watch?v=b"}]}""")
        assertEquals("https://www.youtube.com/watch?v=b", details.trailerUrl)
    }

    @Test fun aBiographyMissingInNorwegianComesInEnglish() {
        val client = SeerrDiscoverClient(Fake(mapOf(
            "person/501?language=nb" to """{"id":501,"name":"M","biography":""}""",
            "person/501?language=en" to """{"id":501,"name":"M","biography":"Born in Montana."}""")))
        assertEquals("Born in Montana.", client.person(seerr, 501).biography)
    }
}
