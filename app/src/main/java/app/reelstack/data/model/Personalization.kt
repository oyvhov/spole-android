package app.reelstack.data.model

enum class AccentPalette(val argb: Long, val softArgb: Long, val containerArgb: Long) {
    LIME(0xFFD5F478, 0xFFDCE9BD, 0xFF344024),
    OCEAN(0xFF96D5FF, 0xFFC8E5F7, 0xFF243B4A),
    IRIS(0xFFCAB8FF, 0xFFE1D6FA, 0xFF39304C),
    CORAL(0xFFFFB5A0, 0xFFFADACF, 0xFF4B332C),
    GOLD(0xFFFFD478, 0xFFFFE7B5, 0xFF493A22),
    MINT(0xFF86E2C2, 0xFFBDEDDD, 0xFF213E35),
    ROSE(0xFFF4ABD0, 0xFFF8D5E7, 0xFF482B3B),
    PEARL(0xFFE3E7EF, 0xFFF0F2F7, 0xFF353942),

    // Two seasons. Spole holds its accents to 7:1 against the ink, which is why neither of these
    // is the post-box red or the burnt orange a poster would use: green dominates relative
    // luminance, so a saturated red simply cannot reach that ratio against near-black without
    // being lightened. The rule is there so accent text stays readable, and a decoration is the
    // last thing that should be allowed to bend it.
    HOLLY(0xFFF6806C, 0xFFF6B3AC, 0xFF4A1F1C),
    PUMPKIN(0xFFFFA74F, 0xFFFFD9AE, 0xFF4B3018);

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
    PLUM(0xFF17111B, 0xFF231A29, 0xFF392D41, 0xFFA9A3AE, 0xFFDEDCE0, 0xFF716B76, 0xFFB6B1B9, 0xFF413C44),

    /**
     * Two seasonal moods, built the same way as the other four: one hue, the same lightness steps.
     * NOEL is spruce at night rather than the poster red people expect — red is the accent, and a
     * red ground under red buttons would leave nothing to look at. HALLOWEEN is aubergine, not
     * black, so the orange has something to sit against.
     */
    NOEL(0xFF0C1210, 0xFF141D18, 0xFF24332C, 0xFF9FAEA6, 0xFFD9E2DD, 0xFF5F6F67, 0xFFAAB9B1, 0xFF37463E),
    HALLOWEEN(0xFF120D16, 0xFF1B1421, 0xFF2E2337, 0xFFA79FAE, 0xFFDDD9E1, 0xFF6E6577, 0xFFB4ADBA, 0xFF423849);
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
    val showNextEpisode: Boolean = true,
    /** Keep the next-episode prompt late and unobtrusive by default. */
    val nextEpisodeLeadSeconds: Int = 15,
    val autoPlayNextEpisode: Boolean = true,
    val nextEpisodeDelaySeconds: Int = 12,
    /** The explanatory direct-play/transcode line under the player controls. */
    val showPlaybackModeInOsd: Boolean = false,
    val lightweightTv: Boolean = false,
    val sidebarExpanded: Boolean? = null,
    val hideTvSidebar: Boolean = false,
    val menuOrder: List<String> = DEFAULT_MENU,
    val hiddenMenuItems: Set<String> = emptySet(),
    /**
     * Downloads always live under Settings. Whether they also take a place in the phone and tablet
     * menu is the owner's choice, and off by default: five tabs left every label cramped.
     */
    val showDownloadsInMenu: Boolean = false,
    /** The search line under the Home header. The search icon beside the profile is always there. */
    val showHomeSearchBar: Boolean = false,
    val showNextUp: Boolean = true,
    val combineContinueWatching: Boolean = false,
    val showHero: Boolean = true,
    val startInLibrary: Boolean = false,
    val showLibraryTitle: Boolean = false,
    val libraryCardsWide: Boolean = true,
    val libraryHubOrder: List<String> = DEFAULT_LIBRARY_HUB,
    val libraryHubHidden: Set<String> = emptySet(),
    /** One order for the library page and for the libraries pinned to the menu. */
    val libraryOrder: List<String> = emptyList(),
    /**
     * Libraries once hidden from the library page only. Showing and hiding is now the library
     * selection itself; the editor folds this into it the first time it saves, then empties it.
     */
    val libraryHidden: Set<String> = emptySet(),
    val reduceMotion: Boolean = false,
    val homeRowFormats: Map<String, String> = emptyMap(),
    val detailBackdrop: Boolean = true,
    /** Resolution, codecs, audio format and channel layout in title details. */
    val showMediaInfo: Boolean = false,
    val slowStartup: Boolean = false,
    val visualTheme: VisualTheme = VisualTheme.FOREST,
    val artworkCorners: ArtworkCorners = ArtworkCorners.SOFT,
    val focusStyle: FocusStyle = FocusStyle.WHITE,
    val highContrast: Boolean = false,
    /**
     * How much of a season shows beyond its colours. FULL by default when a seasonal mood is
     * chosen, because the mood is the whole reason anyone chose it; CALM and OFF are one choice
     * away, and everything that moves stops on its own when animations are turned down.
     */
    val seasonalDecor: SeasonalDecor = SeasonalDecor.FULL,
    /** Lay the calendar's season over the stored mood while it runs. See [seasonal]. */
    val seasonCalendar: Boolean = false,
    /** Set only on what the screen shows while the calendar's season covers the stored mood. Never stored. */
    val seasonOverlay: SeasonOverlay? = null,
    val subtitleStyle: SubtitleStyle = SubtitleStyle.CLEAN,
    val preferredSubtitleLanguage: SubtitleLanguage = SubtitleLanguage.NORWEGIAN,
    val fallbackSubtitleLanguage: SubtitleLanguage = SubtitleLanguage.ENGLISH,
    val showLibraryCardNames: Boolean = true,
    val watchNextEnabled: Boolean = false,
    /** Device-local name shown beside the Spole mark in the main header. */
    val appLabel: String = "Spole",
)

