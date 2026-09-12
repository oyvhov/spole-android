package app.reelstack.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the system wants animation at all, read once instead of once per frame.
 *
 * `Settings.Global.getFloat` is a binder call into the settings provider. It was being made inside
 * [app.reelstack.ui.components.MediaArtwork] on every composition, which on a wall of forty posters
 * meant forty round trips to another process for every frame that touched the grid — a cost paid
 * continuously while scrolling, for a value that changes about once a year.
 *
 * The theme reads it once and hands it down. A developer who turns animations off mid-session sees
 * the change the next time the app is launched, which is the same deal every other app offers.
 */
val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
internal fun rememberMotionEnabled(context: Context): Boolean =
    androidx.compose.runtime.remember(context) { motionEnabled(context) }

internal fun motionEnabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}.getOrDefault(true)
