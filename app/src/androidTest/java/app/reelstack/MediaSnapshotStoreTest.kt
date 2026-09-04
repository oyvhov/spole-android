package app.reelstack

import android.content.Context
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
    fun migratesTheOldRecentlyAddedSettingToBothNewRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)
        preferences.edit()
            .putStringSet("home_sections", setOf("NOW_PLAYING", "RECENTLY_ADDED"))
            .commit()

        val visible = AppPreferencesRepository(context).visibleHomeSections

        assertEquals(setOf(HomeSection.NOW_PLAYING, HomeSection.RECENT_MOVIES, HomeSection.RECENT_SERIES), visible)
        AppPreferencesRepository(context).visibleHomeSections = HomeSection.entries.toSet()
    }

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
            recentMovies = listOf(
                LibraryMedia(
                    "movie-1",
                    "The Odyssey",
                    "Movie · 2026",
                    null,
                    R.drawable.desert_arrival,
                    ServiceKind.JELLYFIN,
                    "https://media.example/Items/movie-1/Images/Primary",
                ),
            ),
            recentSeries = listOf(
                LibraryMedia(
                    "series-1",
                    "Severance",
                    "S02 E04 · Woe's Hollow",
                    null,
                    R.drawable.session_still,
                    ServiceKind.JELLYFIN,
                    "https://media.example/Items/series-1/Images/Primary",
                ),
            ),
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
        assertEquals("The Odyssey", restored?.recentMovies?.single()?.title)
        assertEquals("Severance", restored?.recentSeries?.single()?.title)
        assertEquals("https://media.example/Items/series-1/Images/Primary", restored?.recentSeries?.single()?.artworkUrl)
        assertEquals(Instant.parse("2026-09-04T08:00:00Z").toEpochMilli(), restored?.refreshedAtEpochMillis)

        store.clear()
        assertNull(store.read())
    }
}
