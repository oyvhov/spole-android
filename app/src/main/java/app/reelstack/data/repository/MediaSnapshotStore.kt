package app.reelstack.data.repository

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.repository.cache.CacheDatabase
import app.reelstack.data.repository.cache.CacheMetaRow
import app.reelstack.data.repository.cache.CacheSection
import app.reelstack.data.repository.cache.CachedMediaRow
import java.security.MessageDigest

data class CachedMediaSnapshot(
    val sessions: List<PlaybackSession>,
    val recentMovies: List<LibraryMedia>,
    val recentSeries: List<LibraryMedia>,
    val resume: List<LibraryMedia> = emptyList(),
    val upcoming: List<UpcomingMedia>,
    val recentReleases: List<UpcomingMedia> = emptyList(),
    val incoming: List<IncomingMedia>,
    val discover: List<DiscoverMedia>,
    val recommendations: List<DiscoverMedia> = emptyList(),
    val activity: List<ActivityEvent>,
    val refreshedAtEpochMillis: Long,
)

/**
 * The offline copy of the dashboard, in SQLite.
 *
 * It used to be one JSON blob in SharedPreferences: every read parsed every row, and nothing could
 * be read a page at a time. Rows now live in a Room table keyed by the account fingerprint, so a
 * stale copy is a cheap delete and a rail is a `LIMIT`ed query.
 *
 * What is deliberately *not* stored has not changed: active playback, download queues and the
 * activity feed are shared or personal state that must be re-verified against the server, never
 * replayed from disk. Only artwork-and-title rows are kept.
 */
class MediaSnapshotStore(context: Context) {
    private val dao = CacheDatabase.get(context).cacheDao()

    fun save(snapshot: MediaSyncSnapshot, fingerprint: String) {
        runCatching {
            val rows = buildList {
                addAll(libraryRows(fingerprint, CacheSection.RESUME, snapshot.resume))
                addAll(libraryRows(fingerprint, CacheSection.RECENT_MOVIES, snapshot.recentMovies))
                addAll(libraryRows(fingerprint, CacheSection.RECENT_SERIES, snapshot.recentSeries))
                addAll(upcomingRows(fingerprint, CacheSection.UPCOMING, snapshot.upcoming))
                addAll(upcomingRows(fingerprint, CacheSection.RECENT_RELEASES, snapshot.recentReleases))
                addAll(discoverRows(fingerprint, CacheSection.DISCOVER, snapshot.discover))
                addAll(discoverRows(fingerprint, CacheSection.RECOMMENDATIONS, snapshot.recommendations))
            }
            dao.replaceAll(CacheMetaRow(fingerprint, SCHEMA, snapshot.refreshedAt.toEpochMilli()), rows)
        }
    }

    /**
     * Returns the stored feed only when it belongs to exactly the accounts signed in right now and
     * was written by this version of the feed rules, so a previous user's dashboard can never
     * appear for the next one.
     */
    fun read(fingerprint: String): CachedMediaSnapshot? = runCatching {
        val meta = dao.meta(fingerprint, SCHEMA) ?: return null
        CachedMediaSnapshot(
            sessions = emptyList(),
            resume = library(fingerprint, CacheSection.RESUME),
            recentMovies = library(fingerprint, CacheSection.RECENT_MOVIES),
            recentSeries = library(fingerprint, CacheSection.RECENT_SERIES),
            upcoming = upcoming(fingerprint, CacheSection.UPCOMING),
            recentReleases = upcoming(fingerprint, CacheSection.RECENT_RELEASES),
            incoming = emptyList(),
            discover = discover(fingerprint, CacheSection.DISCOVER),
            recommendations = discover(fingerprint, CacheSection.RECOMMENDATIONS),
            activity = emptyList(),
            refreshedAtEpochMillis = meta.refreshedAtEpochMillis,
        )
    }.getOrNull()

    /** How many rows a rail has cached, so a caller can tell "no more" from "not loaded yet". */
    fun count(fingerprint: String, section: CacheSection): Int =
        runCatching { dao.count(fingerprint, section.name) }.getOrDefault(0)

