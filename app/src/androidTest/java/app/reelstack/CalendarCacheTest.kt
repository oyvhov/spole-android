package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.*
import app.reelstack.data.repository.cache.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class CalendarCacheTest {
    @Test fun wholeCalendarRoundTripsDateIdentityAndLibraryReferencesBeyondOldLimits() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=MediaSnapshotStore(context)
        val items=(1..85).map { number -> UpcomingMedia("episode-$number","Series","Episode $number","date",0,
            R.drawable.media_placeholder,ServiceKind.SEERR,mediaType="Episode",tmdbId=42,season=2,episode=number,
            releaseDate="2026-10-12",region="NO",libraryRemoteId=if(number==1)"own-episode" else null) }
        val snapshot=MediaSyncSnapshot(sessions=emptyList(),recentMovies=emptyList(),recentSeries=emptyList(),
            upcoming=items,incoming=emptyList(),discover=emptyList(),activity=emptyList(),
            successfulServices=setOf(ServiceKind.SEERR),errors=emptyMap(),refreshedAt=Instant.now())
        try {
            store.save(snapshot,"calendar-cache-fixture")
            val restored=requireNotNull(store.read("calendar-cache-fixture")).upcoming
            assertEquals(85,restored.size);assertEquals(items.map { it.calendarIdentity },restored.map { it.calendarIdentity })
            assertTrue(restored.all { it.releaseDate=="2026-10-12" && it.region=="NO" })
            assertEquals("own-episode",restored.first().libraryRemoteId)
        } finally { store.clear() }
    }
    @Test fun unsupportedCachedSourcesAreDroppedInsteadOfBecomingJellyfin() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val dao=CacheDatabase.get(context).cacheDao()
        val row=CachedMediaRow(fingerprint="old-calendar-fixture",section=CacheSection.RECENT_MOVIES.name,position=0,
            id="old",title="Old",subtitle="",source="SONARR",mediaType="Movie",progress=null,artworkUrl=null,
            remoteId="old-server-id",overview=null,facts="",genres="",dateLabel=null,airDateEpochMillis=null,seerrStatus=null)
        try {
            dao.insertRows(listOf(row))
            assertTrue(MediaSnapshotStore(context).libraryPage("old-calendar-fixture",CacheSection.RECENT_MOVIES,30,0).isEmpty())
        } finally { MediaSnapshotStore(context).clear() }
    }
}
