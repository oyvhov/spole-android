package app.reelstack.player

import android.net.Uri
import androidx.media3.datasource.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import java.io.IOException
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Text stays in memory; graphical tracks use private temporary files without URLs or credentials. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class SubtitleMemoryCache(private val http: OkHttpClient, private val headers: Map<String, String>,
    private val bitmapDirectory: File? = null,
    private val onUnavailable: () -> Unit = {}) : AutoCloseable {
    private val files = linkedMapOf<String, ByteArray>()
    private val bitmapFiles = linkedMapOf<String, File>()
    private val ownedBitmapFiles = ConcurrentHashMap.newKeySet<File>()
    private val calls = ConcurrentHashMap.newKeySet<Call>()
    @Volatile private var closed = false

    fun contains(url: String): Boolean = !closed && synchronized(files) {
        url in files || bitmapFiles[url]?.isFile == true
    }

    // Never wait on load's monitor here: closing the player must interrupt the socket,
    // not wait eight seconds for subtitle extraction to time out while releasing video.
    override fun close() {
        closed = true; calls.forEach { it.cancel() }
        synchronized(activeBitmapFiles) { ownedBitmapFiles.toList().forEach(::discardBitmap) }
        synchronized(files) { files.clear(); bitmapFiles.clear() }
    }

    /** Graphical tracks can be much larger than text. Copy with a small buffer, then let Media3
     * seek the private file; the video never waits for a slow server-side subtitle extraction. */
    fun loadBitmap(url: String, maximumBytes: Long = MAX_BITMAP_BYTES) {
        if (closed) throw IOException("Subtitle cache closed")
        synchronized(files) { bitmapFiles[url]?.takeIf(File::isFile) }?.let { return }
        val directory = bitmapDirectory ?: throw IOException("Bitmap subtitle cache is unavailable")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create subtitle cache")
        val call = http.newCall(Request.Builder().url(url).apply {
            headers.forEach { (key, value) -> header(key, value) }
        }.build())
        val (target, budget) = synchronized(activeBitmapFiles) {
            if (closed) throw IOException("Subtitle cache closed")
            // Reclaim only our inactive temporary files, including leftovers after process death.
            directory.listFiles()?.filter { it.name.startsWith("spole-bitmap-") && it.name.endsWith(".bin") &&
                it !in activeBitmapFiles }?.forEach { it.delete() }
            // Reclaim crash leftovers before judging the available budget: they may be the
            // very reason the disk is full. Active players' files are never reclaimed here.
            val budget = minOf(maximumBytes, MAX_BITMAP_BYTES, directory.usableSpace - 64L * 1024 * 1024)
            if (budget <= 0) throw IOException("Not enough space for subtitle cache")
            File.createTempFile("spole-bitmap-", ".bin", directory).also {
                activeBitmapFiles += it; ownedBitmapFiles += it
            } to budget
        }
        calls.add(call)
        if (closed) call.cancel()
        var retained = false
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("Bitmap subtitle fetch failed")
                val body = response.body ?: throw IOException("Empty bitmap subtitle")
                if (body.contentLength() > budget) throw IOException("Bitmap subtitle exceeds disk budget")
                body.byteStream().use { input -> target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (closed || total > budget) throw IOException("Bitmap subtitle cache interrupted or full")
                        output.write(buffer, 0, count)
                    }
                } }
            }
            synchronized(files) {
                if (closed) throw IOException("Subtitle cache closed")
                bitmapFiles.put(url, target)?.let(::discardBitmap)
                while (bitmapFiles.size > 2 || bitmapFiles.values.sumOf { it.length() } > MAX_BITMAP_BYTES) {
                    discardBitmap(bitmapFiles.remove(bitmapFiles.keys.first())!!)
                }
                retained = true
            }
        } finally {
            calls.remove(call)
            if (!retained) discardBitmap(target)
        }
    }

    private fun discardBitmap(file: File) = synchronized(activeBitmapFiles) {
        file.delete(); activeBitmapFiles.remove(file); ownedBitmapFiles.remove(file); Unit
    }

    /**
     * Not one lock around the download: the first extraction of a text track can take the server
     * tens of seconds, and a second track, or closing the player, must not queue behind it.
     */
    fun load(url: String, maximumBytes: Int = 2 * 1024 * 1024, cacheResult: Boolean = true, timeoutMs: Long? = null): ByteArray {
        if (closed) throw IOException("Subtitle cache closed")
        synchronized(files) { files[url] }?.let { return it }
        val request = Request.Builder().url(url).apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        val call = http.newCall(request)
        timeoutMs?.let { call.timeout().timeout(it.coerceAtLeast(1), java.util.concurrent.TimeUnit.MILLISECONDS) }
        calls.add(call)
        if (closed) call.cancel()
        val bytes = try { call.execute().use { response ->
            if (!response.isSuccessful) throw IOException("subtitle fetch failed")
            val body = response.body ?: throw IOException("Tom undertekst")
            body.byteStream().use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (out.size() + count > maximumBytes) throw IOException("subtitle exceeds the size limit")
                    out.write(buffer, 0, count)
                }
                out.toByteArray()
            }
        } } finally { calls.remove(call) }
        if (cacheResult) synchronized(files) {
            if (closed) throw IOException("Subtitle cache closed")
            while (files.isNotEmpty() && (files.size >= 4 || files.values.sumOf { it.size } + bytes.size > 24 * 1024 * 1024)) files.remove(files.keys.first())
            files[url] = bytes
        }
        return bytes
    }

    fun factory(upstream: DataSource.Factory) = DataSource.Factory {
        object : DataSource {
            private var delegate: DataSource? = null
            private var requestedUri: Uri? = null
            private val listeners = mutableListOf<TransferListener>()
            override fun addTransferListener(listener: TransferListener) { listeners += listener; delegate?.addTransferListener(listener) }
            override fun open(dataSpec: DataSpec): Long {
                val path = dataSpec.uri.path.orEmpty()
                val cached = synchronized(files) { files[dataSpec.uri.toString()] }
                val bitmap = synchronized(files) { bitmapFiles[dataSpec.uri.toString()]?.takeIf(File::isFile) }
                val source = if (cached != null) ByteArrayDataSource(cached)
                else if (bitmap != null) FileDataSource()
                else if (path.contains("/Subtitles/") && (path.endsWith("/Stream.vtt") || path.endsWith("/Stream.ass")) && dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET) {
                    val bytes = try { load(dataSpec.uri.toString()) } catch (error: IOException) {
                        if (closed) throw error
                        // Text extraction is optional. Never retry the same failed text through
                        // the 20/45-second video client and turn it into a whole-film failure.
                        onUnavailable()
                        (if (path.endsWith(".ass")) "[Script Info]\nScriptType: v4.00+\n[Events]\n" else "WEBVTT\n\n").toByteArray(Charsets.UTF_8)
                    }
                    ByteArrayDataSource(bytes)
                } else upstream.createDataSource()
                delegate = source
                listeners.forEach(source::addTransferListener)
                requestedUri = dataSpec.uri.takeIf { bitmap != null }
                return source.open(if (bitmap != null) dataSpec.withUri(Uri.fromFile(bitmap)) else dataSpec)
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int) = checkNotNull(delegate).read(buffer, offset, length)
            override fun getUri(): Uri? = requestedUri ?: delegate?.uri
            override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders.orEmpty()
            override fun close() { delegate?.close(); delegate = null; requestedUri = null }
        }
    }

    companion object {
        private const val MAX_BITMAP_BYTES = 256L * 1024 * 1024
        private val activeBitmapFiles = mutableSetOf<File>()
    }
}
