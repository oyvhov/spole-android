package app.reelstack.data.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartShelfTest {
    private fun day(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)

    @Test fun aShelfSurvivesBeingStored() {
        val shelf = SmartShelf("u-1", name = "Hundefilmar", icon = SmartShelfIcon.HEART, tags = listOf("dog", "puppy"),
            genres = listOf("Family"), kinds = SmartShelfKinds.MOVIES, unwatched = true, period = SmartShelfPeriod.EASTER, inMenu = true,
            matchAll = true, inLibrary = false, onHome = true)
        assertEquals(listOf(shelf, SmartShelfPresets.halloween), SmartShelf.decodeAll(SmartShelf.encodeAll(listOf(shelf, SmartShelfPresets.halloween))))
    }

    @Test fun aDamagedStoreLosesOnlyWhatIsDamaged() {
        assertEquals(emptyList<SmartShelf>(), SmartShelf.decodeAll("not json"))
        assertEquals(emptyList<SmartShelf>(), SmartShelf.decodeAll(null))
        val read = SmartShelf.decodeAll("""[{"name":"no id"},{"id":"u-2","icon":"GONE","kinds":"?","tags":[" a ","a",""]}]""")
        assertEquals(listOf(SmartShelf("u-2", tags = listOf("a"))), read)
    }

    @Test fun aPathNamesAShelfAndNothingElse() {
        assertEquals("halloween", SmartShelf.idOfPath(SmartShelfPresets.halloween.pathId))
        assertNull(SmartShelf.idOfPath("f137a2dd21bbc1b99aa5c0f6bf02a805"))
        assertNull(SmartShelf.idOfPath(SmartShelf.PATH_PREFIX))
        assertNull(SmartShelf.idOfPath(null))
    }

    /** Easter Sunday by the Gregorian computus, checked against the almanac. */
    @Test fun easterFallsWhereTheAlmanacSays() {
        assertEquals(day(4, 5, 2026), easterSunday(2026))
        assertEquals(day(3, 28, 2027), easterSunday(2027))
        assertEquals(day(4, 16, 2028), easterSunday(2028))
        assertEquals(day(4, 20, 2025), easterSunday(2025))
    }

    @Test fun easterRunsFromPalmSundayToEasterMonday() {
        assertFalse(SmartShelfPeriod.EASTER.isActive(day(3, 28)))
        assertTrue(SmartShelfPeriod.EASTER.isActive(day(3, 29)))
        assertTrue(SmartShelfPeriod.EASTER.isActive(day(4, 6)))
        assertFalse(SmartShelfPeriod.EASTER.isActive(day(4, 7)))
    }

    @Test fun adventEndsOnChristmasEveAndChristmasOnTheTwentiethDay() {
        assertTrue(SmartShelfPeriod.ADVENT.isActive(day(12, 24)))
        assertFalse(SmartShelfPeriod.ADVENT.isActive(day(12, 25)))
        assertTrue(SmartShelfPeriod.CHRISTMAS.isActive(day(1, 13, 2027)))
        assertTrue(SmartShelfPeriod.HALLOWEEN.isActive(day(10, 20)))
        assertTrue(SmartShelfPeriod.ALWAYS.isActive(day(7, 1)))
    }

    /** The owner's own shelves stay in the library all year; presets come forward in their season. */
    @Test fun theLibraryShowsOwnShelvesAlwaysAndPresetsInSeason() {
        val own = SmartShelf("u-1", name = "Krim", genres = listOf("Crime"), period = SmartShelfPeriod.CHRISTMAS)
        assertEquals(listOf("u-1"), libraryShelves(listOf(own), day(7, 1)).map { it.id })
        assertEquals(listOf("u-1", "halloween"), libraryShelves(listOf(own), day(10, 25)).map { it.id })
        assertEquals(listOf("u-1", "christmas", "christmas-calendar"), libraryShelves(listOf(own), day(12, 10)).map { it.id })
        assertEquals(listOf("easter-crime"), libraryShelves(emptyList(), day(4, 2)).map { it.id })
    }

    @Test fun theMenuCarriesOnlyWhatWasAskedForWhileItRuns() {
        val always = SmartShelf("u-1", name = "Favoritt", tags = listOf("dog"), inMenu = true)
        val seasonal = SmartShelf("u-2", name = "Jul", tags = listOf("christmas"), inMenu = true, period = SmartShelfPeriod.CHRISTMAS)
        val notAsked = SmartShelf("u-3", name = "Anna", tags = listOf("cat"))
        assertEquals(listOf("u-1"), menuShelves(listOf(always, seasonal, notAsked), day(7, 1)).map { it.id })
        assertEquals(listOf("u-1", "u-2"), menuShelves(listOf(always, seasonal, notAsked), day(12, 5)).map { it.id })
    }

    /** A changed preset replaces the original; resetting it is removing the change. */
    @Test fun aChangedPresetTakesThePresetsPlace() {
        val changed = SmartShelfPresets.halloween.copy(tags = listOf("halloween", "witch"))
        val merged = mergedSmartShelves(listOf(changed))
        assertEquals(1, merged.count { it.id == "halloween" })
        assertEquals(listOf("halloween", "witch"), merged.first { it.id == "halloween" }.tags)
        assertEquals(SmartShelfPresets.all.size, merged.size)
        assertEquals(listOf(changed.copy(name = "Skrekk")), listOf(changed).withShelf(changed.copy(name = "Skrekk")))
        assertEquals(listOf(changed, SmartShelf("u-9")), listOf(changed).withShelf(SmartShelf("u-9")))
    }

    /** «Horror» and «Skrekk» are one genre on screen, whichever the server happens to use. */
    @Test fun spellingsOfTheSameGenreCountOnce() {
        assertTrue(sameGenre("Horror", "skrekk"))
        assertTrue(sameGenre("Krim", "Crime"))
        assertFalse(sameGenre("Horror", "Thriller"))
        assertEquals(listOf("Horror", "Drama"), distinctGenres(listOf("Horror", "Drama", "Skrekk", "horror")))
    }

    /** «Both a tag and a genre» needs both sides; with one empty it is the plain rule. */
    @Test fun bothOnlyCountsWithTagsAndGenres() {
        val both = SmartShelf("u-1", tags = listOf("halloween"), genres = listOf("Horror"), matchAll = true)
        assertTrue(both.requiresBoth)
        assertFalse(both.copy(genres = emptyList()).requiresBoth)
        assertFalse(both.copy(matchAll = false).requiresBoth)
        assertTrue(both.copy(matchAll = false).combinesBoth)
        // A child's Halloween loses horror, and with it the «and»: the tagged titles remain.
        assertFalse(both.forViewer(child = true).requiresBoth)
        assertFalse(SmartShelf.decode(kotlinx.serialization.json.buildJsonObject { put("id", kotlinx.serialization.json.JsonPrimitive("u-2")) })!!.matchAll)
    }

    /** Home gets a row only for a shelf that asked for one, and only while its period runs. */
    @Test fun homeRowsComeWhenAskedForAndGoWithTheSeason() {
        val own = SmartShelf("u-1", tags = listOf("dog"), onHome = true)
        val notAsked = SmartShelf("u-2", tags = listOf("cat"))
        val seasonal = SmartShelf("u-3", tags = listOf("snow"), onHome = true, period = SmartShelfPeriod.CHRISTMAS)
        val stored = listOf(own, notAsked, seasonal)
        assertEquals(listOf("u-1"), homeShelves(stored, day(7, 1)).map { it.id })
        assertEquals(listOf("u-1", "u-3", "christmas", "christmas-calendar"), homeShelves(stored, day(12, 10)).map { it.id })
        assertEquals(listOf("halloween"), homeShelves(emptyList(), day(10, 25)).map { it.id })
        assertTrue(homeShelves(emptyList(), day(7, 1)).isEmpty())
        // A shelf with no rule has nothing to ask for.
        assertTrue(homeShelves(listOf(SmartShelf("u-4", onHome = true)), day(7, 1)).isEmpty())
    }

    @Test fun placementIsChosenPerShelf() {
        val offLibrary = SmartShelf("u-1", tags = listOf("dog"), inLibrary = false)
        assertTrue(libraryShelves(listOf(offLibrary), day(7, 1)).isEmpty())
        assertEquals(listOf("u-1"), libraryShelves(listOf(offLibrary.copy(inLibrary = true)), day(7, 1)).map { it.id })
        // The preset's own choice survives a shelf stored before Home rows existed.
        val old = SmartShelf.decodeAll("""[{"id":"halloween","preset":true,"tags":["halloween"]},{"id":"u-9","tags":["dog"]}]""")
        assertTrue(old.first { it.id == "halloween" }.onHome)
        assertFalse(old.first { it.id == "u-9" }.onHome)
        assertTrue(old.all { it.inLibrary })
    }

    @Test fun aChildsShelfLeavesHorrorOutButKeepsTheRest() {
        val shelf = SmartShelf("u-1", tags = listOf("halloween"), genres = listOf("Horror", "Skrekk", "Family"))
        assertEquals(listOf("Family"), shelf.forViewer(child = true).genres)
        assertEquals(shelf, shelf.forViewer(child = false))
    }
}
