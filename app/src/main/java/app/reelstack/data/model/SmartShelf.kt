package app.reelstack.data.model

import java.time.LocalDate
import java.time.Month
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * A shelf that is a question to the server rather than a list of titles.
 *
 * It names tags and genres, and the server answers with whatever it holds today: a film added
 * tomorrow with the right keyword is on the shelf tomorrow, and nobody keeps anything up to date.
 * Jellyfin and Emby store TMDB's keywords as tags, which is what makes «halloween» or «christmas
 * calendar» find the right titles without a plugin on the server. A title matches when it has
 * any of the tags or any of the genres, or, with [matchAll], one of each; [kinds] and [unwatched]
 * then narrow that down.
 *
 * Shelves are kept per profile on the device, like the rest of a profile's own choices.
 */
data class SmartShelf(
    val id: String,
    /** Blank on a built-in shelf the owner has not renamed: it takes its name from the language. */
    val name: String = "",
    val icon: SmartShelfIcon = SmartShelfIcon.STAR,
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val kinds: SmartShelfKinds = SmartShelfKinds.BOTH,
    val unwatched: Boolean = false,
    val period: SmartShelfPeriod = SmartShelfPeriod.ALWAYS,
    val inMenu: Boolean = false,
    /** Whether the library page lists the shelf. Placement is a choice per shelf: library, Home and menu. */
    val inLibrary: Boolean = true,
    /** Whether Home gets a row of the shelf's titles, for as long as its period runs. */
    val onHome: Boolean = false,
    /**
     * With both tags and genres, a title needs one of each rather than one of either: «halloween»
     * and horror is the Halloween horror, not every horror film. Ignored while one side is empty.
     */
    val matchAll: Boolean = false,
    /** One of [SmartShelfPresets]; a preset can be changed and reset, never deleted. */
    val preset: Boolean = false,
) {
    val hasRule: Boolean get() = tags.isNotEmpty() || genres.isNotEmpty()

    /** Tags and genres both, so «or» and «and» ask for different titles. */
    val combinesBoth: Boolean get() = tags.isNotEmpty() && genres.isNotEmpty()

    /** A title must carry a tag and a genre. */
    val requiresBoth: Boolean get() = matchAll && combinesBoth

    /** The library path entry that opens this shelf. It never collides with a server id. */
    val pathId: String get() = PATH_PREFIX + id

    /**
     * What a child's profile asks for. Horror leaves the rule, so a child's Halloween is the
     * films made for the night; what remains is still limited by the child's own server account.
     */
    fun forViewer(child: Boolean): SmartShelf =
        if (!child) this else copy(genres = genres.filterNot { it.lowercase() in ADULT_GENRES })

    fun encode(): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        put("icon", icon.name)
        put("tags", buildJsonArray { tags.forEach { add(JsonPrimitive(it)) } })
        put("genres", buildJsonArray { genres.forEach { add(JsonPrimitive(it)) } })
        put("kinds", kinds.name)
        put("unwatched", unwatched)
        put("period", period.name)
        put("inMenu", inMenu)
        put("inLibrary", inLibrary)
        put("onHome", onHome)
        put("matchAll", matchAll)
        put("preset", preset)
    }

    companion object {
        const val PATH_PREFIX = "spole-shelf-"
        private val ADULT_GENRES = setOf("horror", "skrekk")

        fun idOfPath(pathId: String?): String? =
            pathId?.takeIf { it.startsWith(PATH_PREFIX) }?.removePrefix(PATH_PREFIX)?.takeIf(String::isNotBlank)

        fun decode(value: JsonObject): SmartShelf? {
            fun text(key: String) = (value[key] as? JsonPrimitive)?.contentOrNull
            fun list(key: String) = (value[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.map(String::trim)?.filter(String::isNotBlank)?.distinct().orEmpty()
            val id = text("id")?.takeIf(String::isNotBlank) ?: return null
            return SmartShelf(
                id = id,
                name = text("name").orEmpty().trim().take(40),
                icon = SmartShelfIcon.entries.firstOrNull { it.name == text("icon") } ?: SmartShelfIcon.STAR,
                tags = list("tags"),
                genres = list("genres"),
                kinds = SmartShelfKinds.entries.firstOrNull { it.name == text("kinds") } ?: SmartShelfKinds.BOTH,
                unwatched = (value["unwatched"] as? JsonPrimitive)?.booleanOrNull ?: false,
                period = SmartShelfPeriod.entries.firstOrNull { it.name == text("period") } ?: SmartShelfPeriod.ALWAYS,
                inMenu = (value["inMenu"] as? JsonPrimitive)?.booleanOrNull ?: false,
                inLibrary = (value["inLibrary"] as? JsonPrimitive)?.booleanOrNull ?: true,
                // A changed preset stored before Home rows existed keeps what its preset says.
                onHome = (value["onHome"] as? JsonPrimitive)?.booleanOrNull
                    ?: (value["preset"] as? JsonPrimitive)?.booleanOrNull ?: false,
                matchAll = (value["matchAll"] as? JsonPrimitive)?.booleanOrNull ?: false,
                preset = (value["preset"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }

        fun decodeAll(value: String?): List<SmartShelf> = runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(value ?: "[]").jsonArray
                .mapNotNull { (it as? JsonObject)?.let(::decode) }.distinctBy { it.id }
        }.getOrDefault(emptyList())

        fun encodeAll(shelves: List<SmartShelf>): String = buildJsonArray { shelves.forEach { add(it.encode()) } }.toString()
    }
}

enum class SmartShelfIcon { STAR, HEART, PUMPKIN, SNOWFLAKE, CALENDAR, MOVIE, SCREEN, KIDS, ANIMATION, DOCUMENTARY, MUSIC, SPORT }

/** Films, series or both. A shelf that holds both still shows them apart. */
enum class SmartShelfKinds(val itemTypes: String) { BOTH("Movie,Series"), MOVIES("Movie"), SERIES("Series") }

/**
 * When a shelf comes forward by itself. A shelf with a period appears in the library and, when
 * asked, in the menu while it runs; outside it the shelf waits, and the owner's own shelves stay
 * in the library all year.
 */
enum class SmartShelfPeriod {
    ALWAYS, HALLOWEEN, CHRISTMAS, ADVENT, EASTER;

    fun isActive(date: LocalDate): Boolean = when (this) {
        ALWAYS -> true
        HALLOWEEN -> Season.onCalendar(date) == Season.HALLOWEEN
        CHRISTMAS -> Season.onCalendar(date) == Season.CHRISTMAS
        ADVENT -> date.month == Month.DECEMBER && date.dayOfMonth <= 24
        // Palm Sunday to Easter Monday, the Norwegian Easter holiday.
        EASTER -> easterSunday(date.year).let { sunday -> !date.isBefore(sunday.minusDays(7)) && !date.isAfter(sunday.plusDays(1)) }
    }
}

/** Easter Sunday in the Gregorian calendar (the anonymous algorithm, also called Meeus/Jones/Butcher). */
fun easterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate.of(year, month, day)
}

/**
 * The shelves Spole starts with. Halloween and Christmas are the doors from the seasonal themes;
 * the Christmas calendar and Easter crime are Norwegian traditions no catalogue has a row for.
 * Genres are listed under both their English and Norwegian names, since a library's metadata
 * language decides which one the server uses.
 */
object SmartShelfPresets {
    // Each preset also gets a row on Home while its period runs, so the film for the night is one
    // press from the front page. Turned off per shelf, or hidden from the Home layout, like any row.
    val halloween = SmartShelf("halloween", icon = SmartShelfIcon.PUMPKIN, tags = listOf("halloween"),
        genres = listOf("Horror", "Skrekk"), period = SmartShelfPeriod.HALLOWEEN, onHome = true, preset = true)
    val christmas = SmartShelf("christmas", icon = SmartShelfIcon.SNOWFLAKE, tags = listOf("christmas"),
        period = SmartShelfPeriod.CHRISTMAS, onHome = true, preset = true)
    val christmasCalendar = SmartShelf("christmas-calendar", icon = SmartShelfIcon.CALENDAR,
        tags = listOf("christmas calendar"), period = SmartShelfPeriod.ADVENT, onHome = true, preset = true)
    val easterCrime = SmartShelf("easter-crime", icon = SmartShelfIcon.STAR,
        genres = listOf("Crime", "Krim", "Mystery", "Mysterium"), period = SmartShelfPeriod.EASTER, onHome = true, preset = true)

    val all = listOf(halloween, christmas, christmasCalendar, easterCrime)

    fun of(season: Season): SmartShelf? = when (season) {
        Season.HALLOWEEN -> halloween
        Season.CHRISTMAS -> christmas
        Season.NONE -> null
    }
}

/** Every shelf a profile has: its own, its changed presets, and the presets as they come. */
fun mergedSmartShelves(stored: List<SmartShelf>): List<SmartShelf> =
    stored + SmartShelfPresets.all.filter { preset -> stored.none { it.id == preset.id } }

/**
 * The shelves the library shows on [date]: the owner's own all year, presets while they run.
 * The owner's own come first, in the order they were made.
 */
fun libraryShelves(stored: List<SmartShelf>, date: LocalDate): List<SmartShelf> =
    mergedSmartShelves(stored).filter { it.inLibrary && (!it.preset || it.period.isActive(date)) }
        .sortedBy { if (it.preset) 1 else 0 }

/**
 * The shelves that get a row on Home on [date]: those asked for, while their period runs. A shelf
 * without a period is there all year, so a row on Home never lingers after its season.
 */
fun homeShelves(stored: List<SmartShelf>, date: LocalDate): List<SmartShelf> =
    mergedSmartShelves(stored).filter { it.onHome && it.hasRule && it.period.isActive(date) }
        .sortedBy { if (it.preset) 1 else 0 }

/** The shelves the side menu carries on [date]: those asked for, while their period runs. */
fun menuShelves(stored: List<SmartShelf>, date: LocalDate): List<SmartShelf> =
    mergedSmartShelves(stored).filter { it.inMenu && it.period.isActive(date) }

/** Replace a shelf by id, or add it at the end of the owner's own. */
fun List<SmartShelf>.withShelf(shelf: SmartShelf): List<SmartShelf> =
    if (any { it.id == shelf.id }) map { if (it.id == shelf.id) shelf else it } else this + shelf

/** The genres and tags a profile's films and series carry, for building a shelf by choosing. */
data class CatalogueFacets(val genres: List<String> = emptyList(), val tags: List<String> = emptyList())

/**
 * The shelf builder's state: the shelf as it is being made, what the server offers to choose
 * from, and what the shelf would hold right now.
 */
data class SmartShelfEditor(
    val draft: SmartShelf,
    val isNew: Boolean,
    val facets: CatalogueFacets = CatalogueFacets(),
    val facetsLoading: Boolean = false,
    /** The server did not answer, which is different from a server with no genres. */
    val facetsFailed: Boolean = false,
    /** What the server or the network said, for the line under the message; null while nothing failed. */
    val facetsError: String? = null,
    val previewMovies: Int? = null,
    val previewSeries: Int? = null,
    val previewCapped: Boolean = false,
    val previewLoading: Boolean = false,
)

/**
 * Genre names that mean the same thing. A preset asks for both «Horror» and «Skrekk», because a
 * library's metadata language decides which the server uses; on screen they are one genre.
 */
private val EQUIVALENT_GENRES = listOf(
    setOf("horror", "skrekk", "grøssar", "grøsser"),
    setOf("crime", "krim"),
    setOf("mystery", "mysterium"),
    setOf("comedy", "komedie"),
    setOf("family", "familie"),
)

fun sameGenre(a: String, b: String): Boolean = a.equals(b, ignoreCase = true) ||
    EQUIVALENT_GENRES.any { group -> a.lowercase() in group && b.lowercase() in group }

/** Each genre once, the first spelling kept. */
fun distinctGenres(genres: List<String>): List<String> =
    genres.fold(emptyList()) { kept, genre -> if (kept.any { sameGenre(it, genre) }) kept else kept + genre }
