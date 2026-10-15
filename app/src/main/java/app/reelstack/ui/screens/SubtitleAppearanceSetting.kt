package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.ui.SubtitleView
import app.reelstack.R
import app.reelstack.data.model.SubtitleStyle
import app.reelstack.player.applyAppearance
import app.reelstack.ui.components.SettingsActionRow
import app.reelstack.ui.components.SpoleChoiceDialog
import app.reelstack.ui.components.SpoleChoiceRow
import app.reelstack.ui.components.SpoleIcons

@Composable
internal fun subtitleStyleName(style: SubtitleStyle): String = stringResource(when(style) {
    SubtitleStyle.CLEAN -> R.string.subtitle_clean
    SubtitleStyle.CINEMA -> R.string.subtitle_cinema
    SubtitleStyle.HIGH_CONTRAST -> R.string.subtitle_contrast
    SubtitleStyle.LARGE -> R.string.subtitle_large
})

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun SubtitleAppearanceSetting(value: SubtitleStyle, onChange: (SubtitleStyle) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsActionRow(stringResource(R.string.subtitle_appearance), subtitleStyleName(value), "subtitle-style") { open = true }
    if (open) SpoleChoiceDialog(stringResource(R.string.subtitle_choose), { open = false }, icon = SpoleIcons.Subtitles) {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val sample = stringResource(R.string.subtitle_sample)
                // One quiet frame with both bright and dark areas shows what outlines/backgrounds
                // actually do. It is a local preview, independent of artwork/network loading.
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color(0xFFE0C7A2), Color(0xFF6C8187), Color(0xFF18252E))))
                        drawCircle(Color(0xFFFFE9BE).copy(alpha = .65f), size.height * .17f,
                            Offset(size.width * .19f, size.height * .28f))
                        val horizon = Path().apply {
                            moveTo(0f, size.height * .78f)
                            cubicTo(size.width * .21f, size.height * .4f, size.width * .35f, size.height * .74f,
                                size.width * .54f, size.height * .5f)
                            lineTo(size.width, size.height * .73f)
                            lineTo(size.width, size.height); lineTo(0f, size.height); close()
                        }
                        drawPath(horizon, Color(0xFF263C40).copy(alpha = .8f))
                    }
                    AndroidView(factory = { SubtitleView(it).apply { setBackgroundColor(android.graphics.Color.TRANSPARENT) } },
                        update = {
                            it.applyAppearance(value)
                            it.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, if (value == SubtitleStyle.LARGE) 22.6f else 18f)
                            it.setCues(listOf(Cue.Builder().setText(sample).setSize(.88f)
                                .setLine(.7f, Cue.LINE_TYPE_FRACTION).setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE).build()))
                        }, modifier = Modifier.matchParentSize().testTag("subtitle-preview"))
                }
                SubtitleStyle.entries.forEach { style ->
                    SpoleChoiceRow(subtitleStyleName(style), value == style,
                        Modifier.testTag("subtitle-style-${style.name}"), icon = SpoleIcons.Subtitles) { onChange(style) }
                }
                Text(stringResource(R.string.subtitle_scope), style = MaterialTheme.typography.bodySmall)
            }
        }
}
