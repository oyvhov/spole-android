package app.reelstack

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import app.reelstack.ui.components.steadyRemoteRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Quick presses up and down a grid stay in their column. Without the wait, a press that reached a row
 * not laid out yet sent focus to the first column of that row.
 */
class SteadyRemoteRowsTest {
    @get:Rule val rule = createComposeRule()

    private fun focusedIndex(): Int =
        rule.onNode(isFocused()).fetchSemanticsNode().config[SemanticsProperties.TestTag].removePrefix("cell-").toInt()

    @OptIn(ExperimentalTestApi::class)
    @Test fun quickPressesKeepTheirColumn() {
        rule.setContent {
            val grid = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(4), state = grid,
                modifier = Modifier.size(480.dp, 360.dp).steadyRemoteRows(grid),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(80, key = { it }) { index -> Box(Modifier.height(150.dp).testTag("cell-$index").focusable()) }
            }
        }
        rule.onNodeWithTag("cell-2").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.onNode(isFocused()).performKeyInput { repeat(6) { pressKey(Key.DirectionDown) } }
        rule.waitForIdle()
        val down = focusedIndex()
        assertEquals("Down left the third column for cell $down", 2, down % 4)
        assertTrue("The presses did not move down: cell $down", down >= 10)

        rule.onNode(isFocused()).performKeyInput { repeat(6) { pressKey(Key.DirectionUp) } }
        rule.waitForIdle()
        val up = focusedIndex()
        assertEquals("Up left the third column for cell $up", 2, up % 4)
        assertTrue("The presses did not move up: cell $up", up < down)
    }
}
