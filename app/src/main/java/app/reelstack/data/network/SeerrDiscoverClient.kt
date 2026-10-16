package app.reelstack.data.network

import app.reelstack.data.model.DiscoverGenre
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind

/** The discovery, person and request-option endpoints of Seerr. One instance lives in the repository. */
class SeerrDiscoverClient(private val transport: JsonHttpTransport = HttpTransport()) {
    fun row(connection: ServiceConnection, kind: DiscoverRowKind, page: Int = 1): SeerrSearchPage =
        page(connection, "api/v1/${kind.path}?page=$page&language=nb", page)

    fun genrePage(connection: ServiceConnection, genre: DiscoverGenre, page: Int = 1): SeerrSearchPage {
        val type = if (genre.mediaType == "tv") "tv" else "movies"
        return page(connection, "api/v1/discover/$type/genre/${genre.id}?page=$page&language=nb", page)
    }

    fun genres(connection: ServiceConnection, mediaType: String): List<DiscoverGenre> {
        require(mediaType == "movie" || mediaType == "tv")
        return ServicePayloadParser.genreSlider(get(connection, "api/v1/discover/genreslider/$mediaType?language=nb"), mediaType)
    }

    fun similar(connection: ServiceConnection, mediaType: String, id: Int): List<RemoteDiscoverItem> =
        related(connection, mediaType, id, "similar")

    fun recommendations(connection: ServiceConnection, mediaType: String, id: Int): List<RemoteDiscoverItem> =
        related(connection, mediaType, id, "recommendations")

    /** TMDB lists of similar titles often leave out the type; it is the type that was asked for. */
    private fun related(connection: ServiceConnection, mediaType: String, id: Int, kind: String): List<RemoteDiscoverItem> {
        require(mediaType == "movie" || mediaType == "tv")
        return ServicePayloadParser.discover(get(connection, "api/v1/$mediaType/$id/$kind?page=1&language=nb"))
            .map { if (it.mediaType == mediaType) it else it.copy(mediaType = mediaType, id = "seerr-$mediaType-${it.remoteId}") }
    }

    /** TMDB often has no Norwegian biography; the English one is better than none. */
    fun person(connection: ServiceConnection, id: Int): ServicePayloadParser.RemotePerson {
        val person = ServicePayloadParser.person(get(connection, "api/v1/person/$id?language=nb"))
        if (person.biography != null) return person
        val english = runCatching { ServicePayloadParser.person(get(connection, "api/v1/person/$id?language=en")) }.getOrNull()
        return person.copy(biography = english?.biography)
    }

    fun personCredits(connection: ServiceConnection, id: Int): List<RemoteDiscoverItem> =
        ServicePayloadParser.personCredits(get(connection, "api/v1/person/$id/combined_credits?language=nb"))

    /** Profiles and folders of the default Radarr/Sonarr for this edition, or null when Seerr has none. */
    fun requestOptions(connection: ServiceConnection, mediaType: String, is4k: Boolean): app.reelstack.data.model.RequestOptions? {
        val service = if (mediaType == "tv") "sonarr" else "radarr"
        val servers = ServicePayloadParser.serviceServers(get(connection, "api/v1/service/$service"))
        val server = servers.firstOrNull { it.is4k == is4k && it.isDefault } ?: servers.firstOrNull { it.is4k == is4k } ?: return null
        return ServicePayloadParser.serviceDetails(get(connection, "api/v1/service/$service/${server.id}"), is4k)
    }

    /** Whether Seerr has 4K turned on for this type of title. */
    fun fourKEnabled(connection: ServiceConnection, mediaType: String): Boolean {
        val root = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(get(connection, "api/v1/settings/public")) }.getOrNull()
            as? kotlinx.serialization.json.JsonObject ?: return false
        val key = if (mediaType == "tv") "series4kEnabled" else "movie4kEnabled"
        return (root[key] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
    }

    /** The Rotten Tomatoes critics score Seerr knows for a title; a title without one answers 404. */
    fun criticScore(connection: ServiceConnection, mediaType: String, id: Int): Int? {
        require(mediaType == "movie" || mediaType == "tv")
        val body = runCatching { get(connection, "api/v1/$mediaType/$id/ratings") }.getOrNull() ?: return null
        val root = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(body) }.getOrNull()
            as? kotlinx.serialization.json.JsonObject ?: return null
        return (root["criticsScore"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()?.takeIf { it in 0..100 }
    }

    private fun page(connection: ServiceConnection, path: String, page: Int): SeerrSearchPage {
        val body = get(connection, path)
        return SeerrSearchPage(ServicePayloadParser.discover(body), page, ServicePayloadParser.totalPages(body))
    }

    internal fun get(connection: ServiceConnection, path: String): String {
        require(connection.kind == ServiceKind.SEERR)
        val response = transport.get(EndpointValidator.resolve(connection.baseUrl, path), seerrHeaders(connection))
        response.requireSuccess(connection.kind)
        return response.body
    }
}

internal fun seerrHeaders(connection: ServiceConnection): Map<String, String> =
    if (connection.sessionCookie) seerrCookieHeaders(connection.token) else mapOf("X-Api-Key" to connection.token)
