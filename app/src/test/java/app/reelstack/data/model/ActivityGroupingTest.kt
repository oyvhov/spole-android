package app.reelstack.data.model

import app.reelstack.R
import app.reelstack.localization.LocalizedText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityGroupingTest {
    private val zone = ZoneId.of("Europe/Oslo")
    private val today = LocalDate.of(2026, 9, 7)

    private fun event(time: LocalizedText, millis: Long? = null) = ActivityEvent(
        id = "e", title = "Tittel", detail = LocalizedText(R.string.time_now),
        time = time, timeEpochMillis = millis,
    )

    private fun group(time: LocalizedText, millis: Long? = null) =
        activityDayGroup(event(time, millis), zone, today)

    private val anything = LocalizedText(R.string.time_recently)

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    @Test fun timestampDecidesTheDay() {
        assertEquals(ACTIVITY_GROUP_TODAY, group(anything, at("2026-09-07T06:00:00Z")))
        assertEquals(ACTIVITY_GROUP_YESTERDAY, group(anything, at("2026-09-06T06:00:00Z")))
        assertEquals(ACTIVITY_GROUP_EARLIER, group(anything, at("2026-09-01T06:00:00Z")))
    }

    @Test fun aFutureTimestampStaysWithToday() {
        assertEquals(ACTIVITY_GROUP_TODAY, group(anything, at("2026-09-09T06:00:00Z")))
    }

    /** The regression: every "For …" string used to land in "I DAG". */
    @Test fun daysAgoIsNotToday() {
        assertEquals(ACTIVITY_GROUP_EARLIER, group(LocalizedText.plural(R.plurals.time_days_ago, 5)))
        assertEquals(ACTIVITY_GROUP_EARLIER, group(LocalizedText.plural(R.plurals.time_days_ago, 1)))
    }

    @Test fun minutesAndHoursAgoAreToday() {
        assertEquals(ACTIVITY_GROUP_TODAY, group(LocalizedText.plural(R.plurals.time_minutes_ago, 12)))
        assertEquals(ACTIVITY_GROUP_TODAY, group(LocalizedText.plural(R.plurals.time_hours_ago, 3)))
    }

    /** Both sentences the app produces for something that just happened. */
    @Test fun aJustSentRequestIsToday() {
        assertEquals(ACTIVITY_GROUP_TODAY, group(LocalizedText(R.string.time_now)))
        assertEquals(ACTIVITY_GROUP_TODAY, group(LocalizedText(R.string.time_just_now)))
    }

    @Test fun yesterdayAndUnknownAgeKeepTheirOwnBuckets() {
        assertEquals(ACTIVITY_GROUP_YESTERDAY, group(LocalizedText(R.string.time_yesterday)))
        assertEquals(ACTIVITY_GROUP_EARLIER, group(LocalizedText(R.string.time_recently)))
    }

    /**
     * The point of keying on the resource rather than the words: an English reader groups the same
     * way a nynorsk one does, because the grouping never reads the words at all.
     */
    @Test fun groupingDoesNotDependOnTheLanguage() {
        assertEquals(
            group(LocalizedText.plural(R.plurals.time_hours_ago, 3)),
            group(LocalizedText(resId = R.plurals.time_hours_ago, args = listOf<Any>(3), quantity = 3)),
        )
    }
}
