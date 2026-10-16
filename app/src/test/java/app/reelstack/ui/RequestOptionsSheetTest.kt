package app.reelstack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.RequestDraft
import app.reelstack.data.model.RequestFolder
import app.reelstack.data.model.RequestOptions
import app.reelstack.data.model.RequestProfile
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "nn-w412dp-h900dp-xhdpi", application = app.reelstack.SheetTestApplication::class)
class RequestOptionsSheetTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val options = RequestOptions(0, false, listOf(RequestProfile(4, "HD-1080p"), RequestProfile(5, "Ultra-HD")),
        listOf(RequestFolder("/mnt/w/Filmar 10", 4_815_000_000_000)), defaultProfileId = 4, defaultFolder = "/mnt/w/Filmar 10")
    private val media = DiscoverMedia("seerr-movie-11", "Love of Your Life", "2026", R.drawable.media_placeholder, false, remoteId = 11, mediaType = "movie")
    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)
    private val chosen = mutableListOf<String>()

    private fun show(permissions: Long, draft: RequestDraft) = rule.setContent { ReelstackTheme {
        RequestComposer(ReelstackUiState(connections = listOf(seerr),
            accounts = mapOf(ServiceKind.SEERR to ServiceAccount(ServiceKind.SEERR, "7", "Ø", permissions = permissions)),
            requestDraft = draft), { _, _ -> }, {}, {}, {}, {}, {},
            onEdition = { chosen += "edition:$it" }, onProfile = { chosen += "profile:$it" }, onFolder = { chosen += "folder:$it" })
    } }

    @Test fun advancedUsersSeeQualityAndLocation() {
        show(8224, RequestDraft(media, loading = false, options = options, profileId = 4, folder = "/mnt/w/Filmar 10"))
        rule.onNodeWithTag("request-quality").assertTextContains("HD-1080p", substring = true)
        rule.onNodeWithTag("request-folder").assertTextContains("Filmar 10", substring = true)
        rule.onNodeWithTag("request-folder").assertTextContains("4,8 TB", substring = true)
    }

    @Test fun ordinaryUsersSeeNoChoices() {
        show(32, RequestDraft(media, loading = false))
        rule.onNodeWithTag("request-quality").assertDoesNotExist()
        rule.onNodeWithTag("request-edition").assertDoesNotExist()
    }

    @Test fun aQualityIsChosenFromAListInTheSameSheet() {
        show(8224, RequestDraft(media, loading = false, options = options, profileId = 4, folder = "/mnt/w/Filmar 10"))
        rule.onNodeWithTag("choice-dialog").assertDoesNotExist()
        rule.onNodeWithTag("request-quality").performScrollTo().performClick()
        rule.onNodeWithTag("request-quality-option-5").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf("profile:5"), chosen) }
        rule.onNodeWithTag("request-quality-option-5").assertDoesNotExist()
    }

    @Test fun theEditionShowsOnlyWhenFourKIsPossible() {
        show(1024 + 32, RequestDraft(media, loading = false, fourKAvailable = true))
        rule.onNodeWithTag("request-edition-4k").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf("edition:true"), chosen) }
    }
}
