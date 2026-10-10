package app.reelstack.data.network

import java.io.File
import java.security.MessageDigest

/**
 * The last answer a server gave to a smart shelf's question, kept on the device.
 *
 * A shelf asks a whole catalogue, which can take several seconds on a large server. Without a copy,
 * every visit started from an empty tile, an empty Home row and an empty page, and the posters arrived
 * one by one. With one, the shelf is drawn at once from what it held last time and the server's fresh
 * answer replaces it a moment later.
 */
interface ShelfAnswerCache {
    fun read(key: String): String?
    fun write(key: String, body: String)
    fun clear()
}

/**
 * [ShelfAnswerCache] as files in the app's cache folder: one per question, named by a hash of the
 * server, the profile and the question, so neither shows in a file name. Android may empty the folder
 * when space runs short, which costs nothing but a slower first draw.
 */
class FileShelfAnswerCache(
    private val directory: File,
    private val maxAgeMillis: Long = 14L * 24 * 60 * 60_000,
    private val maxFiles: Int = 240,
) : ShelfAnswerCache {
    override fun read(key: String): String? = runCatching {
        val file = fileFor(key)
        if (!file.isFile || System.currentTimeMillis() - file.lastModified() > maxAgeMillis) return null
        file.readText()
    }.getOrNull()

    override fun write(key: String, body: String) {
        runCatching {
            directory.mkdirs()
            val target = fileFor(key)
            val temporary = File(directory, target.name + ".tmp")
            temporary.writeText(body)
            if (!temporary.renameTo(target)) { target.delete(); temporary.renameTo(target) }
            trim()
        }
    }

    override fun clear() {
        runCatching { directory.listFiles()?.forEach(File::delete) }
    }

    private fun trim() {
        val files = directory.listFiles { file -> file.isFile && !file.name.endsWith(".tmp") } ?: return
        if (files.size <= maxFiles) return
        files.sortedBy(File::lastModified).take(files.size - maxFiles).forEach(File::delete)
    }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return File(directory, digest.joinToString("") { "%02x".format(it) } + ".json")
    }
}
