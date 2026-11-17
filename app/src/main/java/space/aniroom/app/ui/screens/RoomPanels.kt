package space.aniroom.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.aniroom.app.data.Api
import space.aniroom.app.data.Message
import space.aniroom.app.data.Session
import space.aniroom.app.data.SystemNotice
import space.aniroom.app.data.UserHub
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.EmptyState
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.Poster
import space.aniroom.app.ui.components.ProgressLine
import space.aniroom.app.ui.components.RoundIcon
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.components.Tag
import space.aniroom.app.ui.components.UserAvatar
import space.aniroom.app.ui.components.nickColor
import space.aniroom.app.ui.components.NickText
import space.aniroom.app.ui.components.bubbleSkin
import space.aniroom.app.ui.components.bubbleExtras
import space.aniroom.app.ui.theme.BodyFont
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.chatTime
import space.aniroom.app.util.formatTime
import space.aniroom.app.util.parseInstant
import space.aniroom.app.util.rememberHaptic
import kotlin.math.ceil
import kotlin.math.roundToInt

private val REACTION_EMOJI = mapOf("heart" to "❤️", "like" to "👍", "dislike" to "👎")

private sealed interface FeedItem {
    val key: String
    val at: Long

    data class Msg(val message: Message, override val at: Long) : FeedItem {
        override val key get() = message.clientMessageId?.let { "c:$it" } ?: "m:${message.id}"
    }

    data class Notice(val notice: SystemNotice) : FeedItem {
        override val key get() = "n:${notice.id}"
        override val at get() = notice.createdAt
    }
}

// --------------------------------------------------------------------------------------- chat

