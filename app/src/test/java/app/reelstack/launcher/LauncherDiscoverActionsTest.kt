package app.reelstack.launcher

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.network.AccountProfileClient
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import app.reelstack.data.network.SeerrServiceClient
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.RequestTrackingRepository
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Requests sent and withdrawn for Stova against a Seerr that only exists in this test: what Seerr
 * was asked and sent, what Spole follows afterwards, and the word Stova gets back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class LauncherDiscoverActionsTest {
    /**
     * Answers by method and the longest address fragment that matches (`"POST api/v1/request"`);
     * anything else is a 404. Keeps every call as `"METHOD url"` and every body it was sent.
     */
    private class FakeSeerr(private val answers: Map<String, HttpResponse>) : JsonHttpTransport {
        val asked = CopyOnWriteArrayList<String>()
        val bodies = CopyOnWriteArrayList<String>()
        override fun get(url: String, headers: Map<String, String>) = answer("GET", url)
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            bodies += jsonBody
            return answer("POST", url)
        }
        override fun delete(url: String, headers: Map<String, String>) = answer("DELETE", url)

        private fun answer(method: String, url: String): HttpResponse {
            asked += "$method $url"
            val key = answers.keys.filter { it.substringBefore(' ') == method && url.contains(it.substringAfter(' ')) }
                .maxByOrNull { it.length } ?: return HttpResponse(404, "{}")
            return answers.getValue(key)
        }
    }

    /** One Seerr, the repositories that talk to it, and the actions under test. */
    private inner class World(answers: Map<String, HttpResponse>) {
        val seerr = FakeSeerr(answers)
        val tracking = RequestTrackingRepository(context, SeerrServiceClient(seerr), AccountProfileClient(transport = seerr))
        val actions = LauncherDiscoverActions(MediaSyncRepository(seerrServiceClient = SeerrServiceClient(seerr)), tracking)
        fun followed(connection: ServiceConnection = personal) = tracking.list(tracking.scope(connection, connection.userId))
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val browse = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "fixture")
    private val personal = browse.copy(userId = "7", sessionCookie = true)

    private fun ok(body: String) = HttpResponse(200, body)
    /** The person Spole's session belongs to: Seerr user 7, who may ask for films and series. */
    private val me = "GET api/v1/auth/me" to ok("""{"id":7,"username":"eilev","permissions":32}""")
    private val matrix = "GET api/v1/movie/603?language" to ok("""{"title":"The Matrix","overview":"Ein hacker vaknar.",
        "posterPath":"/matrix.jpg","mediaInfo":{"status":1}}""")
    private val thrones = "GET api/v1/tv/1399?language" to ok("""{"name":"Game of Thrones","overview":"Vinter.","posterPath":"/got.jpg",
        "seasons":[{"seasonNumber":1,"episodeCount":10},{"seasonNumber":2,"episodeCount":10},{"seasonNumber":3,"episodeCount":10}],
        "mediaInfo":{"status":4,"seasons":[{"seasonNumber":1,"status":5}]}}""")

    private fun title(type: String, tmdb: Int, vararg seasons: Int) = Bundle().apply {
        putString("type", type)
        putInt("tmdb", tmdb)
        if (seasons.isNotEmpty()) putIntArray("seasons", seasons)
    }

    private fun withdrawal(requestId: Int) = Bundle().apply { putInt("request_id", requestId) }

    @Test fun aPersonalFilmRequestIsSentAndFollowedWithTheBell() {
        val world = World(mapOf(me, matrix, "POST api/v1/request" to HttpResponse(201, """{"id":42}""")))
        assertEquals(LauncherDiscoverResult.OK, world.actions.request(title("movie", 603), personal, LauncherSeerrAccess.PERSONAL))

        assertTrue(world.seerr.asked.toString(), "POST https://seerr.example/api/v1/request" in world.seerr.asked)
        // Seerr's own quality and folder: nothing but the title.
        assertEquals(listOf("""{"mediaType":"movie","mediaId":603}"""), world.seerr.bodies.toList())
        val followed = world.followed().single()
        assertEquals(listOf("movie:603:", 603, "movie", "The Matrix", "https://image.tmdb.org/t/p/w500/matrix.jpg", emptySet<Int>(), true, false),
            listOf(followed.key, followed.mediaId, followed.mediaType, followed.title, followed.artworkUrl, followed.seasons, followed.notify,
                followed.is4k))
    }

    @Test fun aFilmIsFollowedWithoutATitleWhenItsDetailsFail() {
        val world = World(mapOf(me, "GET api/v1/movie/603?language" to HttpResponse(500, "{}"), "POST api/v1/request" to HttpResponse(201, "{}")))
        assertEquals(LauncherDiscoverResult.OK, world.actions.request(title("movie", 603), personal, LauncherSeerrAccess.PERSONAL))
        val followed = world.followed().single()
        assertEquals(listOf("movie:603:", "", null, true), listOf(followed.key, followed.title, followed.artworkUrl, followed.notify))
    }

    @Test fun aSeriesRequestSendsTheChosenSeasons() {
        val world = World(mapOf(me, thrones, "POST api/v1/request" to HttpResponse(201, "{}")))
        assertEquals(LauncherDiscoverResult.OK, world.actions.request(title("tv", 1399, 3, 2), personal, LauncherSeerrAccess.PERSONAL))

        assertEquals(listOf("""{"mediaType":"tv","mediaId":1399,"seasons":[2,3]}"""), world.seerr.bodies.toList())
        val followed = world.followed().single()
        assertEquals(listOf("tv:1399:2,3", "Game of Thrones", setOf(2, 3), true), listOf(followed.key, followed.title, followed.seasons, followed.notify))
    }

    @Test fun anApiKeyOrNoSeerrAsksNothing() {
        val world = World(mapOf(me, matrix, "POST api/v1/request" to HttpResponse(201, "{}")))
        assertEquals(LauncherDiscoverResult.NOT_PERSONAL, world.actions.request(title("movie", 603), browse, LauncherSeerrAccess.BROWSE))
        assertEquals(LauncherDiscoverResult.NOT_PERSONAL, world.actions.withdraw(withdrawal(42), browse, LauncherSeerrAccess.BROWSE))
        // An unknown person, a child, or a profile without a Seerr that is set up.
        assertEquals(LauncherDiscoverResult.UNKNOWN, world.actions.request(title("movie", 603), null, LauncherSeerrAccess.NONE))
        assertEquals(LauncherDiscoverResult.UNKNOWN, world.actions.withdraw(withdrawal(42), null, LauncherSeerrAccess.NONE))
        assertEquals(LauncherDiscoverResult.UNKNOWN, world.actions.request(title("movie", 603), personal, LauncherSeerrAccess.NONE))

        assertEquals(emptyList<String>(), world.seerr.asked.toList())
        assertEquals(emptyList<TrackedRequest>(), world.followed(browse) + world.followed())
    }

    @Test fun whatSeerrRefusesIsSaidInAWordAndNotFollowed() {
        val film = title("movie", 603)
        val series = title("tv", 1399, 2)
        for ((case, expected) in listOf(
            Triple(403, film, "forbidden") to LauncherDiscoverResult.FORBIDDEN,
            Triple(401, film, "signed out") to LauncherDiscoverResult.FORBIDDEN,
            Triple(409, film, "asked for already") to LauncherDiscoverResult.ALREADY,
            // Seerr answers 202 when none of the seasons asked for is new.
            Triple(202, series, "no new seasons") to LauncherDiscoverResult.ALREADY,
            Triple(500, film, "broken") to LauncherDiscoverResult.FAILED,
        )) {
            val (status, extras, why) = case
            val world = World(mapOf(me, matrix, thrones, "POST api/v1/request" to HttpResponse(status, "{}")))
            assertEquals(why, expected, world.actions.request(extras, personal, LauncherSeerrAccess.PERSONAL))
            assertTrue(why, world.seerr.asked.any { it.startsWith("POST ") })
            assertEquals(why, emptyList<TrackedRequest>(), world.followed())
        }
    }

    @Test fun aTitleStovaCannotNameFailsWithoutAsking() {
        for (extras in listOf(title("book", 603), title("movie", 0), title("movie", -3), Bundle(), null)) {
            val world = World(mapOf(me, matrix, "POST api/v1/request" to HttpResponse(201, "{}")))
            assertEquals("$extras", LauncherDiscoverResult.FAILED, world.actions.request(extras, personal, LauncherSeerrAccess.PERSONAL))
            assertEquals("$extras", emptyList<String>(), world.seerr.asked.toList())
        }
    }

    @Test fun withdrawFindsTheRequestByItsSeerrIdAndDeletesIt() {
        val requested = TrackedRequest("movie:603:", 603, "movie", "The Matrix", null, emptySet(), stage = RequestStage.REQUESTED, requestId = 42)
        // A bell is no request, and has nothing Stova could withdraw.
        val bell = TrackedRequest("watch:tv:1399:2", 1399, "tv", "Game of Thrones", null, setOf(2), stage = RequestStage.WATCHING, requestId = 9)
        val world = World(mapOf(me,
            "GET api/v1/request/42" to ok("""{"id":42,"requestedBy":{"id":7}}"""),
            "DELETE api/v1/request/42" to HttpResponse(204, "")))
        val scope = world.tracking.scope(personal, personal.userId)
        world.tracking.put(scope, requested)
        world.tracking.put(scope, bell)

        for (unknown in listOf(41, 9, 0)) {
            assertEquals("$unknown", LauncherDiscoverResult.UNKNOWN, world.actions.withdraw(withdrawal(unknown), personal, LauncherSeerrAccess.PERSONAL))
        }
        assertEquals(LauncherDiscoverResult.UNKNOWN, world.actions.withdraw(Bundle(), personal, LauncherSeerrAccess.PERSONAL))
        assertEquals(emptyList<String>(), world.seerr.asked.toList())

        assertEquals(LauncherDiscoverResult.OK, world.actions.withdraw(withdrawal(42), personal, LauncherSeerrAccess.PERSONAL))
        assertTrue(world.seerr.asked.toString(), "DELETE https://seerr.example/api/v1/request/42" in world.seerr.asked)
        assertEquals(listOf("watch:tv:1399:2"), world.followed().map { it.key })
    }

    @Test fun aWithdrawalSeerrRefusesFailsAndKeepsTheFollow() {
        val requested = TrackedRequest("movie:603:", 603, "movie", "The Matrix", null, emptySet(), stage = RequestStage.DOWNLOADING, requestId = 42)
        val world = World(mapOf(me,
            "GET api/v1/request/42" to ok("""{"id":42,"requestedBy":{"id":7}}"""),
            "DELETE api/v1/request/42" to HttpResponse(500, "{}")))
        world.tracking.put(world.tracking.scope(personal, personal.userId), requested)

        assertEquals(LauncherDiscoverResult.FAILED, world.actions.withdraw(withdrawal(42), personal, LauncherSeerrAccess.PERSONAL))
        assertTrue(world.seerr.asked.toString(), "DELETE https://seerr.example/api/v1/request/42" in world.seerr.asked)
        assertEquals(listOf("movie:603:"), world.followed().map { it.key })
    }
}
