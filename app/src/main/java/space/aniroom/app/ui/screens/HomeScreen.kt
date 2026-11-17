package space.aniroom.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import space.aniroom.app.data.AnimeChoice
import space.aniroom.app.data.Room
import space.aniroom.app.data.Session
import space.aniroom.app.data.UserHub
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.Dialogish
import space.aniroom.app.ui.components.EmptyState
import space.aniroom.app.ui.components.Halftone
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaField
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.Poster
import space.aniroom.app.ui.components.ProgressLine
import space.aniroom.app.ui.components.RoundIcon
import space.aniroom.app.ui.components.SectionHeader
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.components.Tag
import space.aniroom.app.ui.components.UserAvatar
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.formatTime
import space.aniroom.app.util.plural
import space.aniroom.app.util.relativeTime

@Composable
fun HomeTab(
    openRoom: (String) -> Unit,
    openSettings: () -> Unit,
    openProfile: () -> Unit,
    contentPadding: PaddingValues,
) {
    val vm: HomeViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val user by Session.user.collectAsState()
    val game by UserHub.game.collectAsState()
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    var createOpen by remember { mutableStateOf(false) }
    var joinBusy by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<Room?>(null) }
    var deleteTarget by remember { mutableStateOf<Room?>(null) }

    fun join(code: String) {
        joinBusy = true
        joinError = null
        scope.launch {
            vm.join(code).onSuccess { openRoom(it.roomCode) }.onFailure { joinError = it.message }
            joinBusy = false
        }
    }

    PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth()) {
                    Halftone(Modifier.matchParentSizeSafe().height(220.dp), alpha = if (m.isDark) 0.08f else 0.07f)
                    Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.clickable(onClick = openProfile)) {
                                UserAvatar(user?.avatar, user?.username ?: "?", size = 48.dp, color = user?.nicknameColor)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ПРИВЕТ,", style = Kicker, color = m.red)
                                Text(user?.username ?: "", style = MaterialTheme.typography.titleLarge, color = m.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            RoundIcon(Icons.Rounded.Settings, "Настройки", openSettings)
                        }
                        game?.let { g ->
                            Spacer(Modifier.height(14.dp))
                            MangaPanel(Modifier.fillMaxWidth(), color = m.paper2, shadow = 3.dp, onClick = openProfile) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(44.dp).clip(CircleShape).background(m.yellow).border(2.dp, m.ink, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("${g.level}", fontFamily = DisplayFont, color = m.onYellow, fontSize = 16.sp) }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Уровень ${g.level}", fontWeight = FontWeight.Bold, color = m.ink, modifier = Modifier.weight(1f))
                                            Text("${g.intoLevel}/${g.levelNeed} XP", color = m.muted, fontSize = 12.sp)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        ProgressLine(g.progress.toFloat(), Modifier.fillMaxWidth())
                                    }
                                    g.streak?.takeIf { it.days > 0 }?.let { st ->
                                        Spacer(Modifier.width(12.dp))
                                        FlameStreak(st.days, st.atRisk)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                HeroCard(
                    onCreate = { createOpen = true },
                    onJoin = ::join,
                    busy = joinBusy,
                    error = joinError,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (ui.continueWatching.isNotEmpty()) {
                item { SectionHeader("продолжить", "С того же места", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(ui.continueWatching, key = { "${it.progress.roomCode}:${it.progress.episode}" }) { item ->
                            ContinueCard(item, Modifier.animateItem()) { item.progress.roomCode?.let(openRoom) }
                        }
                    }
                }
            }
            if (ui.live.isNotEmpty()) {
                item { SectionHeader("в эфире", "Сейчас смотрят", Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp)) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(ui.live, key = { it.roomCode }) { room ->
                            LiveRoomCard(room, Modifier.animateItem()) { join(room.roomCode) }
                        }
                    }
                }
            }
            item {
                SectionHeader(
                    "твои",
                    "Мои комнаты",
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
                ) {
                    if (ui.history.isNotEmpty()) Text(
                        "${ui.history.size}", fontFamily = DisplayFont, color = m.muted, fontSize = 18.sp,
                    )
                }
            }
            if (ui.loading) {
                items(3) { SkeletonBox(Modifier.padding(horizontal = 16.dp, vertical = 7.dp).fillMaxWidth().height(96.dp)) }
            } else if (ui.history.isEmpty()) {
                item {
                    EmptyState(
                        "Комнат пока нет",
                        "Создай комнату для друзей или войди по коду приглашения",
                        mood = MascotMood.SEARCH,
                    ) { MangaButton("Создать комнату", { createOpen = true }, icon = Icons.Rounded.Add) }
                }
            } else {
                items(ui.history, key = { it.roomCode }) { room ->
                    RoomRow(
                        room = room,
                        isOwner = room.ownerId == user?.id,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp).animateItem(),
                        onOpen = { openRoom(room.roomCode) },
                        onRename = { renameTarget = room },
                        onDelete = { deleteTarget = room },
                    )
                }
            }
            ui.error?.let { err ->
                item {
                    Text(err, color = m.red, modifier = Modifier.padding(16.dp).clickable { vm.clearError() })
                }
            }
        }
    }

    if (createOpen) CreateRoomSheet(
        onDismiss = { createOpen = false },
        onCreate = { title, isPublic, anime ->
            vm.create(title, isPublic, anime).onSuccess {
                createOpen = false
                openRoom(it.roomCode)
                vm.refresh()
            }
        },
    )
    renameTarget?.let { room ->
        var title by remember(room.roomCode) { mutableStateOf(room.title) }
        Dialogish("Переименовать", onDismiss = { renameTarget = null }) {
            MangaField(title, { title = it.take(120) }, placeholder = "Название комнаты", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            MangaButton("Сохранить", { vm.rename(room, title.trim()); renameTarget = null }, enabled = title.trim().length >= 2, modifier = Modifier.fillMaxWidth())
        }
    }
    deleteTarget?.let { room ->
        Dialogish("Удалить комнату?", onDismiss = { deleteTarget = null }) {
            Text("«${room.title}» и её история будут удалены. Это нельзя отменить.", color = m.inkSoft)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MangaButton("Отмена", { deleteTarget = null }, kind = ButtonKind.PAPER, modifier = Modifier.weight(1f))
                MangaButton("Удалить", { vm.delete(room); deleteTarget = null }, icon = Icons.Rounded.Delete, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun Modifier.matchParentSizeSafe(): Modifier = this.fillMaxWidth()

@Composable
private fun FlameStreak(days: Int, atRisk: Boolean) {
    val m = Manga.colors
    val t = rememberInfiniteTransition(label = "flame")
    val s by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.LocalFireDepartment, "Серия дней", tint = if (atRisk) m.muted else m.red, modifier = Modifier.size(26.dp).scale(s))
        Text("$days", fontWeight = FontWeight.Black, color = m.ink, fontSize = 13.sp)
    }
}

@Composable
private fun HeroCard(onCreate: () -> Unit, onJoin: (String) -> Unit, busy: Boolean, error: String?, modifier: Modifier = Modifier) {
    val m = Manga.colors
    var code by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    MangaPanel(modifier.fillMaxWidth(), color = m.red, radius = 20.dp, shadow = 5.dp) {
        Box(Modifier.matchParentSizeSafe()) {
            Halftone(Modifier.fillMaxWidth().height(200.dp), alpha = 0.12f)
        }
        Column(Modifier.padding(18.dp)) {
            Text("СМОТРИМ ВМЕСТЕ", style = Kicker, color = Color.White.copy(alpha = 0.85f))
            Text("Новая комната\nили по коду?", fontFamily = DisplayFont, fontSize = 22.sp, color = Color.White, lineHeight = 28.sp)
            Spacer(Modifier.height(16.dp))
            MangaButton("Создать комнату", onCreate, kind = ButtonKind.YELLOW, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MangaPanel(Modifier.weight(1f).height(52.dp), color = m.paper2, radius = 14.dp, shadow = 3.dp) {
                    BasicTextField(
                        value = code,
                        onValueChange = { v -> code = v.filter { it.isLetterOrDigit() }.take(6).uppercase() },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = DisplayFont, fontSize = 20.sp, color = m.ink, letterSpacing = 6.sp, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { if (code.length == 6) { focus.clearFocus(); onJoin(code) } }),
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 12.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (code.isEmpty()) Text("КОД", fontFamily = DisplayFont, fontSize = 18.sp, color = m.muted, letterSpacing = 6.sp)
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.width(10.dp))
                MangaButton(
                    "Войти", { focus.clearFocus(); onJoin(code) },
                    kind = ButtonKind.INK, enabled = code.length == 6, loading = busy,
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                )
            }
            AnimatedVisibility(error != null, enter = fadeIn() + slideInVertically()) {
                Text(error ?: "", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun ContinueCard(item: ContinueItem, modifier: Modifier, onClick: () -> Unit) {
    val m = Manga.colors
    val p = item.progress
    val anime = item.room?.anime
    Column(modifier.width(230.dp)) {
        MangaPanel(Modifier.fillMaxWidth().aspectRatio(16f / 10f), radius = 14.dp, shadow = 4.dp, onClick = onClick) {
            Poster(anime?.poster, Modifier.fillMaxSize(), radius = 0.dp)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(
                    anime?.title ?: item.room?.title ?: "Комната ${p.roomCode}",
                    color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    (if (p.episode > 0) "Серия ${p.episode} · " else "") + formatTime(p.positionSec),
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
                )
            }
            Box(
                Modifier.align(Alignment.Center).size(46.dp).clip(CircleShape).background(m.red).border(2.dp, m.ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("▶", color = Color.White, fontSize = 18.sp) }
        }
        Spacer(Modifier.height(8.dp))
        if (p.durationSec > 0) ProgressLine((p.positionSec / p.durationSec).toFloat(), Modifier.fillMaxWidth(), height = 6.dp)
    }
}

@Composable
private fun LiveRoomCard(room: Room, modifier: Modifier, onClick: () -> Unit) {
    val m = Manga.colors
    val t = rememberInfiniteTransition(label = "live")
    val pulse by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse")
    MangaPanel(modifier.width(170.dp), radius = 14.dp, shadow = 4.dp, onClick = onClick) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(0.9f)) {
                Poster(room.anime?.poster, Modifier.fillMaxSize(), radius = 0.dp)
                Row(
                    Modifier.padding(8.dp).clip(RoundedCornerShape(8.dp)).background(m.red).padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = pulse)))
                    Spacer(Modifier.width(5.dp))
                    Text("LIVE · ${room.liveCount.coerceAtLeast(room.participants.count { it.online })}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(room.title, color = m.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    room.anime?.let { a -> (a.title ?: "") + if (a.episode > 0) " · ${a.episode} сер." else "" } ?: "Без тайтла",
                    color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RoomRow(
    room: Room,
    isOwner: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val m = Manga.colors
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        MangaPanel(Modifier.fillMaxWidth(), radius = 16.dp, shadow = 4.dp) {
            Row(
                Modifier
                    .combinedClickable(onClick = onOpen, onLongClick = { if (isOwner) menu = true })
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(62.dp).aspectRatio(0.7f)) {
                    if (room.anime?.poster != null) Poster(room.anime?.poster, Modifier.fillMaxSize(), radius = 10.dp)
                    else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(m.paper3), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Movie, null, tint = m.muted)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwner) {
                            Icon(Icons.Rounded.WorkspacePremium, "Ты хост", tint = m.red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(room.title, color = m.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    }
                    Text(
                        room.anime?.title?.let { t -> val ep = room.anime?.episode ?: 0; t + if (ep > 0) " · серия $ep" else "" } ?: "Тайтл не выбран",
                        color = m.inkSoft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(room.roomCode, color = m.paper3, textColor = m.ink)
                        if (room.liveCount > 0) Tag("● ${room.liveCount} ${plural(room.liveCount, "зритель", "зрителя", "зрителей")}", color = m.red, textColor = Color.White)
                        Icon(if (room.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock, null, tint = m.muted, modifier = Modifier.size(14.dp))
                        Text(relativeTime(room.lastViewedAt ?: room.createdAt), color = m.muted, fontSize = 11.sp, maxLines = 1)
                    }
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = m.ink, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Переименовать") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onRename() })
            DropdownMenuItem(text = { Text("Удалить", color = m.red) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = m.red) }, onClick = { menu = false; onDelete() })
        }
    }
}

@Composable
private fun CreateRoomSheet(onDismiss: () -> Unit, onCreate: suspend (String, Boolean, AnimeChoice?) -> Result<Room>) {
    val m = Manga.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val user = Session.user.collectAsState().value
    var title by remember { mutableStateOf(user?.let { "Комната ${it.username}" } ?: "") }
    var isPublic by remember { mutableStateOf(false) }
    var anime by remember { mutableStateOf<AnimeChoice?>(null) }
    var picker by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = m.paper, dragHandle = {
        Box(Modifier.padding(top = 10.dp).size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(m.ink))
    }) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("НОВАЯ КОМНАТА", style = Kicker, color = m.red)
            Text("Настрой просмотр", style = MaterialTheme.typography.headlineSmall, color = m.ink)
            Spacer(Modifier.height(18.dp))
            MangaField(title, { title = it.take(120) }, label = "Название", placeholder = "Например, вечер аниме", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            MangaPanel(Modifier.fillMaxWidth(), color = m.paper2, shadow = 3.dp, onClick = { picker = true }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(48.dp).aspectRatio(0.7f)) {
                        if (anime?.poster != null) Poster(anime?.poster, Modifier.fillMaxSize(), radius = 8.dp)
                        else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(m.paper3), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, null, tint = m.muted)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(anime?.title ?: "Выбрать тайтл", color = m.ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            anime?.let { "${it.translationName} · ${it.quality}p · " + if (it.episode > 0) "серия ${it.episode}" else "фильм" }
                                ?: "Можно выбрать сейчас или уже в комнате",
                            color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            MangaPanel(Modifier.fillMaxWidth(), color = m.paper2, shadow = 3.dp, onClick = { isPublic = !isPublic }) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isPublic) Icons.Rounded.Groups else Icons.Rounded.Lock, null, tint = if (isPublic) m.green else m.muted)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Публичная комната", color = m.ink, fontWeight = FontWeight.Bold)
                        Text(if (isPublic) "Её увидят в «Сейчас смотрят»" else "Вход по ссылке или коду", color = m.muted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isPublic, onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = m.red, checkedThumbColor = Color.White, uncheckedTrackColor = m.paper3, uncheckedBorderColor = m.ink, uncheckedThumbColor = m.ink),
                    )
                }
            }
            error?.let { Text(it, color = m.red, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(20.dp))
            MangaButton(
                "Создать и войти",
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        onCreate(title.trim(), isPublic, anime).onFailure { error = it.message }
                        busy = false
                    }
                },
                loading = busy,
                enabled = title.trim().length >= 2,
                icon = Icons.Rounded.Add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (picker) AnimePickerDialog(
        confirmText = "Выбрать",
        onDismiss = { picker = false },
        onConfirm = { anime = it; picker = false; if (title.isBlank() || title.startsWith("Комната ")) title = it.title.take(120) },
    )
}
