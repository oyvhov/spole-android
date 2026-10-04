package app.reelstack

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.data.network.toLibraryMedia
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.localization.LocalizedText
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.screens.LibraryScreen
import app.reelstack.ui.screens.MediaCardActions
import app.reelstack.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class LibraryPresentationUiTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val context get() = rule.activity
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://media.example", "fixture", userId = "me")
    private val prefs get() = AppPreferencesRepository(context)
    private lateinit var input: androidx.compose.ui.input.InputModeManager

    /** Synthetic local artwork makes bitmap loading deterministic; no real accounts or network. */
    private fun art(name: String, wide: Boolean, colour: Int, key: String): String {
        val file = File(context.cacheDir, "library-review-$key-${if (wide) "wide" else "poster"}.png")
        val bitmap = Bitmap.createBitmap(if (wide) 640 else 320, if (wide) 360 else 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, bitmap.height.toFloat(), colour, 0xff151c2c.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPaint(paint)
        paint.shader = null
        paint.color = 0xffe5cc94.toInt()
        canvas.drawCircle(bitmap.width * .7f, bitmap.height * .29f, bitmap.width * .13f, paint)
        paint.color = 0xff172c3a.toInt()
        val mountain = android.graphics.Path().apply {
            moveTo(0f, bitmap.height * .75f); lineTo(bitmap.width * .4f, bitmap.height * .38f)
            lineTo(bitmap.width.toFloat(), bitmap.height * .85f); lineTo(bitmap.width.toFloat(), bitmap.height.toFloat())
            lineTo(0f, bitmap.height.toFloat()); close()
        }
        canvas.drawPath(mountain, paint)
        paint.color = android.graphics.Color.WHITE; paint.textSize = if (wide) 35f else 29f
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        canvas.drawText(name.uppercase(), 20f, bitmap.height * .85f, paint)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file.toURI().toString()
    }

    private fun state(id: String, series: Boolean = false): ReelstackUiState {
        val names = listOf("Nordlys", "Horisont", "Utferd", "Kysten", "Villmark", "Rundreise", "Vegen heim", "Fjellfolk")
        return ReelstackUiState(connections = listOf(connection), libraryPath = listOf(id to if (series) "Seriar" else "Filmar"),
            libraryCollectionType = if (series) "tvshows" else "movies",
            libraryEntries = names.mapIndexed { index, name ->
                val poster = art(name, false, listOf(0xff31546c, 0xff694c62, 0xff326666, 0xff5e6651)[index % 4].toInt(), "$id-$index")
                val thumb = art(name, true, listOf(0xff31546c, 0xff694c62, 0xff326666, 0xff5e6651)[index % 4].toInt(), "$id-$index")
                RemoteLibraryItem("review-$index", name, "2026", null, if (series) "Series" else "Movie", "review-$index",
                    artworkUrl = if (series) thumb else poster, thumbnailUrl = thumb, posterUrl = poster,
                    facts = listOf(LocalizedText.raw("★ 7,8")), played = index == 0,
                    unplayedItemCount = if (series && index == 1) 8 else null)
            })
    }

    private fun show(state: ReelstackUiState, tv: Boolean, actions: MediaCardActions? = null) {
        rule.setContent {
            input = androidx.compose.ui.platform.LocalInputModeManager.current
            val config = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or
                    if (tv) Configuration.UI_MODE_TYPE_TELEVISION else Configuration.UI_MODE_TYPE_NORMAL
            }
            CompositionLocalProvider(LocalConfiguration provides config, LocalPersonalization provides Personalization()) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(if (tv) DpSize(960.dp, 540.dp) else DpSize(412.dp, 840.dp))) {
                    ReelstackTheme { LibraryScreen(state, {}, {}, {}, cardActions = actions) }
                }
            }
        }
        rule.waitForIdle()
        if (tv) rule.runOnIdle { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
    }

    private fun withDefaults(id: String, block: () -> Unit) {
        val old = prefs.libraryDisplay(id)
        try { prefs.setLibraryDisplay(id, LibraryDisplay()); block() }
        finally { prefs.setLibraryDisplay(id, old) }
    }

    @Test fun tvSeriesHaveUniformWideFramesAndStableRemoteFocus() = withDefaults("review-tv-series") {
        show(state("review-tv-series", true), true)
        val first = rule.onNodeWithTag("library-art-review-0", true).getUnclippedBoundsInRoot()
        val second = rule.onNodeWithTag("library-art-review-1", true).getUnclippedBoundsInRoot()
        assertEquals(16f / 9f, (first.right - first.left).value / (first.bottom - first.top).value, .015f)
        assertEquals(first.top, second.top)
        assertEquals(first.bottom, second.bottom)
        val card = rule.onNodeWithTag("library-item-review-0")
        card.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        card.assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("library-item-review-1").assertIsFocused()
        assertEquals(first, rule.onNodeWithTag("library-art-review-0", true).getUnclippedBoundsInRoot())
        rule.onNodeWithTag("library-unwatched-review-1", true).assertIsDisplayed()
        rule.onAllNodesWithText("2026").assertCountEquals(0)
        saveTvReview("library-tv-series", rule.onNodeWithTag("library-browser").captureToImage())
    }

    @Test fun phoneMoviesHaveCoversAndQuietWatchedIndicators() = withDefaults("review-phone-films") {
        show(state("review-phone-films"), false)
        val first = rule.onNodeWithTag("library-art-review-0", true).getUnclippedBoundsInRoot()
        assertEquals(2f / 3f, (first.right - first.left).value / (first.bottom - first.top).value, .015f)
        rule.onNodeWithTag("library-watched-review-0", true).assertIsDisplayed()
        rule.onNodeWithTag("library-watched-review-1", true).assertDoesNotExist()
        rule.onNodeWithTag("library-rating-review-0", true).assertIsDisplayed()
        val title = rule.onNodeWithText("Nordlys", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val yearNode = rule.onAllNodesWithText("2026", useUnmergedTree = true).onFirst().assertIsDisplayed()
        val year = yearNode.getUnclippedBoundsInRoot()
        assertTrue("The movie year must sit directly below its title", year.top - title.bottom in 0.dp..4.dp)
        saveTvReview("library-phone-films", rule.onNodeWithTag("library-browser").captureToImage())
    }

    @Test fun phoneSeriesHaveTwoWideCardsPerRow() = withDefaults("review-phone-series") {
        show(state("review-phone-series", true), false)
        val first = rule.onNodeWithTag("library-art-review-0", true).getUnclippedBoundsInRoot()
        val second = rule.onNodeWithTag("library-art-review-1", true).getUnclippedBoundsInRoot()
        assertEquals(first.top, second.top)
        assertEquals(16f / 9f, (first.right - first.left).value / (first.bottom - first.top).value, .015f)
        rule.onAllNodesWithText("2026").assertCountEquals(0)
        val nextRow = rule.onNodeWithTag("library-art-review-2", true).getUnclippedBoundsInRoot()
        assertTrue("Captions must not leave a large blank gap between rows", nextRow.top - first.bottom < 95.dp)
        saveTvReview("library-phone-series", rule.onNodeWithTag("library-browser").captureToImage())
    }

    @Test fun coldThumbnailKeepsItsEdgesAfterFadingFromPortraitPlaceholder() {
        // Every run uses a new URI: a memory-cache hit would skip the faulty crossfade.
        val file = File(context.cacheDir, "library-ratio-${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(0xff204060.toInt())
            val paint = Paint().apply { color = 0xffd02030.toInt() }
            drawRect(0f, 0f, 160f, 360f, paint)
            drawRect(480f, 0f, 640f, 360f, paint)
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        rule.setContent {
            ReelstackTheme {
                val density = LocalDensity.current
                MediaArtwork(file.toURI().toString(), null,
                    modifier = Modifier.size(240.dp, 135.dp).testTag("cold-thumbnail"),
                    fallbackRes = R.drawable.media_placeholder,
                    requestSize = with(density) { IntSize(240.dp.roundToPx(), 135.dp.roundToPx()) })
            }
        }
        rule.waitUntil(5_000) {
            val pixels = rule.onNodeWithTag("cold-thumbnail").captureToImage().toPixelMap()
            val centre = pixels[pixels.width / 2, pixels.height / 2]
            centre.blue > .37f && centre.red < .14f
        }
        val pixels = rule.onNodeWithTag("cold-thumbnail").captureToImage().toPixelMap()
        for (x in listOf(pixels.width / 8, pixels.width * 7 / 8)) {
            for (y in listOf(pixels.height / 4, pixels.height * 3 / 4)) {
                val edge = pixels[x, y]
                assertTrue("The original wide artwork must fill its frame without clipping its edges", edge.red > .7f && edge.blue < .3f)
            }
        }
    }

    private fun choose(tv: Boolean, id: String) = withDefaults(id) {
        show(state(id, true), tv)
        val view = rule.onNodeWithTag("library-display-toggle")
        if (tv) {
            view.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            view.performKeyInput { pressKey(Key.DirectionCenter) }
        } else view.performClick()
        fun select(tag: String) {
            val node = rule.onNodeWithTag(tag).performScrollTo()
            if (tv) {
                node.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                node.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
            } else node.performClick()
        }
        select("library-art-POSTER")
        select("library-size-SMALL")
        listOf("library-titles-toggle", "library-ratings-toggle", "library-watched-toggle", "library-unwatched-toggle").forEach {
            rule.onNodeWithTag(it).performScrollTo().assertIsOn()
            select(it)
            rule.onNodeWithTag(it).assertIsOff()
        }
        saveTvReview("library-view-${if (tv) "tv" else "phone"}", rule.onNodeWithTag("choice-dialog").captureToImage())
        rule.onNodeWithContentDescription(context.getString(R.string.action_close)).performClick()
        val saved = AppPreferencesRepository(context).libraryDisplay(id)
        assertEquals(LibraryArtType.POSTER, saved.artType)
        assertEquals(LibraryCardSize.SMALL, saved.size)
        assertFalse(saved.showTitles); assertFalse(saved.showRatings); assertFalse(saved.showWatched); assertFalse(saved.showUnwatchedCount)
        rule.onNodeWithTag("library-rating-review-0", true).assertDoesNotExist()
        rule.onNodeWithTag("library-watched-review-0", true).assertDoesNotExist()
        rule.onNodeWithTag("library-unwatched-review-1", true).assertDoesNotExist()
        val frame = rule.onNodeWithTag("library-art-review-0", true).getUnclippedBoundsInRoot()
        assertEquals(2f / 3f, (frame.right - frame.left).value / (frame.bottom - frame.top).value, .015f)
        rule.onNodeWithContentDescription("Nordlys").assertExists()
    }

    @Test fun phoneDisplayOptionsSaveAndReachEverySwitch() = choose(false, "review-phone-options")
    @Test fun tvDisplayOptionsOpenFromRemoteAndSave() = choose(true, "review-tv-options")

    @Test fun libraryOverviewOffersArtworkAndMetadataChoicesOnItsOwnShelf() = withDefaults("review-hub") {
        val catalogue = state("review-hub", true)
        val content = catalogue.copy(libraryPath = emptyList(),
            libraryEntries = listOf(RemoteLibraryItem("review-hub", "Seriar", "", null, "CollectionFolder", null,
                isFolder = true, collectionType = "tvshows")),
            libraryPeeks = mapOf("review-hub" to catalogue.libraryEntries.map {
                it.toLibraryMedia(ServiceKind.JELLYFIN, R.drawable.media_placeholder, listOf("★ 7,8"))
            }))
        show(content, false)
        rule.onNodeWithTag("library-hub").performScrollToNode(hasTestTag("hub-view-review-hub"))
        rule.onAllNodesWithText("2026").assertCountEquals(0)
        rule.onNodeWithTag("hub-view-review-hub").performClick()
        rule.onNodeWithTag("library-art-POSTER").performScrollTo().performClick()
        rule.onNodeWithTag("library-titles-toggle").performScrollTo().performClick()
        rule.onNodeWithContentDescription(context.getString(R.string.action_close)).performClick()
        rule.onNodeWithTag("library-hub").performScrollToKey("shelf-review-hub")
        val frame = rule.onNodeWithTag("library-artwork-jellyfin-review-0", true).getUnclippedBoundsInRoot()
        assertEquals(2f / 3f, (frame.right - frame.left).value / (frame.bottom - frame.top).value, .015f)
        rule.onNodeWithTag("library-watched-jellyfin-review-0", true).assertExists()
        rule.onNodeWithText("Nordlys").assertDoesNotExist()
        assertFalse(prefs.libraryDisplay("review-hub").showTitles)
    }

    @Test fun libraryOverviewHasOneNextRowIndependentOfHomeSwitches() {
        val current = LibraryMedia("current", "Ei serie", "", progress = .4f, artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, seriesId = "series", mediaType = "Episode")
        val content = ReelstackUiState(connections = listOf(connection), resume = listOf(current),
            nextUp = listOf(current.copy(id = "duplicate", progress = null),
                current.copy(id = "new", title = "Ei anna serie", seriesId = "other", progress = null),
                current.copy(id = "emby-next", source = ServiceKind.EMBY, seriesId = "emby")))
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(412.dp, 840.dp))) {
                ReelstackTheme { CompositionLocalProvider(LocalPersonalization provides Personalization(showNextUp = false)) {
                    LibraryScreen(content, {}, {}, {})
                } }
            }
        }
        rule.onNodeWithText(context.getString(R.string.library_next)).assertIsDisplayed()
        rule.onNodeWithTag("resume-card-current").assertExists()
        rule.onNodeWithTag("resume-card-new").assertExists()
        rule.onNodeWithTag("resume-card-duplicate").assertDoesNotExist()
        rule.onNodeWithTag("resume-card-emby-next").assertDoesNotExist()
    }

    @Test fun libraryNextMergesSeriesAndPreservesResumeActions() = withDefaults("review-next") {
        fun episode(id: String, show: String) = LibraryMedia(id, id, "", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, seriesId = show, mediaType = "Episode")
        val resume = episode("current", "show").copy(progress = .4f)
        val next = episode("new-show", "other")
        val content = state("review-next", true).copy(libraryShelves = LibraryShelves("review-next",
            listOf(resume), listOf(episode("duplicate", "show"), next)))
        show(content, false, MediaCardActions({}, { _, _ -> }, { _, _ -> }))
        rule.onNodeWithText(context.getString(R.string.library_next)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.tv_next_up)).assertDoesNotExist()
        rule.onNodeWithText(context.getString(R.string.home_continue)).assertDoesNotExist()
        rule.onNodeWithTag("resume-card-duplicate").assertDoesNotExist()
        rule.onNodeWithTag("resume-card-new-show").performScrollTo().performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        rule.onNodeWithTag("card-remove-resume").assertDoesNotExist()
        rule.onNodeWithContentDescription(context.getString(R.string.action_close)).performClick()
        rule.onNodeWithTag("resume-card-current").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        rule.onNodeWithTag("card-remove-resume").assertIsDisplayed()
    }
}
