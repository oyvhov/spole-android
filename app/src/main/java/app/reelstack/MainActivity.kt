package app.reelstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.key.onPreviewKeyEvent
import app.reelstack.ui.ReelstackApp
import app.reelstack.ui.ReelstackViewModel
import app.reelstack.ui.theme.ReelstackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : app.reelstack.localization.LocalizedActivity() {
    private var pendingSetupLink by mutableStateOf<String?>(null)
    private var pendingLauncherOpen by mutableStateOf<app.reelstack.launcher.PendingLauncherOpen?>(null)
    private var pendingLauncherMessage by mutableStateOf<Int?>(null)
    private var pendingRequests by mutableStateOf(false)
    private var pendingDownloads by mutableStateOf(false)
    private var activeViewModel: ReelstackViewModel? = null

    override fun onStart() {
        super.onStart()
        activeViewModel?.synchronizeSession()
        // Resume downloads a killed process left unfinished. Only a visible activity may start the
        // service, which is why this is here and not in Application.onCreate.
        val offline = (application as ReelstackApplication).container.offlineDownloads
        lifecycleScope.launch(Dispatchers.IO) { offline.resumeUnfinished() }
    }

    /** Reports touches to [app.reelstack.ui.components.SpoleEggs]; keys are reported in [setContent]. */
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) app.reelstack.ui.components.SpoleEggs.touch()
        return super.dispatchTouchEvent(event)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRequests = intent.getBooleanExtra("open_requests", false)
        pendingDownloads = intent.getBooleanExtra("open_downloads", false)
        if (intent.action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "spole" && intent.data?.host == "setup") {
            pendingSetupLink = intent.dataString
        }
        receiveLauncherIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRequests = savedInstanceState == null && intent.getBooleanExtra("open_requests", false)
        pendingDownloads = savedInstanceState == null && intent.getBooleanExtra("open_downloads", false)
        if (savedInstanceState == null && intent.action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "spole" && intent.data?.host == "setup") {
            pendingSetupLink = intent.dataString
        }
        if (savedInstanceState == null) receiveLauncherIntent(intent)
        enableEdgeToEdge()
        val container = (application as ReelstackApplication).container
        setContent {
            ReelstackTheme {
                val reelstackViewModel: ReelstackViewModel = viewModel(
                    factory = ReelstackViewModel.Factory(container),
                )
                activeViewModel = reelstackViewModel
                // Every key on its way to the focused control passes here first. It is only
                // reported, never handled, so the remote works exactly as it did. See SpoleEggs.
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()
                    .onPreviewKeyEvent { app.reelstack.ui.components.SpoleEggs.key(it.nativeKeyEvent); false }) {
                val shelfActions = androidx.compose.runtime.remember(reelstackViewModel) {
                    object : app.reelstack.ui.components.SmartShelfActions {
                        override fun open(id: String) = reelstackViewModel.openSmartShelf(id)
                        override fun edit(id: String?, template: String?) = reelstackViewModel.openSmartShelfEditor(id, template)
                        override fun update(draft: app.reelstack.data.model.SmartShelf) = reelstackViewModel.updateSmartShelfDraft(draft)
                        override fun applyTemplate(template: String) = reelstackViewModel.applySmartShelfTemplate(template)
                        override fun save() = reelstackViewModel.saveSmartShelf()
                        override fun delete(id: String) = reelstackViewModel.deleteSmartShelf(id)
                        override fun close() = reelstackViewModel.closeSmartShelfEditor()
                        override fun retryFacets() = reelstackViewModel.retryCatalogueFacets()
                        override fun sort(id: String, sort: app.reelstack.data.model.SmartShelfSort) =
                            reelstackViewModel.setSmartShelfSort(id, sort)
                    }
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    app.reelstack.ui.components.LocalOpenSeasonShelf provides reelstackViewModel::openSeasonShelf,
                    app.reelstack.ui.components.LocalSmartShelfActions provides shelfActions,
                ) {
                app.reelstack.ui.StartupReveal(reelstackViewModel) {
                    // Kids mode is a separate shell beside the adult app, not a condition inside
                    // it: no rail, no tabs, no adult detail sheet. Branching here keeps that
                    // promise structural instead of a growing list of `if (isKidMode)` checks.
                    val authority by reelstackViewModel.sessionScope.collectAsStateWithLifecycle()
                    val rendered by reelstackViewModel.renderingSession.collectAsStateWithLifecycle()
                    if (rendered != authority) {
                        androidx.compose.material3.Surface(androidx.compose.ui.Modifier.fillMaxSize(),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.background) { }
                    } else {
                        androidx.compose.runtime.key(authority) {
                            if (authority.kidsMode) app.reelstack.ui.kids.KidsApp(viewModel = reelstackViewModel)
                            else ReelstackApp(viewModel = reelstackViewModel)
                        }
                    }
                }
                }
                // Over everything and touching nothing; see SpoleEggs.
                app.reelstack.ui.components.SpoleRewindOverlay()
                }
                androidx.compose.runtime.LaunchedEffect(pendingSetupLink) {
                    pendingSetupLink?.let(reelstackViewModel::importSetupLink)
                    pendingSetupLink = null
                }
                androidx.compose.runtime.LaunchedEffect(pendingLauncherOpen) {
                    val pending = pendingLauncherOpen
                    pendingLauncherOpen = null
                    val target = pending?.consume(container.connectionRepository, reelstackViewModel::selectProfile) { link, _ ->
                        reelstackViewModel.synchronizeSession()
                        when (link.action) {
                            app.reelstack.launcher.LauncherAction.DETAILS -> reelstackViewModel.openRemoteDetails(link.kind, link.itemId)
                            app.reelstack.launcher.LauncherAction.PLAY -> app.reelstack.player.JellyfinPlayerActivity.open(
                                this@MainActivity, link.itemId, source = link.kind, kidsMode = container.connectionRepository.isKidMode)
                        }
                    }
                    when (target) {
                        app.reelstack.launcher.LauncherTarget.BlockedInKidMode -> pendingLauncherMessage = R.string.launcher_kid_mode
                        app.reelstack.launcher.LauncherTarget.NotSignedIn -> pendingLauncherMessage = R.string.launcher_not_signed_in
                        else -> Unit
                    }
                }
                androidx.compose.runtime.LaunchedEffect(pendingLauncherMessage) {
                    pendingLauncherMessage?.let { resource ->
                        reelstackViewModel.openLauncherHome()
                        showLauncherMessage(resource)
                    }
                    pendingLauncherMessage = null
                }
                androidx.compose.runtime.LaunchedEffect(pendingRequests) {
                    if (pendingRequests) reelstackViewModel.openMyRequests()
                    pendingRequests = false
                }
                androidx.compose.runtime.LaunchedEffect(pendingDownloads) {
                    if (pendingDownloads) reelstackViewModel.selectTab(app.reelstack.ui.AppTab.DOWNLOADS)
                    pendingDownloads = false
                }
            }
        }
    }

    private fun receiveLauncherIntent(intent: android.content.Intent) {
        val repository = (application as ReelstackApplication).container.connectionRepository
        pendingLauncherOpen = app.reelstack.launcher.PendingLauncherOpen.from(intent, repository)
        // The child shell has no snackbar host; this short message is visible in either shell.
        val message = intent.getStringExtra("launcher_message")
        pendingLauncherMessage = listOf(R.string.launcher_not_signed_in, R.string.launcher_kid_mode).firstOrNull {
            message == getString(it)
        }
    }

    private fun showLauncherMessage(@androidx.annotation.StringRes resource: Int) {
        android.widget.Toast.makeText(this, resource, android.widget.Toast.LENGTH_LONG).show()
    }
}
