package app.reelstack.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.serialization.json.*

/** A semaphore bounds requests, but still creates one suspended job per catalogue item. */
internal suspend fun <T, R> boundedMap(items: List<T>, workers: Int = 4, transform: suspend (T) -> R): List<R> = coroutineScope {
    if (items.isEmpty()) return@coroutineScope emptyList()
    val queue = Channel<IndexedValue<T>>(16)
    val results = MutableList<R?>(items.size) { null }
    val consumers = List(workers.coerceIn(1, 4).coerceAtMost(items.size)) {
        launch(Dispatchers.IO) {
            for ((index, item) in queue) results[index] = transform(item)
        }
    }
    try {
        items.forEachIndexed { index, item -> queue.send(IndexedValue(index, item)) }
    } finally { queue.close() }
    consumers.joinAll()
    @Suppress("UNCHECKED_CAST")
    results.map { it as R }
}

/** Only calendar/release fields are retained; cast, images, reviews and other large trees are discarded. */
internal class ProjectedMetadataCache(private val maxBytes: Long = 8L * 1024 * 1024,
    private val store: CatalogueMetadataStore? = null) {
    private data class Entry(val at: Long, val payload: String) { val weight: Long get() = payload.length * 2L + 256 }
    private val entries = LinkedHashMap<String, Entry>(16, .75f, true)
    private var scope = ""
    var bytes: Long = 0
        private set

    @Synchronized fun select(owner: String) {
        if (scope != owner) { entries.clear(); bytes = 0; scope = owner }
    }
    @Synchronized fun clear(owner: String) {
        if (scope == owner) { entries.clear(); bytes = 0; scope = "" }
        runCatching { store?.clear(owner) }
    }
    @Synchronized fun get(owner: String, path: String, now: Long): String? {
        if (owner != scope) return null
        entries[path]?.takeIf { now - it.at in 0 until 6 * 60 * 60_000 }?.let { return it.payload }
        val persisted = runCatching { store?.read(owner, path) }.getOrNull()
            ?.takeIf { now - it.at in 0 until 6 * 60 * 60_000 } ?: return null
        retain(path, Entry(persisted.at, persisted.payload))
        return persisted.payload
    }
    @Synchronized fun put(owner: String, path: String, now: Long, payload: String): String {
        val compact = project(Json.parseToJsonElement(payload).jsonObject).toString()
        if (owner != scope) return compact
        val entry = Entry(now, compact)
        retain(path, entry)
        runCatching { store?.write(CatalogueMetadataStore.Entry(owner, path, now, compact)) }
        return compact
    }
    private fun retain(path: String, entry: Entry) {
        entries.remove(path)?.let { bytes -= it.weight }
        if (entry.weight <= maxBytes) { entries[path] = entry; bytes += entry.weight }
        while (bytes > maxBytes) {
            val first = entries.entries.iterator()
            val old = first.next().value
            bytes -= old.weight
            first.remove()
        }
    }

    private fun project(root: JsonObject): JsonObject = JsonObject(root.filterKeys { it in FIELDS }.mapValues { (key, value) ->
        when (key) {
            "seasons", "episodes" -> JsonArray((value as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map(::projectEpisode))
            "nextEpisodeToAir", "lastEpisodeToAir" -> (value as? JsonObject)?.let(::projectEpisode) ?: JsonNull
            "mediaInfo" -> JsonObject((value as? JsonObject).orEmpty().filterKeys { it == "status" })
            "releases" -> JsonObject(mapOf("results" to JsonArray(((value as? JsonObject)?.get("results") as? JsonArray).orEmpty()
                .mapNotNull { it as? JsonObject }.map { region -> JsonObject(region.filterKeys { it in setOf("iso_3166_1", "release_dates") }
                    .mapValues { (field, data) -> if (field == "release_dates") JsonArray((data as? JsonArray).orEmpty()
                        .mapNotNull { it as? JsonObject }.map { date -> JsonObject(date.filterKeys { it in setOf("type", "release_date") }) }) else data }) })))
            else -> value
        }
    })
    private fun projectEpisode(root: JsonObject) = JsonObject(root.filterKeys {
        it in setOf("seasonNumber", "episodeNumber", "airDate", "name", "overview", "stillPath")
    })
    private companion object {
        val FIELDS = setOf("id", "title", "name", "releaseDate", "posterPath", "backdropPath", "overview", "genres",
            "runtime", "voteAverage", "releases", "mediaInfo", "seasons", "episodes", "nextEpisodeToAir", "lastEpisodeToAir")
    }
}

/** A broken or very large server must produce an incomplete row rather than an endless refresh. */
internal class CataloguePageBudget(
    private val maxPages: Int = 200,
    private val maxMillis: Long = 120_000,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val started = clockMillis()
    private var pages = 0
    fun next() {
        if (Thread.currentThread().isInterrupted) throw kotlinx.coroutines.CancellationException()
        if (pages >= maxPages || clockMillis() - started >= maxMillis) {
            throw java.io.IOException("Catalogue refresh exceeded its page or time budget")
        }
        pages++
    }
}
