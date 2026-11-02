package app.reelstack.ui.screens

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.state.GlobalSearchUiState
import app.reelstack.ui.state.SearchSlice
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Search on a television has a keyboard of its own beside the results. The system keyboard covers
 * the results, and a remote has to reach it key by key; here every letter is on the page and the hits
 * change after each press. A phone keeps its text field.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "b+nn-w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvSearchKeyboardTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val queries = mutableListOf<String>()
    private val opened = mutableListOf<String>()
    private var commits = 0
    private var ups = 0
    private var closes = 0
    private var current = ""

    private val troll = LibraryMedia(id = "jf-1", title = "Trolljegeren", subtitle = "2010", artworkRes = R.drawable.media_placeholder,
        source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")

    private fun show(
        initial: String = "",
        history: List<String> = emptyList(),
        withUp: Boolean = false,
        fontScale: Float? = null,
        phone: Boolean = false,
    ) {
        current = initial
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            var query by remember { mutableStateOf(initial) }
            val base = LocalConfiguration.current
            val configuration = remember(base, phone) {
                if (!phone) base else Configuration(base).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or Configuration.UI_MODE_TYPE_NORMAL
                }
            }
            val density = LocalDensity.current
            val scaled = remember(density, fontScale) { if (fontScale == null) density else Density(density.density, fontScale) }
            CompositionLocalProvider(LocalConfiguration provides configuration, LocalDensity provides scaled) {
                ReelstackTheme {
                    // The app's own Back handler closes the page; the page's must come after it.
                    BackHandler { closes++ }
                    GlobalSearchScreen(
                        state = GlobalSearchUiState(
                            search = SearchSlice(query = query, libraryResults = if (query.trim().length >= 2) listOf(troll) else emptyList()),
                            history = history,
                        ),
                        onQuery = { queries += it; query = it; current = it },
                        onClose = {}, onLibraryDetails = { opened += it }, onDiscoverDetails = {}, onLoadMore = {},
                        onNavigateUp = if (withUp) ({ ups++ }) else null,
                        onCommitSearch = { commits++ },
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    private fun key(id: String) = rule.onNodeWithTag("search-key-$id")

    private fun pressKeys(vararg ids: String) = ids.forEach { key(it).performClick() }

    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    private fun tagStartsWith(prefix: String) = SemanticsMatcher("tag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    // MARK: What the page shows

    @Test fun aTelevisionShowsTheKeyboardBesideTheResultsAndNoTextField() {
        show("Tr")
        rule.onNodeWithTag("search-keyboard").assertIsDisplayed()
        rule.onNodeWithTag("global-search-field").assertDoesNotExist()
        rule.onNodeWithTag("global-search-clear").assertDoesNotExist()
        val keyboard = rule.onNodeWithTag("search-keyboard").fetchSemanticsNode().boundsInRoot
        val results = rule.onNodeWithTag("global-search-results").fetchSemanticsNode().boundsInRoot
        assertTrue("The keyboard stands left of the results: $keyboard against $results", keyboard.right <= results.left + 1f)
        rule.onNodeWithTag("library-hit-jf-1").assertIsDisplayed()
    }

    @Test fun aPhoneKeepsItsTextFieldAndHasNoKeyboard() {
        show("Tr", phone = true)
        rule.onNodeWithTag("global-search-field").assertIsDisplayed()
        rule.onNodeWithTag("search-keyboard").assertDoesNotExist()
        rule.onAllNodes(tagStartsWith("search-key-")).assertCountEquals(0)
    }

    @Test fun theKeysAreAllThereInAlphabeticalOrderWithTheNorwegianLettersAfterZ() {
        show()
        val order = ('A'..'Z').map { it.toString() } + listOf("Æ", "Ø", "Å") + ('0'..'9').map { it.toString() } + listOf("-", "space", "delete")
        order.forEach { key(it).assertExists() }
        val lefts = order.map { key(it).fetchSemanticsNode().boundsInRoot }
        // Six to a row: A..F share a row, G starts the next one.
        assertEquals(lefts[0].top, lefts[5].top, 1f)
        assertTrue(lefts[6].top > lefts[0].top)
        assertEquals(lefts[0].left, lefts[6].left, 1f)
    }

    @Test fun withoutARecogniserThereIsNoMicrophoneKey() {
        show()
        key("speak").assertDoesNotExist()
        key("delete").assertExists()
    }

    /** A device with something that listens: what SpeechRecognizer.isRecognitionAvailable looks for. */
    private fun installRecogniser() {
        val component = ComponentName("com.example.voice", "com.example.voice.Recogniser")
        val packages = shadowOf(ApplicationProvider.getApplicationContext<Application>().packageManager)
        packages.addServiceIfNotPresent(component)
        packages.addIntentFilterForService(component, IntentFilter(RecognitionService.SERVICE_INTERFACE))
    }

    @Test fun withARecogniserTheMicrophoneKeyIsOnTheLastRow() {
        installRecogniser()
        show()
        key("speak").assertExists()
        val delete = key("delete").fetchSemanticsNode().boundsInRoot
        val speak = key("speak").fetchSemanticsNode().boundsInRoot
        assertTrue("The microphone stands below delete: $speak under $delete", speak.top >= delete.bottom - 1f)
    }

    @Test fun theMicrophoneHandsOverToTheSystemRecogniserAndTheAnswerBecomesTheQuery() {
        installRecogniser()
        show()
        key("speak").performClick()
        val screen = shadowOf(rule.activity)
        val started = screen.nextStartedActivityForResult
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, started.intent.action)
        screen.receiveResult(started.intent, Activity.RESULT_OK,
            Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("game of thrones")))
        rule.waitForIdle()
        assertEquals("Spoken words get their capitals like typed ones", "Game Of Thrones", current)
        key("speak").assertIsFocused()
    }

    @Test fun aCancelledSpeechInputLeavesTheQueryAsItWas() {
        installRecogniser()
        show("Dune")
        key("speak").performClick()
        val screen = shadowOf(rule.activity)
        screen.receiveResult(screen.nextStartedActivityForResult.intent, Activity.RESULT_CANCELED, null)
        rule.waitForIdle()
        assertEquals("Dune", current)
        assertTrue(queries.isEmpty())
    }

    // MARK: The query line

    @Test fun theQueryLineOnlyShowsAndNeverTakesFocus() {
        show("Tro")
        rule.onNodeWithTag("search-query").assertExists()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.EditableText))
        rule.onAllNodes(hasAnyAncestor(hasTestTag("search-query")) and SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus),
            useUnmergedTree = true).assertCountEquals(0)
        // The page opens on the keyboard, so a remote can start typing at once.
        rule.waitUntil(3_000) { runCatching { key("A").assertIsFocused() }.isSuccess }
    }

    @Test fun aLongQueryIsCutAtTheFrontSoTheEndStaysVisible() {
        show("Det er ei veldig lang soketittel som aldri far plass pa ei linje")
        val line = rule.onNodeWithTag("search-query").fetchSemanticsNode().boundsInRoot
        // Not boundsInRoot: that one is clipped by the line, and the cut is what is measured here.
        val text = rule.onNodeWithTag("search-query-text", useUnmergedTree = true).fetchSemanticsNode()
        val left = text.positionInRoot.x
        val right = left + text.size.width
        assertTrue("The text is wider than the line: $left..$right in $line", text.size.width > line.width)
        assertTrue("The end stays inside the line: $left..$right in $line", right <= line.right && right >= line.right - 80f)
        assertTrue("The head is cut: $left..$right in $line", left < line.left)
    }

    @Test fun aShortQueryStartsAtTheFrontOfTheLine() {
        show("Tro")
        val line = rule.onNodeWithTag("search-query").fetchSemanticsNode().boundsInRoot
        val text = rule.onNodeWithTag("search-query-text", useUnmergedTree = true).fetchSemanticsNode()
        val left = text.positionInRoot.x
        assertTrue("$left..${left + text.size.width} in $line", left >= line.left && left + text.size.width <= line.right)
    }

    // MARK: Typing

    @Test fun pressingKeysTypesWordsWithCapitalsThroughOnQuery() {
        show()
        pressKeys("G", "A", "M", "E", "space", "O", "F", "space", "T", "H", "R", "O", "N", "E", "S")
        assertEquals(listOf("G", "Ga", "Gam", "Game", "Game ", "Game O", "Game Of", "Game Of ", "Game Of T", "Game Of Th",
            "Game Of Thr", "Game Of Thro", "Game Of Thron", "Game Of Throne", "Game Of Thrones"), queries)
        rule.onNodeWithTag("search-query-text", useUnmergedTree = true).assertTextEquals("Game Of Thrones")
    }

    @Test fun theNorwegianLettersAndDigitsAndTheHyphenType() {
        show()
        pressKeys("Æ", "Ø", "Å", "-", "7")
        assertEquals("Æøå-7", current)
    }

    @Test fun aSpaceAtTheStartOrAfterASpaceChangesNothing() {
        show()
        pressKeys("space")
        assertTrue(queries.isEmpty() || queries.all { it.isEmpty() })
        pressKeys("A", "space", "space")
        assertEquals("A ", current)
    }

    @Test fun deleteRemovesTheLastLetter() {
        show("Game")
        pressKeys("delete")
        assertEquals("Gam", current)
        pressKeys("delete", "delete", "delete", "delete")
        assertEquals("", current)
    }

    @Test fun holdingDeleteForHalfASecondClearsTheWholeQuery() {
        show("Game Of Thrones")
        key("delete").requestFocus()
        // The clock stands still between steps, so the half second is counted by the test and not by
        // however fast the machine runs.
        rule.mainClock.autoAdvance = false
        key("delete").performKeyInput { keyDown(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(400)
        assertEquals("Not yet: ${queries}", "Game Of Thrones", current)
        rule.mainClock.advanceTimeBy(200)
        assertEquals("", current)
        key("delete").performKeyInput { keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals("Letting go after the hold takes nothing more", listOf(""), queries)
    }

    @Test fun aShortPressOnDeleteTakesOneLetterAfterItIsLetGo() {
        show("Game")
        key("delete").requestFocus()
        rule.mainClock.autoAdvance = false
        key("delete").performKeyInput { keyDown(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(200)
        key("delete").performKeyInput { keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(listOf("Gam"), queries)
    }

    @Test fun aHardwareKeyboardTypesIntoTheQueryWhereverFocusIs() {
        show()
        key("Z").requestFocus()
        // One key at a time, as a hand types: each press is read against what the page shows by then.
        fun press(keys: List<Key>) = keys.forEach { pressed -> key("Z").performKeyInput { pressKey(pressed) }; rule.waitForIdle() }
        press(listOf(Key.G, Key.A, Key.M, Key.Spacebar, Key.Three))
        assertEquals("Gam 3", current)
        press(listOf(Key.Backspace))
        assertEquals("Gam ", current)
        press(listOf(Key.Backspace, Key.Minus, Key.O))
        assertEquals("Gam-O", current)
        key("Z").assertIsFocused()
    }

    @Test fun theRemotesOwnKeysAreNotTakenForText() {
        show()
        key("B").requestFocus()
        key("B").performKeyInput { pressKey(Key.DirectionRight) }
        key("C").assertIsFocused()
        assertTrue(queries.isEmpty())
        key("C").performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals("C", current)
    }

    // MARK: Focus

    @Test fun upFromTheTopRowCallsOnNavigateUpAndFromTheSecondRowMovesUp() {
        show(withUp = true)
        key("C").requestFocus()
        key("C").performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals(1, ups)
        key("F").requestFocus()
        key("F").performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals(2, ups)
        key("I").requestFocus()
        key("I").performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals("The second row only moves up", 2, ups)
        key("C").assertIsFocused()
    }

    @Test fun upFromTheTopRowDoesNothingWithoutAnOnNavigateUp() {
        show(withUp = false)
        key("C").requestFocus()
        key("C").performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals(0, ups)
        key("C").assertIsFocused()
    }

    @Test fun rightFromTheLastColumnEntersTheResultsAndLeftComesBackToTheSameKey() {
        show("Tr")
        key("L").requestFocus()
        key("L").performKeyInput { pressKey(Key.DirectionRight) }
        rule.waitForIdle()
        rule.onNode(isFocused()).assert(hasAnyAncestor(hasTestTag("global-search-results")))
        assertEquals("Moving into the results is the moment the term counts", 1, commits)
        rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionLeft) }
        rule.waitForIdle()
        key("L").assertIsFocused()
    }

    @Test fun backInTheResultsGoesToTheLastKeyAndBackInTheKeyboardClosesSearch() {
        show("Tr")
        key("K").requestFocus()
        rule.onNodeWithTag("library-hit-jf-1").requestFocus()
        rule.waitForIdle()
        pressBack()
        key("K").assertIsFocused()
        assertEquals("Back in the results does not leave search", 0, closes)
        pressBack()
        assertEquals("Back in the keyboard closes as before", 1, closes)
    }

    @Test fun focusingAHistoryRowAndPickingItPutsFocusBackOnTheKeys() {
        show(history = listOf("Dune", "Silo"))
        key("D").requestFocus()
        rule.onNodeWithText("Dune").performClick()
        rule.waitForIdle()
        assertEquals("Dune", current)
        assertEquals(1, commits)
        rule.onNode(isFocused()).assert(tagStartsWith("search-key-"))
    }

    @Test fun openingAResultCommitsTheTerm() {
        show("Tr")
        val before = commits
        rule.onNodeWithTag("library-hit-jf-1").performClick()
        assertEquals(listOf("jf-1"), opened)
        assertEquals(before + 1, commits)
    }

    // MARK: Phone

    @Test fun aPhoneCommitsWhenAResultIsTappedAndWhenTheSearchActionIsPressedButNotWhileTyping() {
        show("Tr", phone = true)
        rule.onNodeWithTag("global-search-field").performTextInput("a")
        assertEquals("Typing alone remembers nothing", 0, commits)
        rule.onNodeWithTag("global-search-field").performImeAction()
        assertEquals(1, commits)
        rule.onNodeWithTag("library-hit-jf-1").performClick()
        assertEquals(listOf("jf-1"), opened)
        assertEquals(2, commits)
    }

    // MARK: Large text

    @Test fun atFontScaleTwoEveryKeyKeepsItsHitAreaAndItsCharacter() {
        show("Tr", fontScale = 2f)
        val density = rule.density.density
        val minimum = 48f * density
        val keys = rule.onAllNodes(tagStartsWith("search-key-")).fetchSemanticsNodes()
        assertEquals("Every key stands on the page", 42, keys.size)
        keys.forEach { node ->
            val size = node.boundsInRoot
            assertTrue("${node.config.getOrNull(SemanticsProperties.TestTag)} is ${size.width}x${size.height}, under 48 dp",
                size.width >= minimum - 1f && size.height >= minimum - 1f)
        }
        listOf("W", "M", "Æ", "Ø", "Å").forEach { letter ->
            key(letter).performScrollTo()
            val text = rule.onNode(hasText(letter) and hasAnyAncestor(hasTestTag("search-key-$letter")), useUnmergedTree = true)
            val layouts = mutableListOf<TextLayoutResult>()
            text.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertTrue("$letter has a layout", layouts.isNotEmpty())
            val layout = layouts.first()
            val keySize = key(letter).fetchSemanticsNode().size
            // The letter as wide as it wants to be, and as tall as its line, against the key it stands in.
            val wanted = layout.multiParagraph.maxIntrinsicWidth
            assertTrue("$letter needs ${wanted}x${layout.size.height} px in a key of $keySize", wanted <= keySize.width && layout.size.height <= keySize.height)
            assertFalse("$letter was cut to fit", layout.multiParagraph.didExceedMaxLines)
        }
    }

    @Test fun theKeyboardOnAnyScreenSizeIsReachableByScrolling() {
        show("Tr", fontScale = 2f)
        // The last key may lie below the screen at this text size; it must still be reachable.
        key("delete").performScrollTo().assertIsDisplayed()
    }

    @Test fun focusStaysOnTheKeyThatWasPressed() {
        show()
        key("Q").requestFocus()
        key("Q").performKeyInput { pressKey(Key.DirectionCenter) }
        key("Q").assertIsFocused()
    }
}
