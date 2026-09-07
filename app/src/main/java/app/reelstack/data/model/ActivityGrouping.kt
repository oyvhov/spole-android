package app.reelstack.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Buckets an event by the day it happened, so a long feed can be skimmed instead of read as one
 * undifferentiated list.
 *
 * Prefers the real timestamp. The display string is only a fallback for demo and legacy cached
 * events that carry none, and reading it needs care: "For 5 dagar sidan" and "For 5 min sidan"
 * share the "For " prefix without sharing a day, so the unit decides the bucket, not the prefix.
 */
fun activityDayGroup(
    event: ActivityEvent,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): String {
    event.timeEpochMillis?.let { millis ->
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return when {
            !date.isBefore(today) -> ACTIVITY_GROUP_TODAY
            date == today.minusDays(1) -> ACTIVITY_GROUP_YESTERDAY
            else -> ACTIVITY_GROUP_EARLIER
        }
    }
    val time = event.time.trim()
    return when {
        time.isBlank() -> ACTIVITY_GROUP_EARLIER
        time.startsWith("I går", ignoreCase = true) -> ACTIVITY_GROUP_YESTERDAY
        time.startsWith("I dag", ignoreCase = true) -> ACTIVITY_GROUP_TODAY
        time.equals("No", ignoreCase = true) || time.equals("No nettopp", ignoreCase = true) ||
            time.equals("Akkurat no", ignoreCase = true) -> ACTIVITY_GROUP_TODAY
        time.startsWith("For ", ignoreCase = true) ->
            if (time.contains(" dag", ignoreCase = true)) ACTIVITY_GROUP_EARLIER else ACTIVITY_GROUP_TODAY
        // An unknown age is never claimed as today.
        else -> ACTIVITY_GROUP_EARLIER
    }
}

const val ACTIVITY_GROUP_TODAY = "I DAG"
const val ACTIVITY_GROUP_YESTERDAY = "I GÅR"
const val ACTIVITY_GROUP_EARLIER = "TIDLEGARE"
