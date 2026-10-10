package app.reelstack.data.network

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfAnswerCacheTest {
    private val folder = Files.createTempDirectory("shelf-answers").toFile()

    @Test fun anAnswerComesBackUntilItIsTooOld() {
        val cache = FileShelfAnswerCache(folder, maxAgeMillis = 60_000)
        assertNull(cache.read("https://media.example/Items?a"))
        cache.write("https://media.example/Items?a", "{\"Items\":[]}")
        assertEquals("{\"Items\":[]}", cache.read("https://media.example/Items?a"))
        // No server address or question shows in a file name.
        assertTrue(folder.listFiles()!!.none { "media" in it.name || "Items" in it.name })
        folder.listFiles()!!.forEach { it.setLastModified(System.currentTimeMillis() - 120_000) }
        assertNull(cache.read("https://media.example/Items?a"))
    }

    @Test fun theFolderKeepsOnlyTheNewestAnswers() {
        val cache = FileShelfAnswerCache(folder, maxFiles = 3)
        repeat(5) { index ->
            cache.write("key-$index", "body-$index")
            folder.listFiles()!!.forEach { it.setLastModified(it.lastModified() - 1_000) }
        }
        assertEquals(3, folder.listFiles()!!.size)
        assertEquals("body-4", cache.read("key-4"))
        cache.clear()
        assertNull(cache.read("key-4"))
    }
}
