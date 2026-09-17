package app.reelstack.ui

import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

/**
 * Demo titles belong to an app nobody has connected anything to yet.
 *
 * The rule is in `AI_INSTRUCTIONS.md` and it was previously written out at twelve call sites, each
 * spelling it slightly differently. It matters because the failure mode is not cosmetic: a
 * household whose Jellyfin is unreachable must see that it is unreachable, not five films they do
 * not own and cannot play.
 */
class DemoContentRuleTest {
    private val demo = { listOf("a-made-up-title") }

    @Test fun anAppWithNothingSetUpMayShowDemoTitles() {
        assertEquals(listOf("a-made-up-title"), demoContent(emptySet(), servedByRealService = false, demo = demo))
    }

    @Test fun aConfiguredServiceNeverFallsBackToDemoTitles() {
        // The service is configured but this shelf has no data yet — an empty shelf, not a fake one.
        assertEquals(emptyList<String>(), demoContent(setOf(ServiceKind.JELLYFIN), servedByRealService = false, demo = demo))
    }

    @Test fun aShelfAlreadyServedByARealServiceIsNeverFilledWithDemoTitles() {
        assertEquals(emptyList<String>(), demoContent(emptySet(), servedByRealService = true, demo = demo))
        assertEquals(emptyList<String>(), demoContent(setOf(ServiceKind.SEERR), servedByRealService = true, demo = demo))
    }

    @Test fun anyConfiguredServiceAtAllIsEnoughToSuppressDemoTitlesEverywhere() {
        // Seerr alone is not a media server, but it still means the household has set Spole up —
        // so the film shelves stay empty rather than inventing a library.
        assertEquals(emptyList<String>(), demoContent(setOf(ServiceKind.SEERR), servedByRealService = false, demo = demo))
        assertEquals(emptyList<String>(), demoContent(setOf(ServiceKind.RADARR), servedByRealService = false, demo = demo))
    }
}
