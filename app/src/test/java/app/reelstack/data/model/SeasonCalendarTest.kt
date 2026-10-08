package app.reelstack.data.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonCalendarTest {
    private fun day(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)

    @Test fun halloweenRunsFromTheTwentiethOfOctoberToAllSaintsDay() {
        assertEquals(Season.NONE, Season.onCalendar(day(10, 19)))
        assertEquals(Season.HALLOWEEN, Season.onCalendar(day(10, 20)))
        assertEquals(Season.HALLOWEEN, Season.onCalendar(day(10, 31)))
        assertEquals(Season.HALLOWEEN, Season.onCalendar(day(11, 1)))
        assertEquals(Season.NONE, Season.onCalendar(day(11, 2)))
    }

    /** Knut sweeps Christmas out on the twentieth day, 13 January. */
    @Test fun christmasRunsFromDecemberToTheTwentiethDay() {
        assertEquals(Season.NONE, Season.onCalendar(day(11, 30)))
        assertEquals(Season.CHRISTMAS, Season.onCalendar(day(12, 1)))
        assertEquals(Season.CHRISTMAS, Season.onCalendar(day(1, 13, 2027)))
        assertEquals(Season.NONE, Season.onCalendar(day(1, 14, 2027)))
        assertEquals(day(1, 13, 2027), Season.CHRISTMAS.calendarEnd(day(12, 5)))
        assertEquals(day(1, 13, 2027), Season.CHRISTMAS.calendarEnd(day(1, 2, 2027)))
        assertEquals(day(11, 1), Season.HALLOWEEN.calendarEnd(day(10, 25)))
    }

    @Test fun theCalendarOnlyChangesWhatIsShownAndOnlyWhenAskedTo() {
        val stored = Personalization(visualTheme = VisualTheme.MIDNIGHT, accent = AccentPalette.OCEAN, seasonCalendar = true)
        val shown = stored.seasonal(day(10, 25))
        assertEquals(VisualTheme.HALLOWEEN, shown.visualTheme)
        assertEquals(AccentPalette.PUMPKIN, shown.accent)
        assertEquals(stored, stored.seasonal(day(9, 1)))
        val manual = stored.copy(seasonCalendar = false)
        assertEquals(manual, manual.seasonal(day(10, 25)))
        // Laying the season on twice does not bury the stored mood under it.
        assertEquals(shown, shown.seasonal(day(10, 25)))
    }

    /**
     * Screens save by copying what they were shown. During Halloween that copy carries aubergine
     * and pumpkin, and storing it would keep Halloween all year.
     */
    @Test fun savingWhatWasShownStoresTheChosenColoursNotTheSeason() {
        val stored = Personalization(visualTheme = VisualTheme.MIDNIGHT, accent = AccentPalette.OCEAN, seasonCalendar = true)
        val shown = stored.seasonal(day(10, 25))
        assertEquals(stored, shown.copy(sidebarExpanded = null).withoutCalendarSeason())
        val widened = shown.copy(sidebarExpanded = true).withoutCalendarSeason()
        assertEquals(VisualTheme.MIDNIGHT, widened.visualTheme)
        assertEquals(AccentPalette.OCEAN, widened.accent)
        assertNull(widened.seasonOverlay)
        // A mood picked on top of the season is a real choice, and it is kept.
        val picked = shown.copy(visualTheme = VisualTheme.CINEMA).withoutCalendarSeason()
        assertEquals(VisualTheme.CINEMA, picked.visualTheme)
        assertEquals(AccentPalette.OCEAN, picked.accent)
    }

    @Test fun choosingASeasonRowKeepsAnOwnMoodAndOnlyReplacesASeasonalOne() {
        val own = Personalization(visualTheme = VisualTheme.MIDNIGHT, accent = AccentPalette.OCEAN)
        assertEquals(own, SeasonChoice.NONE.applyTo(own))
        val calendar = SeasonChoice.CALENDAR.applyTo(own)
        assertTrue(calendar.seasonCalendar)
        assertEquals(VisualTheme.MIDNIGHT, calendar.visualTheme)
        assertEquals(SeasonChoice.CALENDAR, SeasonChoice.of(calendar))

        val halloween = SeasonChoice.HALLOWEEN.applyTo(calendar)
        assertFalse(halloween.seasonCalendar)
        assertEquals(VisualTheme.HALLOWEEN, halloween.visualTheme)
        assertEquals(SeasonChoice.HALLOWEEN, SeasonChoice.of(halloween))
        // Leaving a fixed season for the calendar returns to the plain default, as «Heile året» does.
        assertEquals(VisualTheme.FOREST, SeasonChoice.CALENDAR.applyTo(halloween).visualTheme)
        assertEquals(VisualTheme.FOREST, SeasonChoice.NONE.applyTo(halloween).visualTheme)

        // Changing season from inside a calendar season starts from what is stored.
        val shown = calendar.seasonal(day(12, 10))
        assertEquals(SeasonChoice.CALENDAR, SeasonChoice.of(shown))
        assertEquals(VisualTheme.MIDNIGHT, SeasonChoice.NONE.applyTo(shown).visualTheme)
    }

    @Test fun theCountdownIsForTheLastMonthAndEndsOnTheNight() {
        assertEquals(11, Season.HALLOWEEN.daysUntilPeak(day(10, 20)))
        assertEquals(0, Season.HALLOWEEN.daysUntilPeak(day(10, 31)))
        assertNull(Season.HALLOWEEN.daysUntilPeak(day(11, 1)))
        assertNull(Season.HALLOWEEN.daysUntilPeak(day(3, 1)))
        assertEquals(23, Season.CHRISTMAS.daysUntilPeak(day(12, 1)))
        assertNull(Season.CHRISTMAS.daysUntilPeak(day(12, 26)))
        assertNull(Season.NONE.daysUntilPeak(day(10, 25)))
    }

    @Test fun aChildsHalloweenShelfHasTheTagOnly() {
        assertEquals(listOf("Horror", "Skrekk"), Season.HALLOWEEN.shelf(kids = false)?.genres)
        assertEquals(emptyList<String>(), Season.HALLOWEEN.shelf(kids = true)?.genres)
        assertEquals(listOf("halloween"), Season.HALLOWEEN.shelf(kids = true)?.tags)
        assertEquals(listOf("christmas"), Season.CHRISTMAS.shelf(kids = false)?.tags)
        assertNull(Season.NONE.shelf(kids = false))
    }

    @Test fun aShelfIdNamesItsSeasonAndNothingElse() {
        assertEquals(Season.HALLOWEEN, seasonOfShelfId(Season.HALLOWEEN.shelfId()))
        assertEquals(Season.CHRISTMAS, seasonOfShelfId(Season.CHRISTMAS.shelfId()))
        assertNull(seasonOfShelfId(Season.NONE.shelfId()))
        assertNull(seasonOfShelfId("f137a2dd21bbc1b99aa5c0f6bf02a805"))
        assertNull(seasonOfShelfId("spole-season-EASTER"))
        assertNull(seasonOfShelfId(null))
    }

    /** Before the levels there was one switch, and on was everything: now FULL. */
    @Test fun theOldSwitchBecomesALevel() {
        assertEquals(SeasonalDecor.FULL, SeasonalDecor.decode(null, legacyOrnament = true))
        assertEquals(SeasonalDecor.OFF, SeasonalDecor.decode(null, legacyOrnament = false))
        assertEquals(SeasonalDecor.CALM, SeasonalDecor.decode("CALM", legacyOrnament = false))
        assertEquals(SeasonalDecor.FULL, Personalization().seasonalDecor)
    }
}
