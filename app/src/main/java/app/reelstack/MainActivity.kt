package app.reelstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import app.reelstack.ui.ReelstackApp
import app.reelstack.ui.ReelstackViewModel
import app.reelstack.ui.theme.ReelstackTheme

class MainActivity : app.reelstack.localization.LocalizedActivity() {
    private var pendingSetupLink by mutableStateOf<String?>(null)
    private var pendingRequests by mutableStateOf(false)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRequests = intent.getBooleanExtra("open_requests", false)
        if (intent.action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "spole") {
            pendingSetupLink = intent.dataString
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRequests = savedInstanceState == null && intent.getBooleanExtra("open_requests", false)
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
                    ReelstackApp(viewModel = reelstackViewModel)
                }
                androidx.compose.runtime.LaunchedEffect(pendingSetupLink) {
                    pendingSetupLink?.let(reelstackViewModel::importSetupLink)
                    pendingSetupLink = null
                }
                androidx.compose.runtime.LaunchedEffect(pendingRequests) {
                    if (pendingRequests) reelstackViewModel.selectTab(app.reelstack.ui.AppTab.ACTIVITY)
                    pendingRequests = false
                }
            }
        }
    }
}
