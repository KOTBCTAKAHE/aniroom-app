package space.aniroom.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.aniroom.app.data.GameEvent
import space.aniroom.app.data.GameEvents
import space.aniroom.app.data.Session
import space.aniroom.app.data.UnlockItem
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.Sounds
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Gold = Color(0xFFFFC83A)
private val GoldDeep = Color(0xFFB8860B)
private val Silver = Color(0xFFD9E2EC)

/** Manga "stamp" landing (mangaStampIn from the site): big + tilted → slam → settle. */
@Composable
private fun rememberStamp(): StampValues {
    val scale = remember { Animatable(1.45f) }
    val rot = remember { Animatable(-7f) }
    val dy = remember { Animatable(-14f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(280)) }
        launch {
            scale.animateTo(1f, keyframes { durationMillis = 520; 1.45f at 0; 0.94f at 286; 1.03f at 405; 1f at 520 })
        }
        launch { rot.animateTo(-1f, keyframes { durationMillis = 520; -7f at 0; 1.5f at 286; -1f at 405; -1f at 520 }) }
        launch { dy.animateTo(0f, keyframes { durationMillis = 520; -14f at 0; 2f at 286; -1f at 405; 0f at 520 }) }
    }
    return StampValues(scale.value, rot.value, dy.value, alpha.value)
}

private data class StampValues(val scale: Float, val rotation: Float, val dy: Float, val alpha: Float)

private fun Modifier.stamp(s: StampValues, shakeX: Float = 0f) = this.graphicsLayer {
    scaleX = s.scale
    scaleY = s.scale
    rotationZ = s.rotation
    translationY = s.dy * density
    translationX = shakeX * density
    alpha = s.alpha
}

@Composable
private fun rememberShake(enabled: Boolean, delayMs: Long = 286): Float {
    val x = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        delay(delayMs)
        x.animateTo(0f, keyframes { durationMillis = 320; 0f at 0; -4f at 80; 4f at 160; -3f at 240; 0f at 320 })
    }
    return x.value
}

/** Global overlay that announces achievements, levels, quests, streaks and room moments. */
@Composable
fun GameLayer(openAchievements: () -> Unit) {
    val achievements = remember { mutableStateListOf<GameEvent.Achievement>() }
    val toasts = remember { mutableStateListOf<GameEvent>() }
    val levelUps = remember { mutableStateListOf<GameEvent.LevelUp>() }
    var moment by remember { mutableStateOf<GameEvent.Moment?>(null) }
    var goal by remember { mutableStateOf<GameEvent.Goal?>(null) }

    LaunchedEffect(Unit) {
        GameEvents.events.collect { event ->
            when (event) {
                is GameEvent.Achievement -> achievements.add(event)
                is GameEvent.Xp, is GameEvent.Quest, is GameEvent.Streak -> {
                    if (event is GameEvent.Xp) toasts.removeAll { it is GameEvent.Xp }
                    toasts.add(event)
                    while (toasts.size > 3) toasts.removeAt(0)
                    if (event is GameEvent.Quest) { Sounds.play("common", "quest"); Sounds.stamp("common") }
                    val life = when (event) { is GameEvent.Xp -> 3200L; is GameEvent.Quest -> 4600L; else -> 3800L }
                    launch { delay(life); toasts.remove(event) }
                }
                is GameEvent.LevelUp -> {
                    levelUps.removeAll { it.event.level < event.event.level }
                    levelUps.add(event)
                    Sounds.play("legendary", "level_up")
                    Sounds.stamp("legendary")
                }
                is GameEvent.Moment -> {
                    moment = event
                    Sounds.stamp("common")
                    launch { delay(2600); if (moment?.id == event.id) moment = null }
                }
                is GameEvent.Goal -> {
                    goal = event
                    Sounds.play("legendary", "season_together")
                    Sounds.stamp("legendary")
                    launch { delay(6500); if (goal?.id == event.id) goal = null }
                }
            }
        }
    }

    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Box(Modifier.fillMaxSize()) {
        // toasts: top-right stack
        Column(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().displayCutoutPadding()
                .padding(top = if (landscape) 56.dp else 8.dp, end = 10.dp).widthIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            toasts.forEach { t -> key(t.id) { GameToast(t) } }
        }

        achievements.firstOrNull()?.let { a ->
            key(a.id) {
                AchievementCard(
                    a,
                    onDismiss = { achievements.remove(a) },
                    onOpen = { achievements.remove(a); openAchievements() },
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().displayCutoutPadding()
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp).widthIn(max = if (landscape) 440.dp else 600.dp),
                )
            }
        }

        moment?.let { mo -> key(mo.id) { MomentBurst(mo, Modifier.align(Alignment.Center)) } }
        goal?.let { g -> key(g.id) { GoalCelebration(g) { goal = null } } }
        levelUps.firstOrNull()?.let { lv ->
            key(lv.id) {
                LevelUpOverlay(lv, onClose = { levelUps.remove(lv) }, onWear = { levelUps.remove(lv); openAchievements() })
            }
        }
    }
}

