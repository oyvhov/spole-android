package app.reelstack.ui.components

import org.junit.Assert.*
import org.junit.Test

class MediaCaptionTest {
    @Test fun absentAndPlaceholderMetadataHasNoCaption() {
        listOf(null, "", "  ", "TBA", " tba ", "N/A", "-").forEach { assertNull(readableMediaText(it)) }
    }
    @Test fun realOverviewAndTitlesRemainIntact() {
        assertEquals("Ei ekte omtale med TBA inni.", readableMediaText(" Ei ekte omtale med TBA inni. "))
        assertEquals("Unknown", readableMediaText("Unknown"))
    }
}
