package app.reelstack.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The profile menu drops from the avatar on the menu line, so the line stays while it is open. Hiding it took the
 * avatar's focus away: focus fell to «Tittel (A–Å)» behind the menu and the line vanished (Loftet, 2026-11-07).
 */
class TopBarStaysTest {
    @Test fun theMenuLineStaysUnderTheProfileMenu() {
        assertTrue(topBarStaysFor(null))
        assertTrue(topBarStaysFor(AppSheet.ProfileSwitcher))
    }

    @Test fun aSheetThatCoversThePageStillHidesIt() {
        assertFalse(topBarStaysFor(AppSheet.UpcomingCalendar))
    }
}
