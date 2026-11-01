package app.reelstack.launcher

import android.net.Uri
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.RequestTrackingRepository
import app.reelstack.localization.LocalizedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

/** The Discover paths Stova may ask, `discover` and `discover/<segment>`, and the columns of each answer. */
enum class LauncherDiscoverPath(val segment: String?, val columns: Array<String>) {
    ROWS(null, LauncherDiscoverColumns.ITEM),
    REQUESTS("requests", LauncherDiscoverColumns.REQUEST),
    TITLE("title", LauncherDiscoverColumns.TITLE),
    SEASONS("seasons", LauncherDiscoverColumns.SEASON),
    RELATED("related", LauncherDiscoverColumns.ITEM),
    SEARCH("search", LauncherDiscoverColumns.ITEM);

    companion object {
        fun of(segments: List<String>): LauncherDiscoverPath? =
            if (segments.firstOrNull() != "discover" || segments.size > 2) null
            else entries.firstOrNull { it.segment == segments.getOrNull(1) }
    }
}

/** What one Discover path answers: its rows, and for a search the page they are and whether another follows. */
internal class LauncherDiscoverAnswer(val rows: List<Array<Any?>>, val page: Int = 1, val hasMore: Boolean = false) {
    companion object {
        val EMPTY = LauncherDiscoverAnswer(emptyList())
    }
}

/**
 * What Seerr says for one person, asked with that person's Seerr on the binder thread the query
 * came on, as `rows` is. A call that fails gives no rows, never an error: Stova shows what it got.
 * Season names are said by [name], which holds the Context.
 */
internal class LauncherDiscoverQueries(
    private val media: MediaSyncRepository,
    private val tracking: RequestTrackingRepository,
    private val name: (LocalizedText) -> String,
) {
    fun answer(path: LauncherDiscoverPath, uri: Uri, seerr: ServiceConnection, access: LauncherSeerrAccess): LauncherDiscoverAnswer {
        val named = titleIn(uri)
        return when (path) {
            LauncherDiscoverPath.ROWS -> LauncherDiscoverAnswer(rows(seerr))
            LauncherDiscoverPath.REQUESTS -> LauncherDiscoverAnswer(requests(seerr, access, uri.getQueryParameter("refresh") == "1"))
            LauncherDiscoverPath.TITLE -> LauncherDiscoverAnswer(listOfNotNull(named?.let { title(seerr, access, it) }))
            LauncherDiscoverPath.SEASONS -> LauncherDiscoverAnswer(named?.takeIf { it.mediaType == "tv" }?.let { seasons(seerr, it) }.orEmpty())
            LauncherDiscoverPath.RELATED -> LauncherDiscoverAnswer(named?.let { related(seerr, it) }.orEmpty())
            LauncherDiscoverPath.SEARCH -> search(seerr, uri.getQueryParameter("q"),
                uri.getQueryParameter("page")?.toIntOrNull()?.takeIf { it >= 1 } ?: 1)
        }
    }

    /** The title a query names: `type` movie or tv and a TMDB id above zero, or none. */
    private fun titleIn(uri: Uri): DiscoverMedia? {
        val type = uri.getQueryParameter("type")?.takeIf { it == "movie" || it == "tv" } ?: return null
        val tmdb = uri.getQueryParameter("tmdb")?.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return discoverMinimal(type, tmdb)
    }

    private fun rows(seerr: ServiceConnection) = inParallel(DISCOVER_ROWS.map { (kind, row) ->
        row to { media.discoverRow(seerr, kind).items }
    })

    /**
     * The requests the person follows, as Spole keeps them. `refresh=1` asks Seerr first, which only a
     * personal session may; when that fails, what Spole kept is still the answer.
     */
    private fun requests(seerr: ServiceConnection, access: LauncherSeerrAccess, refresh: Boolean): List<Array<Any?>> {
        val refreshed = if (refresh && access == LauncherSeerrAccess.PERSONAL) {
            runCatching { tracking.refresh(seerr).second }.getOrNull()
        } else null
        return (refreshed ?: followed(seerr)).map(::discoverRequestRow)
    }

    /** Spole keeps follows by the Seerr account, and a personal session is saved with that account's id. */
    private fun followed(seerr: ServiceConnection) = tracking.list(tracking.scope(seerr, seerr.userId))

    /** The page of one title. Rotten Tomatoes is its own Seerr call, asked beside the details. */
    private fun title(seerr: ServiceConnection, access: LauncherSeerrAccess, bare: DiscoverMedia): Array<Any?>? = runBlocking {
        val critic = async(Dispatchers.IO) { runCatching { media.criticScore(seerr, bare) }.getOrNull() }
        val details = runCatching { media.details(seerr, bare) }.getOrNull() ?: return@runBlocking null
        val tracked = followed(seerr).firstOrNull { it.mediaId == bare.remoteId && it.mediaType == bare.mediaType && it.stage.isRequest }
        discoverTitleRow(bare, details, critic.await(), access, tracked)
    }

    private fun seasons(seerr: ServiceConnection, series: DiscoverMedia) =
        runCatching { discoverSeasonRows(media.details(seerr, series), name) }.getOrDefault(emptyList())

    private fun related(seerr: ServiceConnection, title: DiscoverMedia) = inParallel(listOf(
        LauncherDiscoverRows.SIMILAR to { media.similar(seerr, title) },
        LauncherDiscoverRows.RECOMMENDED to { media.recommended(seerr, title) },
    ))

    /** One page of a search. Fewer than two letters is no search, and a page that fails has no more after it. */
    private fun search(seerr: ServiceConnection, query: String?, page: Int): LauncherDiscoverAnswer {
        val text = query?.trim()?.takeIf { it.length >= 2 } ?: return LauncherDiscoverAnswer.EMPTY
        val result = runCatching { media.search(seerr, text, page) }.getOrNull() ?: return LauncherDiscoverAnswer(emptyList(), page)
        return LauncherDiscoverAnswer(cards(LauncherDiscoverRows.SEARCH, result.items), result.page, result.hasMore)
    }

    /** The rows asked all at once, in the order given; a row that fails is left out and the others still come. */
    private fun inParallel(fetches: List<Pair<String, () -> List<DiscoverMedia>>>): List<Array<Any?>> = runBlocking {
        fetches.map { (row, fetch) -> async(Dispatchers.IO) { cards(row, runCatching(fetch).getOrDefault(emptyList())) } }.awaitAll()
    }.flatten()

    /** A title without a type or TMDB id could be neither opened nor asked for, so it is not sent. */
    private fun cards(row: String, items: List<DiscoverMedia>) =
        items.filter { it.mediaType != null && it.remoteId != null }.map { discoverItemRow(row, it) }

    private companion object {
        val DISCOVER_ROWS = listOf(
            DiscoverRowKind.TRENDING to LauncherDiscoverRows.TRENDING,
            DiscoverRowKind.UPCOMING_MOVIES to LauncherDiscoverRows.UPCOMING_MOVIES,
            DiscoverRowKind.UPCOMING_SERIES to LauncherDiscoverRows.UPCOMING_SERIES,
        )
    }
}
