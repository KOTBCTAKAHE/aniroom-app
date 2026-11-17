package space.aniroom.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import space.aniroom.app.data.AnimeListPage
import space.aniroom.app.data.Badge
import space.aniroom.app.data.ProfileStats
import space.aniroom.app.data.PublicProfile
import space.aniroom.app.data.Session
import space.aniroom.app.data.ThemeMode
import space.aniroom.app.data.UserHub
import space.aniroom.app.data.api
import space.aniroom.app.data.obj
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.EmptyState
import space.aniroom.app.ui.components.Halftone
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.Poster
import space.aniroom.app.ui.components.ProgressLine
import space.aniroom.app.ui.components.RoundIcon
import space.aniroom.app.ui.components.SelectChip
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.components.Tag
import space.aniroom.app.ui.components.UserAvatar
import space.aniroom.app.ui.components.nickColor
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.formatHours
import space.aniroom.app.util.parseInstant

private val LIST_STATUSES = listOf(null to "Все", "watching" to "Смотрю", "planned" to "В планах", "completed" to "Просмотрено", "dropped" to "Брошено")

@Composable
fun ProfileScreen(
    userId: Int,
    isTab: Boolean,
    onBack: () -> Unit,
    openSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val m = Manga.colors
    val me by Session.user.collectAsState()
    val game by UserHub.game.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMe = me?.id == userId
    var profile by remember(userId) { mutableStateOf<PublicProfile?>(null) }
    var stats by remember(userId) { mutableStateOf<ProfileStats?>(null) }
    var badges by remember(userId) { mutableStateOf<List<Badge>?>(null) }
    var list by remember(userId) { mutableStateOf<AnimeListPage?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var avatarBusy by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        launch { api { publicProfile(userId) }.onSuccess { profile = it }.onFailure { error = it.message } }
        launch { stats = api { profileStats(userId) }.getOrNull() ?: ProfileStats(isPrivate = true) }
        launch { badges = api { userBadges(userId) }.getOrDefault(emptyList()) }
    }
    LaunchedEffect(userId, status) {
        list = null
        list = api { userList(userId, status) }.getOrDefault(AnimeListPage(hidden = true))
    }

    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        avatarBusy = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
            if (bytes != null) {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val part = MultipartBody.Part.createFormData("file", "avatar.${mime.substringAfter('/')}", bytes.toRequestBody(mime.toMediaTypeOrNull()))
                api { uploadAvatar(part) }.onSuccess { u -> Session.setUser(u); profile = profile?.copy(avatar = u.avatar) }.onFailure { error = it.message }
            }
            avatarBusy = false
        }
    }

    val p = profile
    LazyColumn(
        Modifier.fillMaxSize().background(m.paper),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 32.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(170.dp).background(m.red)) {
                Halftone(Modifier.fillMaxSize(), alpha = 0.16f)
                Text(
                    "ANIROOM", fontFamily = DisplayFont, fontSize = 64.sp, color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = 14.dp).rotate(-6f), maxLines = 1,
                )
                Row(Modifier.statusBarsPadding().padding(12.dp)) {
                    if (!isTab) RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
                    Spacer(Modifier.weight(1f))
                    if (isMe) RoundIcon(Icons.Rounded.Settings, "Настройки", openSettings)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().offset(y = (-48).dp).padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Box {
                        UserAvatar(p?.avatar ?: me?.avatar.takeIf { isMe }, p?.username ?: "", size = 96.dp, color = p?.nicknameColor)
                        if (isMe) Box(
                            Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(m.yellow).border(2.dp, m.ink, CircleShape)
                                .clickable(enabled = !avatarBusy) { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.CameraAlt, "Сменить аватар", tint = m.onYellow, modifier = Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    val level = if (isMe) game?.level else p?.game?.level
                    level?.let {
                        MangaPanel(Modifier.padding(bottom = 6.dp), color = m.yellow, radius = 12.dp, shadow = 3.dp) {
                            Text("УР. $it", fontFamily = DisplayFont, fontSize = 13.sp, color = m.onYellow, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (p == null && error == null) SkeletonBox(Modifier.width(180.dp).height(28.dp))
                p?.let { prof ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(prof.username, style = MaterialTheme.typography.headlineMedium, color = nickColor(prof.username, prof.nicknameColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (prof.specialTitle == "owner") { Spacer(Modifier.width(8.dp)); Tag("создатель", color = m.ink, textColor = m.paper) }
                    }
                    prof.game?.title?.let { Text(it, color = m.red, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    prof.bio?.takeIf { it.isNotBlank() }?.let { Text(it, color = m.inkSoft, modifier = Modifier.padding(top = 6.dp)) }
                    val since = parseInstant(prof.createdAt)?.let { java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", java.util.Locale("ru")).format(it.atZone(java.time.ZoneId.systemDefault())) }
                    since?.let { Text("с нами с $it", color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                    if (!isMe) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (prof.isFriend) MangaButton("Удалить из друзей", {
                                scope.launch { api { removeFriend(prof.id) }.onSuccess { profile = prof.copy(isFriend = false); UserHub.refreshFriends() } }
                            }, kind = ButtonKind.PAPER, compact = true, icon = Icons.Rounded.PersonRemove)
                            else if (prof.acceptsFriendRequests) MangaButton("В друзья", {
                                scope.launch { api { addFriend(obj("handle" to prof.username)) }.onSuccess { error = "Запрос отправлен" }.onFailure { error = it.message } }
                            }, compact = true, icon = Icons.Rounded.PersonAdd)
                            MangaButton(if (prof.isBlocked) "Разблокировать" else "Блок", {
                                scope.launch {
                                    if (prof.isBlocked) api { unblockUser(prof.id) }.onSuccess { profile = prof.copy(isBlocked = false) }
                                    else api { blockUser(prof.id) }.onSuccess { profile = prof.copy(isBlocked = true, isFriend = false) }
                                }
                            }, kind = ButtonKind.PAPER, compact = true, icon = Icons.Rounded.Block)
                        }
                    }
                }
                error?.let { Text(it, color = m.red, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        // stats
        item {
            val s = stats
            Column(Modifier.offset(y = (-32).dp).padding(horizontal = 16.dp)) {
                if (s == null) SkeletonBox(Modifier.fillMaxWidth().height(90.dp))
                else if (s.isPrivate) MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = m.muted); Spacer(Modifier.width(8.dp))
                        Text("Статистика скрыта", color = m.muted)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Часов", formatHours(s.watchSeconds ?: p?.watchSeconds ?: 0.0), Modifier.weight(1f))
                        StatTile("Серий", "${s.episodesWatched ?: 0}", Modifier.weight(1f))
                        StatTile("Дней подряд", "${s.streakDays ?: 0}", Modifier.weight(1f), highlight = (s.streakDays ?: 0) > 0)
                    }
                    if (s.activity.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        ActivityChart(s.activity.takeLast(30).map { it.watchSeconds })
                    }
                }
                if (isMe) game?.quests?.items?.takeIf { it.isNotEmpty() }?.let { quests ->
                    Spacer(Modifier.height(14.dp))
                    MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
                        Column(Modifier.padding(14.dp)) {
                            Text("КВЕСТЫ НЕДЕЛИ", style = Kicker, color = m.red)
                            quests.forEach { q ->
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (q.done) "✓" else "○", color = if (q.done) m.green else m.muted, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.width(8.dp))
                                    Text(q.title, color = m.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text("+${q.xp} XP", color = m.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                ProgressLine((q.value / q.target.coerceAtLeast(1.0)).toFloat(), Modifier.fillMaxWidth(), height = 6.dp, color = if (q.done) m.green else m.red)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("Список", tab == 0, { tab = 0 })
                SelectChip("Ачивки ${badges?.count { it.earned && !it.isSpecial }?.let { "· $it" } ?: ""}", tab == 1, { tab = 1 })
                SelectChip("Топ", tab == 2, { tab = 2 })
            }
            Spacer(Modifier.height(12.dp))
        }
        when (tab) {
            0 -> {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LIST_STATUSES) { (key, label) ->
                            val count = if (key == null) list?.total else list?.counts?.get(key)
                            SelectChip(label + (count?.let { " $it" } ?: ""), status == key, { status = key })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                val l = list
                when {
                    l == null -> item { Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { repeat(3) { SkeletonBox(Modifier.weight(1f).aspectRatio(0.68f)) } } }
                    l.hidden -> item { EmptyState("Список скрыт", "Пользователь закрыл свой список", mood = MascotMood.SLEEP) }
                    l.entries.isEmpty() -> item { EmptyState("Пусто", "Тут появятся тайтлы из комнат", mood = MascotMood.SEARCH) }
                    else -> items(l.entries.chunked(3), key = { row -> row.first().shikimoriId + status }) { row ->
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).animateItem(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { e ->
                                Column(Modifier.weight(1f)) {
                                    MangaPanel(Modifier.fillMaxWidth().aspectRatio(0.68f), radius = 10.dp, shadow = 3.dp) {
                                        Poster(e.poster, Modifier.fillMaxSize(), radius = 0.dp)
                                        if (e.episodesTotal > 0) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(4.dp)) {
                                            Text("${e.episodesWatched}/${e.episodesTotal}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(e.title, color = m.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            1 -> {
                val b = badges
                if (b == null) item { SkeletonBox(Modifier.padding(16.dp).fillMaxWidth().height(120.dp)) }
                else if (b.isEmpty()) item { EmptyState("Нет достижений", "Они появятся после первых просмотров", mood = MascotMood.SEARCH) }
                else items(b.filter { !it.secret || it.earned }.sortedByDescending { it.earned }.chunked(2), key = { it.first().key }) { row ->
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { badge -> BadgeCard(badge, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            else -> {
                val top = stats?.topAnime.orEmpty()
                if (top.isEmpty()) item { EmptyState("Пока пусто", "Здесь будут самые просматриваемые тайтлы", mood = MascotMood.SEARCH) }
                else items(top.take(10)) { t ->
                    MangaPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), shadow = 3.dp) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Poster(t.poster, Modifier.width(44.dp).aspectRatio(0.7f), radius = 8.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.title, color = m.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatHours(t.watchSeconds)} · ${t.episodesCount} сер.", color = m.muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    val m = Manga.colors
    MangaPanel(modifier, color = if (highlight) m.yellow else m.paper2, shadow = 3.dp) {
        Column(Modifier.padding(vertical = 12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = DisplayFont, fontSize = 18.sp, color = if (highlight) m.onYellow else m.ink, maxLines = 1)
            Text(label, color = if (highlight) m.onYellow else m.muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ActivityChart(values: List<Double>) {
    val m = Manga.colors
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
        Column(Modifier.padding(12.dp)) {
            Text("АКТИВНОСТЬ · 30 ДНЕЙ", style = Kicker, color = m.red)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                values.forEach { v ->
                    val f = (v / max).toFloat().coerceIn(0.04f, 1f)
                    Box(Modifier.weight(1f).fillMaxHeight(f).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(if (v > 0) m.red else m.paper3))
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge, modifier: Modifier) {
    val m = Manga.colors
    val rarityColor = when (badge.rarity.lowercase()) {
        "legendary", "легендарный" -> m.yellow
        "rare", "редкий" -> m.blue
        "epic", "эпический" -> Color(0xFF8E44AD)
        else -> m.paper3
    }
    MangaPanel(modifier, color = if (badge.earned) m.paper2 else m.paper3, shadow = if (badge.earned) 3.dp else 1.dp) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(if (badge.earned) rarityColor else m.paper).border(2.dp, m.ink, CircleShape), contentAlignment = Alignment.Center) {
                Text(if (badge.earned) "★" else "?", fontWeight = FontWeight.Black, color = m.ink)
            }
            Spacer(Modifier.height(8.dp))
            Text(badge.title, color = if (badge.earned) m.ink else m.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(badge.description, color = m.muted, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (!badge.earned && badge.progress > 0) {
                Spacer(Modifier.height(6.dp))
                ProgressLine(badge.progress.toFloat().let { if (it > 1f) it / 100f else it }, Modifier.fillMaxWidth(), height = 5.dp)
            }
            if (badge.rarity.isNotBlank()) Text(badge.rarity, color = m.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// --------------------------------------------------------------------------------------- settings

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val m = Manga.colors
    val prefs by Session.appPrefs.collectAsState()
    val user by Session.user.collectAsState()
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Column(
        Modifier.fillMaxSize().background(m.paper).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("ПРИЛОЖЕНИЕ", style = Kicker, color = m.red)
                Text("Настройки", style = MaterialTheme.typography.headlineMedium, color = m.ink)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("ТЕМА", style = Kicker, color = m.red)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.SYSTEM to "Авто", ThemeMode.LIGHT to "Светлая", ThemeMode.SEPIA to "Сепия", ThemeMode.DARK to "Тёмная").forEach { (mode, label) ->
                SelectChip(label, prefs.theme == mode, { Session.updatePrefs { it.copy(theme = mode) } })
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("ПРОСМОТР", style = Kicker, color = m.red)
        Spacer(Modifier.height(8.dp))
        MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
            Column {
                PrefRow("Пропускать опенинг сам", "Когда ты хост — прыжок через опенинг", prefs.autoSkipOpening) { v -> Session.updatePrefs { it.copy(autoSkipOpening = v) } }
                PrefRow("Картинка в картинке", "Видео продолжится поверх других приложений", prefs.autoPip) { v -> Session.updatePrefs { it.copy(autoPip = v) } }
                PrefRow("Экономия трафика", "Не выше 480p", prefs.dataSaver) { v -> Session.updatePrefs { it.copy(dataSaver = v) } }
                PrefRow("Летящие реакции", "Показывать реакции других зрителей", prefs.reactions) { v -> Session.updatePrefs { it.copy(reactions = v) } }
                PrefRow("Чат поверх плеера", "Новые сообщения в полноэкранном режиме", prefs.chatToasts) { v -> Session.updatePrefs { it.copy(chatToasts = v) } }
                PrefRow("Вибрация", "Тактильный отклик на нажатия", prefs.haptics) { v -> Session.updatePrefs { it.copy(haptics = v) } }
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Spacer(Modifier.height(16.dp))
            MangaButton("Включить уведомления", { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, kind = ButtonKind.YELLOW, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
        Text("АККАУНТ", style = Kicker, color = m.red)
        Spacer(Modifier.height(8.dp))
        MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(user?.avatar, user?.username ?: "?", size = 44.dp, color = user?.nicknameColor)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.username ?: "", color = m.ink, fontWeight = FontWeight.Bold)
                    Text(user?.email ?: "", color = m.muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        MangaButton("Выйти", { Session.signOut() }, kind = ButtonKind.PAPER, icon = Icons.AutoMirrored.Rounded.Logout, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Text("AniRoom для Android · ${space.aniroom.app.BuildConfig.VERSION_NAME}", color = m.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun PrefRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val m = Manga.colors
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = m.ink, fontWeight = FontWeight.Bold)
            Text(subtitle, color = m.muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = m.red, checkedThumbColor = Color.White, uncheckedTrackColor = m.paper3, uncheckedBorderColor = m.ink, uncheckedThumbColor = m.ink),
        )
    }
}
