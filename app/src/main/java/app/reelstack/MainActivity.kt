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
import app.reelstack.ui.ReelstackApp
import app.reelstack.ui.ReelstackViewModel
import app.reelstack.ui.theme.ReelstackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : app.reelstack.localization.LocalizedActivity() {
    private var pendingSetupLink by mutableStateOf<String?>(null)
    private var pendingRequests by mutableStateOf(false)
    private var pendingDownloads by mutableStateOf(false)

    override fun onStart() {
        super.onStart()
        // Resume downloads a killed process left unfinished. Only a visible activity may start the
        // service, which is why this is here and not in Application.onCreate.
        val offline = (application as ReelstackApplication).container.offlineDownloads
        lifecycleScope.launch(Dispatchers.IO) { offline.resumeUnfinished() }
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
                app.reelstack.ui.StartupReveal(reelstackViewModel) {
                    // Kids mode is a separate shell beside the adult app, not a condition inside
                    // it: no rail, no tabs, no detail sheet. Branching here is what keeps that
                    // promise structural instead of a growing list of `if (isKidMode)` checks.
                    // Only the one flag decides the shell. Collecting the whole state here redrew
                    // the root on every change, down to a download's progress tick.
                    val kidMode by reelstackViewModel.kidShell.collectAsStateWithLifecycle()
                    if (kidMode) {
                        app.reelstack.ui.kids.KidsApp(viewModel = reelstackViewModel)
                    } else {
                        ReelstackApp(viewModel = reelstackViewModel)
                    }
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
