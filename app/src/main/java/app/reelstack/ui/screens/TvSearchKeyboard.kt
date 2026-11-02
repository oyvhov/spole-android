package app.reelstack.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import app.reelstack.R
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.Surface
import app.reelstack.ui.theme.SurfaceRaised
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Holding OK on the delete key this long clears the whole term. */
private const val HOLD_MILLIS = 500L

private val KeyGap = 4.dp
private val CardInset = 12.dp

/** Six keys of the 48 dp a hit area needs, the gaps between them and the card's inset on both sides. */
private val KeyboardWidth = 48.dp * SearchKeys.COLUMNS + KeyGap * (SearchKeys.COLUMNS - 1) + CardInset * 2 + 4.dp

/**
 * Where focus stands on the keyboard, so it can go back there: Back from the results returns to the
 * last key pressed, and Left from the results lands on it too.
 */
internal class SearchKeyFocus {
    private val requesters = HashMap<String, FocusRequester>()

    /** The key that had focus last, by [SearchKeys.Key.id]. */
    var lastKey: String = "A"

    fun requester(id: String): FocusRequester = requesters.getOrPut(id) { FocusRequester() }

    /** Focus to the last key; false if the keyboard is not there (yet). */
    fun toLastKey(): Boolean = runCatching { requester(lastKey).requestFocus() }.isSuccess
}

/**
 * **The on-screen keyboard of TV search: a card with the query on top and the keys below.**
 *
 * The system keyboard is not used. It covers the results, and with a remote one has to walk it key by
 * key anyway; here every letter is on the page beside the hits, and the hits change after each press.
 * Alphabetical keys (see [SearchKeys]), each at least 48 dp square and as tall as its text needs, so
 * nothing is clipped at font scale 2.0. When the card does not fit the screen it scrolls, and focus
 * follows.
 *
 * Focus is a ring, like everywhere else on the television (not a bigger key). Up from the top row
 * calls [onNavigateUp] (the optional top menu); without one the press goes nowhere.
 */
