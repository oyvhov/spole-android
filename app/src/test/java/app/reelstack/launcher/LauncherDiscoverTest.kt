package app.reelstack.launcher

import app.reelstack.R
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.RequestSeason
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.RemoteMediaDetails
import app.reelstack.data.network.ServiceMessage
import app.reelstack.data.network.requireSuccess
import app.reelstack.localization.LocalizedText
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What Spole tells Stova about Seerr: who may look and who may ask, and the rows that carry a title,
 * its seasons and a followed request. All of it pure, so the provider only has to fetch.
 */
class LauncherDiscoverTest {
    private val apiKey = ServiceConnection(ServiceKind.SEERR, "Seerr", "http://seerr.local", "fake-token")
    private val signedIn = apiKey.copy(sessionCookie = true)

    private fun media(status: Int? = 5, metadata: String = "2024 · Film", facts: List<String> = listOf("Film", "2024", "★ 7.1")) = DiscoverMedia(
        id = "seerr-movie-603", title = "Matrix", metadata = metadata, artworkRes = 0, inLibrary = false,
        artworkUrl = "https://image.tmdb.org/t/p/w500/poster.jpg", remoteId = 603, mediaType = "movie",
        overview = "En hacker vaknar.", facts = facts, genres = listOf("Drama", "Krim"), seerrStatus = status,
        backdropUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
    )

    private fun tracked(requestId: Int? = 42, stage: RequestStage = RequestStage.DOWNLOADING) = TrackedRequest(
        key = "tv:1399:1,2", mediaId = 1399, mediaType = "tv", title = "Game of Thrones",
        artworkUrl = "https://image.tmdb.org/t/p/w500/got.jpg", seasons = setOf(2, 1), stage = stage, percent = 40,
        requestId = requestId, is4k = true,
    )

    /** A row by column name, after it has been checked to be as wide as the columns say. */
    private fun named(columns: Array<String>, row: Array<Any?>): Map<String, Any?> {
        assertEquals(columns.size, row.size)
        return columns.zip(row).toMap()
    }

    @Test fun accessIsNoneForChildrenAndMissingSeerr() {
        assertEquals(LauncherSeerrAccess.NONE, launcherSeerrAccess(profileIsChild = true, seerr = signedIn))
        assertEquals(LauncherSeerrAccess.NONE, launcherSeerrAccess(profileIsChild = false, seerr = null))
        assertEquals(LauncherSeerrAccess.NONE, launcherSeerrAccess(false, signedIn.copy(token = "")))
        assertEquals(LauncherSeerrAccess.NONE, launcherSeerrAccess(false, signedIn.copy(token = "  ")))
        assertEquals(LauncherSeerrAccess.NONE, launcherSeerrAccess(false, signedIn.copy(baseUrl = "")))
        assertEquals("none", LauncherSeerrAccess.NONE.wire)
    }

    @Test fun apiKeyBrowsesAndCookieIsPersonal() {
        assertEquals(LauncherSeerrAccess.BROWSE, launcherSeerrAccess(false, apiKey))
        assertEquals(LauncherSeerrAccess.PERSONAL, launcherSeerrAccess(false, signedIn))
        assertEquals("browse", LauncherSeerrAccess.BROWSE.wire)
        assertEquals("personal", LauncherSeerrAccess.PERSONAL.wire)
    }

    @Test fun itemRowCarriesStatusYearAndGenres() {
        val row = named(LauncherDiscoverColumns.ITEM, discoverItemRow(LauncherDiscoverRows.TRENDING, media()))
        assertEquals("trending", row["row"])
        assertEquals("movie", row["type"])
        assertEquals(603, row["tmdb"])
        assertEquals("Matrix", row["title"])
        assertEquals(2024, row["year"])
        assertEquals("En hacker vaknar.", row["overview"])
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", row["poster"])
        assertEquals("https://image.tmdb.org/t/p/w1280/backdrop.jpg", row["backdrop"])
        assertEquals(5, row["status"])
        // Stova's scores are out of ten, as the rows from the libraries give them; Spole keeps them out of 100.
        assertEquals(7.1, row["community"] as Double, .001)
        assertEquals("Drama, Krim", row["genres"])
        // No year in the line, no score in the facts, no status from Seerr: the row says so and does not guess.
        val bare = named(LauncherDiscoverColumns.ITEM, discoverItemRow("search", media(status = null, metadata = "Film", facts = listOf("Film"))))
        assertNull(bare["year"])
        assertNull(bare["community"])
        assertEquals(1, bare["status"])
        // A number with more than four digits is not a year.
        assertNull(named(LauncherDiscoverColumns.ITEM, discoverItemRow("search", media(metadata = "Episode 12345")))["year"])
    }

