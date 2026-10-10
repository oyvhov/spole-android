package app.reelstack.data.model

import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/**
 * When each season runs for someone who lets Spole follow the calendar.
 *
 * Halloween is the twelve days before the night itself and All Saints' Day after it. Christmas
 * starts with December and ends on the twentieth day of Christmas, 13 January, the day Knut sweeps
 * Christmas out of a Norwegian house.
 */
fun Season.Companion.onCalendar(date: LocalDate): Season = when {
    date.month == Month.OCTOBER && date.dayOfMonth >= 20 -> Season.HALLOWEEN
    date.month == Month.NOVEMBER && date.dayOfMonth == 1 -> Season.HALLOWEEN
    date.month == Month.DECEMBER -> Season.CHRISTMAS
    date.month == Month.JANUARY && date.dayOfMonth <= 13 -> Season.CHRISTMAS
    else -> Season.NONE
}

/** The last day the calendar keeps a season, for the line under «Etter kalenderen». */
fun Season.calendarEnd(date: LocalDate): LocalDate? = when (this) {
    Season.HALLOWEEN -> LocalDate.of(date.year, 11, 1)
    Season.CHRISTMAS -> LocalDate.of(if (date.month == Month.JANUARY) date.year else date.year + 1, 1, 13)
    Season.NONE -> null
}

/** The mood and accent a calendar season covers, so that writing the shown value back stores neither. */
data class SeasonOverlay(val season: Season, val visualTheme: VisualTheme, val accent: AccentPalette)

/**
 * What the app shows on [date]: the stored choice, with the calendar's season laid over it while it
 * runs. The stored mood is never rewritten, which is why the app goes back to it on its own.
 */
fun Personalization.seasonal(date: LocalDate): Personalization {
    if (!seasonCalendar || seasonOverlay != null) return this
    val season = Season.onCalendar(date)
    return if (season == Season.NONE) this
    else season.applyTo(this).copy(seasonOverlay = SeasonOverlay(season, visualTheme, accent))
}

/**
 * The value to store: the calendar's season taken off again.
 *
 * Many screens save by copying what they were shown and changing one field. Without this, opening
 * the menu during Halloween would store aubergine and pumpkin for good. A mood or accent changed
 * on top of the season is a real choice and is kept; one still equal to the season's is not.
 */
fun Personalization.withoutCalendarSeason(): Personalization {
    val overlay = seasonOverlay ?: return this
    val laid = overlay.season.applyTo(this)
    return copy(
        visualTheme = if (visualTheme == laid.visualTheme) overlay.visualTheme else visualTheme,
        accent = if (accent == laid.accent) overlay.accent else accent,
        seasonOverlay = null,
    )
}

/**
 * Days until the night a season is for: 0 on Halloween and on Christmas Eve, null once it has
 * passed or while it is more than a month away, when a countdown would only be a number.
 */
fun Season.daysUntilPeak(date: LocalDate): Int? {
    val peak = when (this) {
        Season.HALLOWEEN -> LocalDate.of(date.year, 10, 31)
        Season.CHRISTMAS -> LocalDate.of(date.year, 12, 24)
        Season.NONE -> return null
    }
    return ChronoUnit.DAYS.between(date, peak).toInt().takeIf { it in 0..31 }
}

/** The library path id of a season's shelf: its preset among the smart shelves. See [SmartShelfPresets]. */
fun Season.shelfId(): String? = SmartShelfPresets.of(this)?.pathId
