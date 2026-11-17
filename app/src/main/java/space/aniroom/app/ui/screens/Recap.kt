package space.aniroom.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.aniroom.app.R
import space.aniroom.app.data.Leaderboard
import space.aniroom.app.data.MonthRecap
import space.aniroom.app.data.api
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.EmptyState
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.NickText
import space.aniroom.app.ui.components.SelectChip
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.components.Tag
import space.aniroom.app.ui.components.UserAvatar
import space.aniroom.app.ui.components.siteNickColor
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.ui.theme.MangaColors
import space.aniroom.app.util.plural
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val REACTION_EMOJI = mapOf("laugh" to "😂", "fire" to "🔥", "cry" to "😭", "shock" to "😱", "love" to "❤️", "skull" to "💀")

private fun hours(seconds: Double): String {
    val v = seconds / 3600.0
    return if (v >= 10) Math.round(v).toString() else (Math.round(v * 10) / 10.0).toString().replace(".", ",").removeSuffix(",0")
}

// ------------------------------------------------------------------------------------- leaderboard

@Composable
fun LeaderboardCard(modifier: Modifier = Modifier, openUser: (Int) -> Unit) {
    val m = Manga.colors
    var board by remember { mutableStateOf<Leaderboard?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { api { leaderboard() }.onSuccess { board = it }.onFailure { failed = true } }
    MangaPanel(modifier.fillMaxWidth(), shadow = 4.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("РЕЙТИНГ НЕДЕЛИ", style = Kicker, color = m.red)
                    Text("Опыт среди друзей с понедельника", color = m.muted, fontSize = 12.sp)
                }
                Icon(painterResource(R.drawable.lc_trophy), null, tint = m.red, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            val rows = board?.rows.orEmpty()
            when {
                failed -> Text("Рейтинг сейчас недоступен", color = m.muted)
                board == null -> repeat(3) { SkeletonBox(Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp)) }
                rows.size <= 1 -> EmptyState("Пока ты один", "Соревноваться интереснее с друзьями. Добавь кого-нибудь — и тут появится таблица.", mood = MascotMood.SEARCH)
                else -> {
                    val top = rows.maxOf { it.weekXp }.coerceAtLeast(1)
                    rows.take(8).forEachIndexed { i, row ->
                        val appear = remember { Animatable(0f) }
                        val bar = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(i * 70L)
                            appear.animateTo(1f, spring(dampingRatio = 0.6f))
                            bar.animateTo(row.weekXp.toFloat() / top, tween(700))
                        }
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .graphicsLayer { alpha = appear.value; translationX = (1f - appear.value) * 40f }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (row.isMe) m.yellow.copy(alpha = 0.35f) else m.paper3.copy(alpha = 0.5f))
                                .border(if (row.isMe) 2.dp else 0.dp, m.ink, RoundedCornerShape(12.dp))
                                .drawBehind { drawRect(m.red.copy(alpha = 0.13f), size = androidx.compose.ui.geometry.Size(size.width * bar.value, size.height)) }
                                .clickable { openUser(row.userId) },
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape).background(
                                        when (row.rank) { 1 -> m.yellow; 2 -> m.paper2; 3 -> m.paper3; else -> m.paper }
                                    ).border(2.dp, m.ink, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (row.rank == 1) Icon(painterResource(R.drawable.lc_crown), "1 место", tint = m.onYellow, modifier = Modifier.size(15.dp))
                                    else Text("${row.rank}", fontFamily = DisplayFont, fontSize = 11.sp, color = m.ink)
                                }
                                Spacer(Modifier.width(10.dp))
                                UserAvatar(row.avatar, row.username, size = 34.dp, color = row.nicknameColor, frame = row.cosmetics?.frame)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    NickText(row.username, siteNickColor(row.username, row.nicknameColor), row.cosmetics?.nickFx, fontSize = 14.sp)
                                    Text("${row.weekEpisodes} ${plural(row.weekEpisodes, "серия", "серии", "серий")}", color = m.muted, fontSize = 11.sp)
                                }
                                Tag("ур. ${row.level}", color = m.paper2, textColor = m.ink)
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${row.weekXp}", fontFamily = DisplayFont, fontSize = 15.sp, color = m.red)
                                    Text("опыта", color = m.muted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------- recap

private fun monthOptions(): List<Pair<String, String>> {
    val now = YearMonth.now()
    val fmt = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
    return (0 until 6).map { i ->
        val ym = now.minusMonths(i.toLong())
        ym.toString() to fmt.format(ym).replaceFirstChar { it.uppercase() }
    }
}

@Composable
fun RecapSection(modifier: Modifier = Modifier) {
    val m = Manga.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val options = remember { monthOptions() }
    var month by remember { mutableStateOf(options.first().first) }
    var recap by remember { mutableStateOf<MonthRecap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var card by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(month) {
        loading = true
        card = null
        recap = api { monthRecap(month) }.getOrNull()
        loading = false
        recap?.let { r -> card = withContext(Dispatchers.Default) { renderRecap(context, r, m) } }
    }
    Column(modifier.fillMaxWidth()) {
        Text("ИТОГИ МЕСЯЦА", style = Kicker, color = m.red)
        Text("Карточку можно сохранить картинкой и скинуть друзьям", color = m.muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) -> SelectChip(label, month == key, { month = key }) }
        }
        Spacer(Modifier.height(14.dp))
        val r = recap
        when {
            loading -> SkeletonBox(Modifier.fillMaxWidth().aspectRatio(1080f / 1350f))
            r == null -> Text("Не удалось загрузить итоги", color = m.muted)
            r.watchSeconds <= 0 && r.episodes == 0 && r.xp == 0 ->
                EmptyState("Месяц ещё пустой", "Ничего не посмотрено. Самое время позвать друзей в комнату!", mood = MascotMood.SLEEP)
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(hours(r.watchSeconds) to "часов вместе", "${r.episodes}" to "серий", "${r.daysActive}" to "дней с аниме", "+${r.xp}" to "опыта")
                        .forEachIndexed { i, (v, label) ->
                            val pop = remember(month) { Animatable(0f) }
                            LaunchedEffect(month) { delay(i * 90L); pop.animateTo(1f, spring(dampingRatio = 0.45f)) }
                            MangaPanel(Modifier.weight(1f).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, color = if (i == 0) m.red else m.paper2, shadow = 3.dp) {
                                Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(v, fontFamily = DisplayFont, fontSize = 16.sp, color = if (i == 0) androidx.compose.ui.graphics.Color.White else m.ink, maxLines = 1)
                                    Text(label, fontSize = 10.sp, color = if (i == 0) androidx.compose.ui.graphics.Color.White else m.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                }
                Spacer(Modifier.height(14.dp))
                val bmp = card
                MangaPanel(Modifier.fillMaxWidth().aspectRatio(1080f / 1350f), shadow = 5.dp, radius = 6.dp) {
                    if (bmp != null) Image(bmp.asImageBitmap(), "Карточка итогов месяца", Modifier.fillMaxWidth())
                    else SkeletonBox(Modifier.fillMaxWidth().aspectRatio(1080f / 1350f))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MangaButton("Поделиться", {
                        val b = bmp ?: return@MangaButton
                        scope.launch { shareRecap(context, b, r) }
                    }, icon = Icons.Rounded.Share, enabled = bmp != null, modifier = Modifier.weight(1f))
                    MangaButton("Сохранить", {
                        val b = bmp ?: return@MangaButton
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { saveToGallery(context, b, "aniroom-${r.month}.png") }
                            Toast.makeText(context, if (ok) "Сохранено в «Изображения/AniRoom»" else "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                        }
                    }, kind = ButtonKind.PAPER, icon = Icons.Rounded.Download, enabled = bmp != null, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private suspend fun shareRecap(context: Context, bitmap: Bitmap, recap: MonthRecap) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        File(dir, "aniroom-${recap.month}.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Мой ${recap.monthName.lowercase()} в AniRoom 🎬 aniroom.space")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Итоги месяца"))
}

private fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean = runCatching {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@runCatching false
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AniRoom")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } != null
}.getOrDefault(false)

/** Same 1080×1350 layout as the site's drawRecap() (ProgressPanels.tsx). */
fun renderRecap(context: Context, recap: MonthRecap, m: MangaColors): Bitmap {
    val bmp = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val ink = m.ink.toArgb()
    val paper = m.paper.toArgb()
    val paper2 = m.paper2.toArgb()
    val red = m.red.toArgb()
    val yellow = m.yellow.toArgb()
    val display = ResourcesCompat.getFont(context, R.font.rubik_mono_one) ?: Typeface.DEFAULT_BOLD
    val body = ResourcesCompat.getFont(context, R.font.plex_semibold) ?: Typeface.DEFAULT
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    c.drawColor(paper)
    p.color = ink
    p.alpha = (0.08f * 255).toInt()
    var y = 0
    while (y < 1350) {
        var x = if ((y / 14) % 2 == 1) 7 else 0
        while (x < 1080) { c.drawCircle(x.toFloat(), y.toFloat(), 2f, p); x += 14 }
        y += 14
    }
    p.alpha = 255

    fun panel(x: Float, y: Float, w: Float, h: Float, fill: Int) {
        p.style = Paint.Style.FILL
        p.color = ink; c.drawRect(x + 10, y + 10, x + w + 10, y + h + 10, p)
        p.color = fill; c.drawRect(x, y, x + w, y + h, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = ink
        c.drawRect(x, y, x + w, y + h, p)
        p.style = Paint.Style.FILL
    }
    fun text(s: String, x: Float, y: Float, size: Float, face: Typeface, color: Int, maxWidth: Float = 0f) {
        p.typeface = face; p.textSize = size; p.color = color
        var str = s
        if (maxWidth > 0) while (p.measureText(str) > maxWidth && str.length > 2) str = str.dropLast(2) + "…"
        c.drawText(str, x, y, p)
    }

    panel(60f, 60f, 960f, 250f, yellow)
    text("ANIROOM · ИТОГИ МЕСЯЦА", 100f, 120f, 30f, body, ink)
    text(recap.monthName.uppercase(), 100f, 205f, 64f, display, ink, 880f)
    text("@${recap.username} · ${recap.level} уровень", 100f, 265f, 36f, body, red, 880f)

    val stats = listOf(hours(recap.watchSeconds) to "часов вместе", "${recap.episodes}" to "серий", "${recap.daysActive}" to "дней с аниме", "+${recap.xp}" to "опыта")
    stats.forEachIndexed { i, (value, label) ->
        val x = 60f + (i % 2) * 490f
        val yy = 350f + (i / 2) * 230f
        panel(x, yy, 470f, 200f, if (i == 0) red else paper2)
        val color = if (i == 0) android.graphics.Color.WHITE else ink
        text(value, x + 34, yy + 118, 92f, display, color, 410f)
        text(label, x + 36, yy + 170, 32f, body, color)
    }

    panel(60f, 830f, 960f, 300f, paper2)
    text("ЧТО СМОТРЕЛИ", 100f, 890f, 30f, display, ink)
    if (recap.topTitles.isEmpty()) text("В этом месяце пока пусто", 100f, 960f, 36f, body, ink)
    recap.topTitles.take(3).forEachIndexed { i, item ->
        text("${i + 1}. ${item.title}", 100f, 960f + i * 56, 36f, body, ink, 740f)
        text("${hours(item.watchSeconds)} ч", 880f, 960f + i * 56, 36f, body, red)
    }

    val footer = buildList {
        recap.companion?.let { add("чаще всего с @${it.username}") }
        recap.favoriteReaction?.let { add("любимая реакция ${REACTION_EMOJI[it] ?: ""}") }
        if (recap.badges.isNotEmpty()) add("${recap.badges.size} новых достижений")
    }.joinToString(" · ").ifBlank { "aniroom" }
    text(footer, 60f, 1200f, 32f, body, ink, 960f)
    text("ANIROOM", 60f, 1290f, 26f, display, ink)
    p.color = red
    c.drawRect(250f, 1270f, 1020f, 1280f, p)
    return bmp
}