enum class SubtitleStyle { CLEAN, CINEMA, HIGH_CONTRAST, LARGE }

val DEFAULT_LIBRARY_HUB = listOf("FEATURE", "LIBRARY_NEXT", "FAVOURITES", SMART_SHELVES_SECTION, "LIBRARIES")

/** The library page's row of smart shelves, which shows only while it has a shelf on it. */
const val SMART_SHELVES_SECTION = "SMART_SHELVES"
val DEFAULT_MENU = listOf("HOME", "LIBRARY", "DISCOVER", "ACTIVITY", "SETTINGS")
fun Personalization.requiredMenu(): Set<String> =
    setOf("HOME", "SETTINGS") + if (startInLibrary) setOf("LIBRARY") else emptySet()

fun Personalization.withMenuVisible(id: String, visible: Boolean): Personalization = copy(
    hiddenMenuItems = (if (visible) hiddenMenuItems - id else hiddenMenuItems + id)
        .intersect(DEFAULT_MENU.toSet()) - requiredMenu(),
)

fun Personalization.visibleMenu(): List<String> =
    (menuOrder + DEFAULT_MENU).distinct().filter { it in DEFAULT_MENU && (it !in hiddenMenuItems || it in requiredMenu()) }

const val DOWNLOADS_MENU_ITEM = "DOWNLOADS"

/**
 * The menu a phone or tablet actually shows. Downloads are an adult, touch-only library: they join
 * the menu just before Settings when the owner asks for it, and never on television or for a child.
 */
fun Personalization.touchMenu(television: Boolean, kidMode: Boolean): List<String> =
    visibleMenu().toMutableList().apply {
        remove(DOWNLOADS_MENU_ITEM)
        if (showDownloadsInMenu && !television && !kidMode) add(indexOf("SETTINGS").coerceAtLeast(0), DOWNLOADS_MENU_ITEM)
    }

/**
 * A season is a pairing of mood and accent, chosen as one thing.
 *
 * People do not ask for "aubergine with a pumpkin accent", they ask for Halloween. Keeping the two
 * halves as ordinary [VisualTheme] and [AccentPalette] values means the pairing is a shortcut and
 * not a separate mode: anyone who wants spruce with a gold accent can still have it, and nothing
 * in the app has to know whether a season is running.
 */
enum class Season(val swatch: Long) {
    NONE(0xFF2E332E),
    CHRISTMAS(VisualTheme.NOEL.surface),
    HALLOWEEN(VisualTheme.HALLOWEEN.surface);

    fun applyTo(value: Personalization): Personalization = when (this) {
        NONE -> value.copy(visualTheme = VisualTheme.FOREST, accent = AccentPalette.LIME)
        CHRISTMAS -> value.copy(visualTheme = VisualTheme.NOEL, accent = AccentPalette.HOLLY)
        HALLOWEEN -> value.copy(visualTheme = VisualTheme.HALLOWEEN, accent = AccentPalette.PUMPKIN)
    }

    companion object {
        /** The mood decides. Someone who swapped only the accent is still in the season. */
        fun of(value: Personalization): Season = when (value.visualTheme) {
            VisualTheme.NOEL -> CHRISTMAS
            VisualTheme.HALLOWEEN -> HALLOWEEN
            else -> NONE
        }
    }
}

/**
 * How much of a season shows beyond its colours.
 *
 * CALM is a season you notice without it asking for anything: the mark, the greeting, a pumpkin or
 * a snowflake where the timeline handle was, the buffering wheel. FULL adds everything that moves
 * or fills an edge: snow and embers over the artwork, the scenes in the menu and in sheets, and the
 * spider that comes down the menu when the remote has been left alone. OFF keeps the colours only.
 */
enum class SeasonalDecor { OFF, CALM, FULL;
    companion object {
        /** Before the levels, a single switch: on was everything there was, which is now FULL. */
        fun decode(value: String?, legacyOrnament: Boolean = true) =
            entries.firstOrNull { it.name == value } ?: if (legacyOrnament) FULL else OFF
    }
}

/** The season row in Appearance: none, a fixed season, or whatever the calendar says. */
enum class SeasonChoice { NONE, CALENDAR, CHRISTMAS, HALLOWEEN;

    /**
     * Only a seasonal mood is ever replaced. Someone on MIDNIGHT with an ocean accent who picks
     * «Heile året» or the calendar keeps MIDNIGHT; the calendar lays its season over it while it runs.
     */
    fun applyTo(value: Personalization): Personalization {
        val stored = value.withoutCalendarSeason()
        val plain = if (Season.of(stored) != Season.NONE) Season.NONE.applyTo(stored) else stored
        return when (this) {
            NONE -> plain.copy(seasonCalendar = false)
            CALENDAR -> plain.copy(seasonCalendar = true)
            CHRISTMAS -> Season.CHRISTMAS.applyTo(stored).copy(seasonCalendar = false)
            HALLOWEEN -> Season.HALLOWEEN.applyTo(stored).copy(seasonCalendar = false)
        }
    }

    companion object {
        fun of(value: Personalization): SeasonChoice = if (value.seasonCalendar) CALENDAR else when (Season.of(value)) {
            Season.NONE -> NONE
            Season.CHRISTMAS -> CHRISTMAS
            Season.HALLOWEEN -> HALLOWEEN
        }
    }
}
