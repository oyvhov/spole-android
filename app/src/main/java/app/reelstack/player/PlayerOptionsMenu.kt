package app.reelstack.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.SubtitleStyle
import app.reelstack.data.model.readableAudioTrackLabel
import app.reelstack.ui.components.*

internal enum class PlayerMenu(val label: Int) {
    HOME(R.string.player_options), AUDIO(R.string.player_audio_tracks), SUBTITLES(R.string.player_subtitles),
    QUALITY(R.string.player_quality), CHAPTERS(R.string.phase_chapters), SPEED(R.string.player_speed),
    AUDIO_DELAY(R.string.player_audio_delay), SUBTITLE_DELAY(R.string.player_subtitle_delay),
}

/** One stable viewport with progressive depth. Back returns through the page before closing. */
@Composable
internal fun PlayerOptionsMenu(page: PlayerMenu, state: PlayerScreenState, adjustments: PlaybackAdjustments,
    offline: Boolean, isTelevision: Boolean, fillVideo: Boolean, style: SubtitleStyle,
    onPage: (PlayerMenu) -> Unit, onDismiss: () -> Unit, onAudio: (Int) -> Unit, onSubtitle: (Int) -> Unit,
    onQuality: (Int) -> Unit, onSeek: (Long) -> Unit, onAdjust: (PlaybackAdjustments) -> Unit,
    onStyle: (SubtitleStyle) -> Unit, onFrameMode: () -> Unit, onStats: () -> Unit,
    onRotate: () -> Unit, onMiniPlayer: (() -> Unit)?) {
    var returnPage by remember { mutableStateOf(if (state.audio.isNotEmpty()) PlayerMenu.AUDIO else PlayerMenu.SPEED) }
    val focus = remember(page) { FocusRequester() }
    val keyboardNavigation = isTelevision || androidx.compose.ui.platform.LocalInputModeManager.current.inputMode == androidx.compose.ui.input.InputMode.Keyboard
    val back = { if (page == PlayerMenu.HOME) onDismiss() else onPage(PlayerMenu.HOME) }
    SpoleChoiceDialog(stringResource(page.label), onDismiss = back, onClose = onDismiss, fixedViewport = true,
        onBack = back.takeIf { page != PlayerMenu.HOME }) {
        val inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
        LaunchedEffect(page, keyboardNavigation) {
            if (keyboardNavigation) {
                withFrameNanos { }
                inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
                withFrameNanos { }; focus.requestFocus()
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("player-options-page")) {
            if (page == PlayerMenu.HOME) {
                @Composable fun route(destination: PlayerMenu, summary: String? = null, enabled: Boolean = true) {
                    PlayerOptionRow(stringResource(destination.label),
                        Modifier.testTag("player-option-${destination.name.lowercase()}")
                            .then(if (destination == returnPage) Modifier.focusRequester(focus) else Modifier), summary, enabled) {
                        returnPage = destination; onPage(destination)
                    }
                }
                route(PlayerMenu.AUDIO, state.audio.firstOrNull { it.index == state.audioIndex }?.let { readableAudioTrackLabel(it.label, it.language) }, state.audio.isNotEmpty())
                route(PlayerMenu.SUBTITLES, state.subtitles.firstOrNull { it.index == state.subtitleIndex }?.label ?: stringResource(R.string.player_off), state.subtitles.isNotEmpty())
                if (state.chapters.isNotEmpty()) route(PlayerMenu.CHAPTERS)
                route(PlayerMenu.SPEED, speedLabel(adjustments.speed))
                route(PlayerMenu.AUDIO_DELAY, delayLabel(adjustments.audioDelayMs))
                route(PlayerMenu.SUBTITLE_DELAY, delayLabel(adjustments.subtitleDelayMs), state.subtitleIndex >= 0)
                if (!offline) route(PlayerMenu.QUALITY)
                PlayerOptionRow(stringResource(R.string.player_frame), Modifier.testTag("player-frame-mode"),
                    stringResource(if (fillVideo) R.string.player_frame_fill else R.string.player_frame_fit), onClick = onFrameMode)
                if (!isTelevision) PlayerOptionRow(stringResource(R.string.player_rotate), onClick = onRotate)
                if (onMiniPlayer != null) PlayerOptionRow(stringResource(R.string.phase_mini), modifier = Modifier.testTag("player-mini"), onClick = onMiniPlayer)
                if (!offline) PlayerOptionRow(stringResource(R.string.player_stats_title), modifier = Modifier.testTag("player-stats"), onClick = onStats)
            } else if (page == PlayerMenu.AUDIO_DELAY || page == PlayerMenu.SUBTITLE_DELAY) {
                val audio = page == PlayerMenu.AUDIO_DELAY
                val value = if (audio) adjustments.audioDelayMs else adjustments.subtitleDelayMs
                Text(delayLabel(value), Modifier.padding(16.dp).testTag("player-delay-value"), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.player_delay_hint), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                fun change(delta: Int) = onAdjust(if (audio) adjustments.copy(audioDelayMs = value + delta) else adjustments.copy(subtitleDelayMs = value + delta))
                PlayerOptionRow(stringResource(R.string.player_delay_earlier), modifier = Modifier.focusRequester(focus).testTag("player-delay-earlier")) { change(-100) }
                PlayerOptionRow(stringResource(R.string.player_delay_later), modifier = Modifier.testTag("player-delay-later")) { change(100) }
                PlayerOptionRow(stringResource(R.string.player_reset), modifier = Modifier.testTag("player-delay-reset")) { change(-value) }
            } else {
                if (page == PlayerMenu.SUBTITLES) app.reelstack.ui.screens.SubtitleAppearanceSetting(style, onStyle)
                val options = when (page) {
                    PlayerMenu.AUDIO -> state.audio.map { it.index to readableAudioTrackLabel(it.label, it.language) }
                    PlayerMenu.SUBTITLES -> listOf(-1 to stringResource(R.string.player_off)) + state.subtitles.map { it.index to it.label }
                    PlayerMenu.CHAPTERS -> state.chapters.mapIndexed { index, chapter -> index to "${playbackTime(chapter.startPositionMs)} · ${chapter.name}" }
                    PlayerMenu.SPEED -> listOf(50, 75, 100, 125, 150, 175, 200).map { it to speedLabel(it / 100f) }
                    else -> listOf(0 to stringResource(R.string.player_auto), 80_000_000 to stringResource(R.string.player_quality_ultra),
                        20_000_000 to stringResource(R.string.player_quality_high), 4_000_000 to stringResource(R.string.player_medium_data), 2_000_000 to stringResource(R.string.player_low_data))
                }
                val selected = when (page) {
                    PlayerMenu.AUDIO -> state.audioIndex
                    PlayerMenu.SUBTITLES -> state.subtitleIndex
                    PlayerMenu.CHAPTERS -> state.chapters.indexOfLast { it.startPositionMs <= state.positionMs }
                    PlayerMenu.SPEED -> (adjustments.speed * 100).toInt()
                    else -> state.quality
                }
                options.forEachIndexed { index, (id, label) ->
                    SpoleChoiceRow(label, id == selected,
                        Modifier.then(if (id == selected || index == 0 && options.none { it.first == selected }) Modifier.focusRequester(focus) else Modifier),
                        checkmark = true, onClick = {
                            when (page) {
                                PlayerMenu.AUDIO -> onAudio(id)
                                PlayerMenu.SUBTITLES -> onSubtitle(id)
                                PlayerMenu.QUALITY -> onQuality(id)
                                PlayerMenu.CHAPTERS -> state.chapters.getOrNull(id)?.let { onSeek(it.startPositionMs) }
                                PlayerMenu.SPEED -> onAdjust(adjustments.copy(speed = id / 100f))
                            }
                            onDismiss()
                        })
                }
            }
        }
    }
}

@Composable
private fun PlayerOptionRow(label: String, modifier: Modifier = Modifier, summary: String? = null,
    enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).settingsSurface(interaction)
        .clickable(enabled = enabled, interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
            onClickLabel = label, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .4f))
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(SpoleIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun speedLabel(value: Float): String = stringResource(R.string.player_speed_value, value)
@Composable private fun delayLabel(value: Int): String = if (value == 0) stringResource(R.string.player_delay_none)
    else stringResource(if (value > 0) R.string.player_delay_late_value else R.string.player_delay_early_value, kotlin.math.abs(value))
