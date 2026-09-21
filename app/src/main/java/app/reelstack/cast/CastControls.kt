package app.reelstack.cast

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelstack.R
import app.reelstack.ui.components.SpoleIcons
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

/** Native route picker, intentionally absent from TV, kids and an unconfigured build. */
@Composable
fun CastRouteButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val television = (LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    if (television || !CastConfiguration.isEnabled(context)) return
    val label = stringResource(R.string.cast_connect)
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                addView(MediaRouteButton(viewContext).also { button ->
                    button.contentDescription = label
                    CastButtonFactory.setUpMediaRouteButton(viewContext, button)
                }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        },
        update = { container -> container.contentDescription = label },
    )
}

/** Persistent lightweight remote control. Expanded Cast notification/lock controls remain SDK-owned. */
@Composable
fun CastMiniController(gateway: CastGateway, modifier: Modifier = Modifier) {
    val state = gateway.state.collectAsStateWithLifecycle().value
    if (!state.active || state.phase == CastPhase.CONNECTED || state.title.isBlank()) return
    Card(modifier) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(state.title, modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = { gateway.seekBy(-10_000) }) {
                Icon(SpoleIcons.Replay10, stringResource(R.string.cast_rewind))
            }
            IconButton(onClick = gateway::playPause) {
                Icon(SpoleIcons.Pause, stringResource(R.string.cast_play_pause))
            }
            IconButton(onClick = { gateway.seekBy(10_000) }) {
                Icon(SpoleIcons.Forward10, stringResource(R.string.cast_forward))
            }
            IconButton(onClick = gateway::stop) {
                Icon(SpoleIcons.Close, stringResource(R.string.cast_stop))
            }
        }
    }
}
