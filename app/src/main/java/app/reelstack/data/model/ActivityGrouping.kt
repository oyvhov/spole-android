package app.reelstack.data.model

import app.reelstack.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Buckets an event by the day it happened, so a long feed can be skimmed instead of read as one
 * undifferentiated list.
 *
 * Prefers the real timestamp. Without one, the bucket comes from *which* sentence the event
 * carries, not from its words: matching on "I går" worked only for as long as the app spoke one
 * language, and silently put every English event in "Earlier" the moment it spoke two.
 */
fun activityDayGroup(
    event: ActivityEvent,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): Int {
    event.timeEpochMillis?.let { millis ->
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return when {
            !date.isBefore(today) -> ACTIVITY_GROUP_TODAY
            date == today.minusDays(1) -> ACTIVITY_GROUP_YESTERDAY
            else -> ACTIVITY_GROUP_EARLIER
        }
    }
    return when (event.time.resId) {
        R.string.time_yesterday -> ACTIVITY_GROUP_YESTERDAY
        R.string.time_now, R.string.time_just_now,
        R.plurals.time_minutes_ago, R.plurals.time_hours_ago -> ACTIVITY_GROUP_TODAY
        // An unknown age, and anything counted in days, is never claimed as today.
        else -> ACTIVITY_GROUP_EARLIER
    }
}

/** Day buckets as resource ids: the bucket is a decision, the heading is a word. */
val ACTIVITY_GROUP_TODAY = R.string.activity_group_today
val ACTIVITY_GROUP_YESTERDAY = R.string.activity_group_yesterday
val ACTIVITY_GROUP_EARLIER = R.string.activity_group_earlier
