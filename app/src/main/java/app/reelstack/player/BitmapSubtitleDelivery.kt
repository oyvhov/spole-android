package app.reelstack.player

import java.net.URI
import java.util.Locale

internal const val SPOLE_MKS_DVD_MIME = "application/x-spole-mks-dvd"
internal const val SPOLE_MKS_PGS_MIME = "application/x-spole-mks-pgs"

internal data class BitmapSubtitleDelivery(val url: String, val mimeType: String)

/** Use only an advertised, same-origin file. Never guess a second URL for an IDX/SUB pair. */
internal fun bitmapSubtitleDelivery(baseUrl: String, codec: String?, deliveryUrl: String): BitmapSubtitleDelivery? {
    if (deliveryUrl.isBlank()) return null
    val url = runCatching { safePlaybackUrl(baseUrl, deliveryUrl) }.getOrNull() ?: return null
    val extension = runCatching { URI(url).path.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT) }.getOrNull()
    val mime = when (codec?.lowercase(Locale.ROOT)) {
        "pgssub", "pgs", "sup" -> when (extension) {
            "sup", "pgs", "pgssub" -> SPOLE_SUP_MIME
            "mks" -> SPOLE_MKS_PGS_MIME
            else -> null
        }
        "dvdsub", "vobsub" -> when (extension) {
            // Jellyfin's graphical subtitle endpoint returns a Matroska subtitle container
            // for DVD/VobSub, including Stream.dvdsub/Stream.vobsub routes.
            "mks", "dvdsub", "vobsub" -> SPOLE_MKS_DVD_MIME
            else -> null
        }
        else -> null
    } ?: return null
    return BitmapSubtitleDelivery(url, mime)
}
