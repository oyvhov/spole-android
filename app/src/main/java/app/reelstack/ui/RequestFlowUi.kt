package app.reelstack.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.reelstack.background.LibraryNotifications
import app.reelstack.data.model.*
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.RequestIdentity
import app.reelstack.ui.components.SheetToolbar
import app.reelstack.ui.theme.*

@Composable
fun RequestComposer(state: ReelstackUiState, onSeason: (Int, Boolean) -> Unit, onNotify: (Boolean) -> Unit,
                    onConfirm: () -> Unit, onDismiss: () -> Unit, onRetry: () -> Unit, onAccount: () -> Unit) {
    val draft = state.requestDraft ?: return
    val context = LocalContext.current
    val confirm by rememberUpdatedState(onConfirm)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { confirm() }
    val isSeries = draft.media.mediaType == "tv"
    Column(Modifier.fillMaxSize().testTag("request-composer")) {
        SheetToolbar("Ny førespurnad", "Lukk førespurnaden", onDismiss, enabled = !draft.sending)
        Column(Modifier.weight(1f).testTag("request-scroll").verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaArtwork(draft.media.artworkUrl, draft.media.artworkRes, null,
                    Modifier.width(82.dp).height(123.dp).clip(RoundedCornerShape(10.dp)), ContentScale.Fit, ServiceKind.SEERR)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(draft.media.title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (isSeries) "Vel sesongane du vil leggje til" else "Legg filmen til i mediesamlinga", color = Muted,
                        fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            RequestIdentity(state, onAccount)
            app.reelstack.ui.components.RequestJourney(null, Modifier.padding(top = 20.dp, bottom = 8.dp))
            if (draft.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 24.dp))
                Text("Sjekkar bibliotek og førespurnader…", color = Muted, fontSize = 13.sp)
            } else if (isSeries) {
                Text("Sesongar", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                if (draft.seasons.isNotEmpty() && draft.seasons.none { it.canRequest }) {
                    Text("Ingen nye sesongar å leggje til. Sjå status under Aktivitet.", color = Muted, fontSize = 13.sp)
                }
                draft.seasons.forEach { season ->
                    val enabled = season.canRequest && !draft.sending && draft.mediaStatus != 6
                    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 66.dp)
                        .toggleable(season.number in draft.selected, enabled = enabled, role = Role.Checkbox,
                            onValueChange = { onSeason(season.number, it) }).testTag("request-season-${season.number}")
                        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(season.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(listOfNotNull(season.label, season.episodes.takeIf { it > 0 }?.let { if (it == 1) "1 episode" else "$it episodar" }).joinToString(" · "),
                                color = Muted, fontSize = 12.sp)
                        }
                        if (season.status == 5) Icon(Icons.Rounded.CheckCircle, null, tint = Primary, modifier = Modifier.size(24.dp))
                        else Checkbox(checked = season.number in draft.selected, onCheckedChange = null, enabled = enabled)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 18.dp), color = SurfaceRaised)
            Row(Modifier.fillMaxWidth().toggleable(draft.notify, enabled = !draft.sending, role = Role.Checkbox, onValueChange = onNotify)
                .padding(vertical = 8.dp).testTag("request-notification"), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.NotificationsActive, null, tint = Primary, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Varsle når det er klart", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(if (isSeries) "Når dei valde sesongane er i biblioteket." else "Når filmen er i biblioteket.",
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Checkbox(draft.notify, onCheckedChange = null, enabled = !draft.sending)
            }
            Text(when {
                !state.notificationsEnabled -> "Appvarsel er slått av i Innstillingar. Statusen kan framleis følgjast i Aktivitet."
                !LibraryNotifications.allowed(context) -> "Android må tillate varsel. Du kan framleis følgje status i Aktivitet."
                else -> "Appen sjekkar i bakgrunnen. Android kan forseinke varselet."
            }, color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            draft.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
                TextButton(onClick = onRetry, enabled = !draft.sending) { Text("Sjekk på nytt") }
            }
            Spacer(Modifier.height(18.dp))
        }
        Column(Modifier.fillMaxWidth().background(Surface).padding(horizontal = 24.dp, vertical = 12.dp)) {
            if (state.configuredCount == 0) Text("Førehandsvising · ingenting blir sendt", color = Muted,
                fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp))
            Button(onClick = {
                if (draft.notify && state.notificationsEnabled && state.configuredCount > 0 && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context))
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else onConfirm()
            }, enabled = !draft.loading && !draft.sending && draft.error == null && (!isSeries || draft.selected.isNotEmpty()),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("confirm-request")) {
                if (draft.sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                Text(if (draft.sending) "Sender…" else if (isSeries) "Send førespurnad · ${draft.selected.size} " + (if (draft.selected.size == 1) "sesong" else "sesongar") else "Send førespurnad",
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
fun TrackedRequestCard(item: TrackedRequest, onDetails: () -> Unit, onNotify: (Boolean) -> Unit) {
    val context = LocalContext.current
    val notifyAction by rememberUpdatedState(onNotify)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifyAction(true) }
    val active = item.stage != RequestStage.AVAILABLE
    Surface(
        color = SurfaceRaised,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("tracked-request-${item.key}"),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clickable(onClickLabel = "Vis detaljar for ${item.title}", onClick = onDetails)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediaArtwork(
                        item.artworkUrl, app.reelstack.R.drawable.media_placeholder, null,
                        Modifier.width(68.dp).height(102.dp).clip(RoundedCornerShape(10.dp)),
                        ContentScale.Crop, ServiceKind.SEERR,
                    )
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp,
                            lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(
                            (if (item.seasons.isEmpty()) "Film" else "Sesong ${item.seasons.sorted().joinToString(", ")}") +
                                if (item.is4k) " · 4K" else "",
                            color = Muted, fontSize = 11.sp, maxLines = 1,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 9.dp)) {
                            Icon(
                                when (item.stage) {
                                    RequestStage.AVAILABLE -> Icons.Rounded.CheckCircle
                                    RequestStage.DOWNLOADING -> Icons.Rounded.Download
                                    RequestStage.FAILED, RequestStage.DECLINED -> Icons.Rounded.ErrorOutline
                                    else -> Icons.Rounded.Schedule
                                }, null,
                                tint = when (item.stage) {
                                    RequestStage.AVAILABLE -> Success
                                    RequestStage.FAILED, RequestStage.DECLINED -> Warning
                                    else -> Primary
                                }, modifier = Modifier.size(16.dp),
                            )
                            Text(
                                item.stage.label + (item.percent?.let { " · $it %" } ?: ""),
                                color = when (item.stage) {
                                    RequestStage.AVAILABLE -> Success
                                    RequestStage.DECLINED, RequestStage.FAILED -> Warning
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    if (!active) Icon(Icons.Rounded.ChevronRight, "Vis detaljar", tint = Muted, modifier = Modifier.size(20.dp))
                }
                if (active) {
                    IconToggleButton(
                        checked = item.notify,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context)) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else onNotify(enabled)
                        },
                        modifier = Modifier.padding(end = 8.dp).semantics {
                            contentDescription = if (item.notify) "Varsel på for ${item.title}" else "Varsel av for ${item.title}"
                        },
                    ) {
                        Icon(
                            if (item.notify) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            null, tint = if (item.notify) Primary else Muted,
                        )
                    }
                }
            }
            if (active) {
                CompactRequestProgress(item, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            }
        }
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
    Column(modifier.clearAndSetSemantics { contentDescription = "Framdrift: ${item.stage.label}" }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape)
                    .background(if (index < filled) Primary else Divider))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            Text(item.stage.explanation, color = Muted, fontSize = 11.sp, lineHeight = 16.sp,
                maxLines = 2, modifier = Modifier.weight(1f))
            if (item.seasons.isNotEmpty() && item.availableSeasons.isNotEmpty()) {
                Text("${item.availableSeasons.size}/${item.seasons.size} sesongar", color = PrimarySoft,
                    fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
