@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.*
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import java.io.ByteArrayOutputStream

/** Keep modern extraction-time parsing for standard subtitles; libass consumes original ASS. */
internal class PlaybackSubtitleParserFactory : SubtitleParser.Factory {
    private val standard = DefaultSubtitleParserFactory()
    override fun supportsFormat(format: Format) = format.sampleMimeType != MimeTypes.TEXT_SSA && standard.supportsFormat(format)
    override fun getCueReplacementBehavior(format: Format) = standard.getCueReplacementBehavior(format)
    override fun create(format: Format): SubtitleParser = standard.create(format)
}

internal fun playbackMediaSource(factory: DataSource.Factory, item: MediaItem): MediaSource {
    val sidecars = item.localConfiguration?.subtitleConfigurations.orEmpty()
    val native = sidecars.filter { it.mimeType in setOf(MimeTypes.TEXT_SSA, SPOLE_SUP_MIME, SPOLE_MKS_DVD_MIME, SPOLE_MKS_PGS_MIME) }
    val primary = DefaultMediaSourceFactory(factory).setSubtitleParserFactory(PlaybackSubtitleParserFactory())
        .setLoadOnlySelectedTracks(true).createMediaSource(item.buildUpon().setSubtitleConfigurations(sidecars - native.toSet()).build())
    if (native.isEmpty()) return primary
    val documents = native.map { config ->
        val bitmapCodec = when (config.mimeType) {
            SPOLE_SUP_MIME, SPOLE_MKS_PGS_MIME -> MimeTypes.APPLICATION_PGS
            SPOLE_MKS_DVD_MIME -> MimeTypes.APPLICATION_VOBSUB
            else -> null
        }
        val format = Format.Builder().setId(config.id).setSampleMimeType(if (bitmapCodec != null) MimeTypes.APPLICATION_MEDIA3_CUES else MimeTypes.TEXT_SSA)
            .setCodecs(bitmapCodec)
            .setCueReplacementBehavior(Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE)
            .setLanguage(config.language).setLabel(config.label).setSelectionFlags(config.selectionFlags).build()
        androidx.media3.exoplayer.source.SpoleSubtitleSources.create(factory,
            ExtractorsFactory { arrayOf(when (config.mimeType) {
                SPOLE_SUP_MIME -> SupSubtitleExtractor(format)
                SPOLE_MKS_DVD_MIME, SPOLE_MKS_PGS_MIME -> BitmapMatroskaExtractor(format)
                else -> SubtitleDocumentExtractor(format)
            }) }, format, config.uri)
    }
    return MergingMediaSource(primary, *documents.toTypedArray())
}

/** A bounded original ASS document is one sample; seeks replay the document against a fresh clock. */
private class SubtitleDocumentExtractor(private val format: Format) : Extractor {
    private lateinit var track: TrackOutput
    private var sent = false
    private var sampleTimeUs = 0L
    override fun sniff(input: ExtractorInput) = true
    override fun init(output: ExtractorOutput) {
        track = output.track(0, C.TRACK_TYPE_TEXT)
        track.format(format); output.endTracks()
        output.seekMap(object : SeekMap {
            override fun isSeekable() = true
            override fun getDurationUs() = C.TIME_UNSET
            override fun getSeekPoints(timeUs: Long) = SeekMap.SeekPoints(SeekPoint(timeUs, 0))
        })
    }
    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        if (sent) return Extractor.RESULT_END_OF_INPUT
        val bytes = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val count = input.read(chunk, 0, chunk.size)
            if (count < 0) break
            if (bytes.size() + count > 2 * 1024 * 1024) throw SubtitleSourceException("Subtitle document exceeds limit")
            bytes.write(chunk, 0, count)
        }
        val data = bytes.toByteArray()
        track.sampleData(ParsableByteArray(data), data.size)
        track.sampleMetadata(sampleTimeUs, C.BUFFER_FLAG_KEY_FRAME, data.size, 0, null)
        sent = true
        return Extractor.RESULT_END_OF_INPUT
    }
    override fun seek(position: Long, timeUs: Long) { sent = false; sampleTimeUs = timeUs }
    override fun release() = Unit
}
