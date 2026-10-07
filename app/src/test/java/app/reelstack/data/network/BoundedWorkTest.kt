package app.reelstack.data.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class BoundedWorkTest {
    @Test fun persistedProjectionSurvivesRestartButExpiresAndIsRevokedOnAuthFailure() {
        val rows = mutableMapOf<Pair<String, String>, CatalogueMetadataStore.Entry>()
        val store = object : CatalogueMetadataStore {
            override fun read(scope: String, path: String) = rows[scope to path]
            override fun write(entry: CatalogueMetadataStore.Entry) { rows[entry.scope to entry.path] = entry }
            override fun clear(scope: String) { rows.keys.removeAll { it.first == scope } }
        }
        ProjectedMetadataCache(store = store).apply {
            select("adult"); put("adult", "movie/1", 0, """{"id":1,"title":"Fixture","credits":"unused"}""")
        }
        val restored = ProjectedMetadataCache(store = store).apply { select("adult") }
        assertTrue(restored.get("adult", "movie/1", 1)!!.contains("Fixture"))
        assertNull(restored.get("child", "movie/1", 1))
        assertNull(restored.get("adult", "movie/1", 6 * 60 * 60_000L))
        restored.clear("adult")
        restored.select("adult")
        assertNull(restored.get("adult", "movie/1", 1))
        assertTrue(rows.isEmpty())
    }

    @Test fun endlessPaginationStopsAtThePageOrTimeBudget() {
        var now = 0L
        val pages = CataloguePageBudget(maxPages = 2, clockMillis = { now })
        repeat(2) { pages.next() }
        assertTrue(runCatching { pages.next() }.exceptionOrNull() is java.io.IOException)
        val time = CataloguePageBudget(clockMillis = { now })
        now = 120_000
        assertTrue(runCatching { time.next() }.exceptionOrNull() is java.io.IOException)
    }

    @Test fun largeCatalogueUsesFourWorkersAndPreservesOrder() = runBlocking {
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val results = boundedMap((1..10_000).toList()) { value ->
            val running = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, running) }
            try { value * 2 } finally { active.decrementAndGet() }
        }
        assertEquals((1..10_000).map { it * 2 }, results)
        assertTrue(peak.get() in 1..4)
    }

    @Test fun cacheDropsUnneededMetadataAndIsByteBounded() {
        val cache = ProjectedMetadataCache(maxBytes = 2_000)
        cache.select("one")
        repeat(100) { index ->
            cache.put("one", "$index", 0, """{"id":$index,"title":"Film","overview":"${"x".repeat(100)}","credits":"${"y".repeat(10_000)}"}""")
        }
        assertTrue(cache.bytes <= 2_000)
        assertNull(cache.get("one", "0", 1))
        assertFalse(cache.get("one", "99", 1)!!.contains("credits"))
        cache.select("two")
        cache.put("one", "late", 0, """{"id":1}""")
        assertNull(cache.get("two", "late", 1))
        assertEquals(0, cache.bytes)
    }
}
