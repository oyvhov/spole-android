package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R

@Composable
internal fun LibraryRating(rating: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.library_rating_description, rating)
    Row(modifier.background(Color(0xEE151817), RoundedCornerShape(8.dp))
        .clearAndSetSemantics { contentDescription = description }
        .padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(SpoleIcons.Star, null, Modifier.size(13.dp), tint = Color(0xFFE5C77A))
        Text(rating, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
