package space.aniroom.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import space.aniroom.app.data.Api
import space.aniroom.app.data.Session
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.ui.theme.parseColor
import kotlin.math.abs

/** A comic panel: thick ink border with a hard offset shadow. */
@Composable
fun MangaPanel(
    modifier: Modifier = Modifier,
    color: Color = Manga.colors.paper2,
    radius: Dp = 16.dp,
    shadow: Dp = 4.dp,
    border: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val m = Manga.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed && onClick != null) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    val shape = RoundedCornerShape(radius)
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .drawBehind {
                val s = shadow.toPx() * (1f - press)
                if (s > 0f) drawRoundRect(
                    color = m.ink,
                    topLeft = Offset(s, s),
                    size = size,
                    cornerRadius = CornerRadius(radius.toPx()),
                )
            }
            .graphicsLayer {
                val s = shadow.toPx() * press
                translationX = s
                translationY = s
            }
            .clip(shape)
            .background(color)
            .border(BorderStroke(border, m.ink), shape)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null) {
                    if (Session.appPrefs.value.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                } else Modifier,
            ),
        content = content,
    )
}

enum class ButtonKind { RED, YELLOW, PAPER, INK }

@Composable
fun MangaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.RED,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
) {
    val m = Manga.colors
    val bg = when (kind) {
        ButtonKind.RED -> m.red
        ButtonKind.YELLOW -> m.yellow
        ButtonKind.PAPER -> m.paper2
        ButtonKind.INK -> m.ink
    }
    val fg = when (kind) {
        ButtonKind.RED -> Color.White
        ButtonKind.YELLOW -> m.onYellow
        ButtonKind.PAPER -> m.ink
        ButtonKind.INK -> m.paper
    }
    val alpha by animateFloatAsState(if (enabled) 1f else 0.5f, label = "enabled")
    MangaPanel(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        color = bg,
        radius = if (compact) 12.dp else 14.dp,
        shadow = if (compact) 3.dp else 4.dp,
        onClick = if (enabled && !loading) onClick else null,
    ) {
        Row(
            Modifier
                .align(Alignment.Center)
                .heightIn(min = if (compact) 40.dp else 50.dp)
                .padding(horizontal = if (compact) 14.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.5.dp)
                Spacer(Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = fg,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 14.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun RoundIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Manga.colors.paper2,
    tint: Color = Manga.colors.ink,
    size: Dp = 42.dp,
    badge: Int = 0,
) {
    Box(modifier) {
        MangaPanel(Modifier.size(size), color = color, radius = size / 2, shadow = 2.5.dp, onClick = onClick) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.align(Alignment.Center).size(size * 0.5f))
        }
        AnimatedVisibility(
            visible = badge > 0,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
            enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Manga.colors.red)
                    .border(1.5.dp, Manga.colors.ink, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(if (badge > 99) "99+" else "$badge", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun nickColor(name: String, hex: String?): Color {
    if (!hex.isNullOrBlank() && hex.uppercase() !in NICK_COLORS) return parseColor(hex, Color(0xFFE2342A))
    return siteNickColor(name, hex)
}

@Composable
fun UserAvatar(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: String? = null,
    online: Boolean? = null,
    ring: Boolean = true,
    frame: String? = null,
) {
    if (!frame.isNullOrBlank() && frame != "none") {
        AvatarFrame(frame, size, modifier) { UserAvatar(url, name, Modifier, size, color, online, ring, null) }
        return
    }
    val m = Manga.colors
    Box(modifier.size(size)) {
        val shape = CircleShape
        val base = Modifier
            .size(size)
            .clip(shape)
            .background(nickColor(name, color))
            .then(if (ring) Modifier.border(2.dp, m.ink, shape) else Modifier)
        val media = Api.media(url)
        if (media != null) {
            SubcomposeAsyncImage(
                model = media,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = base,
                error = { Initials(name, size) },
                loading = { Initials(name, size) },
            )
        } else {
            Box(base, contentAlignment = Alignment.Center) { Initials(name, size) }
        }
        if (online != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .background(if (online) m.green else m.muted)
                    .border(2.dp, m.paper2, CircleShape),
            )
        }
    }
}

@Composable
private fun Initials(name: String, size: Dp) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White,
            fontFamily = DisplayFont,
            fontSize = (size.value * 0.38f).sp,
        )
    }
}

@Composable
fun Poster(url: String?, modifier: Modifier = Modifier, radius: Dp = 12.dp, contentScale: ContentScale = ContentScale.Crop) {
    val shape = RoundedCornerShape(radius)
    SubcomposeAsyncImage(
        model = Api.media(url),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.clip(shape),
        loading = { Box(Modifier.fillMaxSize().shimmer()) },
        error = {
            Box(Modifier.fillMaxSize().background(Manga.colors.paper3), contentAlignment = Alignment.Center) {
                Text("▶", color = Manga.colors.muted, fontSize = 22.sp)
            }
        },
    )
}

fun Modifier.shimmer(): Modifier = composed {
    val m = Manga.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "x",
    )
    drawBehind {
        val w = size.width
        drawRect(
            Brush.linearGradient(
                colors = listOf(m.paper3, m.paper2, m.paper3),
                start = Offset(w * x - w, 0f),
                end = Offset(w * x, size.height),
            ),
        )
    }
}

