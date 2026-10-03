package app.reelstack.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ContentDetails

/** A detail sheet keeps its known backdrop; late metadata must not swap it for another image. */
fun detailBackdropUrl(opening: ContentDetails, details: ContentDetails): String? = heroArtworkUrl(
    opening.backdropUrl?.takeIf(String::isNotBlank) ?: details.backdropUrl?.takeIf(String::isNotBlank))

/** A portrait hero must not enlarge the lettering already printed on a Thumb or poster. */
fun mobileHeroArtworkUrl(media: LibraryMedia): String? {
    media.backdropUrl?.takeIf(String::isNotBlank)?.let { return it }
    // Older snapshots can already carry a real backdrop in heroUrl or the card's artwork.
    return listOfNotNull(media.heroUrl, media.artworkUrl).firstOrNull { url ->
        if (url.isBlank()) return@firstOrNull false
        val segments = url.toHttpUrlOrNull()?.pathSegments ?: return@firstOrNull false
        val images = segments.indexOfLast { it.equals("Images", true) }
        if (images < 0) return@firstOrNull url == media.heroUrl
        segments.getOrNull(images + 1).equals("Backdrop", true) &&
            (!media.mediaType.equals("Episode", true) ||
                (!media.seriesId.isNullOrBlank() && segments.getOrNull(images - 1) == media.seriesId))
    }
}

/** Older snapshots only contain card art. Reuse it only when its image owner is suitable. */
fun libraryHeroArtworkUrl(media: LibraryMedia): String? {
    media.heroUrl?.takeIf { it.isNotBlank() }?.let { return it }
    val card = media.artworkUrl?.takeIf { it.isNotBlank() } ?: return null
    val segments = card.toHttpUrlOrNull()?.pathSegments.orEmpty()
    val images = segments.indexOfLast { it.equals("Images", true) }
    val type = segments.getOrNull(images + 1)
    if (media.mediaType.equals("Episode", true)) {
        return card.takeIf {
            images > 0 && !media.seriesId.isNullOrBlank() &&
                segments[images - 1] == media.seriesId && type in setOf("Backdrop", "Thumb")
        }
    }
    return card.takeUnless { images >= 0 && type !in setOf("Backdrop", "Thumb") }
}

/** The large feature must not stretch a rail's low-resolution thumbnail. Keep its image tag. */
fun heroArtworkUrl(url: String?, lightweight: Boolean = false): String? {
    val parsed = url?.toHttpUrlOrNull() ?: return url
    if (!parsed.encodedPath.contains("/Images/")) return url
    return parsed.newBuilder()
        .setQueryParameter("maxWidth", if (lightweight) "1280" else "1920")
        .setQueryParameter("quality", "90")
        .build().toString()
}