    @Test fun aCardWithoutTypeOrIdIsNotSent() {
        val cards = discoverCards(LauncherDiscoverRows.SIMILAR,
            listOf(media(), media().copy(id = "x", mediaType = null), media().copy(id = "y", remoteId = null)))
        assertEquals(1, cards.size)
        assertEquals(listOf("similar", "movie", 603), named(LauncherDiscoverColumns.ITEM, cards.single()).let { listOf(it["row"], it["type"], it["tmdb"]) })
        assertEquals(emptyList<Array<Any?>>(), discoverCards(LauncherDiscoverRows.SEARCH, emptyList()))
    }

    @Test fun titleRowRequestsOnlyWhenPersonalAndRequestable() {
        val fetched = RemoteMediaDetails(title = "The Matrix", artworkUrl = "https://image.tmdb.org/t/p/w500/p2.jpg", tagline = "Free your mind.",
            overview = "Ein datamaskin vaknar.", genres = listOf("Action"), trailerUrl = "https://www.youtube.com/watch?v=abc",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/b2.jpg", tmdbRating = 82f,
            facts = listOf(LocalizedText.raw("1999"), LocalizedText.raw("★ 8.2")))
        // The page opens from a bare card: Seerr has not said anything about the title yet.
        val requestable = media(status = null, metadata = "")
        fun title(access: LauncherSeerrAccess, media: DiscoverMedia = requestable, details: RemoteMediaDetails = fetched,
            tracked: TrackedRequest? = null, critic: Int? = 87) =
            named(LauncherDiscoverColumns.TITLE, discoverTitleRow(media, details, critic, access, tracked))

        // Looking is not asking: an API key may not request.
        assertEquals(0, title(LauncherSeerrAccess.BROWSE)["can_request"])
        assertEquals(0, title(LauncherSeerrAccess.NONE)["can_request"])
        assertEquals(1, title(LauncherSeerrAccess.PERSONAL)["can_request"])
        // Nothing to ask for when it is already in the library, whether the card or Seerr's answer for the page says so.
        assertEquals(0, title(LauncherSeerrAccess.PERSONAL, media = requestable.copy(seerrStatus = 5))["can_request"])
        assertEquals(0, title(LauncherSeerrAccess.PERSONAL, media = requestable.copy(inLibrary = true))["can_request"])
        val inLibrary = title(LauncherSeerrAccess.PERSONAL, details = fetched.copy(seerrStatus = 5))
        assertEquals(0, inLibrary["can_request"])
        assertEquals(5, inLibrary["status"])

        val own = title(LauncherSeerrAccess.PERSONAL, tracked = tracked())
        assertEquals(42, own["request_id"])
        assertEquals("downloading", own["request_status"])
        val none = title(LauncherSeerrAccess.PERSONAL)
        assertNull(none["request_id"])
        assertNull(none["request_status"])
        // A followed title that Seerr gave no request id is not one that can be withdrawn.
        assertNull(title(LauncherSeerrAccess.PERSONAL, tracked = tracked(requestId = null))["request_id"])

        assertEquals("movie", none["type"])
        assertEquals(603, none["tmdb"])
        assertEquals("The Matrix", none["title"])
        assertEquals(1999, none["year"])
        assertEquals("Free your mind.", none["tagline"])
        // A blank tagline is none: Stova would draw an empty line for it.
        assertNull(title(LauncherSeerrAccess.PERSONAL, details = fetched.copy(tagline = "  "))["tagline"])
        assertEquals("Ein datamaskin vaknar.", none["overview"])
        assertEquals("https://image.tmdb.org/t/p/w500/p2.jpg", none["poster"])
        assertEquals("https://image.tmdb.org/t/p/w1280/b2.jpg", none["backdrop"])
        assertNull(none["runtime"])
        assertEquals("Action", none["genres"])
        assertEquals(8.2, none["community"] as Double, .001)
        assertEquals(87, none["critic"])
        assertEquals(1, none["status"])
        assertEquals("https://www.youtube.com/watch?v=abc", none["trailer"])
        assertNull(title(LauncherSeerrAccess.PERSONAL, critic = null)["critic"])
    }

    @Test fun discoverMinimalIsTheBareCardATitleOpensFrom() {
        val media = discoverMinimal("movie", 603)
        assertEquals("seerr-movie-603", media.id)
        assertEquals(603, media.remoteId)
        assertEquals("movie", media.mediaType)
        // It knows no status: whether the title can be asked for is judged by what Seerr says about it.
        assertNull(media.seerrStatus)
        assertEquals("seerr-tv-1399", discoverMinimal("tv", 1399).id)
    }

