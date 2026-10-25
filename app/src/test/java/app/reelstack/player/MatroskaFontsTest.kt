package app.reelstack.player

import org.junit.Assert.*
import org.junit.Test

class MatroskaFontsTest {
    private fun element(id: String, data: ByteArray): ByteArray {
        require(data.size < 127)
        return id.chunked(2).map { it.toInt(16).toByte() }.toByteArray() + byteArrayOf((0x80 or data.size).toByte()) + data
    }
    private fun file(name: String, bytes: ByteArray) = element("61A7", element("466E", name.toByteArray()) + element("465C", bytes))
    private fun document(body: ByteArray) = element("1A45DFA3", byteArrayOf()) + element("18538067", body)
    private fun fonts(bytes: ByteArray) = readMatroskaFonts { start, count ->
        if (start !in 0..bytes.size.toLong()) byteArrayOf() else bytes.copyOfRange(start.toInt(), minOf(bytes.size.toLong(), start + count).toInt())
    }

    @Test fun attachmentReaderLoadsOnlyNamedFontFiles() {
        val data = document(element("1941A469", file("Example.ttf", byteArrayOf(1, 2, 3)) + file("cover.jpg", byteArrayOf(4))))
        val result = fonts(data)
        assertEquals(listOf("Example.ttf"), result.map { it.name })
        assertArrayEquals(byteArrayOf(1, 2, 3), result.single().bytes)
    }

    @Test fun seekHeadFindsAttachmentsWithoutReadingVideoClusters() {
        val attachmentOffset = 100_000L
        val seek = element("114D9B74", element("4DBB", element("53AB", byteArrayOf(0x19, 0x41, 0xA4.toByte(), 0x69)) +
            element("53AC", byteArrayOf(1, 0x86.toByte(), 0xA0.toByte()))))
        val header = document(seek + element("1F43B675", byteArrayOf()))
        val block = element("1941A469", file("Example.otf", byteArrayOf(1)))
        val reads = mutableListOf<Pair<Long, Int>>()
        val result = readMatroskaFonts { start, count ->
            reads += start to count
            when (start) { 0L -> header; attachmentOffset + 10 -> block.take(count).toByteArray(); else -> error("Unexpected scan") }
        }
        assertEquals("Example.otf", result.single().name)
        assertEquals(3, reads.size)
        assertTrue(reads.all { it.second <= 256 * 1024 })
    }

    @Test fun truncatedAndNonMatroskaInputsFailClosed() {
        val data = document(element("1941A469", file("Example.ttf", byteArrayOf(1, 2, 3))))
        assertTrue(fonts(data.dropLast(1).toByteArray()).isEmpty())
        assertTrue(fonts("not an EBML document".toByteArray()).isEmpty())
        assertTrue(fonts(byteArrayOf()).isEmpty())
    }
}
