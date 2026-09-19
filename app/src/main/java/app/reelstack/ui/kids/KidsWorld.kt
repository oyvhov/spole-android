package app.reelstack.ui.kids

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.*
import app.reelstack.data.repository.KidsPreferencesRepository
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.LocalMotionEnabled
import kotlin.math.sin

@Composable
internal fun rememberKidsPreferences(profileId: String): KidsPreferences {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { KidsPreferencesRepository(context) }
    var value by remember(profileId) { mutableStateOf(repository.read(profileId)) }
    DisposableEffect(repository, profileId) {
        val stop = repository.observe(profileId) { value = it }
        onDispose(stop)
    }
    return value
}

@Composable
internal fun KidsWorldTheme(options: KidsPreferences, content: @Composable () -> Unit) {
    val motion = LocalMotionEnabled.current && !options.reduceMotion
    val sky by animateColorAsState(Color(options.world.sky), tween(if (motion) 450 else 0), label = "kids-world-sky")
    val glow by animateColorAsState(Color(options.world.glow), tween(if (motion) 450 else 0), label = "kids-world-glow")
    CompositionLocalProvider(LocalMotionEnabled provides motion) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
            background = sky,
            surface = sky,
            surfaceVariant = glow.copy(alpha = .18f),
            onSurfaceVariant = Color(0xFFCBD0DE),
            primary = glow,
            onPrimary = Color(0xFF101211),
        ), content = content)
    }
}

/** Original vector landscapes: stable at any TV resolution, no external artwork requests. */
@Composable
internal fun WorldLandscape(world: KidsWorld, modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val glow = Color(world.glow)
        drawRect(Brush.verticalGradient(listOf(Color(world.sky), Color(world.sky).copy(alpha = .85f))))
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = .24f), Color.Transparent),
            center = Offset(w * .78f, h * .3f), radius = w * .55f), w * .55f, Offset(w * .78f, h * .3f))
        when (world) {
            KidsWorld.SPACE -> {
                val center = Offset(w * .78f, h * .36f)
                drawCircle(Brush.linearGradient(listOf(glow, Color(0xFF34345C))), h * .18f, center)
                drawOval(glow.copy(alpha = .5f), Offset(center.x - h * .3f, center.y - h * .055f),
                    Size(h * .6f, h * .13f), style = Stroke(h * .008f))
                drawCircle(Color(0xFFD9CEEF), h * .032f, Offset(w * .56f, h * .22f))
            }
            KidsWorld.OCEAN -> {
                repeat(5) { layer ->
                    val path = Path().apply {
                        moveTo(0f, h)
                        for (x in 0..80) {
                            val px = x / 80f * w
                            lineTo(px, h * (.52f + layer * .095f) + sin(x * .1f + layer) * h * .05f)
                        }
                        lineTo(w, h); close()
                    }
                    drawPath(path, glow.copy(alpha = .055f + layer * .025f))
                }
                repeat(7) { i -> drawCircle(glow.copy(alpha = .23f), h * (.008f + i % 3 * .005f),
                    Offset(w * (.57f + i * .06f), h * (.18f + (i * 7 % 11) * .04f)), style = Stroke(1.5f)) }
            }
            KidsWorld.FOREST, KidsWorld.AURORA -> {
                repeat(3) { layer ->
                    val ridge = Path().apply {
                        moveTo(0f, h)
                        for (x in 0..12) lineTo(w * x / 12f,
                            h * (.62f + layer * .13f) - sin(x * 1.7f + layer) * h * .10f)
                        lineTo(w, h); close()
                    }
                    drawPath(ridge, glow.copy(alpha = .09f + layer * .04f))
                }
                if (world == KidsWorld.AURORA) repeat(3) { i ->
                    val ribbon = Path().apply {
                        moveTo(w * .4f, h * (.12f + i * .09f))
                        cubicTo(w * .55f, h * .9f, w * .85f, -h * .2f, w, h * (.36f + i * .09f))
                    }
                    drawPath(ribbon, glow.copy(alpha = .12f), style = Stroke(h * .04f))
                }
            }
            KidsWorld.SUNSET -> {
                drawCircle(Brush.verticalGradient(listOf(Color(0xFFF5BA95), glow.copy(alpha = .12f))),
                    h * .21f, Offset(w * .77f, h * .42f))
                repeat(6) { i -> drawLine(Color(world.sky).copy(alpha = .8f),
                    Offset(w * .5f, h * (.46f + i * .032f)), Offset(w, h * (.46f + i * .032f)), h * .012f) }
            }
            KidsWorld.CINEMA -> repeat(4) { i ->
                val beam = Path().apply {
                    moveTo(w * .8f, -h * .2f)
                    lineTo(w * (.1f + i * .22f), h)
                    lineTo(w * (.2f + i * .22f), h); close()
                }
                drawPath(beam, glow.copy(alpha = .06f))
            }
        }
        if (world != KidsWorld.OCEAN) repeat(35) { i ->
            drawCircle(Color.White.copy(alpha = .18f + (i % 3) * .1f), 1f + i % 2,
                Offset(w * ((i * 37 % 101) / 101f), h * ((i * 23 % 71) / 100f)))
        }
    }
}

@Composable
internal fun KidsWorldPicker(options: KidsPreferences, onChange: (KidsWorld) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KidsWorld.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { world ->
                    val interaction = remember { MutableInteractionSource() }
                    val shape = RoundedCornerShape(20.dp)
                    Column(Modifier.weight(1f).clip(shape).background(Color(world.sky))
                        .focusOutline(interaction, shape)
                        .clickable(interactionSource = interaction, indication = null, role = Role.RadioButton,
                            onClick = { onChange(world) })
                        .semantics { selected = world == options.world }
                        .testTag("kids-world-${world.name}")) {
                        WorldLandscape(world, Modifier.fillMaxWidth().height(100.dp))
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(world.title, Modifier.weight(1f), color = Color.White,
                                style = MaterialTheme.typography.titleMedium)
                            if (world == options.world) Icon(SpoleIcons.DoneCircle, "Valt", tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
