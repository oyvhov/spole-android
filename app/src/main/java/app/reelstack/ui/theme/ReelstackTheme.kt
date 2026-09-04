package app.reelstack.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Ink = Color(0xFF090812)
val Surface = Color(0xFF171421)
val SurfaceRaised = Color(0xFF211C2F)
val Primary = Color(0xFFAD7CF4)
val PrimarySoft = Color(0xFFC9ADFF)
val Text = Color(0xFFF6F0FF)
val Muted = Color(0xFFAAA2BA)
val Success = Color(0xFF56D993)
val Caution = Color(0xFFFFC66D)
val Warning = Color(0xFFFF7A7D)

private val ReelstackColors = darkColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF160D20),
    primaryContainer = Color(0xFF3B2854),
    onPrimaryContainer = PrimarySoft,
    secondary = PrimarySoft,
    background = Ink,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    outline = Color(0xFF62596E),
    error = Warning,
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
