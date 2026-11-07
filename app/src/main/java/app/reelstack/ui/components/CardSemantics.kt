package app.reelstack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * What an accessibility service is told about a media card: its [label], as a button.
 *
 * On a television the card is one node and nothing under it is. With any accessibility service on, Compose rebuilds,
 * sorts and compares the whole semantics tree about every 100 ms while a page moves, and each card brought ten nodes
 * to it (artwork, clip shapes, progress, caption, title, episode line, the caption's reserve). On the Shield (Button
 * Mapper) and on Loftet (only Stova's home button) that work took about 38 % of the main thread while Home scrolled
 * (2026-11-07); with the cards cleared, finding the nodes fell from 19 % to 7 % on Loftet. Compose still sorts every
 * node by position, cleared or not, so the rest only shrinks with fewer nodes. The label says what the caption says,
 * so a screen reader loses nothing and stops once per card instead of three times.
 *
 * Put it after the card's clickable and its test tag: semantics set on the same node further in are cleared with the
 * rest. On a phone the card keeps the semantics it had: [label] as its description, or, with [mergeOnPhone], the
 * texts under it merged into it.
 */
@Composable
internal fun Modifier.cardSemantics(label: String, mergeOnPhone: Boolean = false): Modifier = when {
    isTelevision() -> clearAndSetSemantics { contentDescription = label; role = Role.Button }
    mergeOnPhone -> semantics(mergeDescendants = true) { role = Role.Button }
    else -> semantics { contentDescription = label; role = Role.Button }
}
