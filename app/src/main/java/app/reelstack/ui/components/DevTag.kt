package app.reelstack.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.reelstack.BuildConfig

/**
 * A test tag on a part inside something that already has one, kept out of release builds.
 *
 * A test tag is a node in the semantics tree, and Compose sorts every node there by position about every 100 ms while a
 * page moves. On the Shield that work took a fifth of the main thread, between frames as well as inside them
 * (2026-11-07). The parts of a card (artwork, progress, caption) are only looked up by the instrumentation tests, which
 * run on the debug build; the card itself keeps its real [testTag].
 */
internal fun Modifier.devTag(tag: String): Modifier = if (BuildConfig.DEBUG) testTag(tag) else this
