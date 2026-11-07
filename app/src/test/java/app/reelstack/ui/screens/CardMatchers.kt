package app.reelstack.ui.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/**
 * A media card whose spoken label names [title]. On a television a card is one node with its label and no caption
 * texts under it (see `cardSemantics`), so a card is found by what it says, not by the text drawn under it.
 * "Serie 1" does not match the card for "Serie 10".
 */
internal fun hasCardLabel(title: String): SemanticsMatcher {
    val name = Regex("""(^|\s)${Regex.escape(title)}(,|$)""")
    return SemanticsMatcher("card labelled $title") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { name.containsMatchIn(it) }
    }
}
