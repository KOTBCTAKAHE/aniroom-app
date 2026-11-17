package space.aniroom.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.aniroom.app.ui.theme.DisplayFont
import kotlin.math.max

private val Ink = Color(0xFF141210)
private val Paper = Color(0xFFFAF7F0)
private val Red = Color(0xFFE2342A)
private val Yellow = Color(0xFFFFD23A)
private val Overshoot = CubicBezierEasing(0.2f, 1.3f, 0.35f, 1f)
private val Stamp = CubicBezierEasing(0.2f, 1.7f, 0.4f, 1f)

/** «Пропуск опенинга» and «следующая серия» banners — the site's PlayerActionToast, redrawn natively. */
@Composable
fun ActionBanners(vm: RoomViewModel, isFull: Boolean, modifier: Modifier) {
    val toast by vm.toast.collectAsState()
    when (val t = toast) {
        is PlayerToast.Skip -> key(t.id) { SkipBanner(t, isFull, modifier) }
        is PlayerToast.Episode -> key(t.id) { NextBanner(t, isFull, modifier) }
        else -> Unit
    }
}

/** Shared fade-out at the end of the 2.9 s life (actionLife). */
@Composable
private fun lifeAlpha(loading: Boolean = false): Float {
    val a = remember { Animatable(1f) }
    LaunchedEffect(loading) {
        if (loading) { a.snapTo(1f); return@LaunchedEffect }
        delay(2550)
        a.animateTo(0f, tween(350))
    }
    return a.value
}

@Composable
private fun StrokedText(text: String, size: TextUnit, fill: Color, stroke: Float, shadow: Color, shadowOffset: Float, family: FontFamily = DisplayFont, modifier: Modifier = Modifier) {
    val style = TextStyle(fontFamily = family, fontSize = size)
    Box(modifier) {
        Text(text, style = style.copy(color = shadow), modifier = Modifier.offset(shadowOffset.dp, shadowOffset.dp))
        Text(text, style = style.copy(color = Ink, drawStyle = Stroke(width = stroke)))
        Text(text, style = style.copy(color = fill))
    }
}

private fun parallelogram(slantRatio: Float) = GenericShape { size, _ ->
    val s = size.height * slantRatio
    moveTo(s, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - s, size.height)
    lineTo(0f, size.height)
    close()
}

private fun formatSkip(seconds: Double): String {
    val s = max(0, Math.round(seconds).toInt())
    return if (s >= 60) "${s / 60}:${(s % 60).toString().padStart(2, '0')}" else "$s с"
}

// ------------------------------------------------------------------------------------- skip

