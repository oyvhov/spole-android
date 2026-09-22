package app.reelstack.offline

import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import androidx.media3.exoplayer.offline.Download

class OfflineStoragePolicyTest {
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
