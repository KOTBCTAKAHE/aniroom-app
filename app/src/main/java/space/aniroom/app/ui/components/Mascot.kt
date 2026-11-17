package space.aniroom.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import space.aniroom.app.ui.theme.Manga

enum class MascotMood { IDLE, HAPPY, SHOCK, SLEEP, SEARCH }

private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

private object MascotPaths {
    val body = p("M38 96c2-9 10-13 22-13s20 4 22 13l3 22H35z")
    val collar = p("M54 84l6 9 6-9")
    val strings = p("M57 97v9M63 97v9")
    val band = p("M22 56C22 28 40 14 60 14s38 14 38 42")
    val face = p("M28 58c0-21 14-34 32-34s32 13 32 34c0 19-14 30-32 30S28 77 28 58z")
    val hair = p("M27 60c-2-24 12-40 33-40s35 16 33 40c-5-9-10-14-17-17 1 5 0 9-2 12-5-7-12-12-21-14 1 5-1 10-5 14-2-4-5-7-9-8-4 3-9 7-12 13z")
    val ahoge = p("M60 21c-2-8 2-14 9-16-1 6 0 10 3 14")
    val smile = p("M56 76c2 2 6 2 8 0")
    val happyEyes = p("M38 66c3-5 9-5 12 0M70 66c3-5 9-5 12 0")
    val happyMouth = p("M52 74c3 6 13 6 16 0z")
    val sleepEyes = p("M39 64c3 3 8 3 11 0M70 64c3 3 8 3 11 0")
    val shockMarks = p("M100 22l8-10M106 34l11-4M20 22l-8-10")
    val star1 = p("M104 20l2.5 6 6 2.5-6 2.5-2.5 6-2.5-6-6-2.5 6-2.5z")
    val star2 = p("M14 30l1.6 4 4 1.6-4 1.6-1.6 4-1.6-4-4-1.6 4-1.6z")
    val glassHandle = p("M106 105l9 10")
}

/** Ани — AniRoom's chibi mascot, redrawn natively with idle animation (bob, blink, ahoge sway). */
@Composable
fun Mascot(mood: MascotMood = MascotMood.IDLE, size: Dp = 120.dp, modifier: Modifier = Modifier) {
    val m = Manga.colors
    val ink = Color(0xFF141210)
    val paper = Color(0xFFFAF7F0)
    val transition = rememberInfiniteTransition(label = "mascot")
    val bob by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val blink by transition.animateFloat(
        1f, 1f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 3600
                1f at 0
                1f at 3300
                0.1f at 3400
                1f at 3500
            },
        ),
        label = "blink",
    )
    val sway by transition.animateFloat(
        -8f, 8f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val paths = remember { MascotPaths }
    Canvas(modifier.size(size, size * 1.08f)) {
        val s = this.size.width / 120f
        scale(s, s, pivot = Offset.Zero) {
            val stroke = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun fillStroke(path: Path, fill: Color, width: Float = 3f) {
                drawPath(path, fill)
                drawPath(path, ink, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // body
            fillStroke(paths.body, m.red)
            fillStroke(paths.collar, paper)
            drawPath(paths.strings, ink, style = Stroke(2f, cap = StrokeCap.Round))
            val headY = if (mood == MascotMood.SLEEP) bob * 2f else -bob * 2.5f
            val tilt = if (mood == MascotMood.HAPPY) (bob - 0.5f) * 6f else (bob - 0.5f) * 2f
            translate(0f, headY) {
                rotate(tilt, pivot = Offset(60f, 70f)) {
                    drawPath(paths.band, ink, style = Stroke(6f, cap = StrokeCap.Round))
                    fillStroke(paths.face, paper)
                    fillStroke(paths.hair, ink)
                    rotate(if (mood == MascotMood.SLEEP) 0f else sway, pivot = Offset(64f, 20f)) {
                        fillStroke(paths.ahoge, ink)
                    }
                    // ear cups
                    drawEarCup(13f, m.yellow, ink)
                    drawEarCup(91f, m.yellow, ink)
                    // cheeks
                    drawOval(m.red.copy(alpha = 0.45f), Offset(35f, 69f), Size(10f, 6f))
                    drawOval(m.red.copy(alpha = 0.45f), Offset(75f, 69f), Size(10f, 6f))
                    when (mood) {
                        MascotMood.SLEEP -> {
                            drawPath(paths.sleepEyes, ink, style = stroke)
                            drawPath(paths.smile, ink, style = Stroke(2.5f, cap = StrokeCap.Round))
                        }
                        MascotMood.HAPPY -> {
                            drawPath(paths.happyEyes, ink, style = stroke)
                            fillStroke(paths.happyMouth, m.red, 2.5f)
                        }
                        MascotMood.SHOCK -> {
                            drawCircle(paper, 7f, Offset(45f, 63f)); drawCircle(ink, 7f, Offset(45f, 63f), style = stroke)
                            drawCircle(paper, 7f, Offset(75f, 63f)); drawCircle(ink, 7f, Offset(75f, 63f), style = stroke)
                            drawCircle(ink, 1.6f, Offset(45f, 63f)); drawCircle(ink, 1.6f, Offset(75f, 63f))
                            drawOval(ink, Offset(56f, 73f), Size(8f, 10f))
                        }
                        else -> {
                            val eyeH = 14f * blink
                            drawOval(ink, Offset(39.5f, 63f - eyeH / 2), Size(11f, eyeH))
                            drawOval(ink, Offset(69.5f, 63f - eyeH / 2), Size(11f, eyeH))
                            if (blink > 0.6f) {
                                drawCircle(paper, 2.2f, Offset(47f, 60f))
                                drawCircle(paper, 2.2f, Offset(77f, 60f))
                            }
                            drawPath(paths.smile, ink, style = Stroke(2.5f, cap = StrokeCap.Round))
                        }
                    }
                }
            }
            when (mood) {
                MascotMood.SHOCK -> drawPath(paths.shockMarks, ink, style = stroke)
                MascotMood.HAPPY -> {
                    val twinkle = 0.8f + bob * 0.4f
                    scale(twinkle, twinkle, pivot = Offset(104f, 29f)) { fillStroke(paths.star1, m.yellow, 2f) }
                    scale(2f - twinkle, 2f - twinkle, pivot = Offset(14f, 35f)) { fillStroke(paths.star2, m.yellow, 2f) }
                }
                MascotMood.SLEEP -> {
                    val z = bob
                    drawCircle(ink.copy(alpha = 0.3f + z * 0.7f), 3f, Offset(96f, 28f - z * 6f))
                    drawCircle(ink.copy(alpha = 1f - z * 0.6f), 2f, Offset(105f, 16f - z * 4f))
                }
                MascotMood.SEARCH -> {
                    translate(bob * 3f, -bob * 2f) {
                        drawCircle(paper, 11f, Offset(98f, 96f))
                        drawCircle(ink, 11f, Offset(98f, 96f), style = stroke)
                        drawPath(paths.glassHandle, ink, style = Stroke(5f, cap = StrokeCap.Round))
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun DrawScope.drawEarCup(x: Float, fill: Color, ink: Color) {
    drawRoundRect(fill, Offset(x, 50f), Size(16f, 26f), CornerRadius(7f))
    drawRoundRect(ink, Offset(x, 50f), Size(16f, 26f), CornerRadius(7f), style = Stroke(3f))
}
