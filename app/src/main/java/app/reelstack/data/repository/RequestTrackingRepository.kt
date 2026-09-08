package app.reelstack.data.repository

import android.content.Context
import app.reelstack.data.model.*
import app.reelstack.data.network.*
import app.reelstack.background.LibraryNotifications
import kotlinx.serialization.json.*
import java.security.MessageDigest

/** Local follows are isolated by server and verified Seerr user, never by the entered username. */
class RequestTrackingRepository(
    private val context: Context,
    private val client: SeerrServiceClient = SeerrServiceClient(),
    private val profiles: AccountProfileClient = AccountProfileClient(),
) {
    private val preferences = context.getSharedPreferences("request_follows", Context.MODE_PRIVATE)

    /**
     * Keyed on the server's fixed identity, not its current address, so switching between the home
     * and away route keeps the follows you already have.
     */
    fun scope(connection: ServiceConnection, userId: String): String = MessageDigest.getInstance("SHA-256")
        .digest("${EndpointValidator.normalizeBaseUrl(connection.identity)}|$userId".toByteArray())
        .joinToString("") { "%02x".format(it) }

    fun list(scope: String): List<TrackedRequest> = synchronized(lock) {
        runCatching { Json.parseToJsonElement(preferences.getString(scope, "[]")!!).jsonArray.map { decode(it.jsonObject) } }
            .getOrDefault(emptyList()).sortedByDescending { it.updatedAt }
    }

    fun put(scope: String, item: TrackedRequest) = synchronized(lock) {
        val items = (list(scope).filterNot { it.key == item.key } + item).sortedByDescending { it.updatedAt }.take(100)
        preferences.edit().putString(scope, JsonArray(items.map(::encode)).toString()).commit()
    }

    fun setNotify(scope: String, key: String, enabled: Boolean) = synchronized(lock) {
        list(scope).firstOrNull { it.key == key }?.let { put(scope, it.copy(notify = enabled)) }
    }

    fun follow(connection: ServiceConnection, userId: String, media: DiscoverMedia, seasons: Set<Int>, notify: Boolean) {
        val scope = scope(connection, userId)
        val key = "${media.mediaType}:${media.remoteId}:${seasons.sorted().joinToString(",")}"
        put(scope, TrackedRequest(key, requireNotNull(media.remoteId), requireNotNull(media.mediaType), media.title,
            media.artworkUrl, seasons, notify, updatedAt = System.currentTimeMillis()))
    }

    /** Seerr reads only. A bell must never submit a request or change server monitoring. */
    fun setSeasonWatch(connection: ServiceConnection, userId: String, media: DiscoverMedia, number: Int, enabled: Boolean) {
        require(connection.kind == ServiceKind.SEERR && connection.sessionCookie && media.mediaType == "tv" && media.remoteId != null && number >= 0)
        if (enabled) {
            val actor = profiles.load(connection)
            check(actor.isPersonal && actor.id == userId) { "Seerr-kontoen er endra. Opne sesongane på nytt." }
        }
        val detail = if (enabled) client.details(connection, "tv", media.remoteId, includeOverviewFallback = false) else null
        if (enabled) check(detail?.seerrStatus != 6 && detail?.seasons?.any { it.number == number && it.canWatch } == true) {
            "Sesongstatusen er endra. Sjekk på nytt før du slår på varsel."
        }
        val scope = scope(connection, userId)
        synchronized(lock) {
            val active = ConnectionRepository(context).list().firstOrNull { it.kind == ServiceKind.SEERR }
            check(active?.token == connection.token && active.baseUrl == connection.baseUrl && active.sessionCookie) {
                "Seerr-kontoen er endra. Opne sesongane på nytt."
            }
            val items = list(scope)
            // Reuse an exact single-season personal request, preserving its ID and alert history.
            // A multi-season request has a different completion condition and is not interchangeable.
            val request = items.firstOrNull {
                !it.availabilityOnly && !it.is4k && it.mediaType == "tv" && it.mediaId == media.remoteId && it.seasons == setOf(number)
            }
            val key = "watch:tv:${media.remoteId}:$number"
            if (request != null) {
                put(scope, request.copy(notify = enabled, readyNotificationOnly = if (enabled) true else request.readyNotificationOnly))
                removeLocalWatch(scope, key)
            } else if (enabled) {
                val existing = items.firstOrNull { it.key == key }
                val progress = availabilityWatchProgress(detail!!.seasons, setOf(number), detail.downloads, detail.seerrStatus)
                put(scope, existing?.copy(notify = true) ?: TrackedRequest(
                    key, media.remoteId, "tv", detail.title ?: media.title, detail.artworkUrl ?: media.artworkUrl,
                    setOf(number), stage = progress.stage, percent = progress.percent,
                    updatedAt = System.currentTimeMillis(), availabilityOnly = true, readyNotificationOnly = true,
                ))
            } else removeLocalWatch(scope, key)
        }
    }

    fun watchedSeasons(connection: ServiceConnection, userId: String, mediaId: Int): Set<Int> =
        list(scope(connection, userId)).filter {
            it.notify && !it.is4k && it.mediaType == "tv" && it.mediaId == mediaId &&
                (it.availabilityOnly || it.seasons.size == 1)
        }.flatMap { it.seasons }.toSet()

    private fun removeLocalWatch(scope: String, key: String) = synchronized(lock) {
        val remaining = list(scope).filterNot { it.key == key && it.availabilityOnly }
        preferences.edit().putString(scope, JsonArray(remaining.map(::encode)).toString()).commit()
    }

    /** Withdraws the request in Seerr, then drops the local follow so it cannot reappear. */
    fun cancel(connection: ServiceConnection, userId: String, key: String) {
        val scope = scope(connection, userId)
        val tracked = list(scope).firstOrNull { it.key == key } ?: error("Fann ikkje førespurnaden.")
        if (tracked.availabilityOnly) {
            removeLocalWatch(scope, key)
            return
        }
        val requestId = tracked.requestId
            ?: error("Denne førespurnaden manglar Seerr-ID. Oppdater lista og prøv igjen.")
        client.cancelRequest(connection, requestId, userId)
        synchronized(lock) {
            val remaining = list(scope).filterNot { it.key == key }
            preferences.edit().putString(scope, JsonArray(remaining.map(::encode)).toString()).commit()
        }
    }

    fun refresh(connection: ServiceConnection): Pair<String, List<TrackedRequest>> {
        val actor = profiles.load(connection)
        check(actor.isPersonal) { "Logg inn personleg for å følgje førespurnader." }
        val scope = scope(connection, actor.id)
        val remote = client.requests(connection, actor.id).filter { it.ownerId == actor.id }
        // Import existing personal requests too. Notifications on old requests remain opt-in.
        // Read the stored follows once: re-parsing them per remote request turned an ordinary
        // account into a hundred JSON parses on every poll.
        val known = list(scope).mapTo(mutableSetOf()) { it.key }
        remote.forEach { request ->
            val id = request.remoteId ?: return@forEach
            val key = "${request.mediaType}:$id:${request.seasons.sorted().joinToString(",")}" + if (request.is4k) ":4k" else ""
            if (known.add(key)) synchronized(lock) {
                val items = list(scope)
                if (items.any { it.key == key }) return@synchronized
                val localWatch = items.firstOrNull {
                    it.availabilityOnly && it.mediaId == id && it.mediaType == request.mediaType &&
                        it.seasons == request.seasons && it.is4k == request.is4k
                }
                put(scope, TrackedRequest(key, id, request.mediaType, request.title ?: if (request.mediaType == "tv") "Serie" else "Film",
                    request.artworkUrl, request.seasons, notify = localWatch?.notify ?: false, stage = RequestStage.UNKNOWN, is4k = request.is4k,
                    updatedAt = runCatching { java.time.Instant.parse(request.createdAt).toEpochMilli() }.getOrDefault(0),
                    requestId = request.id, notifiedStages = localWatch?.notifiedStages.orEmpty(), notified = localWatch?.notified ?: false,
                    readyNotificationOnly = localWatch != null))
                localWatch?.let { removeLocalWatch(scope, it.key) }
            }
        }
        val current = list(scope)
        // Round-robin keeps a large account from starving older follows. No redundant synopsis lookup.
        val targets = current.sortedBy { it.checkedAt }.take(20)
        val detailCache = mutableMapOf<Pair<String, Int>, Result<RemoteMediaDetails>>()
        targets.forEach { original ->
            val matching = remote.firstOrNull { !original.availabilityOnly && it.remoteId == original.mediaId && it.mediaType == original.mediaType && it.seasons == original.seasons && it.is4k == original.is4k }
            val result = detailCache.getOrPut(original.mediaType to original.mediaId) {
                runCatching { client.details(connection, original.mediaType, original.mediaId, includeOverviewFallback = false) }
            }
            synchronized(lock) {
                val item = list(scope).firstOrNull { it.key == original.key } ?: return@synchronized
                val updated = result.fold(onSuccess = { detail ->
                    val progress = if (item.availabilityOnly) availabilityWatchProgress(
                        if (item.is4k) detail.seasons4k else detail.seasons, item.seasons,
                        if (item.is4k) detail.downloads4k else detail.downloads,
                        if (item.is4k) detail.status4k else detail.seerrStatus,
                    ) else if (item.is4k) requestProgress(detail.status4k, detail.seasons4k, item.seasons, detail.downloads4k, matching?.status)
                        else requestProgress(detail.seerrStatus, detail.seasons, item.seasons, detail.downloads, matching?.status)
                    item.copy(title = detail.title ?: item.title, artworkUrl = detail.artworkUrl ?: item.artworkUrl,
                        stage = progress.stage, percent = progress.percent,
                        availableSeasons = (if (item.is4k) detail.seasons4k else detail.seasons)
                            .filter { it.number in item.seasons && it.status == 5 }.map { it.number }.toSet())
                }, onFailure = { item.copy(stage = RequestStage.UNKNOWN, percent = null) })
                var saved = updated.copy(checkedAt = System.currentTimeMillis(),
                    requestId = matching?.id ?: updated.requestId)
                // Recheck the configured account before notifying: a switched/removed session must stay silent.
                val active = ConnectionRepository(context).list().firstOrNull { it.kind == ServiceKind.SEERR }
                val event = if ((updated.availabilityOnly || updated.readyNotificationOnly) && updated.stage != RequestStage.AVAILABLE) null else when (updated.stage) {
                    RequestStage.AVAILABLE -> app.reelstack.background.NotificationEvent.READY
                    RequestStage.DOWNLOADING -> app.reelstack.background.NotificationEvent.DOWNLOADING
                    RequestStage.FAILED, RequestStage.DECLINED -> app.reelstack.background.NotificationEvent.FAILED
                    else -> null
                }
                if (event != null && updated.notify && updated.stage.name !in updated.notifiedStages &&
                    active?.token == connection.token && active.baseUrl == connection.baseUrl &&
                    AppPreferencesRepository(context).notificationsEnabled && LibraryNotifications.show(context, scope, updated,
                        stillEligible = {
                            val configured = ConnectionRepository(context).list().firstOrNull { it.kind == ServiceKind.SEERR }
                            configured?.token == connection.token && configured.baseUrl == connection.baseUrl &&
                                AppPreferencesRepository(context).notificationsEnabled && list(scope).firstOrNull { it.key == item.key }?.notify == true
                        }, event = event)) {
                    saved = saved.copy(
                        notifiedStages = saved.notifiedStages + updated.stage.name,
                        notified = saved.notified || updated.stage == RequestStage.AVAILABLE,
                    )
                }
                put(scope, saved)
            }
        }
        return scope to list(scope)
    }

    private fun encode(item: TrackedRequest) = buildJsonObject {
        put("key", item.key); put("id", item.mediaId); put("type", item.mediaType); put("title", item.title)
        item.artworkUrl?.let { put("art", it) }; put("seasons", JsonArray(item.seasons.sorted().map(::JsonPrimitive)))
        put("notify", item.notify); put("notified", item.notified); put("stage", item.stage.name)
        item.percent?.let { put("percent", it) }; put("updated", item.updatedAt)
        put("checked", item.checkedAt); put("is4k", item.is4k)
        put("availabilityOnly", item.availabilityOnly)
        put("readyNotificationOnly", item.readyNotificationOnly)
        item.requestId?.let { put("requestId", it) }
        put("notifiedStages", item.notifiedStages.sorted().joinToString(","))
        put("available", JsonArray(item.availableSeasons.sorted().map(::JsonPrimitive)))
    }
    private fun decode(item: JsonObject) = TrackedRequest(
        key = item.getValue("key").jsonPrimitive.content, mediaId = item.getValue("id").jsonPrimitive.int,
        mediaType = item.getValue("type").jsonPrimitive.content, title = item.getValue("title").jsonPrimitive.content,
        artworkUrl = item["art"]?.jsonPrimitive?.content,
        seasons = item.getValue("seasons").jsonArray.map { it.jsonPrimitive.int }.toSet(),
        notify = item.getValue("notify").jsonPrimitive.boolean, notified = item.getValue("notified").jsonPrimitive.boolean,
        stage = RequestStage.valueOf(item.getValue("stage").jsonPrimitive.content), percent = item["percent"]?.jsonPrimitive?.int,
        updatedAt = item.getValue("updated").jsonPrimitive.long,
        checkedAt = item["checked"]?.jsonPrimitive?.longOrNull ?: 0,
        is4k = item["is4k"]?.jsonPrimitive?.booleanOrNull == true,
        availableSeasons = item["available"]?.jsonArray?.map { it.jsonPrimitive.int }?.toSet().orEmpty(),
        requestId = item["requestId"]?.jsonPrimitive?.intOrNull,
        availabilityOnly = item["availabilityOnly"]?.jsonPrimitive?.booleanOrNull == true,
        readyNotificationOnly = item["readyNotificationOnly"]?.jsonPrimitive?.booleanOrNull == true,
        // A follow saved before per-stage alerts only knew it had announced availability.
        notifiedStages = item["notifiedStages"]?.jsonPrimitive?.content
            ?.split(',')?.filter(String::isNotBlank)?.toSet()
            ?: if (item.getValue("notified").jsonPrimitive.boolean) setOf(RequestStage.AVAILABLE.name) else emptySet(),
    )
    companion object { private val lock = Any() }
}
