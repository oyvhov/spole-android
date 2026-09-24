package app.reelstack.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What a Home row shows. A kind that belongs to a media server exists once per server, so
 * "Hald fram · Emby" and "Hald fram · Jellyfin" are two rows with their own place and switch.
 */
enum class HomeRowKind(val perSource: Boolean) {
    CONTINUE_WATCHING(true),
    NOW_PLAYING(false),
    NEXT_UP(true),
    FAVOURITES(true),
    NEW_MOVIES(true),
    NEW_SERIES(true),
    RECOMMENDATIONS(false),
    RECENT_RELEASES(false),
    UPCOMING(false),
    ;

    /** Whether the reader can choose which of a server's libraries feed this row. */
    val usesLibraries: Boolean get() = perSource

    /**
     * Rows that cost requests and nothing else. Continue watching and next up are always read,
     * because Google TV's Watch Next and the tablet feature use them even when the row is hidden.
     */
    val skippedWhenHidden: Boolean get() = this in setOf(FAVOURITES, NEW_MOVIES, NEW_SERIES, RECENT_RELEASES)

    /** Whether a library of [collectionType] can contain anything for this row. */
    fun accepts(collectionType: String?): Boolean = when (collectionType?.lowercase()) {
        null, "", "mixed" -> true
        "movies" -> this != NEW_SERIES && this != NEXT_UP
        "tvshows", "tv" -> this != NEW_MOVIES
        else -> this == CONTINUE_WATCHING || this == FAVOURITES
    }
}

/** The media servers that get their own Home rows, in the order a new layout lists them. */
val HOME_MEDIA_SOURCES: List<ServiceKind> = listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)

data class HomeRowKey(val kind: HomeRowKind, val source: ServiceKind? = null) {
    /** Stable across versions: stored in preferences and used as the per-row format key. */
    val id: String get() = if (source == null) kind.name else "${kind.name}:${source.name}"

    companion object {
        fun parse(id: String): HomeRowKey? {
            val kind = HomeRowKind.entries.firstOrNull { it.name == id.substringBefore(':') } ?: return null
            val source = id.substringAfter(':', "").takeIf(String::isNotEmpty)
                ?.let { name -> HOME_MEDIA_SOURCES.firstOrNull { it.name == name } ?: return null }
            return if (kind.perSource == (source != null)) HomeRowKey(kind, source) else null
        }
    }
}

/** The key the per-row format was saved under before rows were split per server. */
fun HomeRowKey.legacyFormatKey(): String = when (kind) {
    HomeRowKind.NEW_MOVIES -> if (source == ServiceKind.EMBY) HomeRow.EMBY_MOVIES.name else HomeRow.JELLYFIN_MOVIES.name
    HomeRowKind.NEW_SERIES -> if (source == ServiceKind.EMBY) HomeRow.EMBY_SERIES.name else HomeRow.JELLYFIN_SERIES.name
    else -> kind.name
}

/**
 * A row's chosen card format. [rowKey] is a [HomeRowKey.id]; a format chosen before the split
 * still applies until the row gets one of its own.
 */
fun homeRowFormat(formats: Map<String, String>, rowKey: String?): String? {
    rowKey ?: return null
    formats[rowKey]?.let { return it }
    return HomeRowKey.parse(rowKey)?.legacyFormatKey()?.let(formats::get)
}

/**
 * The order of every Home row and which of them are hidden.
 *
 * Every known row is always in [order], including rows for a server that is not connected, so a
 * server added later appears where it was left rather than at the end.
 */
data class HomeLayout(val order: List<HomeRowKey>, val hidden: Set<HomeRowKey> = emptySet()) {

    fun isVisible(key: HomeRowKey): Boolean = key !in hidden

    fun withVisible(key: HomeRowKey, visible: Boolean): HomeLayout =
        copy(hidden = if (visible) hidden - key else hidden + key)

    /** Shows or hides every row that comes from one server. */
    fun withSourceVisible(source: ServiceKind, visible: Boolean): HomeLayout {
        val rows = order.filter { it.source == source }.toSet()
        return copy(hidden = if (visible) hidden - rows else hidden + rows)
    }

    /**
     * Swaps [key] with its neighbour among [shown], the rows the editor lists. Rows that are not
     * listed, such as those of an unconnected server, keep their place.
     */
    fun moved(key: HomeRowKey, direction: Int, shown: List<HomeRowKey>): HomeLayout {
        if (direction != -1 && direction != 1) return this
        val visibleOrder = order.filter { it in shown }
        val position = visibleOrder.indexOf(key)
        if (position < 0) return this
        val neighbour = visibleOrder.getOrNull(position + direction) ?: return this
        return copy(order = order.toMutableList().apply {
            val from = indexOf(key)
            val to = indexOf(neighbour)
            this[from] = neighbour
            this[to] = key
        })
    }

    fun encode(): String = JsonObject(mapOf(
        "version" to JsonPrimitive(1),
        "order" to JsonArray(order.map { JsonPrimitive(it.id) }),
        "hidden" to JsonArray(hidden.sortedBy { order.indexOf(it) }.map { JsonPrimitive(it.id) }),
    )).toString()

