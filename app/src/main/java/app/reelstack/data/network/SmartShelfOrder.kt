package app.reelstack.data.network

import app.reelstack.data.model.SmartShelfSort
import java.text.Collator

/**
 * Puts a shelf's merged answers in its order. The server sorts each answer; «a tag or a genre» is two
 * answers, and joining them would otherwise put every tagged title before every genre title whatever
 * order was asked for. [SmartShelfSort.RULE] keeps exactly that: tagged first, as the rule reads.
 * The sort is stable, so titles the key cannot tell apart keep the server's order.
 */
internal fun List<RemoteLibraryItem>.inShelfOrder(sort: SmartShelfSort, collator: Collator = Collator.getInstance()):
    List<RemoteLibraryItem> = when (sort) {
    SmartShelfSort.RULE -> this
    SmartShelfSort.ADDED -> sortedByDescending { it.addedAtEpochMillis ?: Long.MIN_VALUE }
    SmartShelfSort.NEWEST -> sortedByDescending { it.releaseYear() ?: Int.MIN_VALUE }
    SmartShelfSort.TITLE -> sortedWith(compareBy(collator) { it.title })
    SmartShelfSort.RATING -> sortedByDescending { it.tmdbRating ?: it.mdblistRating ?: it.criticRating?.toFloat() ?: -1f }
}

/** The year a title came out: from its premiere date, or else the first year in its subtitle. */
internal fun RemoteLibraryItem.releaseYear(): Int? =
    premiereDate?.take(4)?.toIntOrNull() ?: Regex("""\b(18|19|20)\d{2}\b""").find(subtitle)?.value?.toIntOrNull()
