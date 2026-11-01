package app.reelstack.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The hero's line of facts says what a viewer needs: ratings, year, time — or time left — and age. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class HeroFactsTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val episode = LibraryMedia("e", "Skal vi danse", "S21 E9", artworkRes = R.drawable.media_placeholder,
        source = ServiceKind.JELLYFIN, mediaType = "Episode", season = 21, episode = 9,
        facts = listOf("Episode", "2026", "44 min", "★ 7.1"))

    @Test fun anEpisodeIsNotCalledAnEpisodeAndShowsItsCommunityScoreFirst() {
        rule.setContent { ReelstackTheme { HeroMetadataRow(episode) } }
        rule.onAllNodesWithText("Episode").assertCountEquals(0)
        rule.onNodeWithTag("hero-community-rating").assertIsDisplayed()
        rule.onNodeWithText("2026").assertIsDisplayed()
        rule.onNodeWithText("44m").assertIsDisplayed()
    }

    @Test fun partWayThroughItSaysHowMuchIsLeft() {
        rule.setContent { ReelstackTheme { HeroMetadataRow(episode.copy(progress = .5f)) } }
        rule.onNodeWithText("22m left").assertIsDisplayed()
        rule.onAllNodesWithText("44m").assertCountEquals(0)
    }

    @Test fun aLongFilmSaysHoursAndAScoreThatIsNotOutOfTenIsLeftOut() {
        val film = episode.copy(mediaType = "Movie", season = null, episode = null, progress = .1f,
            facts = listOf("Film", "2025", "158 min", "★ 95,0", "NO-15"))
        rule.setContent { ReelstackTheme { HeroMetadataRow(film) } }
        rule.onNodeWithText("2t 23m left").assertIsDisplayed()
        rule.onAllNodesWithTag("hero-community-rating").assertCountEquals(0)
        rule.onNodeWithText("NO-15").assertIsDisplayed()
    }

    @Test fun minutesLeftOnlyBetweenTheStartAndTheEnd() {
        assertEquals(22, heroMinutesLeft(44, .5f))
        assertEquals(2, heroMinutesLeft(44, .97f))
        assertNull(heroMinutesLeft(44, null))
        assertNull(heroMinutesLeft(44, .01f))
        assertNull(heroMinutesLeft(44, .99f))
        assertNull(heroMinutesLeft(null, .5f))
    }
}