    companion object {
        val ALL_KEYS: List<HomeRowKey> = legacyOrder(HomeRow.entries)

        val DEFAULT = HomeLayout(ALL_KEYS)

        /** Drops unknown rows, removes repeats and appends rows a newer version introduced. */
        fun normalized(order: List<HomeRowKey>, hidden: Set<HomeRowKey>): HomeLayout {
            val known = order.distinct().filter { it in ALL_KEYS }
            return HomeLayout(known + ALL_KEYS.filterNot { it in known }, hidden.filterTo(mutableSetOf()) { it in ALL_KEYS })
        }

        fun decode(saved: String?): HomeLayout? = runCatching {
            val root = Json.parseToJsonElement(saved ?: return null).jsonObject
            fun keys(name: String) = root[name]?.jsonArray.orEmpty().mapNotNull { HomeRowKey.parse(it.jsonPrimitive.content) }
            normalized(keys("order"), keys("hidden").toSet())
        }.getOrNull()

        /**
         * The layout the older settings described. One switch used to cover both servers'
         * "continue watching" and favourites, and next up had its own personalisation switch.
         */
        fun fromLegacy(order: List<HomeRow>, sections: Set<HomeSection>, showNextUp: Boolean): HomeLayout {
            val keys = legacyOrder(decodeHomeRowOrder(order.joinToString(",") { it.name }))
            val hidden = HomeRow.entries.flatMap { row ->
                val shown = if (row == HomeRow.NEXT_UP) showNextUp else row.section in sections
                if (shown) emptyList() else legacyKeys(row)
            }.toSet()
            return normalized(keys, hidden)
        }

        private fun legacyOrder(rows: List<HomeRow>): List<HomeRowKey> = rows.flatMap(::legacyKeys)

        private fun legacyKeys(row: HomeRow): List<HomeRowKey> = when (row) {
            HomeRow.CONTINUE_WATCHING -> HOME_MEDIA_SOURCES.map { HomeRowKey(HomeRowKind.CONTINUE_WATCHING, it) }
            HomeRow.NOW_PLAYING -> listOf(HomeRowKey(HomeRowKind.NOW_PLAYING))
            HomeRow.NEXT_UP -> HOME_MEDIA_SOURCES.map { HomeRowKey(HomeRowKind.NEXT_UP, it) }
            HomeRow.FAVOURITES -> HOME_MEDIA_SOURCES.map { HomeRowKey(HomeRowKind.FAVOURITES, it) }
            HomeRow.JELLYFIN_MOVIES -> listOf(HomeRowKey(HomeRowKind.NEW_MOVIES, ServiceKind.JELLYFIN))
            HomeRow.EMBY_MOVIES -> listOf(HomeRowKey(HomeRowKind.NEW_MOVIES, ServiceKind.EMBY))
            HomeRow.JELLYFIN_SERIES -> listOf(HomeRowKey(HomeRowKind.NEW_SERIES, ServiceKind.JELLYFIN))
            HomeRow.EMBY_SERIES -> listOf(HomeRowKey(HomeRowKind.NEW_SERIES, ServiceKind.EMBY))
            HomeRow.RECOMMENDATIONS -> listOf(HomeRowKey(HomeRowKind.RECOMMENDATIONS))
            HomeRow.RECENT_RELEASES -> listOf(HomeRowKey(HomeRowKind.RECENT_RELEASES))
            HomeRow.UPCOMING -> listOf(HomeRowKey(HomeRowKind.UPCOMING))
        }
    }
}

/**
 * Which of one account's libraries feed each of its Home rows. A kind that is absent takes every
 * library, so a library added on the server later shows up without a visit to the settings.
 * This is separate from the libraries listed in the Library tab.
 */
data class HomeLibraryChoice(val included: Map<HomeRowKind, Set<String>> = emptyMap()) {
    fun includes(kind: HomeRowKind, libraryId: String): Boolean = included[kind]?.contains(libraryId) ?: true

    /** Null returns the row to "every library". */
    fun with(kind: HomeRowKind, libraryIds: Set<String>?): HomeLibraryChoice =
        copy(included = if (libraryIds == null) included - kind else included + (kind to libraryIds))

    fun encode(): String = JsonObject(included.entries.sortedBy { it.key.ordinal }.associate { (kind, ids) ->
        kind.name to JsonArray(ids.sorted().map(::JsonPrimitive))
    }).toString()

    /** Stable text for the media-cache fingerprint: a different choice is a different feed. */
    val fingerprint: String get() = encode()

    companion object {
        fun decode(saved: String?): HomeLibraryChoice? = runCatching {
            val root = Json.parseToJsonElement(saved ?: return null).jsonObject
            HomeLibraryChoice(root.mapNotNull { (name, ids) ->
                val kind = HomeRowKind.entries.firstOrNull { it.name == name && it.usesLibraries } ?: return@mapNotNull null
                kind to ids.jsonArray.map { it.jsonPrimitive.content }.toSet()
            }.toMap())
        }.getOrNull()

        /** Carries the older single library selection over to every row, as it applied before. */
        fun fromLibrarySelection(selected: Set<String>?): HomeLibraryChoice =
            if (selected == null) HomeLibraryChoice()
            else HomeLibraryChoice(HomeRowKind.entries.filter { it.usesLibraries }.associateWith { selected })
    }
}

/**
 * What one refresh should read from the media servers. Hidden rows that nothing else needs are
 * not requested at all.
 */
data class HomeFetchPlan(
    val layout: HomeLayout = HomeLayout.DEFAULT,
    val libraries: Map<ServiceKind, HomeLibraryChoice> = emptyMap(),
) {
    fun includes(source: ServiceKind, kind: HomeRowKind, libraryId: String): Boolean {
        val choice = libraries[source] ?: HomeLibraryChoice()
        if (kind == HomeRowKind.RECENT_RELEASES) {
            // Not a per-server row: it reads the libraries the reader's own rows read.
            return layout.isVisible(HomeRowKey(kind)) &&
                HomeRowKind.entries.filter { it.usesLibraries }.any { choice.includes(it, libraryId) }
        }
        if (kind.skippedWhenHidden && !layout.isVisible(HomeRowKey(kind, source.takeIf { kind.perSource }))) return false
        return !kind.usesLibraries || choice.includes(kind, libraryId)
    }
}
