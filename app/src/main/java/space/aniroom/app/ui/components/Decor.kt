package space.aniroom.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import space.aniroom.app.ui.theme.Manga
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Manga speed lines radiating from a focus point, slowly turning. */
@Composable
fun SpeedLines(modifier: Modifier = Modifier, alpha: Float = 0.06f, focusY: Float = 0.22f) {
    val ink = Manga.colors.ink
    val transition = rememberInfiniteTransition(label = "speed")
    val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "angle")
    val rays = remember {
        val r = Random(7)
        List(72) { i -> Triple(i * 5f + r.nextFloat() * 3f, 0.35f + r.nextFloat() * 0.3f, 0.6f + r.nextFloat() * 1.6f) }
    }
    Canvas(modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * focusY)
        val far = hypot(size.width, size.height)
        rotate(angle, center) {
            rays.forEach { (deg, start, width) ->
                val a = Math.toRadians(deg.toDouble())
                val inner = far * start * 0.5f
                val path = Path().apply {
                    val spread = 0.012f * width
                    moveTo(center.x + cos(a).toFloat() * inner, center.y + sin(a).toFloat() * inner)
                    lineTo(center.x + cos(a - spread).toFloat() * far, center.y + sin(a - spread).toFloat() * far)
                    lineTo(center.x + cos(a + spread).toFloat() * far, center.y + sin(a + spread).toFloat() * far)
                    close()
                }
                drawPath(path, ink.copy(alpha = alpha))
            }
        }
    }
}

/** Halftone dot texture for headers. */
@Composable
fun Halftone(modifier: Modifier = Modifier, alpha: Float = 0.12f) {
    val ink = Manga.colors.ink
    Canvas(modifier) {
        val step = 14f
        var y = 0f
        var row = 0
        while (y < size.height + step) {
            var x = if (row % 2 == 0) 0f else step / 2
            val t = (y / size.height).coerceIn(0f, 1f)
            val radius = 1f + 2.6f * (1f - t)
            while (x < size.width + step) {
                drawCircle(ink.copy(alpha = alpha), radius, Offset(x, y))
                x += step
            }
            y += step
            row++
        }
    }
}
