package app.reelstack.ui
import app.reelstack.ui.components.SpoleDropdownMenu as DropdownMenu
import app.reelstack.ui.components.SpoleDropdownMenuItem as DropdownMenuItem

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.reelstack.R

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import app.reelstack.background.LibraryNotifications
import app.reelstack.data.model.*
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.RequestIdentity
import app.reelstack.ui.components.SheetToolbar
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*
import app.reelstack.ui.theme.Text as TextColor

@Composable
fun RequestComposer(state: ReelstackUiState, onSeason: (Int, Boolean) -> Unit, onNotify: (Boolean) -> Unit,
                    onConfirm: () -> Unit, onDismiss: () -> Unit, onRetry: () -> Unit, onAccount: () -> Unit,
                    onSeasonWatch: (Int, Boolean) -> Unit = { _, _ -> }, entered: Boolean = true,
                    onEdition: (Boolean) -> Unit = {}, onProfile: (Int) -> Unit = {}, onFolder: (String) -> Unit = {}) {
    val draft = state.requestDraft ?: return
    val context = LocalContext.current
    val confirm by rememberUpdatedState(onConfirm)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { confirm() }
    val isSeries = draft.media.mediaType == "tv"
    val ready = entered && !draft.loading
    val contentAlpha by animateFloatAsState(if (ready) 1f else 0f, tween(180), label = "season-content")
    val canAdd = state.configuredCount == 0 || state.accounts[ServiceKind.SEERR]?.canRequestType(if (isSeries) "tv" else "movie") == true
    var pendingWatch by remember(draft.media.id) { mutableStateOf<Int?>(null) }
    val watchAction by rememberUpdatedState(onSeasonWatch)
    val watchPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingWatch?.let { watchAction(it, true) }
        pendingWatch = null
    }
    val busy = draft.sending || draft.savingWatch != null
    val hasSelection = !isSeries || draft.selected.isNotEmpty()
    val television = androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val confirmFocus = remember(draft.media.id) { androidx.compose.ui.focus.FocusRequester() }
    val choiceFocus = remember(draft.media.id) { androidx.compose.ui.focus.FocusRequester() }
    val seasonFocus = remember(draft.media.id) { androidx.compose.ui.focus.FocusRequester() }
    val firstOpenSeason = draft.visibleSeasons.firstOrNull { it.canRequest }?.number
    LaunchedEffect(ready, television, firstOpenSeason) {
        if (!television || !ready) return@LaunchedEffect
        // A film starts on Send: the sheet fits on one screen, so every choice above it is in view.
        // A series starts on its first season, since nothing can be sent before one is chosen.
        val target = when {
            isSeries && firstOpenSeason != null -> seasonFocus
            !isSeries && canAdd && draft.error == null && draft.rules?.canRequest != false && !draft.quotaExceeded -> confirmFocus
            else -> return@LaunchedEffect
        }
        repeat(40) {
            androidx.compose.runtime.withFrameNanos { }
            if (runCatching { target.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
    Column(Modifier.fillMaxSize().testTag("request-composer")) {
        SheetToolbar(if (isSeries) stringResource(R.string.flow_seasons) else stringResource(R.string.flow_new_request), stringResource(R.string.flow_close_request), onDismiss, enabled = !draft.sending)
        // Who you are and what you are looking at is worth one screenful only once. On a series
        // the header stays put and the seasons — the thing the sheet exists for — own the height
        // that is left, so a twelve-season show scrolls its list rather than its introduction. A
        // single-title request has no list under it, so it keeps one scroll from top to bottom.
        val header: @Composable ColumnScope.() -> Unit = {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            // A series sheet is a list you act on, so its header pays for itself in one line: the
            // toolbar already says "Seasons" and the picture is the one you just came from. A
            // single-title request has nothing below it, so it keeps the full introduction.
            val wideMedia = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value).useSideBySideMedia
            // A film is one decision: a small cover, its name and who asks, nothing to read first.
            val posterWidth = if (isSeries) 48.dp else 56.dp
            Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaArtwork(draft.media.artworkUrl, null, Modifier.width(posterWidth).height(posterWidth * 1.5f).clip(RoundedCornerShape(10.dp)), fallbackRes = draft.media.artworkRes, ContentScale.Fit, ServiceKind.SEERR)
                Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(draft.media.title,
                        modifier = Modifier.testTag("request-title"),
                        fontSize = if (isSeries) 17.sp else 20.sp, lineHeight = if (isSeries) 22.sp else 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!isSeries) RequestIdentity(state, onAccount, compact = true)
                }
                if (isSeries && wideMedia) Box(Modifier.widthIn(max = 180.dp).padding(start = 12.dp)) {
                    RequestIdentity(state, onAccount, compact = true)
                }
            }
            if (isSeries && !wideMedia) RequestIdentity(state, onAccount, compact = true)
            }
            }
        }
        if (isSeries) Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("request-header")) { header() }
        Column(Modifier.weight(1f).testTag("request-scroll").verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            if (!isSeries) {
                header()
                if (ready && state.configuredCount > 0) RequestPreflight(draft, onRetry, compact = true)
            }
            if (ready) RequestChoiceRows(draft, onEdition, onProfile, onFolder, choiceFocus)
            if (!ready) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("seasons-loading"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.flow_checking), color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
                    repeat(3) {
                        Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceRaised))
                    }
                }
            } else if (isSeries) {
                Column(Modifier.fillMaxWidth().graphicsLayer { alpha = contentAlpha }) {
                draft.nextEpisode?.let { app.reelstack.localization.nextEpisodeDescription(it) }?.let { next ->
                    Text(next, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 20.dp).testTag("next-episode"))
                }
                Text(if (draft.visibleSeasons.any { it.canRequest } && canAdd) stringResource(R.string.flow_choose_only)
                    else stringResource(R.string.flow_status_source), color = Muted, fontSize = 13.sp, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                draft.visibleSeasons.forEach { season ->
                    val enabled = season.canRequest && canAdd && !busy && draft.error == null && draft.mediaStatus != 6
                    val interaction = remember(season.number) { MutableInteractionSource() }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).defaultMinSize(minHeight = 56.dp)
                            .then(if (season.number == firstOpenSeason) Modifier.focusRequester(seasonFocus) else Modifier)
                            .clip(RoundedCornerShape(ReelLayout.ControlCorner))
                            .focusOutline(interaction, RoundedCornerShape(ReelLayout.ControlCorner), glow = false)
                            .focusable(enabled = television && !enabled, interactionSource = interaction)
                            .toggleable(season.number in draft.selected, interactionSource = interaction,
                                indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled, role = Role.Checkbox,
                                onValueChange = { onSeason(season.number, it) }).testTag("request-season-${season.number}")
                            .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(app.reelstack.localization.seasonDisplayName(season), fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
                                Text(listOfNotNull(app.reelstack.localization.seasonDescription(season), season.episodes.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.flow_episode_count, it, it) }).joinToString(" · "),
                                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            if (season.canRequest && canAdd) Checkbox(checked = season.number in draft.selected, onCheckedChange = null, enabled = enabled)
                            else if (!season.canWatch) Icon(if (season.status == 5) app.reelstack.ui.components.SpoleIcons.DoneCircle else app.reelstack.ui.components.SpoleIcons.Info,
                                null, tint = if (season.status == 5) Success else Muted, modifier = Modifier.size(24.dp).padding(end = 2.dp))
                        }
                        if (season.canWatch && draft.mediaStatus != 6 && state.accounts[ServiceKind.SEERR]?.isPersonal == true) {
                            val watched = season.number in draft.watchedSeasons
                            val watchLabel = stringResource(if (watched) R.string.flow_watch_off else R.string.flow_watch_on,
                                app.reelstack.localization.seasonDisplayName(season).lowercase(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]))
                            IconToggleButton(watched, enabled = !busy && draft.error == null,
                                onCheckedChange = { enabledWatch ->
                                    if (enabledWatch && state.notificationsEnabled && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context)) {
                                        pendingWatch = season.number
                                        watchPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else onSeasonWatch(season.number, enabledWatch)
                                }, modifier = Modifier.testTag("watch-season-${season.number}").semantics {
                                    contentDescription = watchLabel
                                }) {
                                if (draft.savingWatch == season.number) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(if (watched) app.reelstack.ui.components.SpoleIcons.Bell else app.reelstack.ui.components.SpoleIcons.BellOff,
                                    null, tint = if (watched) Primary else Muted)
                            }
                        }
                    }
                    HorizontalDivider(color = SurfaceRaised)
                }
                if (draft.visibleSeasons.any { it.status == 4 }) {
                    Text(stringResource(R.string.flow_partial_note),
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 14.dp))
                }
                if (draft.visibleSeasons.any { it.canWatch } && state.accounts[ServiceKind.SEERR]?.isPersonal == true) {
                    Text(stringResource(R.string.flow_watch_note),
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
                }
                draft.watchError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 12.dp))
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = onRetry, enabled = !busy) { Text(stringResource(R.string.flow_recheck)) }
                }
                }
            }
            // What sending costs you is read on the way to the button, so on a series it sits
            // after the list rather than in front of it. Two sentences of reference text above the
            // seasons pushed the second one under the fold on a two-season show.
            if (isSeries && ready && state.configuredCount > 0) RequestPreflight(draft, onRetry)
            if (ready && hasSelection && canAdd) {
            HorizontalDivider(Modifier.padding(vertical = if (isSeries) 18.dp else 4.dp), color = SurfaceRaised)
            Row(Modifier.fillMaxWidth().toggleable(draft.notify, enabled = !draft.sending, role = Role.Checkbox, onValueChange = onNotify)
                .padding(vertical = 8.dp).testTag("request-notification"), verticalAlignment = Alignment.CenterVertically) {
                Icon(app.reelstack.ui.components.SpoleIcons.Bell, null, tint = Primary, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(stringResource(R.string.flow_notify_ready), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    Text(if (isSeries) stringResource(R.string.flow_notify_seasons) else stringResource(R.string.flow_notify_movie),
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Checkbox(draft.notify, onCheckedChange = null, enabled = !draft.sending)
            }
            // For a film only a reason the alert would not arrive is worth a line.
            when {
                !state.notificationsEnabled -> stringResource(R.string.flow_notifications_off)
                !LibraryNotifications.allowed(context) -> stringResource(R.string.flow_android_permission)
                isSeries -> stringResource(R.string.flow_background_note)
                else -> null
            }?.let { Text(it, color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
            if (ready && isSeries && !hasSelection && draft.watchedSeasons.isNotEmpty() &&
                (!state.notificationsEnabled || !LibraryNotifications.allowed(context))) {
                Text(stringResource(R.string.flow_permission_off),
                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
            }
            draft.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 14.dp))
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = onRetry, enabled = !draft.sending) { Text(stringResource(R.string.flow_recheck)) }
            }
            Spacer(Modifier.height(if (isSeries) 18.dp else 4.dp))
        }
        Column(Modifier.fillMaxWidth().background(Surface).padding(horizontal = 24.dp, vertical = if (isSeries) 12.dp else 6.dp)) {
            if (state.configuredCount == 0) Text(stringResource(R.string.flow_preview), color = Muted,
                fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
            if (isSeries && ready && draft.error == null && (!canAdd || draft.visibleSeasons.none { it.canRequest })) {
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = onDismiss, enabled = !draft.sending, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(stringResource(R.string.flow_done)) }
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val cancelInteraction = remember { MutableInteractionSource() }
                if (television) TextButton(onClick = onDismiss, enabled = !draft.sending, interactionSource = cancelInteraction,
                    colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                    modifier = Modifier.heightIn(min = 54.dp).focusOutline(cancelInteraction, RoundedCornerShape(27.dp), glow = false)
                        .testTag("cancel-request")) {
                    Text(stringResource(R.string.request_cancel), style = MaterialTheme.typography.titleMedium)
                }
                val confirmInteraction = remember { MutableInteractionSource() }
                TextButton(onClick = {
                    if (draft.notify && state.notificationsEnabled && state.configuredCount > 0 && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context))
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else onConfirm()
                }, enabled = ready && !busy && canAdd && draft.rules?.canRequest != false && !draft.quotaExceeded && draft.error == null &&
                    hasSelection && !draft.optionsLoading,
                    colors = ButtonDefaults.textButtonColors(contentColor = Primary),
                    interactionSource = confirmInteraction,
                    modifier = Modifier.weight(1f).heightIn(min = 54.dp).focusRequester(confirmFocus)
                        .focusOutline(confirmInteraction, RoundedCornerShape(27.dp), glow = false).testTag("confirm-request")) {
                    if (draft.sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                    Text(if (draft.sending) stringResource(R.string.flow_sending) else if (isSeries && draft.selected.isEmpty()) stringResource(R.string.flow_choose_seasons)
                        else if (isSeries) pluralStringResource(R.plurals.flow_send_seasons, draft.selected.size, draft.selected.size) else stringResource(R.string.flow_send),
                        modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium)
                    Icon(app.reelstack.ui.components.SpoleIcons.ArrowForward, null)
                }
            }
        }
    }
}

