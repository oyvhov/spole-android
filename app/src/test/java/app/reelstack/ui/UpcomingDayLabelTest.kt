package app.reelstack.ui

import app.reelstack.ui.screens.upcomingDayLabel
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bug this covers is one nobody sees on the day they write the code.
 *
 * "I dag" used to be baked into the label by the sync, and the snapshot store keeps that label, so
 * a row written on Tuesday still announced itself as today's release on Wednesday. The date is what
 * the cache is allowed to hold; the day word has to be decided against the clock at the moment the
 * card is drawn.
 *
 * The words come in as arguments rather than being read from resources here, so the rule can be
 * checked on the JVM and stays true in every language.
 */
class UpcomingDayLabelTest {
    private val zone = ZoneId.of("Europe/Oslo")
    private val today = LocalDate.of(2026, 9, 17)

    private fun at(date: LocalDate, hour: Int = 21) =
        ZonedDateTime.of(date.atTime(hour, 0), zone).toInstant().toEpochMilli()

    private fun label(dateLabel: String, millis: Long) =
        upcomingDayLabel(dateLabel, millis, today, zone, "I dag", "I morgon")

    @Test fun todayIsNamedRatherThanDated() {
        assertEquals("I dag", label("tor. 17. sep.", at(today)))
    }

    @Test fun tomorrowIsNamedToo() {
        assertEquals("I morgon", label("fre. 18. sep.", at(today.plusDays(1))))
    }

    @Test fun anAirtimeSurvivesTheDayWord() {
        // Sonarr rows carry the time after the separator. Replacing the whole label would drop it.
        assertEquals("I dag · 21:00", label("tor. 17. sep. · 21:00", at(today)))
        assertEquals("I morgon · 21:00", label("fre. 18. sep. · 21:00", at(today.plusDays(1))))
    }

    @Test fun everyOtherDayKeepsTheDateTheSyncWrote() {
        assertEquals("lau. 19. sep.", label("lau. 19. sep.", at(today.plusDays(2))))
        assertEquals("ons. 16. sep.", label("ons. 16. sep.", at(today.minusDays(1))))
    }

    @Test fun anUnknownTimestampIsNeverCalledToday() {
        // A missing date is the one case where guessing is worse than being out of date.
        assertEquals("TBA", label("TBA", 0L))
        assertEquals("TBA", label("TBA", -1L))
    }

    @Test fun theDayIsDecidedInTheReadersZoneNotUtc() {
        // 23:30 in Oslo on the 17th is 21:30 UTC — still today for the person holding the remote.
        val lateTonight = ZonedDateTime.of(today.atTime(23, 30), zone).toInstant().toEpochMilli()
        assertEquals("I dag · 23:30", label("tor. 17. sep. · 23:30", lateTonight))
    }
}
