package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.localization.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The media line says what a title is. It must not say it in one language.
 *
 * "Film · 2024", "1 sesong", "149 min" were composed by the parser, because the parser is what
 * knows the numbers. It does not know the reader, so those lines stayed nynorsk in an English
 * installation — and worse, other code then read them back: a library grid filtered the type word
 * out by name, a calendar decided a release was physical by comparing against the string "Fysisk
 * utgjeving", and a season header matched "Sesong 3". Each of those was correct in exactly one
 * language and silently wrong in the others.
 *
 * So the parser carries decisions. These tests hold that line: nothing in a parsed fact may be a
 * sentence Spole chose, and anything that came from a server passes through untouched.
 */
class MediaLineTest {

    @Test fun theTypeWordIsNotAFact() {
        // It is derivable from mediaType, which every one of these rows already carries. Storing it
        // as well is what forced the grid to filter it back out by name.
        val movie = ServicePayloadParser.libraryItems(
            """{"Items":[{"Id":"m","Name":"Ein film","Type":"Movie","ProductionYear":2024}]}""",
        ).single()

        assertEquals("Movie", movie.mediaType)
        assertEquals(listOf(LocalizedText.raw("2024")), movie.facts)
    }

    @Test fun aRuntimeIsANumberNotAPhrase() {
        val details = ServicePayloadParser.mediaDetails("""{"title":"Ein film","runtime":149}""")

        assertEquals(listOf(LocalizedText(R.string.media_minutes, 149)), details.facts)
    }

    @Test fun countsCarryTheirQuantitySoAPluralRuleCanApply() {
        val one = ServicePayloadParser.mediaDetails("""{"name":"Pilot","numberOfSeasons":1,"numberOfEpisodes":1}""")
        val many = ServicePayloadParser.mediaDetails("""{"name":"Silo","numberOfSeasons":2,"numberOfEpisodes":20}""")

        assertEquals(listOf(1, 1), one.facts.map { it.quantity })
        assertEquals(listOf(2, 20), many.facts.map { it.quantity })
        // Same decision either way: the singular and plural wording is the resource's business.
        assertEquals(one.facts.map { it.resId }, many.facts.map { it.resId })
    }

    @Test fun serverTextPassesThroughAsItCame() {
        val details = ServicePayloadParser.mediaDetails(
            """{"name":"Silo","status":"A status nobody mapped","networks":[{"name":"Apple TV+"}],
               "numberOfSeasons":1,"voteAverage":8.4}""",
        )

        val literals = details.facts.mapNotNull { it.literal }
        assertTrue(literals.contains("A status nobody mapped"))
        assertTrue(literals.contains("Apple TV+"))
        assertTrue(literals.contains("★ 8.4"))
    }

    @Test fun aDiscReleaseIsAFlagNotASentence() {
        // The calendar used to decide this by comparing the availability text against the nynorsk
        // words "Fysisk utgjeving", which made every other language a digital release.
        val physical = ServicePayloadParser.upcoming(
            """[{"id":1,"title":"Ein film","physicalRelease":"2026-09-12T00:00:00Z"}]""",
            ServiceKind.RADARR,
        ).single()
        val digital = ServicePayloadParser.upcoming(
            """[{"id":2,"title":"Ein annan","digitalRelease":"2026-09-12T00:00:00Z"}]""",
            ServiceKind.RADARR,
        ).single()

        assertTrue(physical.physicalRelease)
        assertFalse(digital.physicalRelease)
        assertTrue(physical.facts.any { it.resId == R.string.release_physical })
        assertTrue(digital.facts.any { it.resId == R.string.release_digital })
    }

    @Test fun theDownloadQueueSaysWhichStateItIsIn() {
        val downloading = ServicePayloadParser.queue(
            """[{"id":"1","title":"Ein film","status":"downloading","size":100,"sizeleft":25}]""",
            ServiceKind.RADARR,
        ).single()

        assertEquals(R.string.queue_downloading_percent, downloading.status.resId)
        assertEquals(listOf<Any>(75), downloading.status.args)
    }

    @Test fun nothingAParserProducedIsAHardcodedSentence() {
        // The rule, stated once. A fact is either a resource this app owns or text a server sent;
        // there is no third kind, and a literal that is not server text is the bug this guards.
        val payload = """{"Items":[{"Id":"e","Name":"Ein episode","Type":"Episode","ParentIndexNumber":7,
            "IndexNumber":1,"ProductionYear":2026,"RunTimeTicks":25800000000,"OfficialRating":"12",
            "CommunityRating":8.3}]}"""

        val facts = ServicePayloadParser.libraryItems(payload).single().facts

        assertEquals(
            listOf(
                LocalizedText.raw("S07 E01"),
                LocalizedText.raw("2026"),
                LocalizedText(R.string.media_minutes, 43),
                LocalizedText.raw("12"),
                LocalizedText.raw("★ 8.3"),
            ),
            facts,
        )
        for (fact in facts) {
            assertTrue("every fact is a resource or server text", fact.resId != 0 || fact.literal != null)
        }
    }

    @Test fun aSeasonNameIsADecisionNotAString() {
        // "Sesong 3" used to be written here and compared against, by name, three layers away.
        val seasons = ServicePayloadParser.mediaDetails(
            """{"name":"Silo","seasons":[{"seasonNumber":0,"episodeCount":2},{"seasonNumber":3,"episodeCount":8}]}""",
        ).seasons

        assertEquals(R.string.media_specials, seasons.first { it.number == 0 }.name.resId)
        val third = seasons.first { it.number == 3 }.name
        assertEquals(R.string.media_season_number, third.resId)
        assertEquals(listOf<Any>(3), third.args)
    }

    @Test fun anActivityDayGroupIsNotItsHeading() {
        // The bucket used to be the literal string "I GÅR", which meant grouping and rendering were
        // the same decision — and an English feed grouped everything as "Earlier".
        val yesterday = app.reelstack.data.model.activityDayGroup(
            app.reelstack.data.model.ActivityEvent(
                id = "e", title = "Ein film", detail = LocalizedText.raw(""),
                time = LocalizedText(R.string.time_yesterday), artworkRes = 0,
            ),
        )

        assertEquals(R.string.activity_group_yesterday, yesterday)
    }
}
