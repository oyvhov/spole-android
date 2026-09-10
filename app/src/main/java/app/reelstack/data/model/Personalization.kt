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

enum class VisualTheme(val background: Long, val surface: Long, val raised: Long) {
    FOREST(0xFF101211, 0xFF191C19, 0xFF2E332E),
    MIDNIGHT(0xFF0D111B, 0xFF151D2D, 0xFF26334B),
    CINEMA(0xFF000000, 0xFF101010, 0xFF292929),
    PLUM(0xFF17111B, 0xFF231A29, 0xFF392D41);
    companion object { fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: FOREST }
}

enum class ArtworkCorners(val radius: Int) {
    CRISP(4), SOFT(12), ROUND(24);
    companion object { fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: SOFT }
}

enum class FocusStyle { WHITE, ACCENT, BOLD;
    companion object { fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: WHITE }
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
