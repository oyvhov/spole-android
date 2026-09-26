package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TouchMenuTest {
    @Test fun downloadsAreNotAMenuItemUntilTheOwnerAsks() {
        assertEquals(listOf("HOME", "LIBRARY", "DISCOVER", "ACTIVITY", "SETTINGS"),
            Personalization().touchMenu(television = false, kidMode = false))
    }

    @Test fun chosenDownloadsSitJustBeforeSettingsInTheOwnersOrder() {
        val options = Personalization(showDownloadsInMenu = true,
            menuOrder = listOf("LIBRARY", "HOME", "SETTINGS", "DISCOVER", "ACTIVITY"))
        assertEquals(listOf("LIBRARY", "HOME", DOWNLOADS_MENU_ITEM, "SETTINGS", "DISCOVER", "ACTIVITY"),
            options.touchMenu(television = false, kidMode = false))
    }

    @Test fun televisionAndChildrenNeverGetDownloads() {
        val options = Personalization(showDownloadsInMenu = true)
        assertFalse(DOWNLOADS_MENU_ITEM in options.touchMenu(television = true, kidMode = false))
        assertFalse(DOWNLOADS_MENU_ITEM in options.touchMenu(television = false, kidMode = true))
    }

    @Test fun hiddenTabsStayHiddenWhenDownloadsAreShown() {
        val options = Personalization(showDownloadsInMenu = true, hiddenMenuItems = setOf("DISCOVER", "ACTIVITY"))
        assertEquals(listOf("HOME", "LIBRARY", DOWNLOADS_MENU_ITEM, "SETTINGS"),
            options.touchMenu(television = false, kidMode = false))
    }
}
