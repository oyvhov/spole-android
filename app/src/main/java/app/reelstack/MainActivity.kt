package app.reelstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reelstack.ui.ReelstackApp
import app.reelstack.ui.ReelstackViewModel
import app.reelstack.ui.theme.ReelstackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (intent.getBooleanExtra("open_requests", false)) reelstackViewModel.selectTab(app.reelstack.ui.AppTab.ACTIVITY)
                }
            }
        }
    }
}
