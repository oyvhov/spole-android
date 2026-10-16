package app.reelstack.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverPerson
import app.reelstack.data.model.PersonPage
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xhdpi", application = app.reelstack.SheetTestApplication::class)
class PersonSheetTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun media(id: Int, owned: Boolean) = DiscoverMedia("seerr-movie-$id", "T$id", "2024", R.drawable.media_placeholder, owned,
        remoteId = id, mediaType = "movie", seerrStatus = if (owned) 5 else 1)

    private val page = PersonPage(DiscoverPerson(501, "Margaret Qualley", null), department = "Acting", birthday = "1994-10-23",
        biography = "A long biography. ".repeat(20), inLibrary = listOf(media(1, true)), more = listOf(media(2, false)))

    @Test fun whatYouHaveComesBeforeTheRestAndOpensTheTitle() {
        val opened = mutableListOf<String>()
        var actor = ""
        rule.setContent {
            ReelstackTheme {
                actor = stringResource(R.string.person_dept_acting)
                PersonSheet(page, onTitle = { opened += it })
            }
        }
        val owned = rule.onNodeWithTag("person-in-library").fetchSemanticsNode().boundsInRoot
        val more = rule.onNodeWithTag("person-more").fetchSemanticsNode().boundsInRoot
        assert(owned.top < more.top)
        rule.onNodeWithText(actor, substring = true).assertIsDisplayed()
        rule.onNodeWithTag("discover-poster-seerr-movie-2").performScrollTo().performClick()
        assertEquals(listOf("seerr-movie-2"), opened)
    }

    @Test fun readMoreOpensTheWholeBiographyAndCanCloseIt() {
        rule.setContent { ReelstackTheme { PersonSheet(page, onTitle = {}) } }
        // The button stays, to read less: one that went away left a remote on nothing.
        rule.onNodeWithTag("person-read-more").performClick()
        rule.onNodeWithTag("person-read-more").assertTextEquals(rule.activity.getString(app.reelstack.R.string.details_less))
        rule.onNodeWithTag("person-read-more").performClick()
        rule.onNodeWithTag("person-read-more").assertTextEquals(rule.activity.getString(app.reelstack.R.string.person_read_more))
    }
}
