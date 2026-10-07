package app.reelstack.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Check parsed origin and decoded path before lending a server credential to an image request. */
internal fun artworkBelongsTo(baseUrl: String, url: String): Boolean {
    val base = runCatching { EndpointValidator.normalizeBaseUrl(baseUrl) }.getOrNull()?.toHttpUrlOrNull() ?: return false
    val target = runCatching { EndpointValidator.validateRequestUrl(url) }.getOrNull()?.toHttpUrlOrNull() ?: return false
    if (base.scheme != target.scheme || base.host != target.host || base.port != target.port) return false
    val prefix = base.pathSegments.filter(String::isNotEmpty)
    return target.pathSegments.take(prefix.size) == prefix &&
        target.pathSegments.none { it == ".." || it == "." || '/' in it || '\\' in it } &&
        target.queryParameterNames.none { it.equals("api_key", true) || it.equals("token", true) || it.equals("access_token", true) }
}
