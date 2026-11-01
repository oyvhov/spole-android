package app.reelstack.launcher

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.network.AccountProfileClient
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import app.reelstack.data.network.SeerrDiscoverClient
import app.reelstack.data.network.SeerrServiceClient
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.RequestTrackingRepository
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Discover for Stova against a Seerr that only exists in this test: which rows, extras and calls
 * one query gives, and what it does when Seerr fails.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class LauncherDiscoverQueriesTest {
    /** Answers by the longest address fragment that matches; anything else is a 404. Rows are asked from several threads. */
    private class FakeSeerr(private val answers: Map<String, HttpResponse>) : JsonHttpTransport {
        val asked = CopyOnWriteArrayList<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            asked += url
            val key = answers.keys.filter { url.contains(it) }.maxByOrNull { it.length } ?: return HttpResponse(404, "{}")
            return answers.getValue(key)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(405, "{}")
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val browse = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "fixture")
    private val personal = browse.copy(userId = "7", sessionCookie = true)

    private fun ok(body: String) = HttpResponse(200, body)
    private val broken = HttpResponse(500, "{}")

    private fun query(seerr: Map<String, HttpResponse>, path: String, connection: ServiceConnection = browse,
        access: LauncherSeerrAccess = LauncherSeerrAccess.BROWSE, seeded: List<TrackedRequest> = emptyList()): Pair<Cursor, FakeSeerr> {
        val fake = FakeSeerr(seerr)
        val tracking = RequestTrackingRepository(context, SeerrServiceClient(fake), AccountProfileClient(transport = fake))
        seeded.forEach { tracking.put(tracking.scope(connection, connection.userId), it) }
        val media = MediaSyncRepository(seerrServiceClient = SeerrServiceClient(fake), seerrDiscoverClient = SeerrDiscoverClient(fake))
        val uri = Uri.parse("content://app.reelstack.launcher/$path")
        val discover = requireNotNull(LauncherDiscoverPath.of(uri.pathSegments))
        val cursor = LauncherDiscoverQueries(media, tracking) { it.literal ?: "Sesong" }.answer(discover, uri, connection, access).cursor(discover, access)
        return cursor to fake
    }

    /** Each row as its values by column name. */
    private fun Cursor.rows(vararg columns: String): List<List<Any?>> = buildList {
        moveToPosition(-1)
        while (moveToNext()) add(columns.map { column ->
            val index = getColumnIndexOrThrow(column)
            when (getType(index)) {
                Cursor.FIELD_TYPE_NULL -> null
                Cursor.FIELD_TYPE_INTEGER -> getInt(index)
                else -> getString(index)
            }
        })
    }

    private fun FakeSeerr.askedFor(fragment: String) = asked.any { it.contains(fragment) }

    @Test fun aFailingRowLeavesTheOtherTwo() {
        val (cursor, seerr) = query(mapOf(
            "discover/trending" to broken,
            // A title without an id and a person are not titles Stova can show.
            "discover/movies/upcoming" to ok("""{"page":1,"totalPages":1,"results":[
                {"id":11,"mediaType":"movie","title":"Kjem snart","releaseDate":"2026-12-01"},
                {"mediaType":"movie","title":"Utan id"},
                {"id":12,"mediaType":"person","name":"Ein person"}]}"""),
            "discover/tv/upcoming" to ok("""{"page":1,"totalPages":1,"results":[{"id":22,"mediaType":"tv","name":"Ny serie"}]}"""),
        ), "discover?kind=emby&user=eilev")
        cursor.use {
            assertEquals(LauncherDiscoverColumns.ITEM.toList(), it.columnNames.toList())
            assertEquals(listOf(listOf("upcoming_movies", "movie", 11, "Kjem snart", 2026), listOf("upcoming_series", "tv", 22, "Ny serie", null)),
                it.rows("row", "type", "tmdb", "title", "year"))
            assertEquals("browse", it.extras.getString("seerr"))
            assertFalse(it.extras.containsKey("page"))
        }
        // All three were asked, the broken one too.
        assertTrue(seerr.askedFor("api/v1/discover/trending"))
        assertTrue(seerr.askedFor("api/v1/discover/movies/upcoming"))
        assertTrue(seerr.askedFor("api/v1/discover/tv/upcoming"))
    }

    @Test fun relatedGivesSimilarBeforeRecommended() {
        val (cursor, seerr) = query(mapOf(
            "movie/603/similar" to ok("""{"results":[{"id":604,"title":"Reloaded"}]}"""),
            "movie/603/recommendations" to ok("""{"results":[{"id":605,"title":"Revolutions"}]}"""),
        ), "discover/related?kind=emby&user=eilev&type=movie&tmdb=603")
        cursor.use { assertEquals(listOf(listOf("similar", 604), listOf("recommended", 605)), it.rows("row", "tmdb")) }
        assertEquals(2, seerr.asked.size)
    }

    @Test fun searchNeedsTwoLettersAndAFailedPageKeepsItsNumber() {
        val answers = mapOf(
            "search?query=matrix&page=2" to ok("""{"page":2,"totalPages":5,"results":[{"id":603,"mediaType":"movie","title":"Matrix"}]}"""),
            "search?query=matrix&page=3" to broken,
        )
        for (q in listOf("a", "%20a%20", "")) {
            val (cursor, seerr) = query(answers, "discover/search?kind=emby&user=eilev&q=$q&page=4")
            cursor.use {
                assertEquals(q, 0, it.count)
                assertEquals(q, 1, it.extras.getInt("page"))
                assertFalse(q, it.extras.getBoolean("has_more", true))
                assertEquals(q, "browse", it.extras.getString("seerr"))
            }
            assertEquals(q, emptyList<String>(), seerr.asked.toList())
        }

        val (found, _) = query(answers, "discover/search?kind=emby&user=eilev&q=matrix&page=2")
        found.use {
            assertEquals(listOf(listOf("search", "movie", 603)), it.rows("row", "type", "tmdb"))
            assertEquals(2, it.extras.getInt("page"))
            assertTrue(it.extras.getBoolean("has_more"))
        }

        val (failed, seerr) = query(answers, "discover/search?kind=emby&user=eilev&q=matrix&page=3")
        failed.use {
            assertEquals(0, it.count)
            assertEquals(3, it.extras.getInt("page"))
            assertFalse(it.extras.getBoolean("has_more", true))
        }
        assertTrue(seerr.askedFor("api/v1/search?query=matrix&page=3"))
    }

    @Test fun seasonsAreOnlyForSeries() {
        val answers = mapOf("tv/1399?language" to ok("""{"name":"Game of Thrones","overview":"Vinter.","seasons":[
            {"seasonNumber":1,"episodeCount":10,"airDate":"2011-04-17"},{"seasonNumber":2,"episodeCount":10}],
            "mediaInfo":{"status":4,"seasons":[{"seasonNumber":1,"status":5}]}}"""))
        val (film, filmSeerr) = query(answers, "discover/seasons?kind=emby&user=eilev&type=movie&tmdb=1399")
        film.use {
            assertEquals(LauncherDiscoverColumns.SEASON.toList(), it.columnNames.toList())
            assertEquals(0, it.count)
        }
        assertEquals(emptyList<String>(), filmSeerr.asked.toList())

        val (series, _) = query(answers, "discover/seasons?kind=emby&user=eilev&type=tv&tmdb=1399")
        series.use {
            assertEquals(listOf(listOf(1, 10, 5, "2011-04-17"), listOf(2, 10, 1, null)), it.rows("number", "episodes", "status", "air_date"))
        }
    }

    @Test fun aBadTypeOrIdAsksNothing() {
        for (title in listOf("type=book&tmdb=603", "type=movie&tmdb=0", "type=movie&tmdb=-3", "type=movie&tmdb=abc", "type=movie", "tmdb=603")) {
            for (path in listOf("discover/title", "discover/related", "discover/seasons")) {
                val (cursor, seerr) = query(mapOf("603" to ok("""{"results":[]}""")), "$path?kind=emby&user=eilev&$title")
                cursor.use { assertEquals("$path $title", 0, it.count) }
                assertEquals("$path $title", emptyList<String>(), seerr.asked.toList())
            }
        }
    }

    @Test fun aTitleComesWithItsCriticAndTheRequestTheyFollow() {
        val answers = mapOf(
            "movie/603?language" to ok("""{"title":"The Matrix","overview":"Ein hacker vaknar.","tagline":"","releaseDate":"1999-03-31",
                "mediaInfo":{"status":3}}"""),
            "movie/603/ratings" to ok("""{"criticsScore":87}"""),
        )
        val following = TrackedRequest("movie:603", 603, "movie", "The Matrix", null, emptySet(), stage = RequestStage.DOWNLOADING,
            requestId = 42, updatedAt = 1)
        // A newer bell on the same title is no request, and has nothing Stova could withdraw.
        val watching = following.copy(key = "watch:movie:603", stage = RequestStage.WATCHING, requestId = 9, updatedAt = 2)
        val (cursor, seerr) = query(answers, "discover/title?kind=emby&user=eilev&type=movie&tmdb=603", personal, LauncherSeerrAccess.PERSONAL,
            seeded = listOf(following, watching))
        cursor.use {
            assertEquals(listOf(listOf("movie", 603, "The Matrix", null, 87, 3, 0, 42, "downloading")),
                it.rows("type", "tmdb", "title", "tagline", "critic", "status", "can_request", "request_id", "request_status"))
            assertEquals("personal", it.extras.getString("seerr"))
        }
        assertTrue(seerr.askedFor("api/v1/movie/603/ratings"))

        val (missing, _) = query(mapOf("movie/603?language" to broken, "movie/603/ratings" to ok("""{"criticsScore":87}""")),
            "discover/title?kind=emby&user=eilev&type=movie&tmdb=603")
        missing.use { assertEquals(0, it.count) }
    }

    @Test fun requestsAskSeerrOnlyForRefreshWithAPersonalSession() {
        val followed = TrackedRequest("tv:1399:1", 1399, "tv", "Game of Thrones", "https://image.tmdb.org/t/p/w500/got.jpg", setOf(1),
            stage = RequestStage.REQUESTED, requestId = 42)
        val signIn = mapOf("auth/me" to broken)
        for (path in listOf("discover/requests?kind=emby&user=eilev", "discover/requests?kind=emby&user=eilev&refresh=1")) {
            val (cursor, seerr) = query(signIn, path, seeded = listOf(followed))
            cursor.use {
                assertEquals(LauncherDiscoverColumns.REQUEST.toList(), it.columnNames.toList())
                assertEquals(path, listOf(listOf("tv", 1399, "requested", "1", 42)), it.rows("type", "tmdb", "stage", "seasons", "request_id"))
                assertEquals("browse", it.extras.getString("seerr"))
            }
            // An API key may not refresh: refresh=1 or not, Seerr hears nothing.
            assertEquals(path, emptyList<String>(), seerr.asked.toList())
        }

        val (kept, personalSeerr) = query(signIn, "discover/requests?kind=emby&user=eilev", personal, LauncherSeerrAccess.PERSONAL, listOf(followed))
        kept.use { assertEquals(1, it.count) }
        assertEquals(emptyList<String>(), personalSeerr.asked.toList())

        // A personal session refreshes; when Seerr fails, what Spole kept is still the answer.
        val (refreshed, refreshSeerr) = query(signIn, "discover/requests?kind=emby&user=eilev&refresh=1", personal, LauncherSeerrAccess.PERSONAL,
            listOf(followed))
        refreshed.use { assertEquals(listOf(listOf("tv", 1399, 42)), it.rows("type", "tmdb", "request_id")) }
        assertTrue(refreshSeerr.askedFor("api/v1/auth/me"))
    }
}
