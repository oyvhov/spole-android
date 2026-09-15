package app.reelstack.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import app.reelstack.data.model.LibraryMedia

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
