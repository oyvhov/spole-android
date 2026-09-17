package app.reelstack.data.network

import app.reelstack.R
import java.net.URI
import java.util.Locale

object EndpointValidator {
    /**
     * Every rejection here names a resource, not a sentence.
     *
     * This runs in the data layer, which has no business knowing the reader's language — and for a
     * long time it decided anyway, in nynorsk, on the very first screen a new user sees.
     */
    fun normalizeBaseUrl(value: String): String {
        if (value.isBlank()) invalidEndpoint(R.string.endpoint_blank)
        if (value.trim().any(Char::isWhitespace)) invalidEndpoint(R.string.endpoint_whitespace)
        val candidate = value.trim().let {
            if (it.contains("://")) it else "https://$it"
        }
        val uri = runCatching { URI(candidate) }
            .getOrElse { invalidEndpoint(R.string.endpoint_invalid) }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") invalidEndpoint(R.string.endpoint_scheme)
        if (uri.host.isNullOrBlank()) invalidEndpoint(R.string.endpoint_incomplete)
        if (uri.userInfo != null) invalidEndpoint(R.string.endpoint_credentials)
        if (uri.port != -1 && uri.port !in 1..65535) invalidEndpoint(R.string.endpoint_port)
        if (scheme == "http" && !isTrustedLanHost(uri.host)) invalidEndpoint(R.string.endpoint_cleartext)

        val path = (uri.path ?: "").trimEnd('/')
        return URI(scheme, null, uri.host.lowercase(Locale.ROOT), uri.port, path.ifEmpty { null }, null, null).toString()
    }

    fun resolve(baseUrl: String, path: String): String =
        "${normalizeBaseUrl(baseUrl)}/${path.trimStart('/')}"

    fun isCleartext(baseUrl: String): Boolean =
        runCatching { URI(normalizeBaseUrl(baseUrl)).scheme == "http" }.getOrDefault(false)

    private fun isTrustedLanHost(host: String): Boolean {
        val normalized = host.lowercase(Locale.ROOT).trim('[', ']')
        if (normalized == "localhost" || normalized.endsWith(".local")) return true
        // Only literal addresses count as private. A registered name that merely begins like one
        // still resolves through public DNS to whatever its owner points it at: "fcbarcelona.com"
        // starts with "fc", and wildcard resolvers hand out names like "192.168.1.5.nip.io".
        // Treating either as LAN would quietly permit cleartext HTTP over the open internet.
        if (normalized.contains(':')) return isPrivateIpv6(normalized)
        return isPrivateIpv4(normalized)
    }

    private fun isPrivateIpv4(host: String): Boolean {
        val octets = host.split('.')
        if (octets.size != 4) return false
        val values = octets.map { octet ->
            if (octet.isEmpty() || octet.length > 3 || !octet.all(Char::isDigit)) return false
            octet.toInt().also { if (it > 255) return false }
        }
        return when (values[0]) {
            10, 127 -> true
            172 -> values[1] in 16..31
            192 -> values[1] == 168
            else -> false
        }
    }

    /** Reached only for a literal address, so a leading "fc"/"fd" here really is fc00::/7. */
    private fun isPrivateIpv6(host: String): Boolean =
        host == "::1" || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")
}
