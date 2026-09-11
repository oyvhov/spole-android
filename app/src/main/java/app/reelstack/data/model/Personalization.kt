package app.reelstack.data.model

enum class AccentPalette(val argb: Long, val softArgb: Long, val containerArgb: Long) {
    LIME(0xFFD5F478, 0xFFDCE9BD, 0xFF344024),
    OCEAN(0xFF96D5FF, 0xFFC8E5F7, 0xFF243B4A),
    IRIS(0xFFCAB8FF, 0xFFE1D6FA, 0xFF39304C),
    CORAL(0xFFFFB5A0, 0xFFFADACF, 0xFF4B332C),
    GOLD(0xFFFFD478, 0xFFFFE7B5, 0xFF493A22),
    MINT(0xFF86E2C2, 0xFFBDEDDD, 0xFF213E35),
    ROSE(0xFFF4ABD0, 0xFFF8D5E7, 0xFF482B3B),
    PEARL(0xFFE3E7EF, 0xFFF0F2F7, 0xFF353942);

    companion object {
        fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: LIME
    }
}

enum class ArtworkSize(val scale: Float) {
    COMPACT(.85f), STANDARD(1f), LARGE(1.2f);
    companion object {
        fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

/**
 * A mood owns its neutrals as well as its surfaces. Muted text, control edges and dividers used to
 * be fixed forest greys (hue ~114°), so choosing MIDNIGHT (223°) or PLUM (276°) left every divider
 * and every secondary label green against a blue or purple ground. Each mood now carries the same
 * lightness at its own hue; FOREST keeps its exact previous values, so the default is unchanged.
 */
enum class VisualTheme(
    val background: Long,
    val surface: Long,
    val raised: Long,
    val muted: Long,
    val mutedHigh: Long,
    val outline: Long,
    val outlineHigh: Long,
    val divider: Long,
) {
    FOREST(0xFF101211, 0xFF191C19, 0xFF2E332E, 0xFFA4ADA3, 0xFFDBE0DD, 0xFF646E63, 0xFFAFB8B0, 0xFF3D443C),
    MIDNIGHT(0xFF0D111B, 0xFF151D2D, 0xFF26334B, 0xFFA3A6AE, 0xFFDCDDE0, 0xFF6B6E76, 0xFFB1B3B9, 0xFF3C3E44),
    CINEMA(0xFF000000, 0xFF101010, 0xFF292929, 0xFFA8A8A8, 0xFFDEDEDE, 0xFF696969, 0xFFB5B5B5, 0xFF404040),
    PLUM(0xFF17111B, 0xFF231A29, 0xFF392D41, 0xFFA9A3AE, 0xFFDEDCE0, 0xFF716B76, 0xFFB6B1B9, 0xFF413C44);
    companion object { fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: FOREST }
}

enum class ArtworkCorners(val radius: Int) {
    CRISP(4), SOFT(12), ROUND(24);
    companion object { fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: SOFT }
}

enum class FocusStyle { WHITE, ACCENT, BOLD;
    companion object {
        /**
         * Focus is the only orientation a D-pad user has, and a 3 dp ring is six pixels on a
         * 1080p panel — a hairline from three metres away. Television therefore starts at BOLD.
         * A stored choice always wins, so nobody's setting is overridden; this only decides what
         * happens before anyone has chosen.
         */
        fun decode(value: String?, television: Boolean = false) =
            entries.firstOrNull { it.name == value } ?: if (television) BOLD else WHITE
    }
}

/** Device-local display choices, never account permissions or credentials. */
data class Personalization(
    val accent: AccentPalette = AccentPalette.LIME,
    val artworkSize: ArtworkSize = ArtworkSize.STANDARD,
    val autoResume: Boolean = true,
    val sidebarExpanded: Boolean? = null,
    val menuOrder: List<String> = DEFAULT_MENU,
    val hiddenMenuItems: Set<String> = emptySet(),
    val showNextUp: Boolean = true,
    val combineContinueWatching: Boolean = false,
    val showHero: Boolean = true,
    val showRatings: Boolean = true,
    val showQuality: Boolean = true,
    val slowStartup: Boolean = true,
    val visualTheme: VisualTheme = VisualTheme.FOREST,
    val artworkCorners: ArtworkCorners = ArtworkCorners.SOFT,
    val focusStyle: FocusStyle = FocusStyle.WHITE,
    val highContrast: Boolean = false,
)

val DEFAULT_MENU = listOf("HOME", "LIBRARY", "DISCOVER", "ACTIVITY", "SETTINGS")
fun Personalization.visibleMenu(): List<String> =
    (menuOrder + DEFAULT_MENU).distinct().filter { it in DEFAULT_MENU && (it !in hiddenMenuItems || it in setOf("HOME", "SETTINGS")) }