@Composable
fun ChatPanel(vm: RoomViewModel, ui: RoomUi, openUser: (Int) -> Unit) {
    val m = Manga.colors
    val me by Session.user.collectAsState()
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    var stickersOpen by remember { mutableStateOf(false) }
    val list = rememberLazyListState()

    val feed = remember(ui.messages, ui.notices) {
        val items = ui.messages.map { FeedItem.Msg(it, parseInstant(it.createdAt)?.toEpochMilli() ?: System.currentTimeMillis()) } +
            ui.notices.map { FeedItem.Notice(it) }
        items.sortedBy { it.at }.asReversed()
    }
    LaunchedEffect(feed.size) {
        if (list.firstVisibleItemIndex <= 3) list.animateScrollToItem(0)
    }
    val showJump by remember { derivedStateOf { list.firstVisibleItemIndex > 4 } }

    fun send() {
        if (text.isBlank()) return
        vm.sendMessage(text, replyTo)
        haptic(false)
        text = ""
        replyTo = null
        scope.launch { list.animateScrollToItem(0) }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (feed.isEmpty()) {
                EmptyState("Тут пока тихо", "Напиши первое сообщение — его увидят все в комнате", mood = MascotMood.IDLE)
            }
            LazyColumn(
                state = list,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (ui.typing.isNotEmpty()) item(key = "typing") { TypingRow(ui.typing.joinToString(", ") { it.username }, Modifier.animateItem()) }
                items(feed.size, key = { feed[it].key }) { index ->
                    when (val item = feed[index]) {
                        is FeedItem.Notice -> NoticeRow(item.notice.text, Modifier.animateItem())
                        is FeedItem.Msg -> {
                            val older = feed.getOrNull(index + 1)
                            val grouped = older is FeedItem.Msg && older.message.userId == item.message.userId && item.at - older.at < 180_000
                            MessageRow(
                                message = item.message,
                                mine = item.message.userId == me?.id,
                                showHeader = !grouped,
                                modifier = Modifier.animateItem(),
                                onReply = { replyTo = item.message; haptic(true) },
                                onReact = { r -> vm.toggleReaction(item.message, r) },
                                onRetry = { vm.retryMessage(item.message) },
                                onDiscard = { vm.discardMessage(item.message) },
                                onCopy = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("message", item.message.text))
                                    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                                },
                                onUser = { openUser(item.message.userId) },
                            )
                        }
                    }
                }
            }
            JumpButton(showJump, Modifier.align(Alignment.BottomEnd).padding(12.dp)) { scope.launch { list.animateScrollToItem(0) } }
        }

        AnimatedVisibility(replyTo != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val r = replyTo
            Row(
                Modifier.fillMaxWidth().background(m.paper3).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(32.dp).background(m.red))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ответ ${r?.username ?: ""}", color = m.red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(r?.text?.ifBlank { "стикер" } ?: "", color = m.inkSoft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.Close, "Отменить ответ", tint = m.ink, modifier = Modifier.size(22.dp).clip(CircleShape).clickable { replyTo = null })
            }
        }

        AnimatedVisibility(stickersOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            StickerTray(vm, ui) { sticker ->
                vm.sendSticker(sticker)
                haptic(false)
                stickersOpen = false
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            RoundIcon(
                Icons.Rounded.EmojiEmotions, "Стикеры",
                { stickersOpen = !stickersOpen; if (stickersOpen) vm.loadStickers() },
                color = if (stickersOpen) m.yellow else m.paper2, tint = if (stickersOpen) m.onYellow else m.ink, size = 44.dp,
            )
            Spacer(Modifier.width(8.dp))
            MangaPanel(Modifier.weight(1f), radius = 22.dp, shadow = 2.dp) {
                Box(Modifier.heightIn(min = 44.dp).padding(horizontal = 14.dp, vertical = 11.dp), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text("Сообщение…", color = m.muted, fontSize = 15.sp)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(1000); vm.onTyping(it) },
                        textStyle = TextStyle(color = m.ink, fontSize = 15.sp, fontFamily = BodyFont),
                        cursorBrush = SolidColor(m.red),
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            val canSend = text.isNotBlank()
            val sendScale by animateFloatAsState(if (canSend) 1f else 0.85f, spring(dampingRatio = 0.45f), label = "send")
            Box(
                Modifier
                    .size(44.dp)
                    .scale(sendScale)
                    .clip(CircleShape)
                    .background(if (canSend) m.red else m.paper3)
                    .border(2.dp, m.ink, CircleShape)
                    .clickable(enabled = canSend) { send() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, "Отправить", tint = if (canSend) Color.White else m.muted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun JumpButton(visible: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val m = Manga.colors
    AnimatedVisibility(visible, modifier = modifier, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        RoundIcon(Icons.Rounded.KeyboardArrowDown, "Вниз", onClick, color = m.yellow, tint = m.onYellow)
    }
}

@Composable
private fun TypingRow(names: String, modifier: Modifier) {
    val m = Manga.colors
    val t = rememberInfiniteTransition(label = "typing")
    Row(modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val y by t.animateFloat(0f, -5f, infiniteRepeatable(tween(400, delayMillis = i * 120), RepeatMode.Reverse), label = "d$i")
            Box(Modifier.offset(y = y.dp).padding(horizontal = 2.dp).size(6.dp).clip(CircleShape).background(m.muted))
        }
        Spacer(Modifier.width(8.dp))
        Text("$names печатает…", color = m.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NoticeRow(text: String, modifier: Modifier) {
    val m = Manga.colors
    Box(modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(
            text, color = m.inkSoft, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(m.paper3).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MessageRow(
    message: Message,
    mine: Boolean,
    showHeader: Boolean,
    modifier: Modifier,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onCopy: () -> Unit,
    onUser: () -> Unit,
) {
    val m = Manga.colors
    val density = LocalDensity.current
    val swipe = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val threshold = with(density) { 64.dp.toPx() }
    val nameColor = nickColor(message.username, message.nicknameColor)

    Box(
        modifier
            .fillMaxWidth()
            .padding(top = if (showHeader) 10.dp else 3.dp)
            .pointerInput(message.id) {
                var fired = false
                detectHorizontalDragGestures(
                    onDragStart = { fired = false },
                    onDragEnd = {
                        scope.launch { swipe.animateTo(0f, spring(dampingRatio = 0.55f)) }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(0f) } },
                    onHorizontalDrag = { change, drag ->
                        change.consume()
                        val next = (swipe.value + drag).coerceIn(0f, threshold * 1.6f)
                        scope.launch { swipe.snapTo(next) }
                        if (!fired && next >= threshold) {
                            fired = true
                            onReply()
                        }
                    },
                )
            },
    ) {
        // reply hint behind the bubble
        Icon(
            Icons.AutoMirrored.Rounded.Reply, null, tint = m.red,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp).size(22.dp)
                .graphicsLayer { alpha = (swipe.value / threshold).coerceIn(0f, 1f); scaleX = alpha; scaleY = alpha },
        )
        Row(
            Modifier.fillMaxWidth().graphicsLayer { translationX = swipe.value },
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!mine) {
                if (showHeader) Box(Modifier.clickable(onClick = onUser)) {
                    UserAvatar(message.avatar, message.username, size = 34.dp, color = message.nicknameColor, frame = message.game?.frame)
                } else Spacer(Modifier.width(34.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.widthIn(max = 290.dp), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                if (showHeader && !mine) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)) {
                    NickText(message.username, nameColor, message.game?.nickFx, fontSize = 13.sp)
                    if (message.specialTitle == "owner") {
                        Spacer(Modifier.width(4.dp))
                        Tag("создатель", color = m.ink, textColor = m.paper)
                    }
                    message.game?.title?.let {
                        Spacer(Modifier.width(4.dp))
                        Text(it, color = m.muted, fontSize = 11.sp, maxLines = 1)
                    }
                }
                Box {
                    val skin = bubbleSkin(
                        message.game?.bubble, mine, m,
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (mine) 18.dp else 6.dp, bottomEnd = if (mine) 6.dp else 18.dp,
                        ),
                    )
                    val shape = skin.shape
                    val bubbleColor = if (message.stickerUrl != null) Color.Transparent else skin.background
                    Column(
                        Modifier
                            .rotate(if (message.stickerUrl == null) skin.tilt else 0f)
                            .clip(shape)
                            .background(bubbleColor)
                            .then(if (message.stickerUrl == null && !skin.dashed) Modifier.border(if (message.game?.bubble == "scroll") 3.dp else 2.dp, m.ink, shape) else Modifier)
                            .then(if (message.stickerUrl == null) Modifier.bubbleExtras(skin, skin.text) else Modifier)
                            .combinedClickable(onClick = {}, onLongClick = { menu = true }, onDoubleClick = { onReact("heart") })
                            .padding(horizontal = if (message.stickerUrl != null) 0.dp else 12.dp, vertical = if (message.stickerUrl != null) 0.dp else 8.dp),
                    ) {
                        message.replyTo?.let { reply ->
                            Row(
                                Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (mine) Color.Black.copy(alpha = 0.15f) else m.paper3).padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Box(Modifier.width(2.dp).height(28.dp).background(if (mine) Color.White else m.red))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(reply.username, color = if (mine) Color.White else nickColor(reply.username, reply.nicknameColor), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(reply.text.ifBlank { "стикер" }, color = if (mine) Color.White.copy(alpha = 0.85f) else m.inkSoft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        if (message.stickerUrl != null) {
                            AsyncImage(
                                model = Api.media(message.stickerUrl),
                                contentDescription = "Стикер",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(128.dp),
                            )
                        } else {
                            Text(message.text, color = skin.text, fontSize = chatFontSize(), lineHeight = (chatFontSize().value + 5).sp)
                        }
                        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                chatTime(message.createdAt), fontSize = 10.sp,
                                color = if (message.stickerUrl == null) skin.text.copy(alpha = 0.65f) else m.muted,
                            )
                            if (message.deliveryState == "sending") {
                                Spacer(Modifier.width(3.dp))
                                Icon(Icons.Rounded.Schedule, "Отправляется", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            REACTION_EMOJI.forEach { (key, emoji) ->
                                Box(
                                    Modifier.size(42.dp).clip(CircleShape).clickable { onReact(key); menu = false },
                                    contentAlignment = Alignment.Center,
                                ) { Text(emoji, fontSize = 22.sp) }
                            }
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Ответить") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Reply, null) }, onClick = { menu = false; onReply() })
                        if (message.text.isNotBlank()) DropdownMenuItem(text = { Text("Копировать") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { menu = false; onCopy() })
                    }
                }
                if (message.deliveryState == "failed") {
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = m.red, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Не отправлено", color = m.red, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Повторить", color = m.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onRetry))
                        Spacer(Modifier.width(8.dp))
                        Text("Удалить", color = m.muted, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onDiscard))
                    }
                }
                val active = message.reactions.filter { it.count > 0 }
                if (active.isNotEmpty()) {
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        active.forEach { r ->
                            val s by animateFloatAsState(if (r.reactedByMe) 1.05f else 1f, spring(dampingRatio = 0.4f), label = "r")
                            Row(
                                Modifier
                                    .scale(s)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (r.reactedByMe) m.yellow else m.paper2)
                                    .border(1.5.dp, m.ink, RoundedCornerShape(10.dp))
                                    .clickable { onReact(r.reaction) }
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(REACTION_EMOJI[r.reaction] ?: "•", fontSize = 12.sp)
                                Spacer(Modifier.width(3.dp))
                                Text("${r.count}", color = if (r.reactedByMe) m.onYellow else m.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerTray(vm: RoomViewModel, ui: RoomUi, onPick: (space.aniroom.app.data.Sticker) -> Unit) {
    val m = Manga.colors
    val stickers = ui.stickers
    Box(Modifier.fillMaxWidth().height(96.dp).background(m.paper3)) {
        when {
            stickers == null -> LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(6) { SkeletonBox(Modifier.size(72.dp)) }
            }
            stickers.isEmpty() -> Text(
                "Стикеров пока нет — их можно загрузить на сайте",
                color = m.muted, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center).padding(16.dp), textAlign = TextAlign.Center,
            )
            else -> LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(stickers, key = { it.id }) { s ->
                    AsyncImage(
                        model = Api.media(s.image), contentDescription = s.name, contentScale = ContentScale.Fit,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(m.paper2).clickable { onPick(s) }.padding(4.dp),
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------- episodes

@Composable
fun EpisodesPanel(vm: RoomViewModel, ui: RoomUi) {
    val m = Manga.colors
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val anime = ui.room?.anime
    val owner = vm.isOwner
    var picker by remember { mutableStateOf(false) }
    val total = anime?.episodesTotal ?: 0
    val current = anime?.episode ?: 0
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    LaunchedEffect(current, total) { if (current > 6) grid.animateScrollToItem((current).coerceAtMost(total)) }

    LazyVerticalGrid(
        GridCells.Adaptive(60.dp),
        state = grid,
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (anime?.shikimoriId == null) {
                EmptyState(
                    "Тайтл не выбран",
                    if (owner) "Найди аниме в каталоге — все зрители подключатся автоматически" else "Хост скоро что-нибудь включит",
                    mood = MascotMood.SLEEP,
                ) { if (owner) MangaButton("Выбрать аниме", { picker = true }, kind = ButtonKind.YELLOW) }
            } else {
                MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Poster(anime.poster, Modifier.width(58.dp).aspectRatio(0.7f), radius = 8.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(anime.title ?: "Аниме", fontWeight = FontWeight.Bold, color = m.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(listOfNotNull(anime.translationName, anime.quality?.let { "${it}p" }).joinToString(" · "), color = m.muted, fontSize = 12.sp)
                                ui.ratings?.title?.avg?.let { avg ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Star, null, tint = m.yellow, modifier = Modifier.size(14.dp))
                                        Text(" ${"%.1f".format(avg)} · ${ui.ratings?.title?.count ?: 0} оц.", color = m.inkSoft, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (owner) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MangaButton("Сменить", { picker = true }, kind = ButtonKind.PAPER, compact = true, icon = Icons.Rounded.SwapHoriz, modifier = Modifier.weight(1f))
                                if (total > current) MangaButton("Дальше", { haptic(false); vm.nextEpisode() }, compact = true, icon = Icons.Rounded.SkipNext, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Обновить поток", color = m.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { vm.retryStream() }.padding(4.dp),
                        )
                    }
                }
            }
        }
        if (anime?.shikimoriId != null && total > 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("СЕРИИ · ${ui.watched.size}/$total ПРОСМОТРЕНО", style = Kicker, color = m.red, modifier = Modifier.padding(top = 6.dp))
            }
            items(total, key = { it + 1 }) { index ->
                val ep = index + 1
                val isCurrent = ep == current
                val watched = ep in ui.watched
                val mine = ui.ratings?.episodes?.firstOrNull { it.episode == ep }?.mine
                val pulse = rememberInfiniteTransition(label = "cur")
                val glow by pulse.animateFloat(1f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "g")
                MangaPanel(
                    Modifier.aspectRatio(1f).animateItem().then(if (isCurrent) Modifier.scale(glow) else Modifier),
                    color = when {
                        isCurrent -> m.red
                        watched -> m.paper3
                        else -> m.paper2
                    },
                    radius = 12.dp,
                    shadow = if (isCurrent) 4.dp else 2.5.dp,
                    onClick = {
                        if (owner) {
                            if (!isCurrent) vm.selectEpisode(ep)
                        } else {
                            Toast.makeText(context, "Серии переключает хост", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(
                        "$ep", fontFamily = DisplayFont, fontSize = 16.sp,
                        color = if (isCurrent) Color.White else if (watched) m.muted else m.ink,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    if (isCurrent) Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(14.dp))
                    else if (watched) Icon(Icons.Rounded.Check, null, tint = m.green, modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(14.dp))
                    if (mine != null && mine > 0) Text(
                        "★$mine", color = if (isCurrent) Color.White else m.inkSoft, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
    if (picker) AnimePickerDialog(
        initialQuery = "",
        onDismiss = { picker = false },
        onConfirm = { vm.setAnime(it); picker = false },
    )
}

// --------------------------------------------------------------------------------------- people

@Composable
fun PeoplePanel(vm: RoomViewModel, ui: RoomUi, openUser: (Int) -> Unit) {
    val m = Manga.colors
    val me by Session.user.collectAsState()
    val friends by UserHub.friends.collectAsState()
    val owner = vm.isOwner
    val room = ui.room ?: return
    LaunchedEffect(Unit) { UserHub.refreshFriends() }
    val inRoom = ui.participants.map { it.id }.toSet()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("В КОМНАТЕ · ${ui.participants.count { it.online }}", style = Kicker, color = m.red) }
        items(ui.participants.sortedWith(compareByDescending<space.aniroom.app.data.Participant> { it.isOwner }.thenByDescending { it.online }), key = { it.id }) { p ->
            var menu by remember { mutableStateOf(false) }
            MangaPanel(Modifier.fillMaxWidth().animateItem(), shadow = 2.5.dp, onClick = { openUser(p.id) }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(p.avatar, p.username, size = 40.dp, color = p.nicknameColor, online = p.online, frame = p.game?.frame)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NickText(p.username, nickColor(p.username, p.nicknameColor), p.game?.nickFx, Modifier.weight(1f, fill = false), fontSize = 15.sp)
                            if (p.isOwner) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.WorkspacePremium, "Хост", tint = m.red, modifier = Modifier.size(16.dp))
                            }
                            if (p.id == me?.id) {
                                Spacer(Modifier.width(4.dp))
                                Tag("ты", color = m.paper3, textColor = m.ink)
                            }
                        }
                        val status = when {
                            !p.online -> "не в сети"
                            p.buffering -> "загружает видео…"
                            p.pingMs != null -> "пинг ${p.pingMs.roundToInt()} мс" + (p.driftMs?.let { d -> if (kotlin.math.abs(d) > 400) " · отстаёт ${(kotlin.math.abs(d) / 1000).let { "%.1f".format(it) }} с" else "" } ?: "")
                            else -> "смотрит"
                        }
                        Text(status, color = if (p.buffering) m.red else m.muted, fontSize = 12.sp)
                    }
                    if (owner && p.id != me?.id) Box {
                        Icon(Icons.Rounded.MoreVert, "Действия", tint = m.ink, modifier = Modifier.size(32.dp).clip(CircleShape).clickable { menu = true }.padding(4.dp))
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Сделать хостом") }, leadingIcon = { Icon(Icons.Rounded.WorkspacePremium, null) }, onClick = { menu = false; vm.transferOwner(p.id) })
                            DropdownMenuItem(text = { Text("Исключить", color = m.red) }, leadingIcon = { Icon(Icons.Rounded.PersonRemove, null, tint = m.red) }, onClick = { menu = false; vm.kick(p.id) })
                        }
                    }
                }
            }
        }
        if (owner) {
            item { Text("НАСТРОЙКИ КОМНАТЫ", style = Kicker, color = m.red, modifier = Modifier.padding(top = 12.dp)) }
            item {
                MangaPanel(Modifier.fillMaxWidth(), shadow = 2.5.dp) {
                    Column {
                        ToggleRow(if (room.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, "Закрыть вход", "Новые зрители не смогут войти", room.isLocked) { vm.toggleLock() }
                        HorizontalDivider(color = m.ink.copy(alpha = 0.1f))
                        ToggleRow(Icons.Rounded.Public, "Публичная", "Показывать в «Сейчас смотрят»", room.isPublic) { vm.togglePublic() }
                    }
                }
            }
        }
        val invitable = friends.filter { it.id !in inRoom }
        if (invitable.isNotEmpty()) {
            item { Text("ПОЗВАТЬ ДРУЗЕЙ", style = Kicker, color = m.red, modifier = Modifier.padding(top = 12.dp)) }
            items(invitable, key = { "f${it.id}" }) { f ->
                var sent by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().animateItem().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(f.avatar, f.username, size = 36.dp, color = f.nicknameColor, online = f.online)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.username, color = m.ink, fontWeight = FontWeight.Bold)
                        Text(if (f.roomCode != null) "смотрит в другой комнате" else if (f.online) "в сети" else "не в сети", color = m.muted, fontSize = 12.sp)
                    }
                    MangaButton(if (sent) "Позвали" else "Позвать", { sent = true; vm.invite(f.id, f.username) }, kind = if (sent) ButtonKind.PAPER else ButtonKind.YELLOW, compact = true, enabled = !sent)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    val m = Manga.colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = m.ink)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = m.ink, fontWeight = FontWeight.Bold)
            Text(subtitle, color = m.muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = m.red, checkedThumbColor = Color.White, uncheckedTrackColor = m.paper3, uncheckedBorderColor = m.ink, uncheckedThumbColor = m.ink),
        )
    }
}

// --------------------------------------------------------------------------------------- end card

@Composable
fun EndCardPanel(vm: RoomViewModel, ui: RoomUi, progress: PlayerProgress) {
    val visible = endCardVisible(ui)
    AnimatedVisibility(visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        EndCardContent(vm, ui, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

@Composable
fun EndCardOverlay(vm: RoomViewModel, ui: RoomUi, progress: PlayerProgress, modifier: Modifier) {
    AnimatedVisibility(endCardVisible(ui), modifier = modifier, enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
        EndCardContent(vm, ui, Modifier.width(380.dp))
    }
}

private fun endCardVisible(ui: RoomUi): Boolean =
    ui.vote?.active == true || ui.voteResult != null || (ui.endCard != null && !ui.endCard.dismissed && ui.room?.anime?.episode == ui.endCard.episode)

@Composable
private fun EndCardContent(vm: RoomViewModel, ui: RoomUi, modifier: Modifier) {
    val m = Manga.colors
    val me by Session.user.collectAsState()
    val haptic = rememberHaptic()
    val vote = ui.vote?.takeIf { it.active }
    val result = ui.voteResult
    val ratingEpisode = ui.endCard?.episode ?: vote?.episode ?: result?.episode ?: (ui.room?.anime?.episode ?: 0)
    val mine = ui.ratings?.episodes?.firstOrNull { it.episode == ratingEpisode }?.mine
    LaunchedEffect(mine, vote, result) {
        if (mine != null && mine > 0 && vote == null && result == null) {
            delay(4500)
            vm.dismissEndCard()
        }
    }
    MangaPanel(modifier, color = m.paper2, radius = 18.dp, shadow = 4.dp) {
        Column(Modifier.padding(14.dp)) {
            if (vote != null) {
                val remaining by produceState(0.0, vote.endsAt, ui.clockOffsetMs) {
                    while (true) {
                        val now = (System.currentTimeMillis() + ui.clockOffsetMs) / 1000.0
                        value = ((vote.endsAt ?: now) - now).coerceAtLeast(0.0)
                        delay(200)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("СЕРИЯ ${vote.episode ?: ""} ПОЗАДИ", style = Kicker, color = m.red)
                        Text("Ещё серию?", fontFamily = DisplayFont, fontSize = 20.sp, color = m.ink)
                    }
                    Box(Modifier.size(44.dp).clip(CircleShape).background(m.ink), contentAlignment = Alignment.Center) {
                        Text("${ceil(remaining).toInt()}", color = m.paper, fontFamily = DisplayFont, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val uid = me?.id
                val myChoice = when {
                    uid == null -> null
                    vote.yes.contains(uid) -> "yes"
                    vote.no.contains(uid) -> "no"
                    else -> null
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MangaButton(
                        "Дальше · ${vote.yes.size}", { haptic(false); vm.castVote("yes") },
                        kind = if (myChoice == "yes") ButtonKind.RED else ButtonKind.PAPER, icon = Icons.Rounded.PlayArrow, modifier = Modifier.weight(1f), compact = true,
                    )
                    MangaButton(
                        "Хватит · ${vote.no.size}", { haptic(false); vm.castVote("no") },
                        kind = if (myChoice == "no") ButtonKind.INK else ButtonKind.PAPER, modifier = Modifier.weight(1f), compact = true,
                    )
                }
                Spacer(Modifier.height(10.dp))
                val duration = vote.duration ?: 0.0
                ProgressLine(if (duration > 0) (remaining / duration).toFloat() else 0f, Modifier.fillMaxWidth(), height = 6.dp)
                Text(
                    "${vote.yes.size + vote.no.size} из ${vote.voters.takeIf { it > 0 } ?: ui.participants.count { it.online }} проголосовали",
                    color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
                if (vm.isOwner) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ты хост:", color = m.inkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text("Включить сейчас", color = m.red, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { vm.decideVote("next") }.padding(4.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Закончить", color = m.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { vm.decideVote("stop") }.padding(4.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = m.ink.copy(alpha = 0.1f))
            } else if (result != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (result.result == "next") "ПОЕХАЛИ!" else "ОТБОЙ!",
                        fontFamily = DisplayFont, fontSize = 22.sp, color = if (result.result == "next") m.red else m.ink,
                        modifier = Modifier.rotate(-4f),
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Close, "Скрыть", tint = m.ink, modifier = Modifier.size(28.dp).clip(CircleShape).clickable { vm.dismissEndCard() }.padding(3.dp))
                }
                Text(
                    if (result.result == "next") "Включаем серию ${result.nextEpisode}" else "Хватит на сегодня",
                    color = m.ink, fontWeight = FontWeight.Bold,
                )
                result.bestMoment?.let { b ->
                    Text("Громче всего было на ${formatTime(b.at)} — ${b.emoji} ×${b.count}", color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = m.ink.copy(alpha = 0.1f))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("СЕРИЯ $ratingEpisode", style = Kicker, color = m.red, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Close, "Скрыть", tint = m.ink, modifier = Modifier.size(28.dp).clip(CircleShape).clickable { vm.dismissEndCard() }.padding(3.dp))
                }
            }
            if (ratingEpisode > 0 && ui.room?.anime?.shikimoriId != null) {
                val summary = ui.ratings?.episodes?.firstOrNull { it.episode == ratingEpisode }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (mine != null && mine > 0) STAR_LABELS[mine] else "Оцени серию", color = m.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        summary?.avg?.let { Text("Средняя ${"%.1f".format(it)} · ${summary.count} оц.", color = m.muted, fontSize = 11.sp) }
                    }
                    StarRow(mine ?: 0) { score -> haptic(false); vm.rateEpisode(ratingEpisode, score) }
                }
            }
        }
    }
}

private val STAR_LABELS = listOf("", "Мимо", "Так себе", "Нормально", "Хорошо", "Шедевр!")

@Composable
private fun StarRow(value: Int, size: Dp = 30.dp, onRate: (Int) -> Unit) {
    val m = Manga.colors
    Row {
        (1..5).forEach { i ->
            val filled = i <= value
            val s by animateFloatAsState(if (filled) 1.12f else 1f, spring(dampingRatio = 0.35f, stiffness = 600f), label = "star$i")
            Icon(
                if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                "$i из 5",
                tint = if (filled) m.yellow else m.muted,
                modifier = Modifier.size(size).scale(s).clip(CircleShape).clickable { onRate(i) },
            )
        }
    }
}

@Composable
private fun chatFontSize() = when (Session.appPrefs.collectAsState().value.chatFont) {
    "sm" -> 13.sp
    "lg" -> 17.sp
    else -> 15.sp
}
