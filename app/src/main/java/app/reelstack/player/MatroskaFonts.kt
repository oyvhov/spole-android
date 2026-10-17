@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

/** Reads only headers and the indexed attachment block; never scans through video clusters. */
internal fun matroskaFonts(factory: DataSource.Factory, uri: Uri, cacheKey: String? = null): List<SubtitleFont> = runCatching {
    readMatroskaFonts { position, length ->
        val source = factory.createDataSource()
        try {
            source.open(DataSpec.Builder().setUri(uri).setPosition(position).setLength(length.toLong()).setKey(cacheKey).build())
            val result = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (result.size() < length) {
                val count = source.read(buffer, 0, minOf(buffer.size, length - result.size()))
                if (count < 0) break
                result.write(buffer, 0, count)
            }
            result.toByteArray()
        } finally { source.close() }
    }
}.getOrDefault(emptyList())

internal fun readMatroskaFonts(read: (Long, Int) -> ByteArray): List<SubtitleFont> {
    val header = read(0, 256 * 1024)
    val first = ebmlElement(header, 0) ?: return emptyList()
    if (first.id != 0x1A45DFA3L) return emptyList()
    val segment = ebmlElement(header, first.end) ?: return emptyList()
    if (segment.id != 0x18538067L) return emptyList()
    var offset = segment.data
    var attachments: Long? = null
    while (offset < header.size) {
        val entry = ebmlElement(header, offset) ?: break
        if (entry.id == 0x1941A469L) { attachments = offset.toLong(); break }
        if (entry.id == 0x114D9B74L) {
            ebmlChildren(header, entry).filter { it.id == 0x4DBBL }.forEach { seek ->
                val children = ebmlChildren(header, seek)
                val id = children.firstOrNull { it.id == 0x53ABL }?.unsigned(header)
                val position = children.firstOrNull { it.id == 0x53ACL }?.unsigned(header)
                if (id == 0x1941A469L && position != null && position in 0..Long.MAX_VALUE - segment.data) attachments = position + segment.data
            }
        }
        if (entry.id == 0x1F43B675L || entry.end <= offset) break
        offset = entry.end
    }
    val location = attachments ?: return emptyList()
    val blockHeader = read(location, 16)
    val block = ebmlElement(blockHeader, 0) ?: return emptyList()
    if (block.id != 0x1941A469L || block.end !in 1..MAX_FONT_BYTES) return emptyList()
    val bytes = read(location, block.end)
    if (bytes.size < block.end) return emptyList()
    return ebmlChildren(bytes, block).asSequence().filter { it.id == 0x61A7L }.take(24).mapNotNull { file ->
        val fields = ebmlChildren(bytes, file)
        val name = fields.firstOrNull { it.id == 0x466EL }?.let { bytes.copyOfRange(it.data, it.end).toString(Charsets.UTF_8).trimEnd('\u0000') } ?: return@mapNotNull null
        if (name.length > 240 || listOf(".ttf", ".otf", ".ttc").none { name.endsWith(it, true) }) return@mapNotNull null
        val data = fields.firstOrNull { it.id == 0x465CL && it.end - it.data in 1..2 * 1024 * 1024 } ?: return@mapNotNull null
        SubtitleFont(name, bytes.copyOfRange(data.data, data.end))
    }.toList()
}

private const val MAX_FONT_BYTES = 8 * 1024 * 1024
private data class EbmlElement(val id: Long, val data: Int, val end: Int) {
    fun unsigned(bytes: ByteArray): Long? {
        if (end - data !in 1..8 || end > bytes.size) return null
        var value = 0L
        for (index in data until end) {
            if (value > Long.MAX_VALUE ushr 8) return null
            value = (value shl 8) or (bytes[index].toLong() and 255)
        }
        return value
    }
}
private fun ebmlElement(bytes: ByteArray, offset: Int): EbmlElement? {
    fun vint(at: Int, retainMarker: Boolean): Pair<Long, Int>? {
        if (at !in bytes.indices) return null
        val first = bytes[at].toInt() and 255
        if (first == 0) return null
        val count = Integer.numberOfLeadingZeros(first) - 23
        if (count !in 1..8 || retainMarker && count > 4 || at + count > bytes.size) return null
        var value = (if (retainMarker) first else first and (255 ushr count)).toLong()
        for (i in 1 until count) value = (value shl 8) or (bytes[at + i].toLong() and 255)
        return value to count
    }
    val (id, idBytes) = vint(offset, true) ?: return null
    val (length, sizeBytes) = vint(offset + idBytes, false) ?: return null
    val start = offset.toLong() + idBytes + sizeBytes
    val end = (start + length).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return EbmlElement(id, start.toInt(), end)
}
private fun ebmlChildren(bytes: ByteArray, parent: EbmlElement): List<EbmlElement> = buildList {
    var offset = parent.data
    val end = minOf(parent.end, bytes.size)
    while (offset < end && size < 1024) {
        val element = ebmlElement(bytes, offset) ?: break
        if (element.end > end || element.end <= offset) break
        add(element); offset = element.end
    }
}
