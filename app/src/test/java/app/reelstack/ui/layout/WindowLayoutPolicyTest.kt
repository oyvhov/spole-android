package app.reelstack.ui.layout

import org.junit.Assert.*
import org.junit.Test

class WindowLayoutPolicyTest {
    @Test fun homeSearchIsOnlyAShortcutForCompactWindows() {
        assertTrue(WindowLayoutPolicy(412f, 892f).showHomeSearch)
        assertTrue(WindowLayoutPolicy(600f, 800f).showHomeSearch)
        assertFalse(WindowLayoutPolicy(800f, 1280f).showHomeSearch)
        assertFalse(WindowLayoutPolicy(1280f, 800f).showHomeSearch)
    }
    @Test fun phoneKeepsBottomNavigationAndBottomSheet() {
        val layout = WindowLayoutPolicy(412f, 892f)
        assertFalse(layout.useNavigationRail)
        assertFalse(layout.useCenteredDialog)
        assertEquals(656f, layout.dialogHeightDp(800f), .01f)
    }
    @Test fun splitScreenUsesWindowNotDeviceWidth() {
        assertTrue(WindowLayoutPolicy(1280f, 800f).useNavigationRail)
        assertFalse(WindowLayoutPolicy(600f, 800f).useNavigationRail)
        assertFalse(WindowLayoutPolicy(600f, 800f).useCenteredDialog)
    }
    @Test fun wideTabletCentersAReadableDialog() {
        val layout = WindowLayoutPolicy(1280f, 800f)
        assertTrue(layout.useCenteredDialog)
        assertEquals(720f, layout.dialogWidthDp)
        assertEquals(1280f, layout.mediaMaxWidthDp)
        assertEquals(688f, layout.dialogHeightDp(800f), .01f)
    }
    @Test fun tallTabletDoesNotMakeUnboundedDialogs() =
        assertEquals(860f, WindowLayoutPolicy(1000f, 1600f).dialogHeightDp(1600f), .01f)
    @Test fun tabletMediaStillExpandsAfterNavigationTakesItsSpace() =
        assertEquals(1184f, WindowLayoutPolicy(1184f, 800f).mediaMaxWidthDp)
    @Test fun keyboardAndShortWindowsNeverOverflowAvailableHeight() {
        listOf(0f, 120f, 300f, 479f, 800f).forEach { height ->
            listOf(400f, 1280f).forEach { width ->
                val result = WindowLayoutPolicy(width, 800f).dialogHeightDp(height)
                assertTrue(result in 0f..height)
            }
        }
    }
    @Test fun landscapePhoneDoesNotBecomeATabletDialog() {
        val layout = WindowLayoutPolicy(892f, 412f)
        assertFalse(layout.useCenteredDialog)
        assertEquals(282f, layout.dialogHeightDp(300f), .01f)
    }
    @Test fun breakpointsAreExplicit() {
        assertFalse(WindowLayoutPolicy(639f, 800f).useNavigationRail)
        assertTrue(WindowLayoutPolicy(640f, 800f).useNavigationRail)
        assertFalse(WindowLayoutPolicy(839f, 800f).useCenteredDialog)
        assertTrue(WindowLayoutPolicy(840f, 480f).useCenteredDialog)
    }
}
