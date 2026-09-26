package app.reelstack.ui.screens

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineDownloadsPresentationTest {
    @Test fun `private storage size stays legible without locale dependent output`() {
        assertEquals("0 B", formatOfflineBytes(0))
        assertEquals("1 KB", formatOfflineBytes(1_024))
        assertEquals("1.5 MB", formatOfflineBytes(1_572_864))
        assertEquals("2.0 GB", formatOfflineBytes(2L * 1_024 * 1_024 * 1_024))
    }

    @Test fun `norwegian sizes use a decimal comma`() {
        // The screen read «53.6 MB av 1.8 GB» in the middle of a Norwegian sentence.
        assertEquals("53,6 MB", formatOfflineBytes(56_203_674, Locale.forLanguageTag("nn")))
        assertEquals("1,8 GB", formatOfflineBytes(1_932_735_283, Locale.forLanguageTag("nb")))
        assertEquals("1.8 GB", formatOfflineBytes(1_932_735_283, Locale.ENGLISH))
    }
}
