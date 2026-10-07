package app.reelstack.data.network

import java.net.URI
import java.util.Locale

/**
 * The addresses worth trying for what a person typed, best guess first.
 *
 * On a television "192.168.1.20" is about as much as anyone will type with a remote. The old
 * rule made that `https://192.168.1.20` — no port, no TLS on a home server — and the sign-in
 * failed with an address error for an address that was right. A bare local host now tries the
 * Jellyfin/Emby defaults over HTTP first, then HTTPS. A typed scheme or port is respected and only
 * filled in, never replaced. Public names keep the HTTPS-only rule from [EndpointValidator]:
 * cleartext over the internet is not offered as a guess.
 */
fun serverAddressCandidates(typed: String): List<String> {
    val hasScheme = typed.contains("://")
    val text = typed.trim().trimEnd('/')
    if (text.isEmpty() || text.any(Char::isWhitespace)) return emptyList()
    val parsed = runCatching { URI(if (hasScheme) text else "placeholder://$text") }.getOrNull() ?: return emptyList()
    val host = parsed.host?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: return emptyList()
    val path = parsed.rawPath.orEmpty().trimEnd('/')
    val port = parsed.port.takeIf { it in 1..65535 }
    val scheme = parsed.scheme?.lowercase(Locale.ROOT)?.takeIf { hasScheme }
    val local = isLocalHost(host)
    val hostPart = if (host.contains(':')) "[$host]" else host
    fun url(s: String, p: Int?) = "$s://$hostPart" + (p?.let { ":$it" } ?: "") + path

    val raw = when {
        scheme != null && port != null -> listOf(url(scheme, port))
        scheme == "http" -> listOf(url("http", null), url("http", DEFAULT_HTTP_PORT))
        scheme == "https" -> listOf(url("https", null), url("https", DEFAULT_HTTPS_PORT))
        scheme != null -> emptyList()
        port != null && local -> listOf(url("http", port), url("https", port))
        port != null -> listOf(url("https", port))
        local -> listOf(url("http", DEFAULT_HTTP_PORT), url("https", DEFAULT_HTTPS_PORT), url("http", null), url("https", null))
        else -> listOf(url("https", null), url("https", DEFAULT_HTTPS_PORT))
    }
    return raw.mapNotNull { candidate -> runCatching { EndpointValidator.normalizeBaseUrl(candidate) }.getOrNull() }.distinct()
}

/** Jellyfin and Emby both listen here unless the owner changed it. */
const val DEFAULT_HTTP_PORT = 8096
const val DEFAULT_HTTPS_PORT = 8920

/** Same set [EndpointValidator] allows cleartext for: private literals, localhost and mDNS names. */
private fun isLocalHost(host: String): Boolean = runCatching {
    EndpointValidator.validateRequestUrl("http://${if (host.contains(':')) "[$host]" else host}/")
}.isSuccess
