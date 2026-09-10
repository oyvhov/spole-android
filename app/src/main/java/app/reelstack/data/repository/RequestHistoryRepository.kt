package app.reelstack.data.repository

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import java.time.Instant

data class PersonalHistoryPage(val items: List<TrackedRequest>, val nextOffset: Int, val hasMore: Boolean, val total: Int?)
class HistoryIdentityChangedException : Exception()

/** Browsing history never enables notifications, creates follows or mutates a Seerr request. */
class RequestHistoryRepository(
    private val client: SeerrServiceClient = SeerrServiceClient(),
    private val profiles: AccountProfileClient = AccountProfileClient(),
) {
    fun load(connection: ServiceConnection, expectedUserId: String, offset: Int, checkActive: () -> Unit = {}): PersonalHistoryPage {
        checkActive()
        val actor = profiles.load(connection)
        if (!actor.isPersonal || actor.id != expectedUserId) throw HistoryIdentityChangedException()
        checkActive()
        val page = client.requestHistory(connection, actor.id, offset)
        val metadata = mutableMapOf<Pair<String, Int>, RemoteMediaDetails?>()
        val items = page.items.map { request ->
            checkActive()
            val id = request.remoteId
            // Metadata work is limited to this 20-row page; a title failure never hides a request.
            val detail = if (id != null && request.mediaType in setOf("tv", "movie") &&
                (request.title.isNullOrBlank() || request.artworkUrl == null)) {
                metadata.getOrPut(request.mediaType to id) {
                    try { client.details(connection, request.mediaType, id, includeOverviewFallback = false) }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                }
            } else null
            // A rejected historical request remains rejected even if somebody later obtains the film.
            val progress = when (request.status) {
                3 -> RequestProgress(RequestStage.DECLINED)
                4 -> RequestProgress(RequestStage.FAILED)
                else -> if (request.mediaStatus == null) RequestProgress(RequestStage.UNKNOWN)
                    else requestProgress(request.mediaStatus, request.availableSeasons, request.seasons, request.downloads, request.status)
            }
            TrackedRequest(key = "history-${request.id}", mediaId = id ?: 0, mediaType = request.mediaType,
                title = request.title?.takeIf(String::isNotBlank) ?: detail?.title.orEmpty(),
                artworkUrl = request.artworkUrl ?: detail?.artworkUrl, seasons = request.seasons,
                notify = false, stage = progress.stage, percent = progress.percent, is4k = request.is4k,
                updatedAt = runCatching { Instant.parse(request.createdAt).toEpochMilli() }.getOrDefault(0),
                requestId = request.id)
        }
        return PersonalHistoryPage(items, page.nextOffset, page.hasMore, page.total)
    }
}