// ------------------------------------------------------------------------------------- achievement

private fun rarityOf(a: GameEvent.Achievement): String = when {
    a.season -> "season"
    a.rarity.contains("legend", true) || a.rarity.contains("легенд", true) -> "legendary"
    a.rarity.contains("rare", true) || a.rarity.contains("редк", true) -> "rare"
    else -> "common"
}

@Composable
private fun AchievementCard(a: GameEvent.Achievement, onDismiss: () -> Unit, onOpen: () -> Unit, modifier: Modifier) {
    val m = Manga.colors
    val rarity = rarityOf(a)
    LaunchedEffect(Unit) {
        Sounds.play(if (a.season) "season" else a.rarity, if (a.season) "rezero_season" else a.key)
        delay(280)
        Sounds.stamp(rarity)
        delay(if (rarity == "legendary") 7000 else 5500)
        onDismiss()
    }
    val stamp = rememberStamp()
    val shake = rememberShake(rarity == "legendary")
    val drag = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val shard = a.season || a.icon.startsWith("rezero-shard")

    Box(
        modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .graphicsLayer { translationY = drag.value; alpha = (1f + drag.value / 300f).coerceIn(0f, 1f) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { scope.launch { if (drag.value < -60f) onDismiss() else drag.animateTo(0f, spring()) } },
                    onVerticalDrag = { change, d -> change.consume(); scope.launch { drag.snapTo((drag.value + d).coerceAtMost(30f)) } },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (rarity == "legendary") Rays(Modifier.size(420.dp, 220.dp), Gold.copy(alpha = 0.55f))
        MangaPanel(
            Modifier.fillMaxWidth().stamp(stamp, shake),
            color = if (rarity == "legendary") Color(0xFF1A1816) else m.paper2,
            radius = 18.dp, shadow = 5.dp,
            onClick = if (a.season) null else onOpen,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    if (rarity == "legendary" || rarity == "rare") Sparks(Modifier.size(110.dp), if (rarity == "legendary") Gold else m.blue, if (rarity == "legendary") 10 else 6)
                    if (a.season) Ripples(Modifier.size(100.dp), Gold)
                    Medal(rarity, shard, a.completedSeasons, a.totalSeasons, completed = !a.season, key = a.key, icon = a.icon)
                }
                Spacer(Modifier.width(12.dp))
                val onDark = rarity == "legendary"
                Column(Modifier.weight(1f)) {
                    Text(
                        when { a.season -> "НОВЫЙ СЕЗОН"; rarity == "legendary" -> "ЛЕГЕНДА"; else -> "ДОСТИЖЕНИЕ" },
                        style = Kicker, color = if (onDark) Gold else m.red,
                    )
                    Text(a.title, color = if (onDark) Color.White else m.ink, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (a.season) "Сезон завершён · ${a.completedSeasons}/${a.totalSeasons}" else a.description,
                        color = if (onDark) Color.White.copy(alpha = 0.75f) else m.inkSoft, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (a.season) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(a.totalSeasons.coerceIn(1, 8)) { i ->
                                val filled = i < a.completedSeasons
                                val fill = remember { Animatable(0f) }
                                LaunchedEffect(Unit) { delay(400L + i * 120L); fill.animateTo(if (filled) 1f else 0.25f, spring(dampingRatio = 0.5f)) }
                                Box(Modifier.width(26.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(m.paper3)) {
                                    Box(Modifier.fillMaxWidth(fill.value).height(6.dp).background(if (filled) Gold else m.muted))
                                }
                            }
                        }
                    } else Tag(
                        when (rarity) { "legendary" -> "Легендарное"; "rare" -> "Редкое"; else -> "Обычное" },
                        color = when (rarity) { "legendary" -> Gold; "rare" -> m.blue; else -> m.paper3 },
                        textColor = if (rarity == "rare") Color.White else m.ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun Medal(rarity: String, shard: Boolean, seasons: Int, totalSeasons: Int, completed: Boolean, key: String = "", icon: String = "") {
    val m = Manga.colors
    val pop = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(200)
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        check.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 700f))
    }
    val t = rememberInfiniteTransition(label = "medal")
    val shine by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "shine")
    Box(Modifier.size(60.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
        Canvas(Modifier.fillMaxSize()) {
            if (shard) drawShard(rarity == "season" || rarity == "legendary", seasons, totalSeasons, shine)
            else {
                val fill = when (rarity) {
                    "legendary" -> Brush.sweepGradient(listOf(GoldDeep, Gold, Color(0xFFFFF7D1), Gold, GoldDeep))
                    "rare" -> Brush.linearGradient(listOf(Color(0xFF6A9BFF), Color(0xFF2F6FDF)))
                    else -> Brush.linearGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFD23A)))
                }
                drawCircle(fill, size.minDimension / 2f)
                drawCircle(Color(0xFF141210), size.minDimension / 2f, style = Stroke(5f))
                // shine sweep
                drawCircle(
                    Brush.linearGradient(
                        0f to Color.Transparent, 0.45f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.55f), 0.55f to Color.Transparent, 1f to Color.Transparent,
                        start = Offset(size.width * shine - size.width, 0f), end = Offset(size.width * shine, size.height),
                    ),
                    size.minDimension / 2f - 3f,
                )
            }
        }
        if (!shard) Box(Modifier.align(Alignment.Center)) {
            BadgeIcon(key, icon, 28.dp, if (rarity == "rare") Color.White else Color(0xFF141210))
        }
        if (completed) Box(
            Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(24.dp)
                .graphicsLayer { scaleX = check.value; scaleY = check.value; rotationZ = (1f - check.value) * -90f }
                .clip(CircleShape).background(m.green).border(2.dp, Color(0xFF141210), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
    }
}

