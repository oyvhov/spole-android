package app.reelstack.player

import android.net.Uri
import androidx.media3.datasource.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Only this playback plan's text files, never video or credentials on disk. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class SubtitleMemoryCache(private val http: OkHttpClient, private val headers: Map<String, String>,
    private val onUnavailable: () -> Unit = {}) : AutoCloseable {
    private val files = linkedMapOf<String, ByteArray>()
    private val calls = ConcurrentHashMap.newKeySet<Call>()
    @Volatile private var closed = false

    // Never wait on load's monitor here: closing the player must interrupt the socket,
    // not wait eight seconds for subtitle extraction to time out while releasing video.
    override fun close() { closed = true; calls.forEach { it.cancel() } }

    /**
     * Not one lock around the download: the first extraction of a text track can take the server
     * tens of seconds, and a second track, or closing the player, must not queue behind it.
     */
    fun load(url: String): ByteArray {
        if (closed) throw IOException("Subtitle cache closed")
        synchronized(files) { files[url] }?.let { return it }
        val request = Request.Builder().url(url).apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        val call = http.newCall(request)
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
                    if (out.size() + count > 2 * 1024 * 1024) throw IOException("subtitle exceeds the size limit")
                    out.write(buffer, 0, count)
                }
                out.toByteArray()
            }
        } } finally { calls.remove(call) }
        synchronized(files) {
            if (files.size >= 4) files.remove(files.keys.first())
            files[url] = bytes
        }
        return bytes
    }

    fun factory(upstream: DataSource.Factory) = DataSource.Factory {
        object : DataSource {
            private var delegate: DataSource? = null
            private val listeners = mutableListOf<TransferListener>()
            override fun addTransferListener(listener: TransferListener) { listeners += listener; delegate?.addTransferListener(listener) }
            override fun open(dataSpec: DataSpec): Long {
                val path = dataSpec.uri.path.orEmpty()
                val source = if (path.contains("/Subtitles/") && path.endsWith("/Stream.vtt") && dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET) {
                    val bytes = try { load(dataSpec.uri.toString()) } catch (error: IOException) {
                        if (closed) throw error
                        // Text extraction is optional. Never retry the same failed text through
                        // the 20/45-second video client and turn it into a whole-film failure.
                        onUnavailable()
                        "WEBVTT\n\n".toByteArray(Charsets.UTF_8)
                    }
                    ByteArrayDataSource(bytes)
                } else upstream.createDataSource()
                delegate = source
                listeners.forEach(source::addTransferListener)
                return source.open(dataSpec)
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int) = checkNotNull(delegate).read(buffer, offset, length)
            override fun getUri(): Uri? = delegate?.uri
            override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders.orEmpty()
            override fun close() { delegate?.close(); delegate = null }
        }
    }
}
