package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.CastMember
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.SurfaceRaised

@Composable
fun CastRail(cast: List<CastMember>, source: app.reelstack.data.model.ServiceKind? = null) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(cast, key = { it.name }) { person ->
            Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
                    if (person.portraitUrl != null) {
                        MediaArtwork(person.portraitUrl, null, Modifier.matchParentSize(), fallbackRes = R.drawable.media_placeholder, source = source)
                    } else {
                        Text(person.name.split(' ').filter(String::isNotBlank).take(2).map { it.first() }.joinToString(""),
                            color = Muted, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Text(person.name, style = MaterialTheme.typography.labelMedium, maxLines = 3,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                person.role?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = Muted, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}