/** Re:Zero crystal shard (gold for seasons, silver for loops). */
internal fun DrawScope.drawShard(gold: Boolean, seasons: Int, total: Int, shine: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.02f)
        lineTo(w * 0.86f, h * 0.38f)
        lineTo(w * 0.62f, h * 0.98f)
        lineTo(w * 0.38f, h * 0.98f)
        lineTo(w * 0.14f, h * 0.38f)
        close()
    }
    val base = if (gold) listOf(Color(0xFFFFF3C4), Gold, GoldDeep) else listOf(Color.White, Silver, Color(0xFF8A9BB0))
    drawPath(path, Brush.linearGradient(base, Offset(0f, 0f), Offset(w, h)))
    drawPath(path, Brush.linearGradient(
        0f to Color.Transparent, 0.45f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.7f), 0.55f to Color.Transparent, 1f to Color.Transparent,
        start = Offset(w * shine - w, 0f), end = Offset(w * shine, h),
    ))
    drawPath(path, Color(0xFF141210), style = Stroke(4f))
    drawLine(Color(0xFF141210).copy(alpha = 0.4f), Offset(w * 0.5f, h * 0.02f), Offset(w * 0.5f, h * 0.98f), 2f)
    if (gold && total > 0) {
        // facets light up per completed season
        repeat(total.coerceAtMost(6)) { i ->
            val y = h * (0.3f + i * 0.6f / total)
            drawLine((if (i < seasons) Color.White else Color(0x33141210)), Offset(w * 0.3f, y), Offset(w * 0.7f, y), 2.5f)
        }
    }
}

@Composable
private fun Rays(modifier: Modifier, color: Color) {
    val t = rememberInfiniteTransition(label = "rays")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "a")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(250); appear.animateTo(1f, tween(400)) }
    Canvas(modifier.graphicsLayer { alpha = appear.value; scaleX = 0.6f + appear.value * 0.4f; scaleY = 0.6f + appear.value * 0.4f }) {
        val c = center
        val r = size.maxDimension
        rotate(angle, c) {
            repeat(16) { i ->
                val a0 = Math.toRadians(i * 22.5).toFloat()
                val a1 = Math.toRadians(i * 22.5 + 9).toFloat()
                val p = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + cos(a0) * r, c.y + sin(a0) * r)
                    lineTo(c.x + cos(a1) * r, c.y + sin(a1) * r)
                    close()
                }
                drawPath(p, Brush.radialGradient(listOf(color, Color.Transparent), c, r * 0.5f))
            }
        }
    }
}

