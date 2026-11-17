package space.aniroom.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.aniroom.app.ui.theme.MangaColors
import space.aniroom.app.ui.theme.Manga
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Same palette as the website (lib/nicknameColor.ts). */
val NICK_COLORS = listOf(
    "#E879F9", "#F472B6", "#FB7185", "#FDBA74", "#FACC15", "#A3E635",
    "#4ADE80", "#2DD4BF", "#22D3EE", "#60A5FA", "#818CF8", "#C4B5FD",
)

/** Nick color exactly as the site computes it. */
fun siteNickColor(username: String, explicit: String?): Color {
    val chosen = explicit?.uppercase()?.takeIf { it in NICK_COLORS }
    val hex = chosen ?: run {
        var hash = 7L
        for (ch in username) hash = (hash * 31 + ch.code) and 0xFFFFFFFFL
        NICK_COLORS[(hash % NICK_COLORS.size).toInt()]
    }
    return Color(android.graphics.Color.parseColor(hex))
}

val PROFILE_COVERS = listOf(
    "default" to "Выпуск", "speed" to "Скорость", "halftone" to "Растр", "dots" to "Горох",
    "night" to "Ночь", "sakura" to "Сакура", "ink" to "Штрих", "sunset" to "Закат",
)

private val Sakura = Color(0xFFFF8FB5)

// ------------------------------------------------------------------------------------------ frames

/** Animated decorative ring around an avatar (website "frames"). Draws outside the given size. */
@Composable
fun AvatarFrame(frame: String?, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val key = frame?.takeIf { it != "none" && it.isNotBlank() }
    if (key == null) {
        Box(modifier) { content() }
        return
    }
    val m = Manga.colors
    val t = rememberInfiniteTransition(label = "frame")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (key == "holo") 3000 else 9000, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val fall by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "fall")
    val ring = (size.value * 0.09f).coerceIn(3f, 9f).dp
    Box(
        modifier
            .padding(ring)
            .drawWithContent {
                drawFrame(key, m, ring.toPx(), spin, pulse, fall, behind = true)
                drawContent()
                drawFrame(key, m, ring.toPx(), spin, pulse, fall, behind = false)
            },
    ) { content() }
}

