package app.reelstack.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The large feature must not stretch a rail's low-resolution thumbnail. Keep its image tag. */
fun heroArtworkUrl(url: String?, lightweight: Boolean = false): String? {
    val parsed = url?.toHttpUrlOrNull() ?: return url
    if (!parsed.encodedPath.contains("/Images/")) return url
    return parsed.newBuilder()
        .setQueryParameter("maxWidth", if (lightweight) "1280" else "1920")
        .setQueryParameter("quality", "90")
        .build().toString()
}