    /** One page of a library rail, without reading the rows before it. */
    fun libraryPage(fingerprint: String, section: CacheSection, limit: Int, offset: Int): List<LibraryMedia> =
        runCatching { dao.page(fingerprint, section.name, limit, offset).map(::toLibrary) }
            .getOrDefault(emptyList())

    /** One page of a discovery rail. Used by "load more" once the first page is exhausted. */
    fun discoverPage(fingerprint: String, section: CacheSection, limit: Int, offset: Int): List<DiscoverMedia> =
        runCatching { dao.page(fingerprint, section.name, limit, offset).map(::toDiscover) }
            .getOrDefault(emptyList())

    fun clear() {
        runCatching {
            dao.clearRows()
            dao.clearMeta()
        }
    }

    private fun library(fingerprint: String, section: CacheSection) =
        libraryPage(fingerprint, section, PAGE_LIMIT, 0)

    private fun upcoming(fingerprint: String, section: CacheSection): List<UpcomingMedia> =
        runCatching { dao.page(fingerprint, section.name, PAGE_LIMIT, 0).map(::toUpcoming) }
            .getOrDefault(emptyList())

    private fun discover(fingerprint: String, section: CacheSection) =
        discoverPage(fingerprint, section, PAGE_LIMIT, 0)

    private fun libraryRows(fingerprint: String, section: CacheSection, items: List<LibraryMedia>) =
        items.take(CACHE_ITEM_LIMIT).mapIndexed { index, item ->
            row(fingerprint, section, index, item.id, item.title, item.subtitle, item.source, item.mediaType).copy(
                progress = item.progress,
                artworkUrl = item.artworkUrl,
                remoteId = item.remoteId,
                overview = item.overview,
                facts = item.facts.joinToString(SEPARATOR),
                genres = item.genres.joinToString(SEPARATOR),
            )
        }

    private fun upcomingRows(fingerprint: String, section: CacheSection, items: List<UpcomingMedia>) =
        items.take(CACHE_ITEM_LIMIT).mapIndexed { index, item ->
            row(fingerprint, section, index, item.id, item.title, item.subtitle, item.source, item.mediaType).copy(
                artworkUrl = item.artworkUrl,
                overview = item.overview,
                facts = item.facts.joinToString(SEPARATOR),
                genres = item.genres.joinToString(SEPARATOR),
                dateLabel = item.dateLabel,
                airDateEpochMillis = item.airDateEpochMillis,
            )
        }

    private fun discoverRows(fingerprint: String, section: CacheSection, items: List<DiscoverMedia>) =
        items.take(CACHE_ITEM_LIMIT).mapIndexed { index, item ->
            row(
                fingerprint, section, index, item.id, item.title, item.metadata,
                ServiceKind.SEERR, item.mediaType.orEmpty(),
            ).copy(
                artworkUrl = item.artworkUrl,
                remoteId = item.remoteId?.toString(),
                overview = item.overview,
                facts = item.facts.joinToString(SEPARATOR),
                genres = item.genres.joinToString(SEPARATOR),
                inLibrary = item.inLibrary,
                requested = item.requested,
                seerrStatus = item.seerrStatus,
            )
        }

    private fun row(
        fingerprint: String,
        section: CacheSection,
        position: Int,
        id: String,
        title: String,
        subtitle: String,
        source: ServiceKind,
        mediaType: String,
    ) = CachedMediaRow(
        fingerprint = fingerprint, section = section.name, position = position,
        id = id, title = title, subtitle = subtitle, source = source.name, mediaType = mediaType,
        progress = null, artworkUrl = null, remoteId = null, overview = null,
        facts = "", genres = "", dateLabel = null, airDateEpochMillis = null, seerrStatus = null,
    )

