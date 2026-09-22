package app.reelstack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineDownloadsPresentationTest {
    @Test fun `private storage size stays legible without locale dependent output`() {
        assertEquals("0 B", formatOfflineBytes(0))
        assertEquals("1 KB", formatOfflineBytes(1_024))
        assertEquals("1.5 MB", formatOfflineBytes(1_572_864))
        assertEquals("2.0 GB", formatOfflineBytes(2L * 1_024 * 1_024 * 1_024))
    }
}
