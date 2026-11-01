package app.reelstack.launcher

import android.os.Bundle
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.RequestTrackingRepository

/**
 * What Stova asks Seerr to do for one person: send a request, or withdraw one they follow. Asked
 * with that person's Seerr on the binder thread the call came on, as [LauncherDiscoverQueries] is.
 * Every outcome is one [LauncherDiscoverResult]: an unknown person, a child or no Seerr that is set
 * up is `unknown`, and an API key is `not_personal` before anything is asked, since a request would
 * not be in the person's name. Only a personal session asks Seerr.
 */
internal class LauncherDiscoverActions(
    private val media: MediaSyncRepository,
    private val tracking: RequestTrackingRepository,
) {
    /**
     * The title [extras] names (`type` movie or tv, `tmdb` above zero, and for a series the
     * `seasons`), asked for as the person with Seerr's own quality and folder. Spole then follows it
     * with the bell on, titled from Seerr's details; it learns the request's Seerr id at the next
     * refresh of the requests, as for a request sent in Spole.
     */
    fun request(extras: Bundle?, seerr: ServiceConnection?, access: LauncherSeerrAccess) = personally(seerr, access) { session ->
        val type = extras?.getString("type")?.takeIf { it == "movie" || it == "tv" } ?: return@personally LauncherDiscoverResult.FAILED
        val tmdb = extras.getInt("tmdb").takeIf { it > 0 } ?: return@personally LauncherDiscoverResult.FAILED
        val seasons = if (type == "tv") extras.getIntArray("seasons")?.toSet().orEmpty() else emptySet()
        val bare = discoverMinimal(type, tmdb)
        runCatching { media.request(session, bare, seasons = seasons) }.exceptionOrNull()?.let { return@personally discoverResultFor(it) }
        val details = runCatching { media.details(session, bare) }.getOrNull()
        // The request is in Seerr now, whatever happens to Spole's own note of it: the next refresh
        // brings it in. Saying `failed` would only make the person ask again.
        runCatching {
            tracking.follow(session, session.userId, bare.copy(title = details?.title.orEmpty(), artworkUrl = details?.artworkUrl),
                seasons, notify = true)
        }
        LauncherDiscoverResult.OK
    }

    /**
     * The request the person follows with Seerr's id `request_id`, withdrawn in Seerr and no longer
     * followed. A bell is no request, and an id Spole does not follow is `unknown`.
     */
    fun withdraw(extras: Bundle?, seerr: ServiceConnection?, access: LauncherSeerrAccess) = personally(seerr, access) { session ->
        val id = extras?.getInt("request_id")?.takeIf { it > 0 } ?: return@personally LauncherDiscoverResult.UNKNOWN
        val tracked = tracking.list(tracking.scope(session, session.userId)).firstOrNull { it.requestId == id && it.stage.isRequest }
            ?: return@personally LauncherDiscoverResult.UNKNOWN
        if (runCatching { tracking.cancel(session, session.userId, tracked.key) }.isSuccess) LauncherDiscoverResult.OK
        else LauncherDiscoverResult.FAILED
    }

    /**
     * [act] with the person's own Seerr session, the only one that may ask in their name. Spole keeps
     * follows by the Seerr account, and a personal session is saved with that account's id.
     */
    private inline fun personally(seerr: ServiceConnection?, access: LauncherSeerrAccess,
        act: (ServiceConnection) -> LauncherDiscoverResult): LauncherDiscoverResult = when {
        seerr == null || access == LauncherSeerrAccess.NONE -> LauncherDiscoverResult.UNKNOWN
        access != LauncherSeerrAccess.PERSONAL -> LauncherDiscoverResult.NOT_PERSONAL
        else -> act(seerr)
    }
}
