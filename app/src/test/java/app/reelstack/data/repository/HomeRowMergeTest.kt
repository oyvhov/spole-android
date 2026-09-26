package app.reelstack.data.repository

import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRowMergeTest {
    private fun items(source: ServiceKind, count: Int) = (1..count).map { index ->
        LibraryMedia(id = "${source.name}-$index", title = "$index", subtitle = "", artworkRes = R.drawable.media_placeholder, source = source)
    }

    @Test fun eachServerKeepsItsOwnRowWhenTheRefreshLands() {
        // With two servers the merged list used to be cut to twelve: each server's row went
        // 6 → 12 → 6 cards in one refresh.
        val merged = mergeHomeRows(listOf(
            homeRowForServer(items(ServiceKind.JELLYFIN, 15), HOME_RESUME_PER_SERVER),
            homeRowForServer(items(ServiceKind.EMBY, 15), HOME_RESUME_PER_SERVER),
        ))
        assertEquals(12, merged.count { it.source == ServiceKind.JELLYFIN })
        assertEquals(12, merged.count { it.source == ServiceKind.EMBY })
    }

    @Test fun theMergeOrderIsFixedByServerNotByWhoAnsweredFirst() {
        val jellyfin = items(ServiceKind.JELLYFIN, 2)
        val emby = items(ServiceKind.EMBY, 2)
        assertEquals(listOf("JELLYFIN-1", "EMBY-1", "JELLYFIN-2", "EMBY-2"), mergeHomeRows(listOf(jellyfin, emby)).map { it.id })
    }

    @Test fun duplicatesFromOneServerAreShownOnce() {
        val twice = items(ServiceKind.EMBY, 2) + items(ServiceKind.EMBY, 1)
        assertEquals(listOf("EMBY-1", "EMBY-2"), homeRowForServer(twice, HOME_ROW_PER_SERVER).map { it.id })
    }
}
