package app.reelstack.update

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import app.reelstack.ui.components.focusOutline
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reelstack.BuildConfig
import app.reelstack.R
import app.reelstack.ui.components.SettingsChoiceRow
import app.reelstack.ui.components.SettingsToggleRow

@Composable
internal fun updateModel(): AppUpdateModel = viewModel(factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
    LocalContext.current.applicationContext as Application))

/**
 * @param grouped draw as rows inside the phone page's section card — icon, label, switch — rather
 * than as the television panel's free-standing rows. The television lays its rows out with spacing
 * of its own; the phone page does not, so the free-standing form arrived there as three cards
 * jammed corner to corner.
 */
@Composable
internal fun AppUpdateSettings(grouped: Boolean = false) {
    val model = updateModel()
    val state by model.state.collectAsState()
    // Which version you are on, or which one is waiting, is the row's value.
    val version = state.release?.let { stringResource(R.string.update_available, it.tag) }
        ?: "Spole ${BuildConfig.VERSION_NAME}"
    if (grouped) {
        app.reelstack.ui.components.SettingsPreferenceAction(
            icon = app.reelstack.ui.components.SpoleIcons.Update,
            label = stringResource(R.string.update_title),
            value = version,
            tag = "app-updates",
            onClick = model::open,
        )
        app.reelstack.ui.components.SettingsPreferenceRow(
            icon = app.reelstack.ui.components.SpoleIcons.Refresh,
            label = stringResource(R.string.update_auto),
            description = stringResource(R.string.update_auto_hint),
            checked = state.automatic,
            tag = "update-auto",
            onCheckedChange = model::automatic,
        )
        app.reelstack.ui.components.SettingsPreferenceRow(
            icon = app.reelstack.ui.components.SpoleIcons.CloudReady,
            label = stringResource(R.string.update_previews),
            description = stringResource(R.string.update_previews_hint),
            checked = state.previews,
            tag = "update-previews",
            onCheckedChange = model::previews,
        )
        return
    }
    SettingsChoiceRow(stringResource(R.string.update_title), version, "app-updates", onClick = model::open)
    SettingsToggleRow(stringResource(R.string.update_auto), stringResource(R.string.update_auto_hint), state.automatic, "update-auto", model::automatic)
    SettingsToggleRow(stringResource(R.string.update_previews), stringResource(R.string.update_previews_hint), state.previews, "update-previews", model::previews)
}

@Composable
internal fun AppUpdateHost(showBanner: Boolean) {
    val model = updateModel()
    val state by model.state.collectAsState()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) { model.check(); kotlinx.coroutines.awaitCancellation() }
    }
    if (state.banner && showBanner && !state.open) Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.TopEnd) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp, modifier = Modifier.widthIn(max = 460.dp).testTag("update-banner")) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.update_available, state.release?.tag.orEmpty()), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.update_banner_hint), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = model::open) { Text(stringResource(R.string.update_view)) }
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = model::later) { Text(stringResource(R.string.update_later)) }
                }
            }
        }
    }
    if (state.open) Dialog(onDismissRequest = model::close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val focus = remember { FocusRequester() }
        val interaction = remember { MutableInteractionSource() }
        val tv = androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        LaunchedEffect(tv, state.release, state.ready, state.downloading, state.checking) {
            if (tv && !state.checking) { withFrameNanos { }; focus.requestFocus() }
        }
        val actionModifier = Modifier.focusRequester(focus).focusOutline(interaction, CircleShape)
        Surface(Modifier.widthIn(max = 740.dp).fillMaxWidth(.94f).fillMaxHeight(.88f), shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(app.reelstack.ui.components.SpoleIcons.Update, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.update_title), style = MaterialTheme.typography.headlineSmall)
                        Text("Spole ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = model::close) { Text(stringResource(R.string.update_close)) }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.release?.let { release ->
                        Text(stringResource(R.string.update_available, release.tag), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.update_size, release.size / (1024f * 1024f)))
                        Text(release.notes.ifBlank { stringResource(R.string.update_no_notes) }, style = MaterialTheme.typography.bodyMedium)
                    } ?: Text(stringResource(R.string.update_intro))
                    state.message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("update-message")) }
                }
                if (state.checking) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.downloading) {
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.update_progress, (state.progress * 100).toInt()))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        state.downloading -> OutlinedButton(onClick = model::cancel, interactionSource = interaction, modifier = actionModifier) { Text(stringResource(R.string.update_cancel)) }
                        state.ready -> Button(onClick = model::install, interactionSource = interaction, modifier = actionModifier.testTag("update-install")) { Text(stringResource(R.string.update_install)) }
                        state.release != null -> Button(onClick = model::download, interactionSource = interaction, enabled = !state.checking, modifier = actionModifier.testTag("update-download")) { Text(stringResource(R.string.update_download)) }
                    }
                    OutlinedButton(onClick = { model.check(true) }, interactionSource = if (state.release == null) interaction else null,
                        modifier = if (state.release == null) actionModifier else Modifier,
                        enabled = !state.checking && !state.downloading) { Text(stringResource(R.string.update_check)) }
                }
                Text(stringResource(R.string.update_install_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
