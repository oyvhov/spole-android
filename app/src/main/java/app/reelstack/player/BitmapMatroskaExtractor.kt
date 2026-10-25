@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.extractor.*
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory

/** Keep the server's single-track identity across lazy loading and extraction-time cue parsing. */
internal class BitmapMatroskaExtractor(private val advertised: Format) : Extractor {
    private val delegate = MatroskaExtractor(DefaultSubtitleParserFactory())
    override fun sniff(input: ExtractorInput) = delegate.sniff(input)
    override fun read(input: ExtractorInput, seekPosition: PositionHolder) = delegate.read(input, seekPosition)
    override fun seek(position: Long, timeUs: Long) = delegate.seek(position, timeUs)
    override fun release() = delegate.release()

    override fun init(output: ExtractorOutput) {
        delegate.init(object : ExtractorOutput {
            private var textId: Int? = null
            override fun track(id: Int, type: Int): TrackOutput {
                if (type != C.TRACK_TYPE_TEXT || textId?.let { it != id } == true) {
                    throw SubtitleSourceException("Expected a single subtitle track in MKS", malformed = false)
                }
                textId = id
                return object : ForwardingTrackOutput(output.track(0, C.TRACK_TYPE_TEXT)) {
                    override fun format(format: Format) {
                        if (format.sampleMimeType != MimeTypes.APPLICATION_MEDIA3_CUES || format.codecs != advertised.codecs) {
                            throw SubtitleSourceException("MKS codec differs from advertised subtitle", malformed = false)
                        }
                        super.format(format.buildUpon().setId(advertised.id)
                            .setLanguage(advertised.language ?: format.language).setLabel(advertised.label ?: format.label)
                            .setSelectionFlags(advertised.selectionFlags).build())
                    }
                }
            }
            override fun endTracks() = output.endTracks()
            override fun seekMap(seekMap: SeekMap) = output.seekMap(seekMap)
        })
    }
}
