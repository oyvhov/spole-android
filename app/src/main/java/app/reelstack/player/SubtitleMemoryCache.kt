package app.reelstack.player

import android.net.Uri
import androidx.media3.datasource.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Only this playback plan's text files, never video or credentials on disk. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class SubtitleMemoryCache(private val http: OkHttpClient, private val headers: Map<String, String>) {
    private val files = linkedMapOf<String, ByteArray>()

    @Synchronized fun load(url: String): ByteArray {
        files[url]?.let { return it }
        val request = Request.Builder().url(url).apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        val bytes = http.newCall(request).execute().use { response ->
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
        }
        if (files.size >= 4) files.remove(files.keys.first())
        files[url] = bytes
        return bytes
    }

    fun factory(upstream: DataSource.Factory) = DataSource.Factory {
        object : DataSource {
            private var delegate: DataSource? = null
            private val listeners = mutableListOf<TransferListener>()
            override fun addTransferListener(listener: TransferListener) { listeners += listener; delegate?.addTransferListener(listener) }
            override fun open(dataSpec: DataSpec): Long {
                val path = dataSpec.uri.path.orEmpty()
                val source = if (path.contains("/Subtitles/") && path.endsWith("/Stream.vtt") && dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET)
                    runCatching { ByteArrayDataSource(load(dataSpec.uri.toString())) }.getOrNull() ?: upstream.createDataSource()
                else upstream.createDataSource()
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
