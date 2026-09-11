package app.reelstack.data

import app.reelstack.data.model.LibraryArtType
import app.reelstack.data.model.LibraryCardSize
import app.reelstack.data.model.LibraryDisplay
import app.reelstack.data.model.LibraryView
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Display choices are stored as one string per library, so the encoding has to survive a version
 * where a field is added and an older value is read back. A saved preference that silently resets
 * is worse than one that never saved.
 */
class LibraryDisplayTest {

    @Test
    fun `a round trip keeps every field`() {
        val display = LibraryDisplay(
            view = LibraryView.LIST,
            size = LibraryCardSize.LARGE,
            artType = LibraryArtType.THUMB,
            showTitles = false,
        )
        assertEquals(display, LibraryDisplay.decode(display.encode()))
    }

    @Test
    fun `a library that was never opened gets the defaults`() {
        val defaults = LibraryDisplay()
        assertEquals(defaults, LibraryDisplay.decode(null))
        assertEquals(defaults, LibraryDisplay.decode(""))
    }

    /** A value written by an older or newer build must not crash or produce nonsense. */
    @Test
    fun `an unreadable value falls back to the defaults`() {
        assertEquals(LibraryDisplay(), LibraryDisplay.decode("GRID|MEDIUM"))
        assertEquals(LibraryDisplay(), LibraryDisplay.decode("nonsense"))
    }

    @Test
    fun `an unknown enum name falls back to that field's default`() {
        val decoded = LibraryDisplay.decode("CAROUSEL|HUGE|HOLOGRAM|true")
        assertEquals(LibraryView.GRID, decoded.view)
        assertEquals(LibraryCardSize.MEDIUM, decoded.size)
        assertEquals(LibraryArtType.AUTO, decoded.artType)
        assertEquals(true, decoded.showTitles)
    }

    /**
     * The ratio is what the grid measures cells with, so a wrong one is a visibly broken wall of
     * cards rather than a subtle error.
     */
    @Test
    fun `image types carry the ratio their artwork actually has`() {
        assertEquals(2f / 3f, LibraryArtType.POSTER.ratio)
        assertEquals(16f / 9f, LibraryArtType.THUMB.ratio)
        assertEquals(1000f / 185f, LibraryArtType.BANNER.ratio)
        // AUTO measures as a poster, because that is what most library items are.
        assertEquals(LibraryArtType.POSTER.ratio, LibraryArtType.AUTO.ratio)
    }

    @Test
    fun `only AUTO leaves the image type to the server`() {
        assertEquals(null, LibraryArtType.AUTO.api)
        assertEquals("Primary", LibraryArtType.POSTER.api)
        assertEquals("Thumb", LibraryArtType.THUMB.api)
    }
}