/**
 * Edition, quality and location: a muted name with its value under it, so neither squeezes the
 * other at large text. A tap on quality or location opens its list right under the line, in the
 * same sheet, and a choice closes it again.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestChoiceRows(draft: RequestDraft, onEdition: (Boolean) -> Unit, onProfile: (Int) -> Unit, onFolder: (String) -> Unit,
    firstFocus: androidx.compose.ui.focus.FocusRequester) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    var picking by remember(draft.media.id, draft.fourK) { mutableStateOf<String?>(null) }
    val options = draft.options
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        if (draft.fourKAvailable) ChoiceRow(stringResource(R.string.request_edition), "request-edition") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to R.string.request_edition_standard, true to R.string.request_edition_4k).forEach { (value, label) ->
                    val chosen = draft.fourK == value
                    val interaction = remember(value) { MutableInteractionSource() }
                    Text(stringResource(label), color = if (chosen) TextColor else Muted,
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.heightIn(min = 44.dp)
                            .then(if (!value) Modifier.focusRequester(firstFocus) else Modifier)
                            .focusOutline(interaction, RoundedCornerShape(8.dp), glow = false)
                            .toggleable(chosen, interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
                                role = Role.RadioButton, enabled = !draft.sending) { onEdition(value) }
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .testTag("request-edition-${if (value) "4k" else "standard"}"))
                }
            }
        }
        if (draft.optionsLoading) Text(stringResource(R.string.flow_checking), color = Muted, fontSize = 13.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 12.dp))
        if (options != null) {
            val profile = options.profiles.firstOrNull { it.id == draft.profileId }
            if (options.profiles.isNotEmpty()) ChoiceRow(stringResource(R.string.request_quality), "request-quality",
                onClick = { picking = if (picking == "quality") null else "quality" }.takeUnless { draft.sending },
                modifier = if (!draft.fourKAvailable) Modifier.focusRequester(firstFocus) else Modifier) {
                ValueColumn(profile?.name.orEmpty(), stringResource(R.string.request_usual_choice).takeIf { draft.profileId == options.defaultProfileId })
            }
            if (picking == "quality") options.profiles.forEach { p ->
                ChoiceOption(p.name, null, p.id == draft.profileId, "request-quality-option-${p.id}") { onProfile(p.id); picking = null }
            }
            val folder = options.folders.firstOrNull { it.path == draft.folder }
            if (options.folders.isNotEmpty()) ChoiceRow(stringResource(R.string.request_folder), "request-folder",
                onClick = { picking = if (picking == "folder") null else "folder" }.takeUnless { draft.sending },
                modifier = if (!draft.fourKAvailable && options.profiles.isEmpty()) Modifier.focusRequester(firstFocus) else Modifier) {
                ValueColumn(listOfNotNull(folder?.name, folder?.freeBytes?.let { stringResource(R.string.request_free_space, formatFreeSpace(it, locale)) })
                    .joinToString("  ·  "),
                    listOfNotNull(folder?.path, stringResource(R.string.request_usual_choice).takeIf { draft.folder == options.defaultFolder }).joinToString(" · "))
            }
            if (picking == "folder") options.folders.forEach { f ->
                ChoiceOption(f.name, listOfNotNull(f.path, f.freeBytes?.let { stringResource(R.string.request_free_space, formatFreeSpace(it, locale)) })
                    .joinToString(" · "), f.path == draft.folder, "request-folder-option-${f.path.hashCode()}") { onFolder(f.path); picking = null }
            }
        }
        if (draft.fourKAvailable || options != null) HorizontalDivider(color = TextColor.copy(alpha = .08f))
    }
}

@Composable
private fun ChoiceRow(label: String, tag: String, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    // A television line is wide enough for name and value side by side, which keeps the whole sheet
    // on one screen; a phone or large text puts the value under its name instead.
    val sideBySide = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION &&
        androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.3f
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = TextColor.copy(alpha = .08f))
        Row(Modifier.fillMaxWidth().heightIn(min = if (sideBySide) 52.dp else 60.dp).then(modifier)
            .then(if (onClick != null) Modifier.focusOutline(interaction, RoundedCornerShape(10.dp), glow = false)
                .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button, onClick = onClick) else Modifier)
            // Inside the focus ring, in line with the season rows below.
            .testTag(tag).padding(horizontal = 12.dp, vertical = if (sideBySide) 6.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sideBySide) {
                Text(label, color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 120.dp)
                    .padding(end = 16.dp).testTag("$tag-label"))
                Box(Modifier.weight(1f).testTag("$tag-value"), contentAlignment = Alignment.CenterEnd) { value() }
            } else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("$tag-label"))
                Box(Modifier.testTag("$tag-value")) { value() }
            }
            if (onClick != null) Icon(app.reelstack.ui.components.SpoleIcons.ChevronDown, null, tint = Muted,
                modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
    }
}

@Composable
private fun ValueColumn(main: String, detail: String?) {
    val end = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION &&
        androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.3f
    Column(horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(main, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        detail?.takeIf(String::isNotBlank)?.let {
            Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** One entry of an open list: its name, a muted detail, and a check on the chosen one. */
