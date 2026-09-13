package app.reelstack.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.reelstack.R
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.SettingsActionRow

@Composable
internal fun SignOutAllSetting(state: ReelstackUiState, onSignOut: () -> Unit) {
    if (state.connections.any { it.baseUrl.isNotBlank() || it.token.isNotBlank() }) {
        SettingsActionRow(stringResource(R.string.sign_out_all), stringResource(R.string.sign_out_all_hint),
            "sign-out-all", onSignOut)
    }
}
