package app.reelstack.data.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartShelfTest {
    private fun day(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)

    private val jellyfin = ServiceConnection(
        kind = ServiceKind.JELLYFIN,
        name = "Jellyfin",
        baseUrl = "https://media.example",
        token = "fixture-token",
        userId = "viewer-1",
        state = ConnectionState.CONNECTED,
    )

    @Test fun periodExplanationMatchesPresetAndOwnLibraryVisibility() {
        val before = day(10, 15)
        val active = day(10, 25)
        val preset = SmartShelfPresets.halloween.copy(onHome = false, inMenu = false)
        assertTrue(preset.waitsForPeriod(before))
        assertFalse(preset.waitsForPeriod(active))
        val own = preset.copy(id = "own", preset = false)
        assertFalse(own.waitsForPeriod(before))
        assertTrue(own.copy(onHome = true).waitsForPeriod(before))
        assertTrue(own.copy(inMenu = true).waitsForPeriod(before))
        assertFalse(preset.copy(inLibrary = false).waitsForPeriod(before))
        assertFalse(own.copy(period = SmartShelfPeriod.ALWAYS, onHome = true).waitsForPeriod(before))
    }

    @Test fun aShelfSurvivesBeingStored() {
        val shelf = SmartShelf("u-1", name = "Hundefilmar", icon = SmartShelfIcon.HEART, tags = listOf("dog", "puppy"),
            genres = listOf("Family"), kinds = SmartShelfKinds.MOVIES, unwatched = true, period = SmartShelfPeriod.EASTER, inMenu = true,
            matchAll = true, inLibrary = false, onHome = true, sort = SmartShelfSort.RATING)
        assertEquals(listOf(shelf, SmartShelfPresets.halloween), SmartShelf.decodeAll(SmartShelf.encodeAll(listOf(shelf, SmartShelfPresets.halloween))))
    }

    @Test fun storedScopesKeepAllNoAndSelectedLibrariesDistinct() {
        val shelves = listOf(
            SmartShelf("all", tags = listOf("dog"), scope = SmartShelfScope.of(jellyfin)),
            SmartShelf("none", tags = listOf("cat"), scope = SmartShelfScope.of(jellyfin, emptySet())),
            SmartShelf("selected", tags = listOf("bird"),
                scope = SmartShelfScope.of(jellyfin, setOf("films", "series"))),
            SmartShelf("legacy", tags = listOf("fish")),
        ) + SmartShelfPresets.all

        val restored = SmartShelf.decodeAll(SmartShelf.encodeAll(shelves))

        assertEquals(shelves, restored)
        assertNull(restored.first { it.id == "all" }.scope!!.libraryIds)
        assertEquals(emptySet<String>(), restored.first { it.id == "none" }.scope!!.libraryIds)
        assertEquals(setOf("films", "series"), restored.first { it.id == "selected" }.scope!!.libraryIds)
        assertNull(restored.first { it.id == "legacy" }.scope)
        assertTrue(restored.filter { it.preset }.all { it.scope == null })
    }

    @Test fun olderShelvesKeepTheirRulesAndRemainAvailableOnBothMediaServices() {
        val old = SmartShelf.decodeAll("""[{"id":"u-old","name":"Hund","tags":["dog"],"inLibrary":false,"onHome":true},{"id":"halloween","preset":true,"tags":["witch"]}]""")
        assertEquals(SmartShelf("u-old", name = "Hund", tags = listOf("dog"), inLibrary = false, onHome = true), old.first())
        assertEquals(listOf("witch"), old.last().tags)
        assertTrue(old.last().onHome)
        val shelves = mergedSmartShelves(old)
        val emby = jellyfin.copy(kind = ServiceKind.EMBY, baseUrl = "https://emby.example", userId = "viewer-2")

        assertEquals(SmartShelfPresets.all.size + 1, shelves.size)
        shelves.forEach { shelf ->
            assertNull(shelf.scope)
            assertTrue(shelf.appliesTo(jellyfin))
            assertTrue(shelf.appliesTo(emby))
        }
        assertEquals(old, SmartShelf.decodeAll(SmartShelf.encodeAll(old)))
    }

    @Test fun scopedShelfKeepsItsAccountAcrossFailoverAndTokenRotation() {
        val original = jellyfin.copy(
            baseUrl = "https://lan.example",
            alternateUrl = "https://remote.example",
            identityUrl = "https://media.example/",
        )
        val failover = original.copy(
            name = "Renamed service",
            baseUrl = original.alternateUrl,
            alternateUrl = original.baseUrl,
            token = "rotated-fixture-token",
        )
        val scope = SmartShelfScope.of(original, setOf("films"))
        val shelf = SmartShelf("u-scoped", tags = listOf("dog"), scope = scope)

        assertEquals(smartShelfAccountKey(original), scope.accountKey)
        assertEquals(smartShelfAccountKey(original), smartShelfAccountKey(failover))
        assertEquals(scope, SmartShelfScope.of(failover, setOf("films")))
        assertTrue(scope.matches(failover))
        assertTrue(shelf.appliesTo(failover))
    }

    @Test fun accountKeyUsesBaseUrlUntilStableIdentityExistsAndIgnoresTrailingSlash() {
        val beforeIdentity = jellyfin.copy(baseUrl = "https://media.example/")
        val withoutSlash = beforeIdentity.copy(baseUrl = "https://media.example")
        val afterIdentity = beforeIdentity.copy(
            baseUrl = "https://alternate.example",
            identityUrl = beforeIdentity.baseUrl,
        )

        assertEquals(smartShelfAccountKey(beforeIdentity), smartShelfAccountKey(withoutSlash))
        assertEquals(smartShelfAccountKey(beforeIdentity), smartShelfAccountKey(afterIdentity))
        assertTrue(SmartShelfScope.of(beforeIdentity).matches(afterIdentity))
    }

    @Test fun scopedShelfRejectsAnotherServiceServerOrUser() {
        val scope = SmartShelfScope.of(jellyfin)
        val shelf = SmartShelf("u-scoped", tags = listOf("dog"), scope = scope)
        val others = listOf(
            jellyfin.copy(kind = ServiceKind.EMBY),
            jellyfin.copy(baseUrl = "https://other.example"),
            jellyfin.copy(userId = "viewer-2"),
        )

        assertEquals(ServiceKind.JELLYFIN, scope.source)
        assertTrue(scope.matches(jellyfin))
        assertTrue(shelf.appliesTo(jellyfin))
        others.forEach { other ->
            assertFalse(scope.matches(other))
            assertFalse(shelf.appliesTo(other))
            assertFalse(smartShelfAccountKey(jellyfin) == smartShelfAccountKey(other))
        }
        // The explicit service discriminator is checked even if an account key is reused.
        assertFalse(scope.copy(source = ServiceKind.EMBY).matches(jellyfin))
        assertFalse(SmartShelfScope.of(jellyfin.copy(userId = "")).matches(jellyfin.copy(userId = "")))
    }

    @Test fun malformedExplicitScopeNeverTurnsIntoAGlobalShelf() {
        val global = SmartShelf("legacy", tags = listOf("dog"))
        val damagedScopes = listOf(
            """{"source":"UNKNOWN","accountKey":"key"}""",
            """{"source":"JELLYFIN","accountKey":""}""",
            """{"source":"JELLYFIN","accountKey":"key","libraryIds":"all"}""",
        )
        damagedScopes.forEach { scope ->
            val encoded = """[${global.encode()},{"id":"damaged","tags":["dog"],"scope":$scope}]"""
            assertEquals(listOf(global), SmartShelf.decodeAll(encoded))
        }
    }

    @Test fun editingOneScopedShelfKeepsTheOtherAccountsShelf() {
        val own = SmartShelf("u-own", tags = listOf("dog"), scope = SmartShelfScope.of(jellyfin, setOf("films")))
        val other = SmartShelf("u-other", tags = listOf("cat"),
            scope = SmartShelfScope.of(jellyfin.copy(userId = "viewer-2"), emptySet()))
        val edited = own.copy(name = "Hund", tags = listOf("dog", "puppy"), onHome = true)

        val stored = listOf(own, other).withShelf(edited)
        val restored = SmartShelf.decodeAll(SmartShelf.encodeAll(stored))

        assertEquals(listOf(edited, other), restored)
        assertEquals(own.scope, restored.first().scope)
        assertEquals(other, restored.last())
        assertEquals(emptySet<String>(), restored.last().scope!!.libraryIds)
    }

    @Test fun childViewerRulesKeepTheSameAccountAndLibraryScope() {
        val scope = SmartShelfScope.of(jellyfin, setOf("family"))
        val shelf = SmartShelf("u-family", tags = listOf("halloween"), genres = listOf("Horror", "Family"), scope = scope)

        val child = shelf.forViewer(child = true)

        assertEquals(listOf("Family"), child.genres)
        assertEquals(scope, child.scope)
        assertTrue(child.appliesTo(jellyfin))
        assertFalse(child.appliesTo(jellyfin.copy(userId = "another-child")))
        assertEquals(shelf, shelf.forViewer(child = false))
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

    /** A period that has not begun says when it does, so the builder can tell the owner. */
    @Test fun aPeriodKnowsWhenItNextBegins() {
        assertEquals(day(10, 20), SmartShelfPeriod.HALLOWEEN.nextStart(day(10, 9)))
        assertEquals(day(10, 25), SmartShelfPeriod.HALLOWEEN.nextStart(day(10, 25)))
        assertEquals(day(12, 1), SmartShelfPeriod.ADVENT.nextStart(day(10, 9)))
        assertEquals(day(3, 21, 2027), SmartShelfPeriod.EASTER.nextStart(day(6, 1)))
        assertNull(SmartShelfPeriod.ALWAYS.nextStart(day(6, 1)))
    }

    /** The builder writes a period's dates on its chip, so the run must be whole: start behind today when it is under way. */
    @Test fun aPeriodKnowsItsWholeRun() {
        val halloween = SmartShelfPeriod.HALLOWEEN.run(LocalDate.of(2026, 10, 9))!!
        assertEquals(LocalDate.of(2026, 10, 20), halloween.start)
        assertEquals(LocalDate.of(2026, 11, 1), halloween.endInclusive)
        val midway = SmartShelfPeriod.CHRISTMAS.run(LocalDate.of(2026, 12, 25))!!
        assertEquals(LocalDate.of(2026, 12, 1), midway.start)
        assertEquals(LocalDate.of(2027, 1, 13), midway.endInclusive)
        val easter = SmartShelfPeriod.EASTER.run(LocalDate.of(2027, 1, 1))!!
        assertEquals(LocalDate.of(2027, 3, 21), easter.start)
        assertEquals(LocalDate.of(2027, 3, 29), easter.endInclusive)
        assertNull(SmartShelfPeriod.ALWAYS.run(LocalDate.of(2026, 10, 9)))
    }

    @Test fun aChildsShelfLeavesHorrorOutButKeepsTheRest() {
        val shelf = SmartShelf("u-1", tags = listOf("halloween"), genres = listOf("Horror", "Skrekk", "Family"))
        assertEquals(listOf("Family"), shelf.forViewer(child = true).genres)
        assertEquals(shelf, shelf.forViewer(child = false))
    }
}
