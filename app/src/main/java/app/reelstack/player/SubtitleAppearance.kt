package app.reelstack.player

import android.graphics.Color
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import app.reelstack.data.model.SubtitleStyle

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun SubtitleView.applyAppearance(style: SubtitleStyle) {
    setApplyEmbeddedStyles(false)
    setApplyEmbeddedFontSizes(false)
    val background = when (style) {
        SubtitleStyle.CINEMA -> 0x99000000.toInt()
        SubtitleStyle.HIGH_CONTRAST -> Color.BLACK
        SubtitleStyle.LARGE -> 0xB8000000.toInt()
        else -> Color.TRANSPARENT
    }
    setStyle(CaptionStyleCompat(Color.WHITE, background, Color.TRANSPARENT,
        if (style == SubtitleStyle.CLEAN) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
        Color.BLACK, android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)))
    setFractionalTextSize(if (style == SubtitleStyle.LARGE) .067f else .0533f)
    setBottomPaddingFraction(.08f)
}