@Composable
internal fun TvSearchKeyboard(
    term: String,
    searching: Boolean,
    onChange: (String) -> Unit,
    focus: SearchKeyFocus,
    onSpeak: (() -> Unit)?,
    onNavigateUp: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val rows = remember(onSpeak != null) { SearchKeys.rows(speech = onSpeak != null) }
    val label = stringResource(R.string.search_keyboard)
    Column(
        modifier.width(KeyboardWidth)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(CardInset)
            // Coming back from the results lands on the key that was left, not on whichever lies beside.
            .focusRestorer(focus.requester("A"))
            .focusGroup()
            .semantics { contentDescription = label }
            .testTag("search-keyboard"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchQueryLine(term, searching)
        Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
            rows.forEachIndexed { rowIndex, row ->
                // The tallest key sets the height of its row, so an icon key never sits lower than a letter.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                    row.forEach { key ->
                        SearchKeyButton(
                            key, term, onChange, focus, onSpeak,
                            onUp = if (rowIndex == 0) onNavigateUp else null,
                            modifier = Modifier.weight(key.span.toFloat()).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The typed term, for looking at. It never takes focus and has no caret that blinks: a caret that
 * stood still at the end tells where the next letter goes, and a blinking one would be a pulse in the
 * corner of the eye. A long term is cut at the front, so the end the reader just typed stays visible.
 */
@Composable
private fun SearchQueryLine(term: String, searching: Boolean) {
    val hint = stringResource(R.string.discover_search_hint)
    val described = if (term.isEmpty()) hint else stringResource(R.string.search_query_described, term)
    val style = MaterialTheme.typography.bodyLarge
    val caretHeight = with(LocalDensity.current) {
        (if (style.lineHeight.isSpecified) style.lineHeight else style.fontSize * 1.3f).toDp()
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(RoundedCornerShape(ReelLayout.ControlCorner))
            .background(SurfaceRaised)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("search-query")
            // After the tag: what comes after clearAndSetSemantics is cleared with the children.
            .clearAndSetSemantics { contentDescription = described },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(SpoleIcons.Search, null, Modifier.size(20.dp), tint = Muted)
        Box(Modifier.weight(1f).clipToBounds()) {
            Row(Modifier.keepEndInView(), verticalAlignment = Alignment.CenterVertically) {
                if (term.isEmpty()) {
                    Caret(caretHeight)
                    Text(hint, style = style, color = Muted, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
                } else {
                    // A trailing space is not always measured; a no-break space is, so the caret moves on.
                    val shown = term.trimEnd(' ') + " ".repeat(term.length - term.trimEnd(' ').length).replace(' ', ' ')
                    Text(shown, style = style, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false,
                        modifier = Modifier.testTag("search-query-text"))
                    Caret(caretHeight)
                }
            }
        }
        if (searching) CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Caret(height: androidx.compose.ui.unit.Dp) {
    Box(Modifier.padding(start = 1.dp).width(2.dp).height(height).background(Primary))
}

/** Measures its content at the width it wants and places it so that its end, not its start, is in view. */
private fun Modifier.keepEndInView(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints(minWidth = 0, maxWidth = Constraints.Infinity, minHeight = 0, maxHeight = constraints.maxHeight))
    layout(minOf(placeable.width, constraints.maxWidth), placeable.height) {
        placeable.place(minOf(0, constraints.maxWidth - placeable.width), 0)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchKeyButton(
    key: SearchKeys.Key,
    term: String,
    onChange: (String) -> Unit,
    focus: SearchKeyFocus,
    onSpeak: (() -> Unit)?,
    onUp: (() -> Unit)?,
    modifier: Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(ReelLayout.ControlCorner)
    val muted = when (key.kind) {
        SearchKeys.Kind.SPACE -> !SearchKeys.spaceFits(term)
        SearchKeys.Kind.DELETE -> term.isEmpty()
        else -> false
    }
    val label = when (key.kind) {
        SearchKeys.Kind.LETTER -> key.character
        SearchKeys.Kind.SPACE -> stringResource(R.string.search_key_space)
        SearchKeys.Kind.DELETE -> stringResource(R.string.search_key_delete)
        SearchKeys.Kind.SPEAK -> stringResource(R.string.search_key_speak)
    }
    val latestTerm by rememberUpdatedState(term)
    val change by rememberUpdatedState(onChange)
    fun typeInto(next: String) { if (next != latestTerm) change(next) }
    val press: () -> Unit = {
        when (key.kind) {
            SearchKeys.Kind.LETTER -> typeInto(SearchKeys.typed(key.character, latestTerm))
            SearchKeys.Kind.SPACE -> typeInto(SearchKeys.withSpace(latestTerm))
            SearchKeys.Kind.DELETE -> typeInto(SearchKeys.withoutLast(latestTerm))
            SearchKeys.Kind.SPEAK -> onSpeak?.invoke()
        }
    }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fill = when {
        pressed -> onSurface.copy(alpha = 0.18f)
        focused -> onSurface.copy(alpha = 0.10f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val content = if (muted) Muted.copy(alpha = 0.6f) else onSurface
    val hold = if (key.kind == SearchKeys.Kind.DELETE) Modifier.holdToClear(latestTerm, change) else Modifier
    val gesture = if (key.kind == SearchKeys.Kind.DELETE) {
        // A pointer can long-press as well; the remote's OK is handled by [holdToClear].
        Modifier.combinedClickable(interactionSource = interaction, indication = null, role = Role.Button,
            onLongClick = { typeInto("") }, onClick = press)
    } else {
        Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = press)
    }
    Box(
        modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
            .focusRequester(focus.requester(key.id))
            .onFocusChanged { if (it.isFocused) focus.lastKey = key.id }
            .then(hold)
            .onPreviewKeyEvent { event ->
                if (onUp != null && event.key == Key.DirectionUp) {
                    if (event.type == KeyEventType.KeyDown) onUp()
                    true
                } else false
            }
            .focusOutline(interaction, shape, glow = false)
            .clip(shape)
            .background(fill)
            .then(gesture)
            // A letter is read from its text; the keys without one are named here.
            .then(if (key.kind == SearchKeys.Kind.SPACE || key.kind == SearchKeys.Kind.DELETE) Modifier.semantics { contentDescription = label } else Modifier)
            .testTag("search-key-${key.id}"),
        contentAlignment = Alignment.Center,
    ) {
        when (key.kind) {
            SearchKeys.Kind.LETTER -> Text(key.character, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                color = content, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
            SearchKeys.Kind.SPACE -> Icon(SpoleIcons.SpaceBar, null, Modifier.size(24.dp), tint = content)
            SearchKeys.Kind.DELETE -> Icon(SpoleIcons.Backspace, null, Modifier.size(24.dp), tint = content)
            SearchKeys.Kind.SPEAK -> Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(SpoleIcons.Mic, null, Modifier.size(24.dp), tint = content)
                Text(label, style = MaterialTheme.typography.titleSmall, color = content, maxLines = 1)
            }
        }
    }
}

private fun KeyEvent.isSelect() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

/**
 * OK on the delete key takes one letter when it is let go, and the whole term if it is held for
 * [HOLD_MILLIS]: there is no fourth key for "clear all". The press is decided here, not by the
 * click: a held key repeats, and a click would fire on release even after the hold had cleared.
 */
@Composable
private fun Modifier.holdToClear(term: String, onChange: (String) -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val hold = remember { HoldToClear() }
    val latestTerm by rememberUpdatedState(term)
    val change by rememberUpdatedState(onChange)
    return this
        .onFocusChanged { if (!it.isFocused) hold.reset() }
        .onPreviewKeyEvent { event ->
            if (!event.isSelect()) return@onPreviewKeyEvent false
            when (event.type) {
                KeyEventType.KeyDown -> {
                    // A held key repeats; only the first press starts the count.
                    if (hold.pending == null) {
                        hold.cleared = false
                        hold.pending = scope.launch {
                            delay(HOLD_MILLIS)
                            hold.cleared = true
                            if (latestTerm.isNotEmpty()) change("")
                        }
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    // A release without a press here began on another key: not ours.
                    val counting = hold.pending ?: return@onPreviewKeyEvent false
                    counting.cancel()
                    val wasCleared = hold.cleared
                    hold.reset()
                    if (!wasCleared && latestTerm.isNotEmpty()) change(SearchKeys.withoutLast(latestTerm))
                    true
                }
                else -> false
            }
        }
}

/** The half second of a press on delete: counting, and whether it ran out. Not state: nothing draws it. */
private class HoldToClear {
    var pending: Job? = null
    var cleared = false

    fun reset() {
        pending?.cancel()
        pending = null
        cleared = false
    }
}

private val LetterKeys = listOf(Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
    Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z)
private val DigitKeys = listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)

/** What a key press types: the layout's own character when it has one (Æ, Ø and Å on a Nordic keyboard), else by key. */
private fun KeyEvent.typedText(): String? {
    if (utf16CodePoint > 0) return String(Character.toChars(utf16CodePoint))
    LetterKeys.indexOf(key).takeIf { it >= 0 }?.let { return ('a' + it).toString() }
    DigitKeys.indexOf(key).takeIf { it >= 0 }?.let { return it.toString() }
    return when (key) {
        Key.Minus -> "-"
        Key.Spacebar -> " "
        else -> null
    }
}

/**
 * A hardware keyboard (USB or Bluetooth) types into the term as well: letters, digits, the hyphen
 * and the space like the keys of the card, Backspace and Delete like the delete key. Put it on the
 * page with onPreviewKeyEvent, so it is read before the focused key; the remote's own keys have no
 * character and are left alone, and so is anything held with Ctrl, Alt or Meta.
 */
internal fun typeFromHardwareKeyboard(event: KeyEvent, term: String, onChange: (String) -> Unit): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.key == Key.Backspace || event.key == Key.Delete) {
        if (term.isEmpty()) return false
        onChange(SearchKeys.withoutLast(term))
        return true
    }
    if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
    val text = event.typedText()?.takeIf { SearchKeys.canType(it) } ?: return false
    // What the keyboard can type is ours even when it changes nothing: a space that does not fit
    // must not press the focused key. Anything else goes on to whoever wants it.
    val next = SearchKeys.hardware(text, term)
    if (next != term) onChange(next)
    return true
}

/**
 * Voice input, if the device has a recogniser: the key hands over to the system's own screen, so no
 * microphone permission is asked for, and the answer becomes the query. Null when nothing can listen,
 * and then the key is not drawn at all.
 */
@Composable
internal fun rememberSpeechSearch(onResult: (String) -> Unit, onFinished: () -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val available = remember(context) { runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false) }
    if (!available) return null
    val prompt = stringResource(R.string.search_speak_prompt)
    val latestResult by rememberUpdatedState(onResult)
    val latestFinished by rememberUpdatedState(onFinished)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.let(SearchKeys::capitalizeWords)?.takeIf { it.isNotEmpty() }?.let { latestResult(it) }
        }
        latestFinished()
    }
    return remember(launcher, prompt) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // A recogniser service without a screen to go with it is possible; then nothing happens.
            runCatching { launcher.launch(intent) }.onFailure { latestFinished() }
            Unit
        }
    }
}