@Composable
private fun Sparks(modifier: Modifier, color: Color, count: Int) {
    val progress = remember { Animatable(0f) }
    val dirs = remember { List(count) { i -> (i * 360f / count + Random.nextFloat() * 20f) to (0.75f + Random.nextFloat() * 0.5f) } }
    LaunchedEffect(Unit) {
        delay(280)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val maxR = size.minDimension / 2f
        dirs.forEach { (deg, speed) ->
            val a = Math.toRadians(deg.toDouble()).toFloat()
            val d = maxR * (0.35f + 0.65f * t * speed)
            val p = center + Offset(cos(a) * d, sin(a) * d)
            val s = 5f * (1f - t) + 1f
            drawStar(p, s, color.copy(alpha = 1f - t))
        }
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(c.x, c.y - r * 2)
        lineTo(c.x + r * 0.5f, c.y - r * 0.5f)
        lineTo(c.x + r * 2, c.y)
        lineTo(c.x + r * 0.5f, c.y + r * 0.5f)
        lineTo(c.x, c.y + r * 2)
        lineTo(c.x - r * 0.5f, c.y + r * 0.5f)
        lineTo(c.x - r * 2, c.y)
        lineTo(c.x - r * 0.5f, c.y - r * 0.5f)
        close()
    }
    drawPath(path, color)
}

@Composable
private fun Ripples(modifier: Modifier, color: Color) {
    val t = rememberInfiniteTransition(label = "ripples")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p")
    Canvas(modifier) {
        repeat(3) { i ->
            val v = (p + i / 3f) % 1f
            drawCircle(color.copy(alpha = (1f - v) * 0.7f), size.minDimension / 2f * (0.45f + v * 0.55f), style = Stroke(3f))
        }
    }
}

// ------------------------------------------------------------------------------------- confetti

private val CONFETTI_COLORS = listOf(Color(0xFFE2342A), Color(0xFFFFD23A), Color(0xFF2F6FDF), Color(0xFF2E9E5B), Color(0xFFFF8FB5), Color(0xFF141210))

@Composable
fun Confetti(modifier: Modifier, count: Int = 36, delayMs: Long = 200) {
    val progress = remember { Animatable(0f) }
    val pieces = remember {
        List(count) {
            val angle = Random.nextFloat() * 360f
            Triple(angle, 0.5f + Random.nextFloat() * 0.8f, CONFETTI_COLORS[it % CONFETTI_COLORS.size]) to Random.nextFloat() * 720f
        }
    }
    LaunchedEffect(Unit) {
        delay(delayMs)
        progress.animateTo(1f, tween(1700, easing = LinearEasing))
    }
    Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val reach = size.minDimension * 0.55f
        pieces.forEach { (spec, spin) ->
            val (deg, speed, color) = spec
            val a = Math.toRadians(deg.toDouble()).toFloat()
            val ease = 1f - (1f - t) * (1f - t)
            val pos = center + Offset(cos(a) * reach * speed * ease, sin(a) * reach * speed * ease + t * t * size.height * 0.35f)
            rotate(spin * t, pos) {
                drawRect(color.copy(alpha = if (t > 0.75f) (1f - t) / 0.25f else 1f), pos - Offset(5f, 3f), Size(10f, 6f))
            }
        }
    }
}

// ------------------------------------------------------------------------------------- toasts

