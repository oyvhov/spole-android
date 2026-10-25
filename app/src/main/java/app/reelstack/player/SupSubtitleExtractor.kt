@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.*
import androidx.media3.extractor.text.CueEncoder
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.pgs.PgsParser
import java.io.ByteArrayOutputStream

internal const val SPOLE_SUP_MIME = "application/x-spole-sup"

/** Incremental SUP display sets. Only a compressed display set and Media3's current cues are live. */
internal class SupSubtitleExtractor(private val format: Format) : Extractor, SeekMap {
    private lateinit var track: TrackOutput
    private val parser = PgsParser()
    private val encoder = CueEncoder()
    private val epochs = java.util.TreeMap<Long, Long>()
    override fun sniff(input: ExtractorInput): Boolean {
        val header = ByteArray(2)
        return input.peekFully(header, 0, 2, true) && header[0] == 80.toByte() && header[1] == 71.toByte()
    }
    override fun init(output: ExtractorOutput) {
        track = output.track(0, C.TRACK_TYPE_TEXT)
        track.format(format.buildUpon().setSampleMimeType(MimeTypes.APPLICATION_MEDIA3_CUES)
            .setCodecs(MimeTypes.APPLICATION_PGS).setCueReplacementBehavior(Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE).build())
        output.endTracks(); output.seekMap(this)
    }
    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        val start = input.position
        val display = ByteArrayOutputStream()
        val header = ByteArray(13)
        var pts = 0L
        var epoch = false
        while (true) {
            if (!input.readFully(header, 0, 13, true)) return Extractor.RESULT_END_OF_INPUT
            if (header[0] != 80.toByte() || header[1] != 71.toByte()) throw SubtitleSourceException("Invalid SUP segment")
            pts = (2..5).fold(0L) { value, i -> (value shl 8) or (header[i].toLong() and 255) } * 1_000_000 / 90_000
            val kind = header[10].toInt() and 255
            val length = ((header[11].toInt() and 255) shl 8) or (header[12].toInt() and 255)
            if (display.size() + length + 3 > 2 * 1024 * 1024) throw SubtitleSourceException("SUP display set exceeds limit")
            val data = ByteArray(length); input.readFully(data, 0, length)
            if (kind == 0x16 && length >= 8 && data[7].toInt() and 0x80 != 0) epoch = true
            display.write(kind); display.write(header[11].toInt()); display.write(header[12].toInt()); display.write(data)
            if (kind == 0x80) break
        }
        if (epochs.size < 20_000 && (epoch || epochs.isEmpty())) epochs[pts] = start
        val bytes = display.toByteArray()
        parser.parse(bytes, 0, bytes.size, SubtitleParser.OutputOptions.allCues()) { cues ->
            val encoded = encoder.encode(cues.cues, C.TIME_UNSET)
            track.sampleData(ParsableByteArray(encoded), encoded.size)
            track.sampleMetadata(pts, C.BUFFER_FLAG_KEY_FRAME, encoded.size, 0, null)
        }
        return Extractor.RESULT_CONTINUE
    }
    override fun seek(position: Long, timeUs: Long) { parser.reset() }
    override fun release() = Unit
    override fun isSeekable() = true
    override fun getDurationUs() = C.TIME_UNSET
    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        val entry = epochs.floorEntry(timeUs) ?: epochs.firstEntry()
        return SeekMap.SeekPoints(if (entry == null) SeekPoint.START else SeekPoint(entry.key, entry.value))
    }
}
