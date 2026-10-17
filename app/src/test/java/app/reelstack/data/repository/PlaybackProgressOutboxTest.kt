package app.reelstack.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.player.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class PlaybackProgressOutboxTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val c = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://example.test", "fixture", "user")
    private val scope get() = PlaybackProgressOutbox.scope("", c)
    private var now = 1_000L
    private fun store() = PlaybackProgressOutbox(context) { now }
    private fun entry(id: String = "first", item: String = "film") = PendingPlaybackProgress(id, scope, item, 60_000,
        false, PlaybackUserState(0, false), now, id)
    @Before fun reset() { store().clear() }

    @Test fun freshStoreReadsTheCommittedLatestCheckpointWithoutCredentials() {
        store().put(entry())
        store().put(entry("latest").copy(positionMs = 30_000))
        assertEquals(30_000L, store().pending(scope).single().positionMs)
        val saved = context.getSharedPreferences("playback_progress_outbox", 0).getString("entries", "")!!
        assertFalse(saved.contains("fixture"))
        assertFalse(saved.contains("example.test"))
        assertFalse(saved.contains("PlaySessionId"))
    }
    @Test fun profilesUsersServersAndTokensNeverShareAQueue() {
        store().put(entry())
        for (other in listOf(PlaybackProgressOutbox.scope("child", c),
            PlaybackProgressOutbox.scope("", c.copy(userId = "child")),
            PlaybackProgressOutbox.scope("", c.copy(baseUrl = "https://other.test")),
            PlaybackProgressOutbox.scope("", c.copy(token = "new")),
            PlaybackProgressOutbox.scope("", c.copy(kind = ServiceKind.EMBY)))) {
            assertNotEquals(scope, other)
            assertTrue(store().pending(other).isEmpty())
        }
    }
    @Test fun staleAcknowledgementOrBlockCannotRemoveANewerSeek() {
        val first = entry()
        val latest = entry("latest")
        store().put(first); store().put(latest)
        store().remove(first); store().block(first)
        assertEquals(listOf(latest), store().pending(scope))
    }
    @Test fun blockedEndpointStaysLocalUntilNewPlaybackReplacesIt() {
        val first = entry()
        store().put(first); store().block(first)
        assertTrue(store().pending(scope).isEmpty())
        store().put(entry("new"))
        assertEquals("new", store().pending(scope).single().id)
    }
    @Test fun boundedQueueCoalescesAndRetiresEntriesAfterThirtyDays() {
        repeat(100) { store().put(entry("$it", "film$it")) }
        assertEquals(PlaybackProgressOutbox.MAX_ENTRIES, store().pending(scope).size)
        assertEquals("99", store().pending(scope).last().id)
        now += PlaybackProgressOutbox.RETENTION_MS + 1
        assertTrue(store().pending(scope).isEmpty())
    }
    @Test fun explicitForgetAndClearInvalidateOtherInstancesImmediately() {
        val first = store()
        first.put(entry()); first.put(entry("other", "episode"))
        store().forget(scope, "film")
        assertFalse(first.current(entry()))
        assertEquals("episode", first.pending(scope).single().itemId)
        store().clear()
        assertTrue(first.pending(scope).isEmpty())
    }
}
