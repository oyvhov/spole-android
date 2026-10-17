package androidx.media3.exoplayer.source;

import android.net.Uri;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.extractor.ExtractorsFactory;

/**
 * Narrow compatibility seam for pinned Media3 1.11.1. Its default source uses this same lazy
 * subtitle hook internally, but has no public API for a renderer that consumes original ASS.
 * Keeping it here avoids loading every subtitle or using the deprecated single-sample pipeline.
 */
@UnstableApi
public final class SpoleSubtitleSources {
  private SpoleSubtitleSources() {}
  public static MediaSource create(DataSource.Factory data, ExtractorsFactory extractors, Format format, Uri uri) {
    return new ProgressiveMediaSource.Factory(data, extractors)
        .enableLazyLoadingWithSingleTrack(0, format)
        .setLoadOnlySelectedTracks(true)
        .createMediaSource(MediaItem.fromUri(uri));
  }
}
