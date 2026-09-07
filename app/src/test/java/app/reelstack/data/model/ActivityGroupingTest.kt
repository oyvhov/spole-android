package app.reelstack.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityGroupingTest {
    private val zone = ZoneId.of("Europe/Oslo")
    private val today = LocalDate.of(2026, 9, 7)

    private fun event(time: String, millis: Long? = null) =
        ActivityEvent(id = "e", title = "Tittel", detail = "", time = time, timeEpochMillis = millis)

    private fun group(time: String, millis: Long? = null) = activityDayGroup(event(time, millis), zone, today)

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    @Test fun timestampDecidesTheDay() {
        assertEquals(ACTIVITY_GROUP_TODAY, group("kva som helst", at("2026-09-07T06:00:00Z")))
        assertEquals(ACTIVITY_GROUP_YESTERDAY, group("kva som helst", at("2026-09-06T06:00:00Z")))
        assertEquals(ACTIVITY_GROUP_EARLIER, group("kva som helst", at("2026-09-01T06:00:00Z")))
    }

    @Test fun aFutureTimestampStaysWithToday() {
        assertEquals(ACTIVITY_GROUP_TODAY, group("", at("2026-09-09T06:00:00Z")))
    }

    /** The regression: every "For …" string used to land in "I DAG". */
    @Test fun daysAgoIsNotToday() {
        assertEquals(ACTIVITY_GROUP_EARLIER, group("For 5 dagar sidan"))
        assertEquals(ACTIVITY_GROUP_EARLIER, group("For 1 dag sidan"))
    }

    @Test fun minutesAndHoursAgoAreToday() {
        assertEquals(ACTIVITY_GROUP_TODAY, group("For 12 min sidan"))
        assertEquals(ACTIVITY_GROUP_TODAY, group("For 3 t sidan"))
    }

    /** Both wordings the app produces for something that just happened. */
    @Test fun aJustSentRequestIsToday() {
        assertEquals(ACTIVITY_GROUP_TODAY, group("No nettopp"))
        assertEquals(ACTIVITY_GROUP_TODAY, group("Akkurat no"))
        assertEquals(ACTIVITY_GROUP_TODAY, group("No"))
    }

    @Test fun yesterdayAndUnknownAgeKeepTheirOwnBuckets() {
        assertEquals(ACTIVITY_GROUP_YESTERDAY, group("I går"))
        assertEquals(ACTIVITY_GROUP_TODAY, group("I dag kl. 09:00"))
        assertEquals(ACTIVITY_GROUP_EARLIER, group("Nyleg"))
        assertEquals(ACTIVITY_GROUP_EARLIER, group(""))
    }
}
