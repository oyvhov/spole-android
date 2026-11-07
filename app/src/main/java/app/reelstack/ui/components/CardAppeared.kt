package app.reelstack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Whether a card has had its first frame, for the reveal that fades it in.
 *
 * A card without a reveal (a television, or reduced motion) has appeared from the start, and nothing is written.
 * Writing "appeared" a frame after the card was built made Compose build the card a second time: the six cards of a
 * shelf coming into view were built twice, and the second build was a 14 ms frame at every press on the Shield
 * (2026-11-07), the one frame in six that came late.
 */
@Composable
internal fun rememberCardAppeared(key: Any?, reveal: Boolean): Boolean {
    var appeared by rememberSaveable(key) { mutableStateOf(!reveal) }
    if (reveal) LaunchedEffect(key) { appeared = true }
    return appeared
}
