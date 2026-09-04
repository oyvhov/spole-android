package app.reelstack.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Ink = Color(0xFF101211)
val Surface = Color(0xFF191C19)
val SurfaceRaised = Color(0xFF232723)
val Primary = Color(0xFFD5F478)
val PrimarySoft = Color(0xFFDCE9BD)
val Text = Color(0xFFF3F3EC)
val Muted = Color(0xFFA4ADA3)
val Success = Color(0xFF56D993)
val Caution = Color(0xFFFFC66D)
val Warning = Color(0xFFFF7A7D)
val Divider = Color(0xFF343A34)

private val ReelstackColors = darkColorScheme(
    primary = Primary,
    onPrimary = Ink,
    primaryContainer = Color(0xFF344024),
    onPrimaryContainer = PrimarySoft,
    secondary = PrimarySoft,
    onSecondary = Ink,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = PrimarySoft,
    tertiary = Success,
    onTertiary = Ink,
    tertiaryContainer = SurfaceRaised,
    onTertiaryContainer = Success,
    background = Ink,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    surfaceDim = Ink,
    surfaceBright = SurfaceRaised,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceRaised,
    outline = Color(0xFF51594F),
    outlineVariant = Divider,
    inverseSurface = Text,
    inverseOnSurface = Ink,
    inversePrimary = Color(0xFF415522),
    error = Warning,
    onError = Ink,
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

    MaterialTheme(
        colorScheme = ReelstackColors,
        typography = reelstackTypography(),
        content = content,
    )
}
