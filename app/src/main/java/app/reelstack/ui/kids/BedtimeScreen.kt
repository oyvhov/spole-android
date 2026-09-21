package app.reelstack.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.components.SpoleSecondaryButton

/** Calm local end state for bedtime. It never cuts an active scene; the player reaches it at end. */
@Composable
fun BedtimeScreen(
    modifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF070A0E))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.kids_bedtime_done_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.kids_bedtime_done_body),
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        onOpenProfile?.let { openProfile ->
            SpoleSecondaryButton(onClick = openProfile, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.kids_bedtime_profile_action))
            }
        }
        onClose?.let { close ->
            SpoleSecondaryButton(
                onClick = close,
                modifier = Modifier.fillMaxWidth().padding(top = if (onOpenProfile == null) 0.dp else 12.dp),
            ) { Text(stringResource(R.string.kids_bedtime_back_action)) }
        }
    }
}
