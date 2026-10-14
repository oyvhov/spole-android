package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.LocalPlaybackStore
import app.reelstack.player.PlayableItem
import org.junit.Assert.*
import org.junit.Test

class LocalPlaybackStoreTest {
    @Test fun rewindingToTheBeginningReplacesAnOlderResumeWithoutMakingATinyResumeCard() {
        val store = LocalPlaybackStore(context)
        store.clear()
        try {
            store.record(account, item, 40_000, 60_000, false)
            val rewind = store.record(account, item, 5_000, 60_000, false)!!
            assertEquals(5_000L, store.resume(account, item).resumeMs)
            assertTrue(store.merge(account, emptyList()).isEmpty())
            store.confirm(account, item.id, rewind, 5_000, false)
            assertEquals(5_000L, store.resume(account, item.copy(resumeMs = 5_000), fromServer = true).resumeMs)
            assertTrue(store.merge(account, emptyList()).isEmpty())
        } finally { store.clear() }
    }
    @Test fun acknowledgedProgressDefersToFreshServerReadsEvenWithAnOlderTimestamp() {
        val store = LocalPlaybackStore(context)
        store.clear()
        try {
            for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
                val connection = account.copy(kind = kind)
                val journal = store.record(connection, item, 30_000, 60_000, false)!!
                store.confirm(connection, item.id, journal, 30_000, false)
                val remote = item.copy(resumeMs = 12_000, lastPlayedEpochMillis = 1)
                assertEquals(30_000L, store.resume(connection, remote).resumeMs)
                assertEquals(12_000L, store.resume(connection, remote, fromServer = true).resumeMs)
                val row = LibraryMedia("${kind.name.lowercase()}-episode", "Series", "", artworkRes = 0,
                    source = kind, progress = .2f, lastActivityEpochMillis = 1)
                assertEquals(listOf(row), store.merge(connection, listOf(row), fromServer = true))
                assertEquals(listOf(row), store.merge(connection, listOf(row)))
                assertTrue(store.merge(connection, emptyList(), fromServer = true).isEmpty())
                assertEquals(listOf(row), store.nextUp(connection, listOf(row), fromServer = true))
                assertTrue(store.resume(connection, remote.copy(played = true, resumeMs = 0), fromServer = true).played)
                // A new unsaved local seek still wins until that exact journal is acknowledged.
                store.record(connection, item, 40_000, 60_000, false)
                assertEquals(40_000L, store.resume(connection, remote, fromServer = true).resumeMs)
                assertEquals(2f / 3, store.merge(connection, listOf(row), fromServer = true).single().progress!!, .001f)
            }
        } finally { store.clear() }
    }
    @Test fun fiveSecondsRemainingDoesNotReappearAndServerConfigurationWins() {
        val store = LocalPlaybackStore(context)
        store.clear()
        try {
            val journal = store.record(account, item, 595_000, 600_000, false)!!
            assertTrue(store.merge(account, emptyList()).isEmpty())
            assertTrue(LocalPlaybackStore(context).resume(account, item).played)
            // A server with a custom 99.9% threshold still considers this resumable.
            store.confirm(account, item.id, journal, 595_000, false)
            assertEquals(595_000L, store.resume(account, item).resumeMs)
            assertFalse(store.resume(account, item).played)
            assertEquals(1, store.merge(account, emptyList()).size)
            store.confirm(account, item.id, journal, 0, true)
            assertTrue(store.merge(account, emptyList()).isEmpty())
            assertEquals(0L, store.resume(account, item).resumeMs)
        } finally { store.clear() }
    }

    @Test fun staleServerAcknowledgementCannotOverwriteARewatchAndZeroPositionDoesNotMakeAResumeCard() {
        val store = LocalPlaybackStore(context)
        store.clear()
        try {
            val old = store.record(account, item, 595_000, 600_000, false)!!
            val latest = store.record(account, item, 60_000, 600_000, false)!!
            store.confirm(account, item.id, old, 0, true)
            assertEquals(60_000L, store.resume(account, item).resumeMs)
            store.confirm(account, item.id, latest, 0, false)
            assertTrue(store.merge(account, emptyList()).isEmpty())
            assertFalse(store.resume(account, item).played)
            val next = LibraryMedia("jellyfin-episode", "Series", "", artworkRes = 0, source = ServiceKind.JELLYFIN)
            assertEquals(listOf(next), store.nextUp(account, listOf(next)))
        } finally { store.clear() }
    }
    @Test fun embyAndJellyfinWithIdenticalItemIdsKeepSeparateProgress() {
        val store = LocalPlaybackStore(context)
        val emby = account.copy(kind = ServiceKind.EMBY)
        store.clear()
        try {
            store.record(account, item, 10_000, 60_000, false)
            store.record(emby, item, 30_000, 60_000, false)
            val rows = store.merge(emby, store.merge(account, emptyList()))
            assertEquals(setOf("jellyfin-episode", "emby-episode"), rows.map { it.id }.toSet())
            assertEquals(.5f, rows.single { it.source == ServiceKind.EMBY }.progress!!, .0001f)
            assertEquals(10_000L, store.resume(account, item).resumeMs)
            assertEquals(30_000L, store.resume(emby, item).resumeMs)
            assertEquals(listOf("jellyfin-episode"), store.nextUp(emby, rows).map { it.id })
        } finally { store.clear() }
    }
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
            store.record(account, item, 10_000, 60_000, false)
            assertEquals("jellyfin-episode", store.merge(account, emptyList()).single().id)
            assertEquals(10_000L, store.resume(account, item).resumeMs)
            val current = store.merge(account, emptyList()).single()
            assertTrue(store.nextUp(account, listOf(current)).isEmpty())
            store.forget(account, item.id)
            assertEquals(listOf(current), store.nextUp(account, listOf(current)))
            // A fresh start is intentionally not a visible resume entry.
            store.record(account, item, 0, 60_000, false)
            assertTrue(store.merge(account, emptyList()).isEmpty())
        } finally { store.clear() }
    }
}
