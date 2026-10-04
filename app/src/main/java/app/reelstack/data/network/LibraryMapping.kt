package app.reelstack.data.network

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind

/** Browsing, shelves and title opening must carry the same artwork and user flags. */
fun RemoteLibraryItem.toLibraryMedia(source: ServiceKind, artworkRes: Int, words: List<String>) = LibraryMedia(
    id = "${source.name.lowercase(java.util.Locale.ROOT)}-$id",
    title = title, subtitle = subtitle, progress = progress, artworkRes = artworkRes, source = source,
    artworkUrl = artworkUrl, remoteId = id, overview = overview, facts = words, genres = genres, mediaType = mediaType,
    logoUrl = logoUrl, heroUrl = heroUrl, posterUrl = posterUrl, backdropUrl = backdropUrl,
    lastActivityEpochMillis = lastActivityEpochMillis, addedAtEpochMillis = addedAtEpochMillis,
    season = season, episode = episode, runtimeMinutes = runtimeMinutes, childCount = childCount,
    libraryId = libraryId, seriesId = seriesId, favourite = favourite, played = played, available = available,
    premiereDate = premiereDate, tmdbId = tmdbId, criticRating = criticRating, tmdbRating = tmdbRating,
    mdblistRating = mdblistRating, unplayedItemCount = unplayedItemCount, thumbnailUrl = thumbnailUrl, bannerUrl = bannerUrl,
)
