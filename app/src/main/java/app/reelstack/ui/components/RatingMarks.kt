package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.ui.theme.Muted
import java.util.Locale

/** Small marks shared by the detail page and the cinematic hero. */
@Composable
internal fun RottenTomatoesRating(score: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = "Rotten Tomatoes $score%" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        androidx.compose.material3.Icon(
            painterResource(R.drawable.ic_tomato), null, Modifier.size(22.dp),
            tint = Color.Unspecified,
        )
        Text("$score%", color = Muted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun TmdbRating(score: Float, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TmdbMark()
        Text("${"%.1f".format(Locale.ROOT, score / 10f)}", color = Muted,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun TmdbMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier.size(width = 42.dp, height = 20.dp)
            .clip(RoundedCornerShape(3.dp)).background(Color(0xFF01B4E4)),
        contentAlignment = Alignment.Center,
    ) {
        Text("TMDB", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}