    @Test fun seasonAndRequestRowsFollowTheColumns() {
        val details = RemoteMediaDetails(title = null, artworkUrl = null, seasons = listOf(
            RequestSeason(1, LocalizedText.raw("Sesong 1"), 10, 5, LocalDate.of(2011, 4, 17)),
            RequestSeason(2, LocalizedText.raw("Sesong 2"), 8, 1),
        ))
        val seasons = discoverSeasonRows(details) { it.literal.orEmpty() }
        assertEquals(2, seasons.size)
        assertEquals(mapOf("number" to 1, "name" to "Sesong 1", "episodes" to 10, "status" to 5, "air_date" to "2011-04-17"),
            named(LauncherDiscoverColumns.SEASON, seasons[0]))
        assertEquals(mapOf("number" to 2, "name" to "Sesong 2", "episodes" to 8, "status" to 1, "air_date" to null),
            named(LauncherDiscoverColumns.SEASON, seasons[1]))
        assertEquals(emptyList<Array<Any?>>(), discoverSeasonRows(RemoteMediaDetails(null, null)) { it.literal.orEmpty() })

        val request = named(LauncherDiscoverColumns.REQUEST, discoverRequestRow(tracked()))
        assertEquals("tv", request["type"])
        assertEquals(1399, request["tmdb"])
        assertEquals("Game of Thrones", request["title"])
        assertEquals("https://image.tmdb.org/t/p/w500/got.jpg", request["poster"])
        assertEquals("downloading", request["stage"])
        assertEquals(40, request["percent"])
        assertEquals("1,2", request["seasons"])
        assertEquals(42, request["request_id"])
        assertEquals(1, request["is4k"])
        val plain = named(LauncherDiscoverColumns.REQUEST, discoverRequestRow(tracked(requestId = null).copy(is4k = false, percent = null)))
        assertNull(plain["request_id"])
        assertNull(plain["percent"])
        assertEquals(0, plain["is4k"])
    }

    @Test fun theWordsStovaReadsAreFixed() {
        assertEquals(listOf("row", "type", "tmdb", "title", "year", "overview", "poster", "backdrop", "status", "community", "genres"),
            LauncherDiscoverColumns.ITEM.toList())
        assertEquals(listOf("type", "tmdb", "title", "poster", "backdrop", "stage", "percent", "seasons", "request_id", "is4k"),
            LauncherDiscoverColumns.REQUEST.toList())
        assertEquals(listOf("type", "tmdb", "title", "year", "tagline", "overview", "poster", "backdrop", "runtime", "genres",
            "community", "critic", "status", "can_request", "request_id", "request_status", "trailer"), LauncherDiscoverColumns.TITLE.toList())
        assertEquals(listOf("number", "name", "episodes", "status", "air_date"), LauncherDiscoverColumns.SEASON.toList())
        assertEquals(listOf("trending", "upcoming_movies", "upcoming_series", "similar", "recommended", "search"),
            listOf(LauncherDiscoverRows.TRENDING, LauncherDiscoverRows.UPCOMING_MOVIES, LauncherDiscoverRows.UPCOMING_SERIES,
                LauncherDiscoverRows.SIMILAR, LauncherDiscoverRows.RECOMMENDED, LauncherDiscoverRows.SEARCH))
        assertEquals(listOf("ok", "already", "not_personal", "forbidden", "failed", "unknown"), LauncherDiscoverResult.entries.map { it.wire })
    }

    @Test fun discoverResultForMapsErrors() {
        // What Spole's Seerr client throws for an answer with this status.
        fun seerrSaid(status: Int) = requireNotNull(runCatching { HttpResponse(status, "{}").requireSuccess(ServiceKind.SEERR) }.exceptionOrNull())
        fun spoleSaid(resId: Int, vararg args: Any) = ServiceMessage(LocalizedText(resId, *args))

        // Already asked for: Seerr's 409, its 202 («no new seasons»), seasons that are asked for already, or a message saying so.
        listOf(seerrSaid(409), spoleSaid(R.string.err_ingen_nye_sesongar), spoleSaid(R.string.err_sesongane_er_endra),
            IllegalStateException("Request already exists"), IllegalStateException("Media EXISTS")).forEach {
            assertEquals(it.toString(), LauncherDiscoverResult.ALREADY, discoverResultFor(it))
        }
        // Not allowed: Seerr's 401 or 403, on the request or on the sign-in check, or an account that may not ask for this type.
        listOf(seerrSaid(401), seerrSaid(403), spoleSaid(R.string.err_avviste_kontotilgangen_logg_inn, "Seerr"),
            spoleSaid(R.string.err_seerr_medietype_ikkje_lov)).forEach {
            assertEquals(it.toString(), LauncherDiscoverResult.FORBIDDEN, discoverResultFor(it))
        }
        // Anything else failed: another status, another of Spole's messages, no network.
        listOf(seerrSaid(500), seerrSaid(404), seerrSaid(418), spoleSaid(R.string.err_seerr_konto_endra_send),
            java.io.IOException("timeout")).forEach {
            assertEquals(it.toString(), LauncherDiscoverResult.FAILED, discoverResultFor(it))
        }
    }
}
