package app.reelstack.data.model

import app.reelstack.data.network.RemoteLibraryItem

/** Choose the frame once for the catalogue, before any bitmap has loaded. */
fun libraryArtType(display: LibraryDisplay, collectionType: String?, entries: List<RemoteLibraryItem>): LibraryArtType {
    if (display.artType != LibraryArtType.AUTO) return display.artType
    return when (collectionType?.lowercase(java.util.Locale.ROOT)) {
        "movies", "boxsets" -> LibraryArtType.POSTER
        "tvshows", "tv" -> LibraryArtType.THUMB
        else -> if (entries.firstOrNull()?.mediaType in setOf("Series", "Season", "Episode", "Video", "Photo"))
            LibraryArtType.THUMB else LibraryArtType.POSTER
    }
}

/** Use advertised images and their own tags. Never probe an invented image URL per card. */
fun libraryArtworkUrl(item: RemoteLibraryItem, type: LibraryArtType): String? {
    return libraryArtworkUrl(type, item.artworkUrl, item.thumbnailUrl, item.posterUrl, item.heroUrl,
        item.backdropUrl, item.bannerUrl, item.logoUrl, item.mediaType.equals("Episode", true))
}

fun libraryArtworkUrl(item: LibraryMedia, type: LibraryArtType): String? =
    libraryArtworkUrl(type, item.artworkUrl, item.thumbnailUrl, item.posterUrl, item.heroUrl,
        item.backdropUrl, item.bannerUrl, item.logoUrl, item.mediaType.equals("Episode", true))

private fun libraryArtworkUrl(type: LibraryArtType, artwork: String?, thumbnail: String?, poster: String?,
    hero: String?, backdrop: String?, banner: String?, logo: String?, episode: Boolean): String? {
    val wide = thumbnail ?: artwork.takeIf { episode } ?: hero ?: backdrop
    return when (type) {
        LibraryArtType.POSTER -> poster ?: artwork ?: wide
        LibraryArtType.THUMB, LibraryArtType.AUTO -> wide ?: artwork ?: poster
        LibraryArtType.BANNER -> banner ?: wide ?: poster ?: artwork
        LibraryArtType.LOGO -> logo ?: wide ?: poster ?: artwork
    }
}

/** Stable sizes share the disk cache across grid, list and neighbour prefetch. */
fun librarySizedArtwork(url: String?, widthPx: Int): String? {
    if (url == null || !url.contains("/Images/")) return url
    val width = listOf(320, 480, 640, 960, 1280).firstOrNull { it >= widthPx } ?: 1280
    return url.replace(Regex("([?&])maxWidth=\\d+"), "$1maxWidth=$width")
        .replace(Regex("([?&])quality=\\d+"), "$1quality=85")
}
