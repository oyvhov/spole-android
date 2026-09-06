package app.reelstack

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.canRequest
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
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
            .remove("home_sections_version")
            .putStringSet("home_sections", setOf("NOW_PLAYING", "RECENTLY_ADDED"))
            .commit()

        val visible = AppPreferencesRepository(context).visibleHomeSections

        assertEquals(setOf(HomeSection.NOW_PLAYING, HomeSection.RECOMMENDATIONS, HomeSection.RECENT_RELEASES, HomeSection.JELLYFIN_MOVIES, HomeSection.EMBY_MOVIES, HomeSection.JELLYFIN_SERIES, HomeSection.EMBY_SERIES), visible)
        AppPreferencesRepository(context).visibleHomeSections = HomeSection.entries.toSet()
    }

    @Test
    fun persistsVisibleHomeSections() {
        val repository = AppPreferencesRepository(ApplicationProvider.getApplicationContext())
        val selected = setOf(HomeSection.EMBY_MOVIES, HomeSection.JELLYFIN_SERIES, HomeSection.UPCOMING)

        repository.visibleHomeSections = selected

        assertEquals(selected, AppPreferencesRepository(ApplicationProvider.getApplicationContext()).visibleHomeSections)
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
            recentReleases = listOf(
                UpcomingMedia(
                    id = "recent-release-1",
                    title = "Ny film",
                    subtitle = "Film · 2026",
                    dateLabel = "I går",
                    airDateEpochMillis = Instant.parse("2026-09-03T20:00:00Z").toEpochMilli(),
                    artworkRes = R.drawable.desert_arrival,
                    source = ServiceKind.RADARR,
                    artworkUrl = "https://media.example/poster.jpg",
                    mediaType = "Movie",
                ),
            ),
            incoming = emptyList(),
            discover = listOf(DiscoverMedia("blocked", "Blokkert film", "Film", R.drawable.desert_arrival, false, seerrStatus = 6)),
            activity = emptyList(),
            successfulServices = setOf(ServiceKind.JELLYFIN),
            errors = emptyMap(),
            refreshedAt = Instant.parse("2026-09-04T08:00:00Z"),
        )

        store.save(snapshot)
        val restored = store.read()

        assertEquals(emptyList<PlaybackSession>(), restored?.sessions)
        assertEquals("The Odyssey", restored?.recentMovies?.single()?.title)
        assertEquals(R.drawable.media_placeholder, restored?.recentMovies?.single()?.artworkRes)
        assertEquals(6, restored?.discover?.single()?.seerrStatus)
        assertEquals(false, restored?.discover?.single()?.canRequest)
        assertEquals(R.drawable.media_placeholder, restored?.discover?.single()?.artworkRes)
        assertEquals("Film · 2026", restored?.recentMovies?.single()?.subtitle)
        assertEquals("Severance", restored?.recentSeries?.single()?.title)
        assertEquals("https://media.example/Items/series-1/Images/Primary", restored?.recentSeries?.single()?.artworkUrl)
        assertEquals("Ny film", restored?.recentReleases?.single()?.title)
        assertEquals(Instant.parse("2026-09-04T08:00:00Z").toEpochMilli(), restored?.refreshedAtEpochMillis)

        store.clear()
        assertNull(store.read())
    }
}
