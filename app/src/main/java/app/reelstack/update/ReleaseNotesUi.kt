package app.reelstack.update

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun ReleaseNotes(notes: String) {
    val blocks = remember(notes) { releaseNoteBlocks(notes) }
    Column(Modifier.fillMaxWidth().testTag("release-notes"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (block.bullet) Text("•", style = MaterialTheme.typography.bodyMedium)
                Text(block.text, modifier = Modifier.weight(1f),
                    style = if (block.heading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
