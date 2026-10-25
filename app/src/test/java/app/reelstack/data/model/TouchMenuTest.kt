package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchMenuTest {
    @Test fun storedActivityIsDroppedFromTheMenu() {
        val stored = Personalization(menuOrder = listOf("ACTIVITY", "DISCOVER", "HOME", "LIBRARY", "SETTINGS"),
            hiddenMenuItems = setOf("ACTIVITY"))
        assertEquals(listOf("DISCOVER", "HOME", "LIBRARY", "SETTINGS"), stored.visibleMenu())
        assertEquals(listOf("HOME", "LIBRARY", "DISCOVER", "SETTINGS"), DEFAULT_MENU)
        assertTrue(stored.withMenuVisible("ACTIVITY", true).hiddenMenuItems.isEmpty())
    }

    @Test fun downloadsAreNotAMenuItemUntilTheOwnerAsks() {
        assertEquals(listOf("HOME", "LIBRARY", "DISCOVER", "SETTINGS"),
            Personalization().touchMenu(television = false, kidMode = false))
    }

    @Test fun chosenDownloadsSitJustBeforeSettingsInTheOwnersOrder() {
        val options = Personalization(showDownloadsInMenu = true,
            menuOrder = listOf("LIBRARY", "HOME", "SETTINGS", "DISCOVER"))
        assertEquals(listOf("LIBRARY", "HOME", DOWNLOADS_MENU_ITEM, "SETTINGS", "DISCOVER"),
            options.touchMenu(television = false, kidMode = false))
    }

    @Test fun televisionAndChildrenNeverGetDownloads() {
        val options = Personalization(showDownloadsInMenu = true)
        assertFalse(DOWNLOADS_MENU_ITEM in options.touchMenu(television = true, kidMode = false))
        assertFalse(DOWNLOADS_MENU_ITEM in options.touchMenu(television = false, kidMode = true))
    }

    @Test fun hiddenTabsStayHiddenWhenDownloadsAreShown() {
        val options = Personalization(showDownloadsInMenu = true, hiddenMenuItems = setOf("DISCOVER"))
        assertEquals(listOf("HOME", "LIBRARY", DOWNLOADS_MENU_ITEM, "SETTINGS"),
            options.touchMenu(television = false, kidMode = false))
    }
}
