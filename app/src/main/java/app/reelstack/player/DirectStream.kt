package app.reelstack.player

import kotlinx.serialization.json.JsonObject

/** Emby can deny filesystem DirectPlay while offering the untouched file over HTTP. */
internal fun originalDirectStreamUrl(source: JsonObject): String? {
    if (!source.flag("SupportsDirectStream")) return null
    val url = source.str("DirectStreamUrl").takeIf { it.isNotBlank() } ?: return null
    // A remux/playlist is not an untouched file and needs the negotiated track/timeline path.
    return url.takeIf {
        playbackUrlValue(it, "static").equals("true", true) &&
            !it.substringBefore('?').endsWith(".m3u8", true)
    }
}