@Composable
private fun ChoiceOption(name: String, detail: String?, chosen: Boolean, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp)
        .focusOutline(interaction, RoundedCornerShape(10.dp), glow = false)
        .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
            role = Role.RadioButton, onClick = onClick)
        .testTag(tag).padding(vertical = 8.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, color = if (chosen) TextColor else Muted, style = MaterialTheme.typography.bodyLarge)
            detail?.takeIf(String::isNotBlank)?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        if (chosen) Icon(app.reelstack.ui.components.SpoleIcons.Done, null, tint = TextColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CompactRequestProgress(item: TrackedRequest, modifier: Modifier = Modifier) {
    val filled = when (item.stage) {
        RequestStage.AVAILABLE -> 3
        RequestStage.DOWNLOADING, RequestStage.IMPORTING -> 2
        RequestStage.REQUESTED -> 1
        else -> 0
    }
    val progressLabel = stringResource(R.string.flow_progress, app.reelstack.localization.requestStageLabel(item.stage))
    Column(modifier.clearAndSetSemantics { contentDescription = progressLabel }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape)
                    .background(if (index < filled) Primary else Divider))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            Text(app.reelstack.localization.requestStageExplanation(item.stage), color = Muted, fontSize = 11.sp, lineHeight = 16.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (item.seasons.isNotEmpty() && item.availableSeasons.isNotEmpty()) {
                Text(pluralStringResource(R.plurals.flow_season_fraction, item.seasons.size, item.availableSeasons.size, item.seasons.size), color = PrimarySoft,
                    fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
