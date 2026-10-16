package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryMenuTest {
    @Test fun aTelevisionMenuAlwaysOpensTheLibraryPage() {
        // Films, series and smart shelves have their own entries in the menu; Bibliotek is the page about them.
        assertTrue(libraryMenuOpensRoot(television = true, alreadyThere = false))
        assertTrue(libraryMenuOpensRoot(television = true, alreadyThere = true))
    }

    @Test fun aPhoneKeepsItsPlaceUntilBibliotekIsPressedAgain() {
        assertFalse(libraryMenuOpensRoot(television = false, alreadyThere = false))
        assertEquals(true, libraryMenuOpensRoot(television = false, alreadyThere = true))
    }
}
