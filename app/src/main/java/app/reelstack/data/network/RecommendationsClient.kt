package app.reelstack.data.network

/**
 * A small public catalogue kept in GitHub instead of a new application backend. The app caches
 * the parsed result in the normal dashboard snapshot, so a temporary GitHub outage does not make
 * the Home row disappear after it has loaded once.
 *
 * The path is versioned. Installations are never all on the same build, so the shape of this file
 * can only ever change by publishing a new path beside the old one — a field renamed under
 * `v1` would reach a two-year-old phone that still parses it the old way. [FALLBACK_ENDPOINT] is
 * what every build before 0.13.4 asked for, and it is tried second so the row keeps working while
 * the catalogue is being moved.
 */
class RecommendationsClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val endpoints: List<String> = listOf(DEFAULT_ENDPOINT, FALLBACK_ENDPOINT),
) {
    /** One fixed address, for a test that wants to name the URL it is serving. */
    constructor(transport: JsonHttpTransport, endpoint: String) : this(transport, listOf(endpoint))

    fun feed(): List<RemoteRecommendationItem> {
        var lastStatus: Int? = null
        endpoints.forEach { endpoint ->
            val response = transport.get(endpoint, emptyMap())
            if (response.statusCode in 200..299) return ServicePayloadParser.recommendations(response.body)
            // Only a missing file is worth stepping past. A 500 or a rejected request says the
            // host is unhappy, and asking it the same question again will not change that.
            if (response.statusCode != 404) serviceError("GitHub-lista svara med status ${response.statusCode}")
            lastStatus = response.statusCode
        }
        serviceError("Fann ikkje tilrådingslista (status ${lastStatus ?: 404})")
    }

    private companion object {
        const val DEFAULT_ENDPOINT =
            "https://raw.githubusercontent.com/oyvhov/spole-recommendations/main/v1/recommendations.json"
        const val FALLBACK_ENDPOINT =
            "https://raw.githubusercontent.com/oyvhov/spole-recommendations/main/recommendations.json"
    }
}
