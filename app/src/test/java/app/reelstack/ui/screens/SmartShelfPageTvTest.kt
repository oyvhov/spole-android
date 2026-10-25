package app.reelstack.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfPresets
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.LocalSmartShelfActions
import app.reelstack.ui.components.SmartShelfActions
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A smart shelf's page on a television: its picture, its titles, and its one action among the choices. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class SmartShelfPageTvTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Heim", "https://media.example", "token", userId = "me")
    private fun item(id: String) = RemoteLibraryItem(id, "Title $id", "2024", null, "Movie", id,
        backdropUrl = "https://media.example/Items/$id/Images/Backdrop")

    private class Actions : SmartShelfActions {
        var editing: String? = null
        override fun open(id: String) {}
        override fun edit(id: String?, template: String?) { editing = id }
        override fun update(draft: SmartShelf) {}
        override fun applyTemplate(template: String) {}
        override fun save() {}
        override fun delete(id: String) {}
        override fun close() {}
        override fun retryFacets() {}
    }

    private fun page(actions: Actions = Actions()) {
        val state = ReelstackUiState(connections = listOf(connection), libraryPath = listOf(SmartShelfPresets.halloween.pathId to "Halloween"),
            libraryEntries = List(12) { item("m$it") })
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme { CompositionLocalProvider(LocalSmartShelfActions provides actions) { LibraryScreen(state, {}, {}, {}) } }
        }
    }

    @Test fun theShelfStartsOnItsFirstTitleAndEditsFromAmongTheChoices() {
        val actions = Actions()
        page(actions)
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("smart-item-m0").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("smart-shelf-surprise").assertDoesNotExist()
        rule.onNode(hasTestTag("smart-shelf-edit") and hasAnyAncestor(hasTestTag("smart-shelf-controls"))).performClick()
        rule.runOnIdle { assertEquals(SmartShelfPresets.halloween.id, actions.editing) }
    }

    @Test fun thePictureTurnsAndRunsOnUnderTheChoices() {
        page()
        rule.waitForIdle()
        rule.onNodeWithTag("smart-shelf-backdrop-m0", useUnmergedTree = true).assertExists()
        rule.mainClock.advanceTimeBy(8_500)
        rule.waitForIdle()
        rule.onNodeWithTag("smart-shelf-backdrop-m1", useUnmergedTree = true).assertExists()
        val hero = rule.onNodeWithTag("smart-shelf-hero").fetchSemanticsNode().boundsInRoot
        val picture = rule.onNodeWithTag("smart-shelf-art", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("the picture ends ${picture.bottom - hero.bottom} px below a ${hero.height} px top",
            picture.bottom - hero.bottom >= hero.height * .4f)
    }
}
