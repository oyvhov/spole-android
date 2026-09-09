package app.reelstack

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.*
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalendarAndSheetTest {
    @get:Rule val rule = createComposeRule()
    private fun release(id: String, source: ServiceKind, offset: Long = 0) = UpcomingMedia(
        id = id, title = id, subtitle = if (source == ServiceKind.SONARR) "S03 E10 · Ny episode" else "Film · 2026",
        dateLabel = "I dag", airDateEpochMillis = LocalDate.now().plusDays(offset).atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        artworkRes = R.drawable.media_placeholder, source = source,
    )

    @Test fun englishCalendarUsesPluralCountsAndKeepsSelectedDayAcrossLanguageChange() {
        val language = mutableStateOf(app.reelstack.localization.AppLanguage.ENGLISH)
        rule.setContent {
            val localized = app.reelstack.localization.AppLanguages.wrap(
                androidx.compose.ui.platform.LocalContext.current, language.value)
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localized,
                androidx.compose.ui.platform.LocalConfiguration provides localized.resources.configuration,
            ) {
                ReelstackTheme { UpcomingCalendarSheet(listOf(release("Episode", ServiceKind.SONARR, 1)), {}, {}) }
            }
        }
        rule.onNodeWithContentDescription("Close calendar").assertIsDisplayed()
        rule.onNodeWithText("1 release").assertIsDisplayed()
        rule.onNodeWithText("Episodes").performClick()
        rule.onNodeWithTag("calendar-day-1").performClick().assertIsSelected()
        rule.onNodeWithText("1 release this day").assertIsDisplayed()
        rule.onNodeWithText("Tomorrow").assertIsDisplayed()
        rule.runOnIdle { language.value = app.reelstack.localization.AppLanguage.NYNORSK }
        rule.onNodeWithContentDescription("Lukk kalenderen").assertIsDisplayed()
        rule.onNodeWithTag("calendar-day-1").assertIsSelected()
        rule.onNodeWithText("1 utgjeving denne dagen").assertIsDisplayed()
        rule.onNodeWithText("I morgon").assertIsDisplayed()
    }

    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test fun tabletCalendarAtDoubleFontKeepsDateTextAndCloseVisible() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(androidx.compose.ui.unit.DpSize(
                androidx.compose.ui.unit.Dp(1100f), androidx.compose.ui.unit.Dp(900f)))) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                        androidx.compose.ui.platform.LocalDensity.current.density, 2f)) {
                    ReelstackTheme { UpcomingCalendarSheet(emptyList(), {}, {}) }
                }
            }
        }
        rule.onNodeWithContentDescription("Lukk kalenderen").assertIsDisplayed()
        rule.onNodeWithTag("calendar-day-0").performClick().assertIsSelected()
        rule.onNodeWithText("Alle dagar").assertIsDisplayed().performClick()
        rule.onNodeWithText("Ingen planlagde utgjevingar").assertIsDisplayed()
        val dateTexts = rule.onAllNodes(hasAnyAncestor(hasTestTag("calendar-day-0")) and
            SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
        org.junit.Assert.assertTrue(dateTexts.fetchSemanticsNodes().isNotEmpty())
        repeat(dateTexts.fetchSemanticsNodes().size) { index ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            dateTexts[index].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.forEach { org.junit.Assert.assertFalse("Date text clipped: ${it.layoutInput.text} ${it.size}", it.hasVisualOverflow) }
        }
    }

    @Test fun calendarFiltersDatesTypesAndOpensExactEpisode() {
        var opened = ""
        rule.setContent {
            ReelstackTheme {
                UpcomingCalendarSheet(
                    listOf(release("Film A", ServiceKind.RADARR), release("Episode A", ServiceKind.SONARR), release("Episode B", ServiceKind.SONARR, 1)),
                    { opened = it }, {},
                )
            }
        }
        rule.onNodeWithText("Filmar").performClick()
        rule.onNodeWithText("Film A").assertIsDisplayed()
        rule.onNodeWithText("Episode A").assertDoesNotExist()
        rule.onNodeWithText("Episodar").performClick()
        rule.onNodeWithText("Episode A").performClick()
        assertEquals("Episode A", opened)
        rule.onNodeWithTag("calendar-day-1").performClick()
        rule.onNodeWithText("Episode A").assertDoesNotExist()
        rule.onNodeWithText("Episode B").assertIsDisplayed()
        rule.onNodeWithTag("calendar-day-2").performClick()
        rule.onNodeWithText("Ingen planlagde utgjevingar").assertIsDisplayed()
        rule.onNodeWithText("Alle dagar").performClick()
        rule.onNodeWithText("Episode A").assertIsDisplayed()
    }

    @Test fun calendarExcludesPastAndBeyondWindowAndCloses() {
        var closed = false
        rule.setContent {
            ReelstackTheme {
                UpcomingCalendarSheet(listOf(release("Old", ServiceKind.RADARR, -1), release("Future", ServiceKind.SONARR, 28)), {}, { closed = true })
            }
        }
        rule.onNodeWithText("Ingen planlagde utgjevingar").assertIsDisplayed()
        rule.onNodeWithText("Old").assertDoesNotExist()
        rule.onNodeWithText("Future").assertDoesNotExist()
        rule.onNodeWithContentDescription("Lukk kalenderen").performClick()
        assertEquals(true, closed)
    }

    @Test fun asyncDetailsDoNotMoveSheetViewport() {
        val details = ContentDetails(key = "movie", title = "Film", eyebrow = "Radarr", subtitle = "2026", artworkRes = R.drawable.media_placeholder, mediaType = "movie", loading = true)
        val state = mutableStateOf(ReelstackUiState(activeSheet = AppSheet.TitleDetails("movie"), contentDetails = details))
        rule.setContent {
            ReelstackTheme {
                ReelstackSheets(
                    state = state.value, connectionDraft = null, onDismiss = {}, onPlaybackToggle = {},
                    onConnectionNameChange = {}, onConnectionUrlChange = {}, onConnectionTokenChange = {},
                    onConnectionUserIdChange = {}, onConnectionAuthModeChange = {}, onConnectionUsernameChange = {},
                    onConnectionPasswordChange = {}, onTestAndSaveConnection = {}, onRemoveConnection = {},
                    onAddMedia = {}, onUpcomingClick = {},
                )
            }
        }
        rule.waitForIdle()
        val before = rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { state.value = state.value.copy(contentDetails = details.copy(loading = false, overview = "Ein lang omtale med mykje informasjon. ".repeat(50))) }
        rule.waitForIdle()
        val after = rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
        rule.onNodeWithTag("overview-expand").performScrollTo().performClick()
        rule.onNodeWithText("Vis mindre").performScrollTo().assertIsDisplayed()
        assertEquals(before, rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNodeWithText("Ein lang omtale med mykje informasjon. ".repeat(50))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        org.junit.Assert.assertFalse(layouts.single().hasVisualOverflow)
        rule.runOnIdle { state.value = state.value.copy(contentDetails = details.copy(loading = false, overview = "Kort omtale.")) }
        rule.waitForIdle()
        assertEquals(before, rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun largeTextKeepsCompleteSynopsisReadableWithoutResizingSheet() {
        val overview = "Ei forteljing som skal kunne lesast i sin heilskap. ".repeat(8)
        rule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    androidx.compose.ui.platform.LocalDensity.current.density, 2f),
            ) {
                ReelstackTheme {
                    ReelstackSheets(
                        state = ReelstackUiState(activeSheet = AppSheet.TitleDetails("large"),
                            contentDetails = ContentDetails("large", "Ein film med ein lang tittel", "Seerr", "Film",
                                artworkRes = R.drawable.media_placeholder, mediaType = "movie", overview = overview,
                                facts = listOf("Film", "2026", "120 min", "★ 8,2"), genres = listOf("Drama", "Eventyr"))),
                        connectionDraft = null, onDismiss = {}, onPlaybackToggle = {},
                        onConnectionNameChange = {}, onConnectionUrlChange = {}, onConnectionTokenChange = {},
                        onConnectionUserIdChange = {}, onConnectionAuthModeChange = {}, onConnectionUsernameChange = {},
                        onConnectionPasswordChange = {}, onTestAndSaveConnection = {}, onRemoveConnection = {},
                        onAddMedia = {}, onUpcomingClick = {},
                    )
                }
            }
        }
        val before = rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("overview-expand").performScrollTo().performClick()
        rule.onNodeWithText("Vis mindre").performScrollTo().assertIsDisplayed()
        assertEquals(before, rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun calendarRetainsDateAndFilterAfterTitleDetailsAndHasExplicitClose() {
        val state = mutableStateOf(ReelstackUiState(
            activeSheet = AppSheet.UpcomingCalendar,
            upcoming = listOf(release("Episode A", ServiceKind.SONARR), release("Episode B", ServiceKind.SONARR, 1), release("Film B", ServiceKind.RADARR, 1)),
        ))
        var closed = false
        rule.setContent {
            ReelstackTheme {
                ReelstackSheets(
                    state = state.value, connectionDraft = null, onDismiss = { closed = true }, onPlaybackToggle = {},
                    onConnectionNameChange = {}, onConnectionUrlChange = {}, onConnectionTokenChange = {},
                    onConnectionUserIdChange = {}, onConnectionAuthModeChange = {}, onConnectionUsernameChange = {},
                    onConnectionPasswordChange = {}, onTestAndSaveConnection = {}, onRemoveConnection = {}, onAddMedia = {},
                    onUpcomingClick = { id -> state.value = state.value.copy(
                        activeSheet = AppSheet.TitleDetails(id), returnToCalendar = true,
                        contentDetails = ContentDetails(id, id, "Sonarr", "S03 E10", artworkRes = R.drawable.media_placeholder, mediaType = "Episode"),
                    ) },
                    onBackToCalendar = { state.value = state.value.copy(activeSheet = AppSheet.UpcomingCalendar, returnToCalendar = false) },
                )
            }
        }
        rule.onNodeWithText("Episodar").performClick()
        rule.onNodeWithTag("calendar-day-1").performClick()
        rule.onNodeWithText("Episode B").performClick()
        rule.onNodeWithText("S03 E10").assertIsDisplayed()
        rule.onNodeWithText("Kalender").performClick()
        rule.onNodeWithText("Episode B").assertIsDisplayed()
        rule.onNodeWithText("Episode A").assertDoesNotExist()
        rule.onNodeWithText("Film B").assertDoesNotExist()
        rule.onNodeWithText("Episode B").performClick()
        rule.onNodeWithContentDescription("Lukk detaljane").performClick()
        rule.waitUntil(timeoutMillis = 3_000) { closed } // Allow for a busy emulator while the exit animation settles.
        assertEquals(true, closed)
    }
}
