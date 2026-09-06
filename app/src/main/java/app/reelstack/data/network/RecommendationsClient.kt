package app.reelstack.data.network

/**
 * A small public catalogue kept in GitHub instead of a new application backend. The app caches
 * the parsed result in the normal dashboard snapshot, so a temporary GitHub outage does not make
 * the Home row disappear after it has loaded once.
 */
class RecommendationsClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    fun feed(): List<RemoteRecommendationItem> {
        val response = transport.get(endpoint, emptyMap())
        if (response.statusCode !in 200..299) error("GitHub-lista svara med status ${response.statusCode}")
        return ServicePayloadParser.recommendations(response.body)
    }

    private companion object {
        const val DEFAULT_ENDPOINT =
            "https://raw.githubusercontent.com/oyvhov/reelstack-android/main/recommendations.json"
    }
}