    private fun toLibrary(row: CachedMediaRow) = LibraryMedia(
        id = row.id,
        title = row.title,
        subtitle = row.subtitle.nynorskLegacyText(),
        progress = row.progress,
        artworkRes = R.drawable.media_placeholder,
        source = kind(row.source),
        artworkUrl = row.artworkUrl,
        remoteId = row.remoteId,
        overview = row.overview,
        facts = row.facts.split(SEPARATOR).filter(String::isNotBlank),
        genres = row.genres.split(SEPARATOR).filter(String::isNotBlank),
        mediaType = row.mediaType,
    )

    private fun toUpcoming(row: CachedMediaRow) = UpcomingMedia(
        id = row.id,
        title = row.title,
        subtitle = row.subtitle.nynorskLegacyText(),
        dateLabel = row.dateLabel.orEmpty().nynorskLegacyText(),
        airDateEpochMillis = row.airDateEpochMillis ?: 0,
        artworkRes = R.drawable.media_placeholder,
        source = kind(row.source),
        artworkUrl = row.artworkUrl,
        overview = row.overview,
        facts = row.facts.split(SEPARATOR).filter(String::isNotBlank),
        genres = row.genres.split(SEPARATOR).filter(String::isNotBlank),
        mediaType = row.mediaType,
    )

    private fun toDiscover(row: CachedMediaRow) = DiscoverMedia(
        id = row.id,
        title = row.title,
        metadata = row.subtitle.nynorskLegacyText(),
        artworkRes = R.drawable.media_placeholder,
        inLibrary = row.inLibrary,
        requested = row.requested,
        seerrStatus = row.seerrStatus,
        artworkUrl = row.artworkUrl,
        remoteId = row.remoteId?.toIntOrNull(),
        mediaType = row.mediaType.takeIf(String::isNotBlank),
        overview = row.overview,
        facts = row.facts.split(SEPARATOR).filter(String::isNotBlank),
        genres = row.genres.split(SEPARATOR).filter(String::isNotBlank),
    )

    private fun kind(name: String) = ServiceKind.entries.firstOrNull { it.name == name } ?: ServiceKind.JELLYFIN

    /**
     * A row cached by an older build — or one whose server labels types in English — must still
     * read as nynorsk. Carried over from the JSON store this replaced.
     */
    private fun String.nynorskLegacyText(): String = this
        .replace("Direct play", "Direkteavspeling", ignoreCase = true)
        .replace(" min left", " min att", ignoreCase = true)
        .replace("Movie ·", "Film ·", ignoreCase = true)
        .replace("Series ·", "Serie ·", ignoreCase = true)
        .replace("Tonight", "I kveld", ignoreCase = true)
        .replace("Tomorrow", "I morgon", ignoreCase = true)
        .replace("Today", "I dag", ignoreCase = true)
        .replace("Downloading", "Lastar ned", ignoreCase = true)
        .replace("Requested", "Lagd til", ignoreCase = true)
        .replace("Bestilt", "Lagd til", ignoreCase = true)
        .replace("Approved by Seerr", "Godkjend i Seerr", ignoreCase = true)
        .replace("Imported by Radarr", "Importert av Radarr", ignoreCase = true)
        .replace(Regex("([0-9]+) min ago", RegexOption.IGNORE_CASE), "For $1 min sidan")
        .replace("Yesterday", "I går", ignoreCase = true)

    companion object {
        private const val CACHE_ITEM_LIMIT = 60
        private const val PAGE_LIMIT = 30
        /** Unit separator, written as an escape so an editor cannot silently strip it. */
        private const val SEPARATOR = "\u001F"

        /** Bump when the feed rules change, so an older copy is dropped instead of shown. */
        private const val SCHEMA = 3

        /**
         * Identifies the exact set of signed-in services a cached feed belongs to. Any change of
         * server, account or token produces a different value. Tokens are hashed, never stored.
         * Keyed on the server's fixed identity so switching between the home and away address
         * does not discard the cache.
         */
        fun fingerprint(connections: List<ServiceConnection>): String {
            val digest = MessageDigest.getInstance("SHA-256")
            connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
                .map { "${it.kind.name}|${it.identity}|${it.userId}|${it.sessionCookie}|${hash(it.token)}" }
                .sorted()
                .forEach { digest.update(it.toByteArray(Charsets.UTF_8)) }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
