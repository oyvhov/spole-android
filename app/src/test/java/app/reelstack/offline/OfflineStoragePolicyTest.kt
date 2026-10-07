package app.reelstack.offline

import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import androidx.media3.exoplayer.offline.Download

class OfflineStoragePolicyTest {
    @Test fun `wifi rule rejects unmetered cellular and accepts cable`() {
        assertEquals(false, offlineNetworkAllowed(true, false, false, true))
        assertEquals(false, offlineNetworkAllowed(true, true, false, true))
        assertEquals(true, offlineNetworkAllowed(true, false, true, false))
        assertEquals(true, offlineNetworkAllowed(true, true, false, false))
        assertEquals(true, offlineNetworkAllowed(false, false, false, true))
    }
    @Test fun `actual cache budget leaves space and accounts for the next file`() {
        val gib = 1024L * 1024 * 1024
        assertEquals(true, OfflineSpacePolicy.allows(gib, 4 * gib, 16 * gib, gib))
        assertEquals(false, OfflineSpacePolicy.allows(gib, 512 * 1024, 16 * gib))
        assertEquals(false, OfflineSpacePolicy.allows(31 * gib, 100 * gib, 256 * gib, 2 * gib))
        assertEquals(false, OfflineSpacePolicy.allows(8 * gib, 4 * gib, 10 * gib, 1))
    }
    @Test fun `only direct complete jellyfin and emby files qualify`() {
        assertNull(OfflineMediaCandidate("item", ServiceKind.JELLYFIN, directDownloadUrl = "https://example/file.mp4").offlineRefusal())
        assertEquals(OfflineRefusal.TRANSCODE_REQUIRED, OfflineMediaCandidate("item", ServiceKind.EMBY, requiresTranscode = true, directDownloadUrl = "https://example/file.mp4").offlineRefusal())
        assertEquals(OfflineRefusal.LIVE_TV, OfflineMediaCandidate("item", ServiceKind.JELLYFIN, isLive = true, directDownloadUrl = "https://example/file.ts").offlineRefusal())
        assertEquals(OfflineRefusal.UNSUPPORTED_SERVICE, OfflineMediaCandidate("item", ServiceKind.SEERR, directDownloadUrl = "https://example/file.mp4").offlineRefusal())
    }

    @Test fun `download state presentation never treats a failed or paused job as ready`() {
        assertEquals(OfflineDownloadState.COMPLETE, offlineDownloadState(Download.STATE_COMPLETED))
        assertEquals(OfflineDownloadState.PAUSED, offlineDownloadState(Download.STATE_STOPPED))
        assertEquals(OfflineDownloadState.FAILED, offlineDownloadState(Download.STATE_FAILED))
        assertEquals(OfflineDownloadState.DOWNLOADING, offlineDownloadState(Download.STATE_DOWNLOADING))
    }
}
