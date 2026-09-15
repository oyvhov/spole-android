package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.LocalPlaybackStore
import app.reelstack.player.PlayableItem
import org.junit.Assert.*
import org.junit.Test

class LocalPlaybackStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val account = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://example.test", "fixture", "user")
    private val item = PlayableItem("episode", "Series", "Episode", durationMs = 60_000, season = 2, episode = 3)

    @Test fun storedProgressWinsStaleServerRowsWithoutLosingArtwork() {
        val store = LocalPlaybackStore(context)
        store.clear()
        try {
            store.record(account, item, 20_000, 60_000, false)
            val old = LibraryMedia("jellyfin-episode", "Series", "", artworkRes = 0, source = ServiceKind.JELLYFIN,
                artworkUrl = "https://example.test/art", heroUrl = "https://example.test/hero", progress = .1f)
            val row = LocalPlaybackStore(context).merge(account, listOf(old)).single()
            assertEquals(1f / 3, row.progress!!, .0001f)
            assertEquals(old.heroUrl, row.heroUrl)
            assertEquals(old.artworkUrl, row.artworkUrl)
            assertTrue(store.merge(account.copy(token = "different"), emptyList()).isEmpty())
            assertTrue(store.merge(account.copy(userId = "different"), emptyList()).isEmpty())
            val newer = old.copy(lastActivityEpochMillis = System.currentTimeMillis() + 60_000, progress = .6f)
            assertEquals(newer, store.merge(account, listOf(newer)).single())
        } finally { store.clear() }
    }

    @Test fun finishedItemIsRemovedButARewatchCanReturn() {
        val store = LocalPlaybackStore(context)
        store.clear()
        try {
            store.record(account, item, 59_000, 60_000, true)
            assertTrue(store.merge(account, emptyList()).isEmpty())
            assertEquals(0L, store.resume(account, item).resumeMs)
            store.record(account, item, 5_000, 60_000, false)
            assertEquals("jellyfin-episode", store.merge(account, emptyList()).single().id)
            assertEquals(5_000L, store.resume(account, item).resumeMs)
            val current = store.merge(account, emptyList()).single()
            assertTrue(store.nextUp(account, listOf(current)).isEmpty())
            store.forget(account, item.id)
            assertEquals(listOf(current), store.nextUp(account, listOf(current)))
            store.record(account, item, 0, 60_000, false)
            assertEquals("jellyfin-episode", store.merge(account, emptyList()).single().id)
        } finally { store.clear() }
    }
}
