package app.reelstack.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.CompactSessionCard
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w892dp-h600dp-land-xhdpi", application = app.reelstack.SheetTestApplication::class)
class CardCaptionLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun captionFlow(resume: Boolean, fontScale: Float) {
        val short = LibraryMedia(id = "short", title = "Skal vi danse", subtitle = "S21 E02",
            artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN,
            mediaType = "Episode", season = 21, episode = 2)
        val long = short.copy(id = "long", title = "Ein mykje lengre serietittel som treng fleire linjer")
        var media by mutableStateOf(listOf(short, long))
        rule.setContent {
            ReelstackTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale),
                    LocalPersonalization provides Personalization(), LocalMotionEnabled provides false) {
                    if (resume) ResumeRail(media, {}) else LibraryRail(media, {}, wide = true)
                }
            }
        }
        val prefix = if (resume) "resume" else "library"
        fun checkVisibleLineGap(id: String): TextLayoutResult {
            val title = rule.onNodeWithTag("$prefix-title-$id", useUnmergedTree = true).assertIsDisplayed()
            val subtitle = rule.onNodeWithTag("$prefix-subtitle-$id", useUnmergedTree = true).assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            val titleBounds = title.fetchSemanticsNode().boundsInRoot
            val actualLastLineBottom = titleBounds.top + layout.getLineBottom(layout.lineCount - 1)
            val gap = subtitle.fetchSemanticsNode().boundsInRoot.top - actualLastLineBottom
            assertTrue("Episode info must follow the actual final title line at font $fontScale: $gap px",
                gap >= -1f && gap <= with(rule.density) { 4.dp.toPx() })
            return layout
        }
        assertEquals("The short-title fixture must exercise one actual line", 1, checkVisibleLineGap("short").lineCount)
        val longLayout = checkVisibleLineGap("long")
        if (resume || fontScale < 1.5f) {
            assertEquals("The long-title fixture must exercise the reserved second line", 2, longLayout.lineCount)
        }
        val art = rule.onNodeWithTag("$prefix-artwork-short", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val caption = rule.onNodeWithTag("$prefix-caption-short", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

        // A newly longer title uses the reserve beneath the caption, while artwork and rail height
        // remain steady. The reserve must not become a blank line above the episode again.
        rule.runOnIdle { media = listOf(short.copy(title = long.title), long) }
        checkVisibleLineGap("short")
        assertEquals(art, rule.onNodeWithTag("$prefix-artwork-short", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot)
        assertEquals("Caption reserve stays stable as the title wraps", caption.height,
            rule.onNodeWithTag("$prefix-caption-short", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height, 1f)
    }

    @Test fun resumeEpisodeFollowsItsActualTitleLine() = captionFlow(resume = true, fontScale = 1f)
    @Test fun resumeEpisodeFollowsItsActualTitleLineAtDoubleText() = captionFlow(resume = true, fontScale = 2f)
    @Test fun libraryEpisodeFollowsItsActualTitleLine() = captionFlow(resume = false, fontScale = 1f)
    @Test fun libraryEpisodeFollowsItsActualTitleLineAtDoubleText() = captionFlow(resume = false, fontScale = 2f)

    private fun secondaryCaptionFlow(session: Boolean, fontScale: Float) {
        val short = LibraryMedia(id = "secondary", title = "Serie", subtitle = "S01 E02",
            artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN,
            mediaType = "Episode", season = 1, episode = 2)
        var title by mutableStateOf(short.title)
        val playback = PlaybackSession("Test", "TV", short.title, "Svartisen", .4f, 20,
            false, "1080p", false, sessionId = "caption", source = ServiceKind.JELLYFIN,
            season = 1, episode = 2)
        rule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                if (!session) uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or
                    Configuration.UI_MODE_TYPE_TELEVISION
            }
            ReelstackTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale),
                    LocalConfiguration provides configuration, LocalMotionEnabled provides false) {
                    Column(Modifier.width(if (session) 360.dp else 180.dp)) {
                        if (session) CompactSessionCard(playback.copy(title = title), false, false, {}, {})
                        else LibraryHitCard(short.copy(title = title), {})
                    }
                }
            }
        }
        val prefix = if (session) "compact-session" else "library-hit"
        val id = if (session) playback.key else short.id
        fun checkGap(): TextLayoutResult {
            val heading = rule.onNodeWithTag("$prefix-title-$id", useUnmergedTree = true).assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            heading.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            val actualBottom = heading.fetchSemanticsNode().boundsInRoot.top + layout.getLineBottom(layout.lineCount - 1)
            val subtitle = rule.onNodeWithTag("$prefix-subtitle-$id", useUnmergedTree = true).assertIsDisplayed()
            val gap = subtitle.fetchSemanticsNode().boundsInRoot.top - actualBottom
            val maxGap = with(rule.density) { (if (session) 5.dp else 3.dp).toPx() }
            assertTrue("Episode follows the visible title line at font $fontScale: $gap px", gap >= -1f && gap <= maxGap)
            return layout
        }
        assertEquals(1, checkGap().lineCount)
        val artTag = if (session) "compact-session-art-$id" else "library-hit-artwork-$id"
        val art = rule.onNodeWithTag(artTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val caption = rule.onNodeWithTag("$prefix-caption-$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val action = if (session) rule.onNodeWithTag("compact-session-toggle-$id").fetchSemanticsNode().boundsInRoot else null
        rule.runOnIdle { title = "Ein mykje lengre serietittel som treng fleire linjer" }
        assertEquals(2, checkGap().lineCount)
        assertEquals(art, rule.onNodeWithTag(artTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot)
        assertEquals("Reserve remains below the whole caption", caption.height,
            rule.onNodeWithTag("$prefix-caption-$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height, 1f)
        if (session) assertEquals(action,
            rule.onNodeWithTag("compact-session-toggle-$id").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun sessionEpisodeFollowsItsActualTitleLine() = secondaryCaptionFlow(session = true, fontScale = 1f)
    @Test fun sessionEpisodeFollowsItsActualTitleLineAtDoubleText() = secondaryCaptionFlow(session = true, fontScale = 2f)
    @Test fun searchLibraryEpisodeFollowsItsActualTitleLineOnTv() = secondaryCaptionFlow(session = false, fontScale = 1f)
    @Test fun searchLibraryEpisodeFollowsItsActualTitleLineOnTvAtDoubleText() = secondaryCaptionFlow(session = false, fontScale = 2f)
}