@Composable
private fun SkipBanner(t: PlayerToast.Skip, isFull: Boolean, modifier: Modifier) {
    val zoom = if (isFull) 1f else 0.72f
    val alpha = lifeAlpha()
    val slide = remember { Animatable(0f) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { slide.animateTo(1f, tween(550, easing = Overshoot)) }
        progress.animateTo(0f, tween(2700, easing = LinearEasing))
    }
    val chev = rememberInfiniteTransition(label = "chev")
    val nudge by chev.animateFloat(0f, 1f, infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "n")

    BoxWithConstraints(modifier.graphicsLayer { this.alpha = alpha }) {
        val w = constraints.maxWidth.toFloat()
        // whoosh streaks across the whole video
        Whoosh(Modifier.fillMaxSize())
        val shape = remember { parallelogram(0.16f) }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .then(if (isFull) Modifier.statusBarsPadding() else Modifier)
                .padding(top = if (isFull) 28.dp else 44.dp)
                .graphicsLayer {
                    scaleX = zoom; scaleY = zoom
                    val p = slide.value
                    translationX = (1f - p) * -0.7f * w
                    rotationZ = (1f - p) * -6f
                    this.alpha = (p * 2f).coerceIn(0f, 1f)
                },
        ) {
            Box(
                Modifier
                    .drawBehind {
                        val o = 5.dp.toPx()
                        val outline = shape.createOutline(size, layoutDirection, this)
                        androidx.compose.ui.graphics.drawscope.translate(o, o) { drawOutline(outline, Ink) }
                    }
                    .clip(shape)
                    .background(Paper)
                    .drawBehind {
                        var x = 0f
                        while (x < size.width) {
                            drawRect(Ink.copy(alpha = 0.07f), Offset(x, 0f), Size(2.dp.toPx(), size.height))
                            x += 10.dp.toPx()
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        val outline = shape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, Ink, style = Stroke(3.dp.toPx()))
                        // progress strip
                        drawRect(Ink, Offset(0f, size.height - 4.dp.toPx()), Size(size.width * progress.value, 4.dp.toPx()))
                    }
                    .padding(start = 18.dp, end = 30.dp, top = 9.dp, bottom = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).background(Yellow).border(3.dp, Ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardDoubleArrowRight, null, tint = Ink,
                            modifier = Modifier.size(30.dp).graphicsLayer { translationX = if (slide.value >= 1f) nudge * 6.dp.toPx() else 0f },
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        StrokedText("ВЖУ-У-УХ!", 24.sp, Red, stroke = 4f, shadow = Ink, shadowOffset = 2f)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                buildAnnotatedString {
                                    if (t.by.isNotBlank()) {
                                        withStyle(SpanStyle(background = Yellow, fontWeight = FontWeight.Bold)) { append(" ${t.by} ") }
                                        append(" пропустил опенинг")
                                    } else append("Опенинг пропущен")
                                },
                                color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            )
                            t.seconds?.takeIf { it > 0 }?.let { sec ->
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "+${formatSkip(sec)}", color = Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.background(Ink).padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Whoosh(modifier: Modifier) {
    // top %, height dp, width %, delay ms
    val streaks = remember { listOf(Triple(0.18f, 4f, 0.45f) to 0, Triple(0.31f, 7f, 0.60f) to 50, Triple(0.46f, 4f, 0.45f) to 100, Triple(0.58f, 6f, 0.55f) to 30, Triple(0.71f, 4f, 0.45f) to 120, Triple(0.84f, 3f, 0.45f) to 80) }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(800f, tween(800, easing = LinearEasing)) }
    val density = LocalDensity.current
    Canvas(modifier) {
        streaks.forEach { (spec, delayMs) ->
            val (top, h, wFrac) = spec
            val local = ((clock.value - delayMs) / 600f).coerceIn(0f, 1f)
            if (local <= 0f || local >= 1f) return@forEach
            val eased = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f).transform(local)
            val width = size.width * wFrac
            val x = -1.1f * width + eased * (3.7f * width)
            val alpha = if (local < 0.15f) local / 0.15f else 1f - (local - 0.15f) / 0.85f
            val height = with(density) { h.dp.toPx() }
            val y = size.height * top
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.White), x, x + width),
                Offset(x, y), Size(width, height), CornerRadius(height / 2), alpha = alpha.coerceIn(0f, 1f),
            )
            drawRoundRect(Ink.copy(alpha = 0.55f * alpha), Offset(x, y), Size(width, height), CornerRadius(height / 2), style = Stroke(1.5f))
        }
    }
}

// ------------------------------------------------------------------------------------- next episode

