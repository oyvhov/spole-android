package app.reelstack.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ReelstackApplication
import app.reelstack.data.security.PinResult
import app.reelstack.localization.LocalizedActivity
import app.reelstack.ui.components.PinEntrySheet
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelstackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stova asks to sign in with one of Spole's accounts (see [LauncherApproval]). The viewer picks the
 * account, confirms with Spole's PIN when one is set, and Spole approves Stova's Quick Connect code with
 * that account. Only the answer goes back; Stova then fetches its own sign-in from the server.
 */
class LauncherApproveActivity : LocalizedActivity() {
    private val container get() = (application as ReelstackApplication).container

    private sealed interface Step {
        data object Finding : Step
        data class Choose(val choices: List<LauncherApproval.Choice>) : Step
        data class Pin(val choice: LauncherApproval.Choice, val choices: List<LauncherApproval.Choice>) : Step
        data object Working : Step
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = LauncherApproval.request(callingActivity?.packageName, intent.getStringExtra(LauncherApproval.EXTRA_SERVER_URL),
            intent.getStringExtra(LauncherApproval.EXTRA_SERVER_ID), intent.getStringExtra(LauncherApproval.EXTRA_CODE),
            intent.getBooleanExtra(LauncherApproval.EXTRA_FOR_CHILD, false))
        if (request == null) { answer(LauncherApproval.ERROR_DECLINED); return }
        val label = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(request.launcher, 0)).toString() }
            .getOrDefault("Stova")
        val server = runCatching { java.net.URI(request.serverUrl).host }.getOrNull() ?: request.serverUrl
        setContent { ReelstackTheme { ApproveScreen(request, label, server) } }
    }

    private fun answer(error: String?) {
        if (error == null) setResult(RESULT_OK) else setResult(RESULT_CANCELED, Intent().putExtra(LauncherApproval.EXTRA_ERROR, error))
        finish()
    }

    @Composable
    private fun ApproveScreen(request: LauncherApproval.Request, label: String, server: String) {
        var step by remember { mutableStateOf<Step>(Step.Finding) }
        var pinError by remember { mutableStateOf<String?>(null) }
        var lockout by remember { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()
        val repository = container.connectionRepository
        LaunchedEffect(request) {
            val choices = withContext(Dispatchers.IO) {
                LauncherApproval.choices(request, repository.listProfiles(), { repository.list(it) }) { url -> container.serverProbe.info(url)?.id }
            }
            // Stova says so itself: Spole has no account on that server to offer.
            if (choices.isEmpty()) answer(LauncherApproval.ERROR_NOT_SIGNED_IN) else step = Step.Choose(choices)
        }
        fun approve(choice: LauncherApproval.Choice) {
            step = Step.Working
            scope.launch {
                val approved = withContext(Dispatchers.IO) {
                    runCatching { container.jellyfinAuthenticationClient.authorizeQuickConnect(choice.connection.baseUrl, choice.connection.token, request.code) }.isSuccess
                }
                answer(if (approved) null else LauncherApproval.ERROR_FAILED)
            }
        }
        fun pick(choice: LauncherApproval.Choice, choices: List<LauncherApproval.Choice>) {
            if (container.pinSecurity.isPinConfigured()) { pinError = null; step = Step.Pin(choice, choices) } else approve(choice)
        }

        Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
            when (val current = step) {
                Step.Finding, Step.Working -> CircularProgressIndicator()
                is Step.Pin -> PinEntrySheet(title = stringResource(R.string.launcher_approve_pin), error = pinError, lockoutSeconds = lockout,
                    onPinComplete = { pin ->
                        scope.launch {
                            when (val result = withContext(Dispatchers.IO) { container.pinSecurity.verifyPin(pin) }) {
                                PinResult.Success -> approve(current.choice)
                                is PinResult.Incorrect -> pinError = getString(R.string.profile_wrong_pin)
                                is PinResult.LockedOut -> { lockout = result.secondsRemaining; pinError = getString(R.string.profile_pin_locked, result.secondsRemaining) }
                                PinResult.Corrupted -> answer(LauncherApproval.ERROR_FAILED)
                            }
                        }
                    }, onCancel = { step = Step.Choose(current.choices) })
                is Step.Choose -> Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth(.9f), shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface) {
                    val first = remember { FocusRequester() }
                    LaunchedEffect(Unit) { delay(60); runCatching { first.requestFocus() } }
                    Column(Modifier.padding(24.dp).focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.launcher_approve_title, label), Modifier.semantics { heading() },
                            style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.launcher_approve_body, label, server), style = MaterialTheme.typography.bodyMedium, color = Muted)
                        current.choices.forEachIndexed { index, choice ->
                            SpoleSecondaryButton({ pick(choice, current.choices) },
                                Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(first) else Modifier)) {
                                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                    Text(choice.profile.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(if (choice.profile.isKid) R.string.launcher_approve_child else R.string.launcher_approve_main),
                                        style = MaterialTheme.typography.bodySmall, color = Muted)
                                }
                            }
                        }
                        SpoleSecondaryButton({ answer(LauncherApproval.ERROR_DECLINED) }) { Text(stringResource(R.string.account_cancel)) }
                    }
                }
            }
        }
    }
}
