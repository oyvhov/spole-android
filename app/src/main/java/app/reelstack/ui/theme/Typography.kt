package app.reelstack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun reelstackTypography() = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.4).sp,
    ),
    // Every level the app actually references is declared here. A style left out does not fall
    // back to a neighbour — it falls back to Material's Roboto defaults, which are Regular weight
    // with positive tracking, the opposite of this scale. That is how the Library page title ended
    // up in a different typeface family than Discover's.
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 30.sp,
        lineHeight = 35.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.9).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 21.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    // The app's most-used secondary style. Held at the same 1.42 leading ratio as bodyMedium so a
    // caption under a title sits where the rhythm expects it, not at Material's 12/16.
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)
