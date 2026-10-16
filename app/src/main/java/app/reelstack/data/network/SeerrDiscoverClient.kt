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
