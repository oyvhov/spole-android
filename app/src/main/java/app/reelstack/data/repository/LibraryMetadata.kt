package app.reelstack.data.repository

import app.reelstack.data.model.LibraryMedia
import kotlinx.serialization.json.*

/** Optional additions to cached cards. Older rows remain readable without discarding their art. */
internal fun encodeLibraryMetadata(item: LibraryMedia): String = buildJsonObject {
    put("heroUrl", item.heroUrl); put("posterUrl", item.posterUrl)
    put("season", item.season); put("episode", item.episode)
    put("seriesId", item.seriesId); put("libraryId", item.libraryId); put("logoUrl", item.logoUrl)
    put("runtime", item.runtimeMinutes); put("children", item.childCount)
    put("favourite", item.favourite); put("played", item.played)
    put("available", item.available); put("premiere", item.premiereDate); put("tmdb", item.tmdbId)
    put("critic", item.criticRating); put("tmdbRating", item.tmdbRating); put("mdblist", item.mdblistRating)
}.toString()

internal fun LibraryMedia.restoreLibraryMetadata(value: String): LibraryMedia {
    val data = runCatching { Json.parseToJsonElement(value).jsonObject }.getOrNull() ?: return this
    fun text(key: String) = (data[key] as? JsonPrimitive)?.contentOrNull
    fun number(key: String) = text(key)?.toIntOrNull()
    fun decimal(key: String) = text(key)?.toFloatOrNull()
    fun flag(key: String, fallback: Boolean) = text(key)?.toBooleanStrictOrNull() ?: fallback
    return copy(heroUrl = text("heroUrl"), posterUrl = text("posterUrl"), season = number("season"), episode = number("episode"), seriesId = text("seriesId"),
        libraryId = text("libraryId"), logoUrl = text("logoUrl"), runtimeMinutes = number("runtime"),
        childCount = number("children"), favourite = flag("favourite", false), played = flag("played", false),
        available = flag("available", true), premiereDate = text("premiere"), tmdbId = number("tmdb"),
        criticRating = number("critic"), tmdbRating = decimal("tmdbRating"), mdblistRating = decimal("mdblist"))
}