@Composable
private fun GameToast(event: GameEvent) {
    val m = Manga.colors
    val stamp = rememberStamp()
    MangaPanel(Modifier.stamp(stamp), color = m.paper2, radius = 14.dp, shadow = 4.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            when (event) {
                is GameEvent.Xp -> {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(m.yellow).border(2.dp, m.ink, CircleShape), contentAlignment = Alignment.Center) {
                        Text("+${event.gained}", fontFamily = DisplayFont, fontSize = 11.sp, color = m.onYellow)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.widthIn(max = 220.dp)) {
                        Text("+${event.gained} опыта", fontWeight = FontWeight.Black, color = m.ink)
                        if (event.label.isNotBlank()) Text(event.label, color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        ProgressLine(event.progress, Modifier.width(160.dp), height = 6.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${event.level}", fontFamily = DisplayFont, color = m.red, fontSize = 18.sp)
                }
                is GameEvent.Quest -> {
                    Box(Modifier.size(40.dp).rotate(-8f).clip(RoundedCornerShape(8.dp)).background(m.red).border(2.dp, m.ink, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.widthIn(max = 200.dp)) {
                        Text("Задание выполнено!", fontWeight = FontWeight.Black, color = m.ink)
                        Text(event.title, color = m.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("+${event.xp}", fontFamily = DisplayFont, color = m.red, fontSize = 14.sp)
                }
                is GameEvent.Streak -> {
                    val t = rememberInfiniteTransition(label = "flame")
                    val f by t.animateFloat(0.9f, 1.15f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "f")
                    Text("🔥", fontSize = 28.sp, modifier = Modifier.graphicsLayer { scaleX = f; scaleY = f })
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("${event.days} дн. подряд!", fontWeight = FontWeight.Black, color = m.ink)
                        Text("Бонус за серию дней", color = m.muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("+${event.bonus}", fontFamily = DisplayFont, color = m.red, fontSize = 14.sp)
                }
                else -> Unit
            }
        }
    }
}

// ------------------------------------------------------------------------------------- moment

@Composable
private fun MomentBurst(event: GameEvent.Moment, modifier: Modifier) {
    val m = Manga.colors
    val p = event.payload
    val gasp = p.kind == "all_gasped"
    val emoji = if (gasp) mapOf("laugh" to "😂", "fire" to "🔥", "cry" to "😭", "shock" to "😱", "love" to "❤️", "skull" to "💀")[p.reaction] ?: "😱" else "✋"
    val pop = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 420f))
        delay(1700)
        fade.animateTo(0f, tween(400))
    }
    Box(modifier.graphicsLayer { alpha = fade.value }, contentAlignment = Alignment.Center) {
        Rays(Modifier.size(360.dp), m.yellow.copy(alpha = 0.6f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Text(emoji, fontSize = 72.sp)
            Text(
                if (gasp) "ВСЕ АХНУЛИ!" else "ЕДИНОГЛАСНО!",
                fontFamily = DisplayFont, fontSize = 26.sp, color = m.red,
                modifier = Modifier.rotate(-5f).background(m.paper2, RoundedCornerShape(8.dp)).border(2.dp, m.ink, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (gasp) "${p.users.filter { it.isNotBlank() }.take(4).joinToString(", ")} — в одну секунду" else "${p.count} из ${p.count} за «ещё серию»",
                color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

// ------------------------------------------------------------------------------------- goal

@Composable
private fun GoalCelebration(event: GameEvent.Goal, onClose: () -> Unit) {
    val m = Manga.colors
    val g = event.goal
    val stamp = rememberStamp()
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Confetti(Modifier.fillMaxSize(), count = 48)
        MangaPanel(Modifier.padding(24.dp).stamp(stamp), color = m.paper2, radius = 20.dp, shadow = 6.dp) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ВМЕСТЕ!", fontFamily = DisplayFont, fontSize = 34.sp, color = m.red, modifier = Modifier.rotate(-4f))
                Spacer(Modifier.height(8.dp))
                Text("Сезон досмотрен всей компанией", fontWeight = FontWeight.Black, color = m.ink, fontSize = 17.sp, textAlign = TextAlign.Center)
                Text("${g.title ?: "Тайтл"} · ${g.total} из ${g.total} серий", color = m.muted, textAlign = TextAlign.Center)
                if (g.rewardXp > 0) {
                    Spacer(Modifier.height(10.dp))
                    Tag("+${g.rewardXp} XP каждому", color = m.yellow, textColor = m.onYellow)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------- level up

@Composable
private fun LevelUpOverlay(event: GameEvent.LevelUp, onClose: () -> Unit, onWear: () -> Unit) {
    val m = Manga.colors
    val e = event.event
    val user by Session.user.collectAsState()
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val stamp = rememberStamp()
    val shake = rememberShake(true)
    var showTo by remember { mutableStateOf(false) }
    val bar = remember { Animatable(0f) }
    val sfx = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { delay(150); sfx.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 500f)) }
        delay(450)
        showTo = true
        bar.animateTo(e.progress.toFloat().coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        SpeedLines(Modifier.fillMaxSize(), alpha = 0.18f, focusY = 0.5f)
        Rays(Modifier.size(520.dp), m.yellow.copy(alpha = 0.35f))
        Confetti(Modifier.fillMaxSize(), count = 44, delayMs = 300)
        MangaPanel(
            Modifier.padding(if (landscape) 12.dp else 24.dp).widthIn(max = if (landscape) 680.dp else 380.dp).stamp(stamp, shake)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
            color = m.paper2, radius = 22.dp, shadow = 7.dp,
        ) {
            val unlocked = e.unlocked.take(6)
            val header: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "ДЗЫНЬ!", fontFamily = DisplayFont, fontSize = 22.sp, color = m.red,
                        modifier = Modifier.graphicsLayer { scaleX = sfx.value; scaleY = sfx.value; rotationZ = -8f },
                    )
                    Text("НОВЫЙ УРОВЕНЬ", style = Kicker, color = m.muted)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${e.fromLevel}", fontFamily = DisplayFont, fontSize = 30.sp, color = m.muted, modifier = Modifier.graphicsLayer { alpha = if (showTo) 0.45f else 1f })
                        Text("  →  ", fontFamily = DisplayFont, fontSize = 18.sp, color = m.muted)
                        AnimatedContent(
                            showTo,
                            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                            label = "lvl",
                        ) { visible ->
                            Box(
                                Modifier.size(78.dp).clip(CircleShape).background(if (visible) m.yellow else m.paper3).border(3.dp, m.ink, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text(if (visible) "${e.level}" else "?", fontFamily = DisplayFont, fontSize = 32.sp, color = m.onYellow) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ProgressLine(bar.value, Modifier.width(260.dp), height = 10.dp)
                    Text(
                        if (e.toNext > 0) "До ${e.level + 1} уровня — ${e.toNext} опыта" else "Максимальный уровень!",
                        color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            val rest: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (unlocked.isNotEmpty()) {
                        Text("ОТКРЫТО", style = Kicker, color = m.red)
                        Spacer(Modifier.height(6.dp))
                        unlocked.forEachIndexed { i, item ->
                            val appear = remember { Animatable(0f) }
                            LaunchedEffect(Unit) { delay(700L + i * 120L); appear.animateTo(1f, spring(dampingRatio = 0.55f)) }
                            Row(
                                Modifier.width(260.dp).padding(vertical = 4.dp).graphicsLayer { alpha = appear.value; translationX = (1f - appear.value) * 60f },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.width(56.dp).height(44.dp), contentAlignment = Alignment.Center) { UnlockPreview(item, user?.username ?: "Ник") }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(item.name, color = m.ink, fontWeight = FontWeight.Bold)
                                    Text(
                                        when (item.kind) { "frame" -> "Рамка аватара"; "bubble" -> "Облачко чата"; "nick_fx" -> "Эффект ника"; else -> "Титул" },
                                        color = m.muted, fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (unlocked.isNotEmpty()) MangaButton("Надеть", onWear, kind = ButtonKind.YELLOW, compact = true)
                        MangaButton("Отлично!", onClose, compact = true)
                    }
                }
            }
            // Fullscreen video is landscape: lay the panel out side by side so nothing gets cut off.
            if (landscape) {
                Row(
                    Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) { header(); rest() }
            } else {
                Column(
                    Modifier.padding(22.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { header(); Spacer(Modifier.height(14.dp)); rest() }
            }
        }
    }
}

@Composable
private fun UnlockPreview(item: UnlockItem, name: String) {
    val m = Manga.colors
    when (item.kind) {
        "frame" -> FrameSample(item.key, 32.dp)
        "bubble" -> {
            val skin = bubbleSkin(item.key, false, m, RoundedCornerShape(12.dp))
            Box(Modifier.clip(skin.shape).background(skin.background).border(2.dp, m.ink, skin.shape).bubbleExtras(skin, skin.text).padding(horizontal = 6.dp, vertical = 3.dp)) {
                Text("Привет", color = skin.text, fontSize = 10.sp)
            }
        }
        "nick_fx" -> NickText(name.take(6), siteNickColor(name, Session.user.value?.nicknameColor), item.key, fontSize = 13.sp)
        else -> Text("Тт", fontFamily = DisplayFont, color = m.red, fontSize = 18.sp)
    }
}
