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
        if (intent.action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "spole") {
            pendingSetupLink = intent.dataString
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRequests = savedInstanceState == null && intent.getBooleanExtra("open_requests", false)
        pendingDownloads = savedInstanceState == null && intent.getBooleanExtra("open_downloads", false)
        if (savedInstanceState == null && intent.action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "spole") {
            pendingSetupLink = intent.dataString
        }
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
                    // it: no rail, no tabs, no detail sheet. Branching here is what keeps that
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
                androidx.compose.runtime.LaunchedEffect(pendingRequests) {
                    if (pendingRequests) reelstackViewModel.selectTab(app.reelstack.ui.AppTab.ACTIVITY)
                    pendingRequests = false
                }
                androidx.compose.runtime.LaunchedEffect(pendingDownloads) {
                    if (pendingDownloads) reelstackViewModel.selectTab(app.reelstack.ui.AppTab.DOWNLOADS)
                    pendingDownloads = false
                }
            }
        }
    }
}
