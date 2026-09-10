package app.reelstack.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Ink: Color @Composable get() = MaterialTheme.colorScheme.background
val Surface: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val Primary: Color @Composable get() = MaterialTheme.colorScheme.primary
val PrimarySoft: Color @Composable get() = MaterialTheme.colorScheme.secondary
val Text = Color(0xFFF3F3EC)
val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Success = Color(0xFF56D993)
val Caution = Color(0xFFFFC66D)
val Warning = Color(0xFFFF7A7D)

/** Follows the chosen mood's hue. See [app.reelstack.data.model.VisualTheme]. */
val Divider: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

/**
 * Boundary colour for the few controls that already draw an edge — text fields, unselected chips.
 * At least 3.2:1 against every mood's surface, so those existing edges satisfy WCAG 1.4.11 without
 * lifting the matte surfaces to a light grey. Never add it to a card or a row: containers are
 * separated by their fill, not by a line.
 */
val ControlOutline: Color @Composable get() = MaterialTheme.colorScheme.outline

/** Checked switch track. Lime stays in the thumb so a settings list is not a wall of accent. */
val SwitchTrackOn: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer

private val ReelstackColors = darkColorScheme(
    primary = Color(0xFFD5F478),
    onPrimary = Color(0xFF101211),
    primaryContainer = Color(0xFF344024),
    onPrimaryContainer = Color(0xFFDCE9BD),
    secondary = Color(0xFFDCE9BD),
    onSecondary = Color(0xFF101211),
    secondaryContainer = Color(0xFF2E332E),
    onSecondaryContainer = Color(0xFFDCE9BD),
    tertiary = Success,
    onTertiary = Color(0xFF101211),
    tertiaryContainer = Color(0xFF2E332E),
    onTertiaryContainer = Success,
    background = Color(0xFF101211),
    onBackground = Text,
    surface = Color(0xFF191C19),
    onSurface = Text,
    surfaceVariant = Color(0xFF2E332E),
    onSurfaceVariant = Color(0xFFA4ADA3),
    surfaceDim = Color(0xFF101211),
    surfaceBright = Color(0xFF2E332E),
    surfaceContainerLowest = Color(0xFF101211),
    surfaceContainerLow = Color(0xFF191C19),
    surfaceContainer = Color(0xFF191C19),
    surfaceContainerHigh = Color(0xFF2E332E),
    surfaceContainerHighest = Color(0xFF2E332E),
    outline = Color(app.reelstack.data.model.VisualTheme.FOREST.outline),
    outlineVariant = Color(app.reelstack.data.model.VisualTheme.FOREST.divider),
    inverseSurface = Text,
    inverseOnSurface = Color(0xFF101211),
    inversePrimary = Color(0xFF415522),
    error = Warning,
    onError = Color(0xFF101211),
    errorContainer = Color(0xFF43282A),
    onErrorContainer = Color(0xFFFFDADC),
)

@Composable
fun ReelstackTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    val personalization = rememberPersonalization()
    val palette = personalization.accent
    val mood = personalization.visualTheme
    val background = Color(mood.background)
    val surface = Color(mood.surface)
    val raised = Color(mood.raised)
    androidx.compose.runtime.CompositionLocalProvider(LocalPersonalization provides personalization) {
    MaterialTheme(
        colorScheme = ReelstackColors.copy(
            background = background, surface = surface, surfaceVariant = raised,
            surfaceDim = background, surfaceBright = raised,
            surfaceContainerLowest = background, surfaceContainerLow = surface,
            surfaceContainer = surface, surfaceContainerHigh = raised, surfaceContainerHighest = raised,
            secondaryContainer = raised, tertiaryContainer = raised,
            // Neutrals follow the mood's own hue, so a divider in MIDNIGHT is not a forest grey.
            onSurfaceVariant = Color(if (personalization.highContrast) mood.mutedHigh else mood.muted),
            outline = Color(if (personalization.highContrast) mood.outlineHigh else mood.outline),
            outlineVariant = Color(mood.divider),
            primary = Color(palette.argb),
            secondary = Color(palette.softArgb),
            primaryContainer = Color(palette.containerArgb),
            onPrimaryContainer = Color(palette.softArgb),
            onSecondaryContainer = Color(palette.softArgb),
        ),
        typography = reelstackTypography(),
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Text,
            ) { content() }
        },
    )
    }
}
