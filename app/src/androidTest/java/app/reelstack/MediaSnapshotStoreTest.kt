package app.reelstack

import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.repository.MediaSnapshotStore
import app.reelstack.data.repository.MediaSyncSnapshot
import app.reelstack.data.repository.AppPreferencesRepository
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaSnapshotStoreTest {
    @Test
    fun persistsVisibleHomeSections() {
        val repository = AppPreferencesRepository(ApplicationProvider.getApplicationContext())
        val selected = setOf(HomeSection.NOW_PLAYING, HomeSection.UPCOMING)

        repository.visibleHomeSections = selected

        assertEquals(selected, repository.visibleHomeSections)
        repository.visibleHomeSections = HomeSection.entries.toSet()
    }

    @Test
    fun roundTripsDashboardWithoutConnectionSecrets() {
        val store = MediaSnapshotStore(ApplicationProvider.getApplicationContext())
        store.clear()
        val snapshot = MediaSyncSnapshot(
            sessions = listOf(PlaybackSession(
                userName = "Maya",
                deviceName = "TV",
                title = "Severance",
                subtitle = "S02 E04",
                progress = 0.5f,
                timeLeft = "30 min left",
                streamMethod = "Direct play",
                quality = "4K",
                paused = false,
                artworkUrl = "https://media.example/Items/series-1/Images/Primary",
                sessionId = "session-1",
                source = ServiceKind.JELLYFIN,
            )),
            continueWatching = listOf(
                LibraryMedia(
                    "episode-4",
                    "Severance",
                    "S02 E04",
                    0.5f,
                    R.drawable.session_still,
                    ServiceKind.JELLYFIN,
                    "https://media.example/Items/series-1/Images/Primary",
                ),
            ),
            recentlyAdded = emptyList(),
            upcoming = emptyList(),
            incoming = emptyList(),
            discover = emptyList(),
            activity = emptyList(),
            successfulServices = setOf(ServiceKind.JELLYFIN),
            errors = emptyMap(),
            refreshedAt = Instant.parse("2026-09-04T08:00:00Z"),
        )

        store.save(snapshot)
        val restored = store.read()

        assertEquals("Severance", restored?.sessions?.single()?.title)
        assertEquals("session-1", restored?.sessions?.single()?.sessionId)
        assertEquals("https://media.example/Items/series-1/Images/Primary", restored?.sessions?.single()?.artworkUrl)
        assertEquals(0.5f, restored?.continueWatching?.single()?.progress ?: 0f, 0.001f)
        assertEquals("https://media.example/Items/series-1/Images/Primary", restored?.continueWatching?.single()?.artworkUrl)
        assertEquals(Instant.parse("2026-09-04T08:00:00Z").toEpochMilli(), restored?.refreshedAtEpochMillis)

        store.clear()
        assertNull(store.read())
    }
}