@Composable
private fun NextBanner(t: PlayerToast.Episode, isFull: Boolean, modifier: Modifier) {
    val zoom = if (isFull) 1f else 0.72f
    val alpha = lifeAlpha(t.loading)
    val drop = remember { Animatable(0f) }
    val kicker = remember { Animatable(0f) }
    val strike = remember { Animatable(0f) }
    val fromOut = remember { Animatable(0f) }
    val arrow = remember { Animatable(0f) }
    val stamp = remember { Animatable(0f) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { drop.animateTo(1f, tween(500, easing = Overshoot)) }
        launch { delay(200); kicker.animateTo(1f, tween(400, easing = Stamp)) }
        launch { delay(350); strike.animateTo(1f, tween(250)) }
        launch { delay(500); arrow.animateTo(1f, tween(350)) }
        launch { delay(550); fromOut.animateTo(1f, tween(500)) }
        launch { delay(600); stamp.animateTo(1f, tween(500, easing = Stamp)) }
        if (!t.loading) progress.animateTo(0f, tween(2700, easing = LinearEasing))
    }
    val stripes = rememberInfiniteTransition(label = "stripes")
    val stripeShift by stripes.animateFloat(0f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing)), label = "s")

    Box(modifier.graphicsLayer { this.alpha = alpha }) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .then(if (isFull) Modifier.statusBarsPadding() else Modifier)
                .padding(top = if (isFull) 34.dp else 50.dp)
                .graphicsLayer {
                    scaleX = zoom * (0.9f + 0.1f * drop.value); scaleY = scaleX
                    translationY = (1f - drop.value) * -40.dp.toPx()
                    rotationZ = -1.5f - 4.5f * (1f - drop.value)
                    this.alpha = drop.value.coerceIn(0f, 1f)
                },
        ) {
            Column(
                Modifier
                    .widthIn(min = 220.dp)
                    .drawBehind {
                        val o = 5.dp.toPx()
                        val b = 3.dp.toPx()
                        drawRect(Ink, Offset(o - b, o - b), Size(size.width + 2 * b, size.height + 2 * b))
                        drawRect(Red, Offset(o, o), size)
                        drawRect(Ink)
                        val step = 6.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            var x = 0f
                            while (x < size.width) { drawCircle(Color.White.copy(alpha = 0.08f), 1f, Offset(x, y)); x += step }
                            y += step
                        }
                    }
                    .border(3.dp, Paper)
                    .drawWithContent {
                        drawContent()
                        val h = 4.dp.toPx()
                        val top = size.height - h
                        if (t.loading) {
                            val stripe = 8.dp.toPx()
                            var x = -2 * stripe + stripeShift * 2 * stripe
                            drawRect(Ink, Offset(0f, top), Size(size.width, h))
                            while (x < size.width) {
                                drawRect(Yellow, Offset(x, top), Size(stripe, h))
                                x += 2 * stripe
                            }
                        } else drawRect(Yellow, Offset(0f, top), Size(size.width * progress.value, h))
                    }
                    .padding(horizontal = 26.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("СЕРИЯ", fontFamily = DisplayFont, fontSize = 13.sp, color = Color(0xFFCFC8BB))
                    if (t.from != null) {
                        Box(
                            Modifier.graphicsLayer {
                                this.alpha = 1f - 0.45f * fromOut.value
                                translationY = 3.dp.toPx() * fromOut.value
                                scaleX = 1f - 0.15f * fromOut.value; scaleY = scaleX
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${t.from}", fontFamily = DisplayFont, fontSize = 30.sp, color = Color(0xFF8B8478))
                            Canvas(Modifier.matchParentSize()) {
                                val y = size.height / 2
                                val len = (size.width + 8.dp.toPx()) * strike.value
                                drawLine(Red, Offset(-4.dp.toPx(), y + 4f), Offset(-4.dp.toPx() + len, y - 4f * strike.value), 4.dp.toPx())
                            }
                        }
                    }
                    if (t.from != null && t.to != null) {
                        Text(
                            "→", color = Yellow, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer { this.alpha = arrow.value; translationX = (1f - arrow.value) * -12.dp.toPx() },
                        )
                    }
                    StrokedText(
                        "${t.to ?: "…"}", 50.sp, Yellow, stroke = 6f, shadow = Red, shadowOffset = 3f,
                        modifier = Modifier.graphicsLayer {
                            val v = stamp.value
                            this.alpha = (v * 1.6f).coerceIn(0f, 1f)
                            val sc = if (v < 0.6f) 2.4f - (2.4f - 0.9f) * (v / 0.6f) else 0.9f + 0.1f * ((v - 0.6f) / 0.4f)
                            scaleX = sc; scaleY = sc
                            rotationZ = if (v < 0.6f) -14f + 17f * (v / 0.6f) else 3f - 6f * ((v - 0.6f) / 0.4f)
                        },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (t.loading) {
                        CircularProgressIndicator(Modifier.size(12.dp), color = Color(0xFFCFC8BB), strokeWidth = 1.5.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        if (t.loading) "Загружаем серию" else t.title ?: "Приятного просмотра",
                        color = Color(0xFFCFC8BB), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 300.dp),
                    )
                }
            }
            // ДАЛЬШЕ! kicker
            Text(
                "ДАЛЬШЕ!", fontFamily = DisplayFont, fontSize = 13.sp, color = Color.White,
                modifier = Modifier
                    .offset((-12).dp, (-13).dp)
                    .graphicsLayer {
                        val k = kicker.value
                        scaleX = k; scaleY = k
                        rotationZ = -6f - 24f * (1f - k)
                        this.alpha = k.coerceIn(0f, 1f)
                    }
                    .background(Red)
                    .border(2.5.dp, Ink)
                    .padding(horizontal = 9.dp, vertical = 2.dp),
            )
        }
    }
}
