package space.aniroom.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.aniroom.app.ui.theme.DisplayFont
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF141210)
private val Paper = Color(0xFFFAF7F0)
private val Red = Color(0xFFE2342A)
private val Yellow = Color(0xFFFFD23A)
private val Overshoot = CubicBezierEasing(0.2f, 1.5f, 0.4f, 1f)
private val BigOvershoot = CubicBezierEasing(0.2f, 1.8f, 0.4f, 1f)

/** Burst outline from the site's clip-path (percent points). */
private val BURST = listOf(
    50 to 0, 57 to 17, 72 to 6, 72 to 24, 90 to 18, 82 to 34, 100 to 38, 86 to 50, 100 to 62, 82 to 66, 90 to 82, 72 to 76, 72 to 94,
    57 to 83, 50 to 100, 43 to 83, 28 to 94, 28 to 76, 10 to 82, 18 to 66, 0 to 62, 14 to 50, 0 to 38, 18 to 34, 10 to 18, 28 to 24, 28 to 6, 43 to 17,
)

/**
 * «ПАУЗА!» — the website's manga pause screen: rotating speed lines, a yellow burst that
 * spins in, a bobbing red pause badge, letters dropping one by one and a caption card.
 */
@Composable
fun PauseScreen(who: String?, isOwner: Boolean, compact: Boolean) {
    val t = rememberInfiniteTransition(label = "pause")
    val lines by t.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "lines")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "spin")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "bob")

    val burstIn = remember { Animatable(0f) }
    val badgeIn = remember { Animatable(0f) }
    val captionIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { burstIn.animateTo(1f, tween(500, easing = Overshoot)) }
        launch { delay(120); badgeIn.animateTo(1f, tween(450, easing = BigOvershoot)) }
        launch { delay(450); captionIn.animateTo(1f, tween(400, easing = Overshoot)) }
    }

    val centerW: Dp = if (compact) 130.dp else 210.dp
    val centerH: Dp = if (compact) 104.dp else 170.dp
    val badge: Dp = if (compact) 46.dp else 72.dp
    val letterSize = if (compact) 28.sp else 46.sp

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .drawBehind {
                // dark vignette + fine dot screen
                drawRect(Brush.radialGradient(listOf(Ink.copy(alpha = 0.35f), Ink.copy(alpha = 0.82f)), center, size.maxDimension * 0.7f))
                val step = 6.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    var x = 0f
                    while (x < size.width) {
                        drawCircle(Color.White.copy(alpha = 0.07f), 1f, Offset(x, y))
                        x += step
                    }
                    y += step
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // speed lines ring
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.45f)
            val outer = size.maxDimension * 0.75f
            val inner = size.minDimension * 0.16f
            rotate(lines, c) {
                var deg = 0f
                while (deg < 360f) {
                    val a0 = Math.toRadians(deg.toDouble()).toFloat()
                    val a1 = Math.toRadians((deg + 1.2f).toDouble()).toFloat()
                    val p = Path().apply {
                        moveTo(c.x + cos(a0) * inner, c.y + sin(a0) * inner)
                        lineTo(c.x + cos(a0) * outer, c.y + sin(a0) * outer)
                        lineTo(c.x + cos(a1) * outer, c.y + sin(a1) * outer)
                        lineTo(c.x + cos(a1) * inner, c.y + sin(a1) * inner)
                        close()
                    }
                    drawPath(p, Brush.radialGradient(listOf(Color.White.copy(alpha = 0.09f), Color.Transparent), c, outer * 0.6f))
                    deg += 9f
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp)) {
            Box(Modifier.size(centerW, centerH), contentAlignment = Alignment.Center) {
                // burst
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 6.dp)
                        .graphicsLayer {
                            val s = 0.2f + 0.8f * burstIn.value
                            scaleX = s; scaleY = s
                            alpha = burstIn.value.coerceIn(0f, 1f)
                            rotationZ = -40f * (1f - burstIn.value) + if (burstIn.value >= 1f) spin else 0f
                        },
                ) {
                    val path = Path().apply {
                        BURST.forEachIndexed { i, (x, y) ->
                            val px = size.width * x / 100f
                            val py = size.height * y / 100f
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    val shadow = (if (compact) 4.dp else 6.dp).toPx()
                    translate(shadow, shadow) { drawPath(path, Ink) }
                    drawPath(path, Yellow)
                }
                // pause badge
                Box(
                    Modifier
                        .offset(y = if (compact) (-10).dp else (-18).dp)
                        .graphicsLayer {
                            scaleX = badgeIn.value; scaleY = badgeIn.value
                            rotationZ = -90f * (1f - badgeIn.value) + (if (badgeIn.value >= 1f) -4f * bob else 0f)
                            translationY = if (badgeIn.value >= 1f) -5.dp.toPx() * bob else 0f
                        }
                        .size(badge)
                        .drawBehind {
                            val o = (if (compact) 3.dp else 4.dp).toPx()
                            drawCircle(Ink, size.minDimension / 2f, center + Offset(o, o))
                        }
                        .clip(CircleShape)
                        .background(Red)
                        .border(if (compact) 3.dp else 4.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)) {
                        repeat(2) {
                            Box(
                                Modifier
                                    .size(if (compact) 7.dp else 11.dp, if (compact) 19.dp else 30.dp)
                                    .background(Paper)
                                    .border(if (compact) 2.dp else 2.5.dp, Ink),
                            )
                        }
                    }
                }
                // ПАУЗА! letters
                Row(Modifier.align(Alignment.BottomCenter).offset(y = 6.dp).graphicsLayer { rotationZ = -7f }) {
                    "ПАУЗА!".forEachIndexed { i, ch -> PauseLetter(ch.toString(), i, i == 5, letterSize) }
                }
            }

            // caption card
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = captionIn.value.coerceIn(0f, 1f)
                        translationY = (1f - captionIn.value) * 14.dp.toPx()
                        rotationZ = 1f + 3f * (1f - captionIn.value)
                    }
                    .drawBehind {
                        val o = (if (compact) 3.dp else 5.dp).toPx()
                        val b = 2.5.dp.toPx()
                        drawRect(Ink, Offset(o - b, o - b), androidx.compose.ui.geometry.Size(size.width + b * 2, size.height + b * 2))
                        drawRect(Red, Offset(o, o), size)
                    }
                    .background(Paper)
                    .border(2.5.dp, Ink)
                    .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    buildAnnotatedString {
                        if (who != null) {
                            withStyle(SpanStyle(background = Yellow, fontWeight = FontWeight.Bold)) { append(" $who ") }
                            append(if (isOwner) " поставил паузу" else " поставил на паузу")
                        } else append("Ждём хоста комнаты")
                    },
                    color = Ink, fontWeight = FontWeight.Bold, fontSize = if (compact) 13.sp else 16.sp, textAlign = TextAlign.Center,
                )
                if (!compact) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (isOwner) "Нажми плей — и все продолжат вместе с тобой" else "Продолжим вместе, как только хост нажмёт плей",
                            color = Color(0xFF5B544A), fontSize = 12.5.sp, textAlign = TextAlign.Center,
                        )
                        PauseDots()
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseLetter(letter: String, index: Int, red: Boolean, size: androidx.compose.ui.unit.TextUnit) {
    val drop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250L + index * 60L)
        drop.animateTo(1f, tween(500, easing = Overshoot))
    }
    val t = rememberInfiniteTransition(label = "wiggle$index")
    val wiggle by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            keyframes { durationMillis = 2800; 0f at 0; 1f at 1400; 0f at 2800 },
            initialStartOffset = StartOffset(1000 + index * 120),
        ),
        label = "w",
    )
    val style = TextStyle(fontFamily = DisplayFont, fontSize = size)
    Box(
        Modifier.graphicsLayer {
            val d = drop.value
            alpha = d.coerceIn(0f, 1f)
            translationY = (1f - d) * -40.dp.toPx() - wiggle * 3.dp.toPx()
            val s = 1.6f - 0.6f * d
            scaleX = s; scaleY = s
            rotationZ = -20f * (1f - d) + wiggle * 3f
        },
    ) {
        // hard shadow, ink outline, then fill — like -webkit-text-stroke + text-shadow
        Text(letter, style = style.copy(color = Ink), modifier = Modifier.offset(4.dp, 4.dp))
        Text(letter, style = style.copy(color = Ink, drawStyle = Stroke(width = if (size.value > 30) 7f else 5f)))
        Text(letter, style = style.copy(color = if (red) Red else Paper))
    }
}

@Composable
private fun PauseDots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row {
        repeat(3) { i ->
            val v by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(keyframes { durationMillis = 1200; 0f at 0; 1f at 360; 0f at 720; 0f at 1200 }, initialStartOffset = StartOffset(i * 200)),
                label = "d$i",
            )
            Text(".", color = Color(0xFF5B544A), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.graphicsLayer { alpha = 0.2f + 0.8f * v; translationY = -3.dp.toPx() * v })
        }
    }
}
