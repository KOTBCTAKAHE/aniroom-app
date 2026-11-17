package space.aniroom.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.aniroom.app.data.AppNotification
import space.aniroom.app.data.FriendRequestItem
import space.aniroom.app.data.UserHub
import space.aniroom.app.data.api
import space.aniroom.app.data.describe
import space.aniroom.app.data.obj
import space.aniroom.app.data.roomCode
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.EmptyState
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaField
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.ProgressLine
import space.aniroom.app.ui.components.RoundIcon
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.components.UserAvatar
import space.aniroom.app.ui.components.nickColor
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.formatHours
import space.aniroom.app.util.relativeTime

@Composable
private fun TabTitle(kicker: String, title: String, action: (@Composable () -> Unit)? = null) {
    val m = Manga.colors
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(kicker.uppercase(), style = Kicker, color = m.red)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = m.ink)
        }
        action?.invoke()
    }
}

@Composable
fun FriendsTab(openUser: (Int) -> Unit, openRoom: (String) -> Unit, contentPadding: PaddingValues) {
    val m = Manga.colors
    val friends by UserHub.friends.collectAsState()
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<FriendRequestItem>?>(null) }
    var handle by remember { mutableStateOf("") }
    var addBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    suspend fun reload() {
        UserHub.refreshFriends()
        requests = api { friendRequests("incoming") }.getOrDefault(emptyList())
        loaded = true
    }
    LaunchedEffect(Unit) { reload() }

    PullToRefreshBox(refreshing, onRefresh = { scope.launch { refreshing = true; reload(); refreshing = false } }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { TabTitle("компания", "Друзья") }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        MangaField(handle, { handle = it.take(64) }, placeholder = "Ник или почта друга", icon = Icons.Rounded.PersonAdd, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        MangaButton("Добавить", {
                            addBusy = true
                            scope.launch {
                                api { addFriend(obj("handle" to handle.trim())) }
                                    .onSuccess { message = "Запрос отправлен"; handle = "" }
                                    .onFailure { message = it.message }
                                addBusy = false
                            }
                        }, enabled = handle.trim().length >= 2, loading = addBusy, kind = ButtonKind.INK)
                    }
                    AnimatedVisibility(message != null) {
                        Text(message ?: "", color = m.inkSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp).clickable { message = null })
                    }
                }
            }
            val incoming = requests.orEmpty()
            if (incoming.isNotEmpty()) {
                item { Text("ЗАПРОСЫ · ${incoming.size}", style = Kicker, color = m.red, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                items(incoming, key = { "r${it.id}" }) { r ->
                    MangaPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateItem(), color = m.yellow, shadow = 3.dp) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(r.user.avatar, r.user.username, size = 40.dp, color = r.user.nicknameColor)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.user.username, color = m.onYellow, fontWeight = FontWeight.Bold)
                                Text("хочет дружить · ${relativeTime(r.createdAt)}", color = m.onYellow.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            RoundIcon(Icons.Rounded.Close, "Отклонить", {
                                scope.launch { api { rejectFriend(r.id) }; requests = requests?.filterNot { it.id == r.id } }
                            }, size = 36.dp)
                            Spacer(Modifier.width(8.dp))
                            RoundIcon(Icons.Rounded.Check, "Принять", {
                                scope.launch { api { acceptFriend(r.id) }; requests = requests?.filterNot { it.id == r.id }; UserHub.refreshFriends() }
                            }, size = 36.dp, color = m.red, tint = Color.White)
                        }
                    }
                }
            }
            item { Text("ДРУЗЬЯ · ${friends.size}", style = Kicker, color = m.red, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            if (!loaded && friends.isEmpty()) {
                items(4) { SkeletonBox(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(66.dp)) }
            } else if (friends.isEmpty()) {
                item { EmptyState("Пока никого", "Добавь друга по нику — смотреть вместе веселее", mood = MascotMood.SEARCH) }
            } else {
                items(friends.sortedWith(compareByDescending<space.aniroom.app.data.Friend> { it.roomCode != null }.thenByDescending { it.online }), key = { it.id }) { f ->
                    MangaPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateItem(), shadow = 3.dp, onClick = { openUser(f.id) }) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(f.avatar, f.username, size = 44.dp, color = f.nicknameColor, online = f.online)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.username, color = nickColor(f.username, f.nicknameColor), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    when {
                                        f.roomCode != null -> "смотрит сейчас"
                                        f.online -> "в сети"
                                        else -> "не в сети"
                                    } + (f.watchSeconds?.takeIf { it > 0 }?.let { " · вместе ${formatHours(it)}" } ?: ""),
                                    color = if (f.roomCode != null) m.red else m.muted, fontSize = 12.sp,
                                )
                                f.friendship?.takeIf { it.name.isNotBlank() }?.let { fs ->
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(fs.name, color = m.inkSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(6.dp))
                                        ProgressLine(fs.progress.toFloat(), Modifier.weight(1f), height = 5.dp)
                                    }
                                }
                            }
                            f.roomCode?.let { code ->
                                Spacer(Modifier.width(8.dp))
                                MangaButton("К ним", { openRoom(code) }, compact = true, icon = Icons.Rounded.PlayArrow)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsTab(openRoom: (String) -> Unit, openProfile: () -> Unit, openFriends: () -> Unit, contentPadding: PaddingValues) {
    val m = Manga.colors
    val items by UserHub.items.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { UserHub.refresh() }

    fun open(item: AppNotification) {
        UserHub.markRead(item)
        when (item.describe().action) {
            "room" -> item.roomCode()?.let(openRoom)
            "profile", "list" -> openProfile()
            "friends" -> openFriends()
        }
    }

    PullToRefreshBox(refreshing, onRefresh = { scope.launch { refreshing = true; UserHub.refresh(); refreshing = false } }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TabTitle("входящие", "Уведомления") {
                    if (items.any { !it.isRead }) RoundIcon(Icons.Rounded.DoneAll, "Прочитать все", { UserHub.markAllRead() })
                }
            }
            if (items.isEmpty()) item { EmptyState("Тишина", "Здесь появятся приглашения, друзья и новые серии", mood = MascotMood.SLEEP) }
            items(items, key = { it.id }) { n ->
                val copy = n.describe()
                MangaPanel(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateItem(),
                    color = if (n.isRead) m.paper2 else m.paper3, shadow = if (n.isRead) 2.dp else 4.dp,
                    onClick = { open(n) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(if (n.isRead) m.paper3 else m.red), contentAlignment = Alignment.Center) {
                            Text(
                                when (n.kind) {
                                    "room_invite" -> "📨"; "friend_request", "friend_accepted" -> "🤝"; "friend_watching" -> "👀"
                                    "badge_earned", "achievement_earned" -> "🏆"; "new_episode" -> "📺"; else -> "📣"
                                },
                                fontSize = 18.sp,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(copy.title, color = m.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (!n.isRead) Box(Modifier.size(8.dp).clip(CircleShape).background(m.red))
                            }
                            Text(copy.body, color = m.inkSoft, fontSize = 14.sp)
                            Text(relativeTime(n.createdAt), color = m.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