private fun DrawScope.drawFrame(key: String, m: MangaColors, ring: Float, spin: Float, pulse: Float, fall: Float, behind: Boolean) {
    val r = min(size.width, size.height) / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    val radius = r + ring / 2f
    if (behind) when (key) {
        "ink" -> {
            drawCircle(m.red, r + ring, c + Offset(ring * 0.6f, ring * 0.6f))
            drawCircle(Color(0xFF141210), r + ring, c)
        }
        "speed" -> rotate(spin, c) {
            val rays = 28
            repeat(rays) { i ->
                val a = Math.toRadians(i * 360.0 / rays).toFloat()
                drawLine(
                    m.ink, c + Offset(cos(a) * r, sin(a) * r), c + Offset(cos(a) * (r + ring * 1.6f), sin(a) * (r + ring * 1.6f)),
                    strokeWidth = ring * 0.45f,
                )
            }
        }
        "halftone" -> {
            drawCircle(m.paper2, r + ring, c)
            val dots = 26
            repeat(dots) { i ->
                val a = Math.toRadians(i * 360.0 / dots).toFloat()
                drawCircle(m.red, ring * 0.28f, c + Offset(cos(a) * radius, sin(a) * radius))
            }
            drawCircle(m.ink, r + ring, c, style = Stroke(1.5f))
        }
        "sakura" -> rotate(spin, c) {
            drawCircle(Brush.sweepGradient(listOf(Sakura, Color(0xFFFFF1F6), Sakura, Color(0xFFFFD3E6), Sakura), c), r + ring, c)
        }
        "flame" -> {
            val glow = 1f + pulse * 0.12f
            drawCircle(Brush.radialGradient(listOf(Color(0x00FF6A00), Color(0x55FF3D00), Color(0x00FF3D00)), c, (r + ring * 2.2f) * glow), (r + ring * 2.2f) * glow, c)
            rotate(spin * 2f, c) {
                drawCircle(Brush.sweepGradient(listOf(Color(0xFFFF3D00), Color(0xFFFF8A00), Color(0xFFFFD166), Color(0xFFFF8A00), Color(0xFFFF3D00)), c), r + ring, c)
            }
        }
        "gold" -> rotate(spin, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Color(0xFFB8860B), Color(0xFFFFE27A), Color(0xFFFFF7D1), Color(0xFFFFD23A), Color(0xFFC9900C), Color(0xFFFFE9A0), Color(0xFFB8860B)), c),
                r + ring, c,
            )
            drawCircle(Color(0xFF7A5600), r + ring, c, style = Stroke(1.5f))
        }
        "holo" -> rotate(spin, c) {
            drawCircle(
                Brush.sweepGradient(listOf(Color(0xFFFF6EC7), Color(0xFF7DF9FF), Color(0xFFB2FF59), Color(0xFFFFF176), Color(0xFFFF8A80), Color(0xFFFF6EC7)), c),
                r + ring, c,
            )
        }
        else -> drawCircle(m.ink, r + ring, c)
    } else if (key == "sakura") {
        // falling petals over the avatar
        repeat(3) { i ->
            val p = (fall + i / 3f) % 1f
            val x = size.width * (0.15f + i * 0.33f) - p * ring * 2
            val y = -ring + p * (size.height * 0.9f)
            val alpha = if (p < 0.15f) p / 0.15f else 1f - p
            rotate(p * 220f, Offset(x, y)) {
                drawOval(Sakura.copy(alpha = alpha.coerceIn(0f, 1f)), Offset(x - ring * 0.5f, y - ring * 0.35f), Size(ring, ring * 0.7f))
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ nick fx

@Composable
fun NickText(
    name: String,
    color: Color,
    fx: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    style: TextStyle = TextStyle.Default,
) {
    val m = Manga.colors
    val base = style.merge(TextStyle(fontSize = fontSize, fontWeight = fontWeight, color = color))
    when (fx) {
        "outline" -> Box(modifier) {
            Text(name, style = base.copy(color = color, drawStyle = Stroke(width = 4f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(name, style = base.copy(color = m.paper2), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        "shadow" -> Text(name, modifier, style = base.copy(color = m.ink, shadow = Shadow(color, Offset(4f, 4f), 0f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
        "shine", "rainbow" -> {
            val t = rememberInfiniteTransition(label = "nick")
            val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(if (fx == "shine") 2200 else 3000, easing = LinearEasing)), label = "x")
            val brush = if (fx == "shine") Brush.linearGradient(
                0f to color, 0.38f to color, 0.5f to Color.White, 0.62f to color, 1f to color,
                start = Offset(x * 300f - 300f, 0f), end = Offset(x * 300f, 40f),
            ) else Brush.linearGradient(
                listOf(Color(0xFFFF4D4D), Color(0xFFFFB74D), Color(0xFFFFF176), Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFB388FF), Color(0xFFFF4D4D)),
                start = Offset(x * 200f - 200f, 0f), end = Offset(x * 200f + 100f, 0f),
            )
            Text(name, modifier, style = base.copy(brush = brush), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        "glitch" -> {
            val t = rememberInfiniteTransition(label = "glitch")
            val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "p")
            val on = phase in 0.86f..0.9f || phase in 0.93f..0.95f
            val dx = if (on) (if (phase < 0.9f) 3f else -2f) else 0f
            Box(modifier) {
                if (on) {
                    Text(name, Modifier.padding(start = 2.dp), style = base.copy(color = Color(0xFF00E5FF).copy(alpha = 0.8f)), maxLines = 1)
                    Text(name, style = base.copy(color = Color(0xFFFF1744).copy(alpha = 0.8f)), maxLines = 1)
                }
                Text(name, Modifier.drawWithContent { drawIntoCanvas { it.translate(dx, 0f) }; drawContent() }, style = base, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        else -> Text(name, modifier, style = base, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ------------------------------------------------------------------------------------------ bubbles

data class BubbleSkin(val background: Color, val text: Color, val shape: Shape, val border: Color, val dashed: Boolean, val dots: Boolean, val tilt: Float)

fun bubbleSkin(key: String?, mine: Boolean, m: MangaColors, defaultShape: Shape): BubbleSkin {
    val bg = if (mine) m.red else m.paper2
    val fg = if (mine) Color.White else m.ink
    return when (key) {
        "tone" -> BubbleSkin(bg, fg, defaultShape, m.ink, dashed = false, dots = true, tilt = 0f)
        "torn" -> BubbleSkin(bg, fg, defaultShape, m.ink, dashed = true, dots = false, tilt = -0.6f)
        "ink" -> if (mine) BubbleSkin(m.red, Color.White, defaultShape, m.ink, false, false, 0f) else BubbleSkin(m.ink, m.paper, defaultShape, m.ink, false, false, 0f)
        "scroll" -> BubbleSkin(if (mine) bg else Color(0xFFF6E7C1), if (mine) fg else Color(0xFF3A2A12), RoundedCornerShape(4.dp), m.ink, false, false, 0f)
        "burst" -> BubbleSkin(if (mine) bg else m.yellow, if (mine) fg else m.onYellow, CutCornerShape(9.dp), m.ink, false, false, 0.8f)
        else -> BubbleSkin(bg, fg, defaultShape, m.ink, false, false, 0f)
    }
}

/** Halftone dots / dashed border extras for a bubble skin. */
fun Modifier.bubbleExtras(skin: BubbleSkin, dotColor: Color): Modifier = this.drawWithContent {
    drawContent()
    if (skin.dots) {
        val step = 6.dp.toPx()
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawCircle(dotColor.copy(alpha = 0.16f), 1.dp.toPx(), Offset(x, y))
                x += step
            }
            y += step
        }
    }
    if (skin.dashed) {
        drawRoundRect(
            skin.border, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
        )
    }
}

// ------------------------------------------------------------------------------------------ covers

/** Draws a profile cover pattern (same names as the website's covers). */
fun DrawScope.drawCover(id: String?, dark: Boolean) {
    val ink = if (dark) Color(0xFFF3EEE4) else Color(0xFF141210)
    val red = Color(0xFFE2342A)
    when (id) {
        "speed" -> {
            drawRect(Brush.horizontalGradient(listOf(red, Color(0xFFFF6B4A))))
            var y = 0f
            while (y < size.height) {
                drawLine(ink.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1.5f)
                y += 13f
            }
        }
        "halftone" -> {
            drawRect(Brush.linearGradient(listOf(red, Color(0xFFFF6B4A))))
            dotsGrid(Color(0x52141210), 10f, 2.4f)
        }
        "dots" -> {
            drawRect(Color(0xFFFFD23A))
            dotsGrid(Color(0xFF141210), 22f, 3.8f)
        }
        "night" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF14123A), Color(0xFF2B1D52), Color(0xFF0F0E0D))))
            val rnd = java.util.Random(11)
            repeat(70) {
                val x = rnd.nextFloat() * size.width
                val y = rnd.nextFloat() * size.height
                drawCircle(if (it % 6 == 0) Color(0xFFFFD23A) else Color(0xFFF3EEE4), if (it % 6 == 0) 2f else 1.2f, Offset(x, y))
            }
            drawCircle(Color(0xFFFFF3C4), size.height * 0.18f, Offset(size.width * 0.82f, size.height * 0.3f))
        }
        "sakura" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFD6E5), Color(0xFFFFB3CD))))
            val rnd = java.util.Random(5)
            repeat(40) {
                val x = rnd.nextFloat() * size.width
                val y = rnd.nextFloat() * size.height
                rotate(rnd.nextFloat() * 180f, Offset(x, y)) {
                    drawOval(if (it % 3 == 0) Color.White else Color(0x88E2342A), Offset(x, y), Size(10f, 7f))
                }
            }
        }
        "ink" -> {
            drawRect(if (dark) Color(0xFF1A1816) else Color(0xFFFAF7F0))
            var x = -size.height
            while (x < size.width) {
                drawLine(ink, Offset(x, size.height), Offset(x + size.height, 0f), 2f)
                x += 11f
            }
        }
        "sunset" -> {
            val h = size.height
            drawRect(Color(0xFFFFD23A), size = Size(size.width, h * 0.34f))
            drawRect(Color(0xFFFF9B3A), Offset(0f, h * 0.34f), Size(size.width, h * 0.22f))
            drawRect(Color(0xFFE2342A), Offset(0f, h * 0.56f), Size(size.width, h * 0.22f))
            drawRect(Color(0xFF8E1A13), Offset(0f, h * 0.78f), Size(size.width, h * 0.22f))
        }
        else -> {
            drawRect(red)
            dotsGrid(Color(0x29141210), 14f, 2.6f)
        }
    }
}

private fun DrawScope.dotsGrid(color: Color, step: Float, radius: Float) {
    var y = 0f
    var row = 0
    while (y < size.height + step) {
        var x = if (row % 2 == 0) 0f else step / 2f
        while (x < size.width + step) {
            drawCircle(color, radius, Offset(x, y))
            x += step
        }
        y += step / 2f
        row++
    }
}

@Composable
fun CoverSwatch(id: String, modifier: Modifier) {
    val dark = Manga.colors.isDark
    Canvas(modifier) { drawCover(id, dark) }
}

@Suppress("unused")
private fun near(a: Float, b: Float) = abs(a - b) < 0.001f

@Composable
fun FrameSample(frame: String, size: Dp = 44.dp) {
    val m = Manga.colors
    AvatarFrame(frame, size) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawCircle(m.paper3) }
            Text("Ты", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = m.ink)
        }
    }
}
