package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackProgressSyncTest {
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://example.test/jellyfin", "fixture", "user")
    private val baseline = PlaybackUserState(100_000_000, false, 2, "2026-10-16T10:00:00Z")
    private val entry = PendingPlaybackProgress("one", "scope", "film", 60_000, false, baseline, 1_792_230_000_000, "journal")
    private class Queue(var row: PendingPlaybackProgress?) : PlaybackProgressQueue {
        override fun pending(scope: String) = listOfNotNull(row?.takeIf { it.scope == scope && !it.blocked })
        override fun current(entry: PendingPlaybackProgress) = row == entry
        override fun remove(entry: PendingPlaybackProgress) { if (current(entry)) row = null }
        override fun block(entry: PendingPlaybackProgress) { if (current(entry)) row = entry.copy(blocked = true) }
    }
    private inner class Remote(var state: PlaybackUserState = baseline) : PlaybackProgressRemote {
        var reads = 0
        var writes = 0
        var failRead = false
        var failAfterWrite = false
        var supported = true
        var onRead: () -> Unit = {}
        override fun read(connection: ServiceConnection, itemId: String): RemotePlaybackProgress {
            reads++
            if (failRead) throw java.io.IOException()
            onRead()
            return RemotePlaybackProgress(state, JsonObject(emptyMap()))
        }
        override fun write(connection: ServiceConnection, entry: PendingPlaybackProgress, remote: RemotePlaybackProgress): Boolean {
            writes++
            if (!supported) return false
            state = state.copy(positionTicks = entry.positionTicks, played = entry.completed || state.played)
            if (failAfterWrite) throw java.io.IOException()
            return true
        }
    }

    @Test fun unchangedRemoteIsUpdatedAndReadBackBeforeAcknowledgement() {
        val queue = Queue(entry)
        val remote = Remote()
        var confirmed: PlaybackUserState? = null
        assertEquals(PlaybackSyncResult.SAVED, PlaybackProgressSync(queue, remote).sync(connection, entry, { true }) { confirmed = it })
        assertEquals(2, remote.reads)
        assertEquals(1, remote.writes)
        assertEquals(600_000_000L, confirmed!!.positionTicks)
        assertNull(queue.row)
    }

    @Test fun rewindWatchedResetPlayCountAndLastPlayedChangesAllDefeatAnOldCheckpoint() {
        for (changed in listOf(baseline.copy(positionTicks = 20_000_000), baseline.copy(played = true),
            baseline.copy(playCount = 3), baseline.copy(lastPlayed = "2026-10-15T10:00:00Z"))) {
            val queue = Queue(entry)
            val remote = Remote(changed)
            var confirmed: PlaybackUserState? = null
            assertEquals(PlaybackSyncResult.CONFLICT, PlaybackProgressSync(queue, remote).sync(connection, entry, { true }) { confirmed = it })
            assertEquals(0, remote.writes)
            assertEquals(changed, confirmed)
            assertNull(queue.row)
        }
    }

    @Test fun lostPostResponseIsAcknowledgedOnRetryWithoutWritingTwice() {
        val queue = Queue(entry)
        val remote = Remote().apply { failAfterWrite = true }
        val sync = PlaybackProgressSync(queue, remote)
        assertEquals(PlaybackSyncResult.RETRY, sync.sync(connection, entry, { true }) { fail() })
        assertEquals(entry, queue.row)
        assertEquals(PlaybackSyncResult.SAVED, sync.sync(connection, entry, { true }) {})
        assertEquals(1, remote.writes)
        assertNull(queue.row)
    }

    @Test fun networkFailureRetainsCheckpointAndDoesNotWrite() {
        val queue = Queue(entry)
        val remote = Remote().apply { failRead = true }
        assertEquals(PlaybackSyncResult.RETRY, PlaybackProgressSync(queue, remote).sync(connection, entry, { true }) { fail() })
        assertEquals(entry, queue.row)
        assertEquals(0, remote.writes)
    }

    @Test fun switchAwayAndBackDuringReadStillRevokesThisAttempt() {
        var generation = 5
        val owner = generation
        val queue = Queue(entry)
        val remote = Remote().apply { onRead = { generation += 2 } }
        assertEquals(PlaybackSyncResult.CANCELLED,
            PlaybackProgressSync(queue, remote).sync(connection, entry, { generation == owner }) { fail() })
        assertEquals(0, remote.writes)
        assertEquals(entry, queue.row)
    }

    @Test fun newerLocalCheckpointDuringReadIsNeitherWrittenNorAcknowledged() {
        val queue = Queue(entry)
        val newer = entry.copy(id = "new", positionMs = 30_000)
        val remote = Remote().apply { onRead = { queue.row = newer } }
        assertEquals(PlaybackSyncResult.CANCELLED, PlaybackProgressSync(queue, remote).sync(connection, entry, { true }) { fail() })
        assertEquals(0, remote.writes)
        assertEquals(newer, queue.row)
    }

    @Test fun livePlaybackPreventsEvenTheReadAndUnsupportedWritesStayLocal() {
        val queue = Queue(entry)
        val remote = Remote().apply { supported = false }
        val sync = PlaybackProgressSync(queue, remote)
        assertEquals(PlaybackSyncResult.CANCELLED, sync.sync(connection, entry, { false }) { fail() })
        assertEquals(0, remote.reads)
        assertEquals(PlaybackSyncResult.BLOCKED, sync.sync(connection, entry, { true }) { fail() })
        assertTrue(queue.row!!.blocked)
        assertTrue(queue.pending("scope").isEmpty())
    }

    @Test fun onlyAnActualEndSetsWatchedAndARewatchDoesNotResetAnExistingWatchedFlag() {
        val end = entry.copy(completed = true)
        val remote = Remote()
        assertEquals(PlaybackSyncResult.SAVED, PlaybackProgressSync(Queue(end), remote).sync(connection, end, { true }) {})
        assertTrue(remote.state.played)
        assertEquals(0L, remote.state.positionTicks)
        val rewatch = entry.copy(baseline = baseline.copy(played = true))
        val watched = Remote(rewatch.baseline)
        PlaybackProgressSync(Queue(rewatch), watched).sync(connection, rewatch, { true }) {}
        assertTrue(watched.state.played)
        assertEquals(600_000_000L, watched.state.positionTicks)
    }

    @Test fun parserRejectsIncompleteStateAndNormalisesDateRepresentations() {
        assertNull(PlaybackUserState.parse(json("""{"PlaybackPositionTicks":0}""")))
        assertNull(PlaybackUserState.parse(json("""{"Played":false,"PlaybackPositionTicks":-1}""")))
        val one = PlaybackUserState.parse(json("""{"Played":false,"PlaybackPositionTicks":0,"LastPlayedDate":"2026-10-16T10:00:00.000Z"}"""))
        val two = PlaybackUserState.parse(json("""{"Played":false,"PlaybackPositionTicks":0,"LastPlayedDate":"2026-10-16T12:00:00+02:00"}"""))
        assertEquals(one, two)
    }

    @Test fun transportUsesOnlyUserDataWithOwnUserAndPreservesEmbyFlags() {
        for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            val c = connection.copy(kind = kind, userId = "user/id")
            var posted = ""
            var body = JsonObject(emptyMap())
            val data = json("""{"PlaybackPositionTicks":100000000,"Played":false,"PlayCount":7,"IsFavorite":true,"Rating":8.5,"Likes":true}""")
            val transport = object : JsonHttpTransport {
                override fun get(url: String, headers: Map<String, String>): HttpResponse {
                    assertTrue(url.endsWith("/Users/user%2Fid/Items/film"))
                    assertFalse(url.contains("fixture"))
                    return HttpResponse(200, buildJsonObject { put("UserData", data) }.toString())
                }
                override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
                    posted = url; body = json(jsonBody)
                    assertTrue(headers.values.any { it.contains("fixture") })
                    return HttpResponse(204, "")
                }
            }
            val client = PlaybackProgressClient("device", transport)
            assertTrue(client.write(c, entry, client.read(c, entry.itemId)))
            assertEquals(600_000_000L, body["PlaybackPositionTicks"]!!.jsonPrimitive.long)
            assertFalse(posted.contains("Sessions"))
            if (kind == ServiceKind.JELLYFIN) {
                assertTrue(posted.endsWith("/UserItems/film/UserData?userId=user%2Fid"))
                assertFalse(body.containsKey("IsFavorite"))
                assertFalse(body.containsKey("PlayCount"))
            } else {
                assertTrue(posted.endsWith("/Users/user%2Fid/Items/film/UserData"))
                assertEquals(JsonPrimitive(true), body["IsFavorite"])
                assertEquals(JsonPrimitive(7), body["PlayCount"])
                assertEquals(JsonPrimitive(8.5), body["Rating"])
            }
        }
    }

    @Test fun unsupportedEndpointNeverFallsBackToAPlaybackSession() {
        for (status in listOf(400, 401, 403, 404, 405)) {
            var calls = 0
            val client = PlaybackProgressClient("device", object : JsonHttpTransport {
                override fun get(url: String, headers: Map<String, String>) = error("Not used")
                override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
                    calls++; assertTrue(url.contains("UserData")); return HttpResponse(status, "")
                }
            })
            assertFalse(client.write(connection, entry, RemotePlaybackProgress(baseline, JsonObject(emptyMap()))))
            assertEquals(1, calls)
        }
    }

    @Test fun revokedAccessOrDeletedItemBlocksRetriesBeforeAnyWrite() {
        for (status in listOf(401, 403, 404)) {
            val queue = Queue(entry)
            val client = PlaybackProgressClient("device", object : JsonHttpTransport {
                override fun get(url: String, headers: Map<String, String>) = HttpResponse(status, "")
                override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Must not write")
            })
            assertEquals(PlaybackSyncResult.BLOCKED, PlaybackProgressSync(queue, client).sync(connection, entry, { true }) { fail() })
            assertTrue(queue.row!!.blocked)
        }
    }
    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject
}