@Composable
fun SkeletonBox(modifier: Modifier, radius: Dp = 14.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).shimmer())
}

@Composable
fun SectionHeader(kicker: String?, title: String, modifier: Modifier = Modifier, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (kicker != null) Text(kicker.uppercase(), style = Kicker, color = Manga.colors.red)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Manga.colors.ink)
        }
        if (action != null) Row(verticalAlignment = Alignment.CenterVertically, content = action)
    }
}

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color = Manga.colors.yellow, textColor: Color = Manga.colors.onYellow) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(1.5.dp, Manga.colors.ink, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val m = Manga.colors
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, spring(dampingRatio = 0.45f), label = "chip")
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .scale(scale)
            .clip(shape)
            .background(if (selected) m.ink else m.paper2)
            .border(2.dp, m.ink, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = if (selected) m.paper else m.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier, mood: MascotMood = MascotMood.SEARCH, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Mascot(mood = mood, size = 110.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Manga.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Manga.colors.muted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun LoadingScreen(text: String) {
    Box(Modifier.fillMaxSize().background(Manga.colors.paper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(mood = MascotMood.IDLE, size = 120.dp)
            Spacer(Modifier.height(16.dp))
            Text(text.uppercase(), style = Kicker, color = Manga.colors.ink)
            Spacer(Modifier.height(12.dp))
            LoaderBar(Modifier.width(140.dp))
        }
    }
}

@Composable
fun LoaderBar(modifier: Modifier = Modifier) {
    val m = Manga.colors
    val t = rememberInfiniteTransition(label = "bar")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "x")
    Box(
        modifier
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(2.dp, m.ink, RoundedCornerShape(6.dp))
            .background(m.paper2),
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.35f)
                .height(10.dp)
                .graphicsLayer { translationX = size.width * (x * (1f / 0.35f - 1f)) }
                .background(m.red),
        )
    }
}

@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, color: Color = Manga.colors.red, height: Dp = 8.dp) {
    val m = Manga.colors
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "progress")
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.height(height).clip(shape).background(m.paper3).border(1.5.dp, m.ink, shape)) {
        Box(Modifier.fillMaxWidth(animated).height(height).background(color))
    }
}

@Composable
fun Dialogish(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        MangaPanel(Modifier.fillMaxWidth(), color = Manga.colors.paper2, radius = 20.dp) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Manga.colors.ink)
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

val ScreenPadding = PaddingValues(horizontal = 16.dp)

@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier {
    val s by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.5f), label = "pressScale")
    return this.scale(s)
}

@Composable
fun animatedDp(target: Dp): Dp {
    val v by animateDpAsState(target, spring(dampingRatio = 0.7f), label = "dp")
    return v
}

@Suppress("unused")
val NoShape: Shape = RoundedCornerShape(0.dp)
