package app.reelstack.offline

import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineStoragePolicyTest {
    @Test fun `only direct complete jellyfin and emby files qualify`() {
        assertNull(OfflineMediaCandidate("item", ServiceKind.JELLYFIN, directDownloadUrl = "https://example/file.mp4").offlineRefusal())
        assertEquals(OfflineRefusal.TRANSCODE_REQUIRED, OfflineMediaCandidate("item", ServiceKind.EMBY, requiresTranscode = true, directDownloadUrl = "https://example/file.mp4").offlineRefusal())
        assertEquals(OfflineRefusal.LIVE_TV, OfflineMediaCandidate("item", ServiceKind.JELLYFIN, isLive = true, directDownloadUrl = "https://example/file.ts").offlineRefusal())
        assertEquals(OfflineRefusal.UNSUPPORTED_SERVICE, OfflineMediaCandidate("item", ServiceKind.SEERR, directDownloadUrl = "https://example/file.mp4").offlineRefusal())
    }
}
