package app.reelstack.player

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * androidx/media3/exoplayer/source/SpoleSubtitleSources.java calls a package-private Media3 method
 * (enableLazyLoadingWithSingleTrack). A Media3 upgrade can change it without a compile error.
 */
class Media3VersionGuardTest {
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun media3UpgradesReviewTheVendoredSubtitleSource() {
        assertEquals("Re-check SpoleSubtitleSources and run NativeAssPlaybackTest/BitmapSidecarPlaybackTest on 5562 and 5566, then update this version",
            "1.11.1", androidx.media3.common.MediaLibraryInfo.VERSION)
    }
}
