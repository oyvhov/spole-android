package app.reelstack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.ui.theme.LocalMotionEnabled

/** Open list rows at rest; a soft wash and outline locate selection and remote focus. */
@Composable
internal fun Modifier.settingsSurface(
    interaction: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(16.dp),
    selected: Boolean = false,
): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        when {
            focused -> colors.onSurface.copy(alpha = .10f)
            pressed -> colors.onSurface.copy(alpha = .08f)
            selected -> colors.onSurface.copy(alpha = .045f)
            else -> Color.Transparent
        },
        tween(if (motion) 140 else 0), label = "settings-surface")
    val scale by animateFloatAsState(if (pressed && motion) .988f else 1f,
        tween(if (motion) 100 else 0), label = "settings-press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape).background(background).focusOutline(interaction, shape, glow = false)
}

@Composable
internal fun SettingsIconBadge(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 40.dp,
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * .65f), tint = tint)
    }
}

internal fun hasSettingsPreview(value: Any?): Boolean = value is VisualTheme || value is AccentPalette ||
    value is ArtworkCorners || value is ArtworkSize || value is FocusStyle || value is Season ||
    value is app.reelstack.data.model.SeasonChoice || value is app.reelstack.data.model.SeasonalDecor ||
    value is LibraryArtType || value is LibraryView || value is LibraryCardSize

/** Small, literal previews of the actual option, not additional controls or spoken content. */
@Composable
internal fun SettingsOptionPreview(value: Any?) {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.size(64.dp, 44.dp).clearAndSetSemantics { }) {
        val unit = size.width / 64f
        fun card(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Color) =
            drawRoundRect(color, Offset(x * unit, y * unit), Size(w * unit, h * unit),
                CornerRadius(radius * unit))
        fun seasonSample(season: Season, x: Float = 0f, width: Float = 64f) {
            val palette = season.applyTo(Personalization())
            val background = Color(palette.visualTheme.background)
            val raised = Color(palette.visualTheme.raised)
            val accent = Color(palette.accent.argb)
            card(x, 0f, width, 44f, 7f, background)
            card(x + 3f, 4f, 5f, 36f, 2f, raised)
            repeat(2) { index ->
                card(x + 12f + index * width * .19f, 7f, width * .15f, 23f, 2f,
                    Color(palette.visualTheme.muted).copy(alpha = .34f + index * .12f))
            }
            card(x + 12f, 34f, width * .36f, 4f, 2f, accent)
            if (season != Season.NONE) drawTimelineHandle(season,
                Offset((x + width * .76f) * unit, 20f * unit), width * .15f * unit, accent)
        }
        when (value) {
            is LibraryArtType -> {
                val width = if (value.ratio > 1f) 52f else 22f
                val height = width / value.ratio
                card((64f - width) / 2f, (44f - height) / 2f, width, height, 3f,
                    colors.onSurfaceVariant.copy(alpha = .55f))
            }
            is LibraryView -> {
                repeat(3) { row ->
                    if (value == LibraryView.LIST) {
                        card(4f, 4f + row * 13f, 14f, 10f, 2f, colors.onSurfaceVariant.copy(alpha = .55f))
                        card(24f, 7f + row * 13f, 34f, 3f, 1f, colors.onSurfaceVariant.copy(alpha = .4f))
                    } else repeat(4) { column ->
                        card(4f + column * 15f, 4f + row * 13f, 11f, 10f, 2f, colors.onSurfaceVariant.copy(alpha = .55f))
                    }
                }
            }
            is LibraryCardSize -> {
                val width = 13f * value.scale
                val height = 27f * value.scale
                repeat(3) { index -> card(3f + index * (width + 3f), (44f - height) / 2f,
                    width, height, 3f, colors.onSurfaceVariant.copy(alpha = .3f + index * .2f)) }
            }
            is VisualTheme -> {
                card(0f, 0f, 64f, 44f, 7f, Color(value.background))
                card(4f, 4f, 9f, 36f, 3f, Color(value.raised))
                card(18f, 7f, 31f, 3f, 1f, Color(value.mutedHigh))
                repeat(3) { card(18f + it * 14f, 16f, 11f, 22f, 3f, Color(value.raised)) }
            }
            is AccentPalette -> {
                card(0f, 0f, 64f, 44f, 7f, Color(value.containerArgb))
                drawCircle(Color(value.argb), 10f * unit, Offset(18f * unit, 22f * unit))
                card(34f, 15f, 22f, 4f, 2f, Color(value.softArgb))
                card(34f, 24f, 14f, 4f, 2f, Color(value.softArgb).copy(alpha = .5f))
            }
            is ArtworkCorners -> repeat(3) { index ->
                card(3f + index * 19f, 6f, 16f, 32f, value.radius * .25f,
                    colors.onSurfaceVariant.copy(alpha = .3f + index * .2f))
            }
            is ArtworkSize -> {
                val width = 14f * value.scale
                val height = 28f * value.scale
                repeat(3) { index -> card(3f + index * (width + 3f), (44f - height) / 2f,
                    width, height, 3f, colors.onSurfaceVariant.copy(alpha = .3f + index * .2f)) }
            }
            is FocusStyle -> {
                repeat(3) { index -> card(3f + index * 20f, 9f, 16f, 27f, 3f,
                    colors.onSurfaceVariant.copy(alpha = .22f)) }
                drawRoundRect(if (value == FocusStyle.ACCENT) colors.primary else colors.onSurface,
                    Offset(23f * unit, 7f * unit), Size(17f * unit, 30f * unit), CornerRadius(4f * unit),
                    style = Stroke((if (value == FocusStyle.BOLD) 3.5f else 1.5f) * unit))
            }
            is Season -> {
                seasonSample(value)
            }
            is app.reelstack.data.model.SeasonChoice -> {
                when (value) {
                    app.reelstack.data.model.SeasonChoice.CALENDAR -> {
                        seasonSample(Season.HALLOWEEN, width = 31f)
                        seasonSample(Season.CHRISTMAS, x = 33f, width = 31f)
                    }
                    app.reelstack.data.model.SeasonChoice.CHRISTMAS -> seasonSample(Season.CHRISTMAS)
                    app.reelstack.data.model.SeasonChoice.HALLOWEEN -> seasonSample(Season.HALLOWEEN)
                    app.reelstack.data.model.SeasonChoice.NONE -> seasonSample(Season.NONE)
                }
            }
            is app.reelstack.data.model.SeasonalDecor -> {
                // The same card with nothing, one figure, or a scatter: how much there is, literally.
                card(0f, 0f, 64f, 44f, 7f, colors.onSurfaceVariant.copy(alpha = .12f))
                card(6f, 30f, 30f, 4f, 2f, colors.onSurfaceVariant.copy(alpha = .4f))
                if (value != app.reelstack.data.model.SeasonalDecor.OFF)
                    drawCircle(colors.primary, 5f * unit, Offset(48f * unit, 13f * unit))
                if (value == app.reelstack.data.model.SeasonalDecor.FULL) listOf(
                    Offset(14f, 9f), Offset(28f, 16f), Offset(38f, 6f), Offset(56f, 30f), Offset(44f, 38f), Offset(20f, 20f),
                ).forEach { drawCircle(colors.primary.copy(alpha = .6f), 1.6f * unit, it * unit) }
            }
        }
    }
}
