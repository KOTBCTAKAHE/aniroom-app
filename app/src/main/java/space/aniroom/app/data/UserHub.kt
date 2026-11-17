package space.aniroom.app.data

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import space.aniroom.app.AniRoomApp
import space.aniroom.app.MainActivity
import space.aniroom.app.R

data class NotificationCopy(val title: String, val body: String, val action: String)

fun AppNotification.describe(): NotificationCopy {
    fun p(key: String): String? = (payload[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
    return when (kind) {
        "room_invite" -> NotificationCopy(
            "Приглашение в комнату",
            "${p("from_username") ?: "Друг"} зовёт в «${p("room_title") ?: "комнату"}»" +
                (p("anime_title")?.let { " · $it" + (p("episode")?.let { e -> ", серия $e" } ?: "") } ?: ""),
            "room",
        )
        "friend_request" -> NotificationCopy("Запрос в друзья", "${p("username") ?: "Пользователь"} хочет добавить тебя в друзья", "friends")
        "friend_accepted" -> NotificationCopy("Новый друг", "${p("username") ?: "Пользователь"} принял запрос", "friends")
        "friend_watching" -> NotificationCopy(
            "Друг начал смотреть",
            "${p("username") ?: "Друг"} · ${p("anime_title") ?: p("room_title") ?: "совместный просмотр"}" + (p("episode")?.let { ", серия $it" } ?: ""),
            "room",
        )
        "badge_earned" -> NotificationCopy("Новый бейдж", p("title") ?: "Ты получил достижение", "profile")
        "achievement_earned" -> NotificationCopy("Достижение Re:Zero", p("title") ?: "Ты получил достижение", "profile")
        "new_episode" -> {
            val behind = (p("episode")?.toIntOrNull() ?: 0) - (p("watched")?.toIntOrNull() ?: 0)
            NotificationCopy("Вышла новая серия", "«${p("title") ?: "Тайтл"}» — серия ${p("episode") ?: ""}" + if (behind > 1) " · ты отстаёшь на $behind" else "", "list")
        }
        "announcement" -> NotificationCopy("Объявление AniRoom", p("text") ?: "", "none")
        else -> NotificationCopy("Событие AniRoom", p("text") ?: "У тебя новое уведомление", "none")
    }
}

fun AppNotification.roomCode(): String? = (payload["room_code"] as? JsonPrimitive)?.contentOrNull

/** App-wide realtime link for notifications + cached game profile. */
object UserHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var socket: SocketClient? = null
    private var poller: Job? = null
    private lateinit var appContext: Context

    private val _items = MutableStateFlow<List<AppNotification>>(emptyList())
    val items: StateFlow<List<AppNotification>> = _items.asStateFlow()

    private val _toasts = MutableSharedFlow<AppNotification>(extraBufferCapacity = 8)
    val toasts: SharedFlow<AppNotification> = _toasts.asSharedFlow()

    private val _game = MutableStateFlow<GameMe?>(null)
    val game: StateFlow<GameMe?> = _game.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val known = mutableSetOf<Long>()

    val isForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    fun start(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            Session.token.distinctUntilChanged().collect { token ->
                socket?.disconnect()
                socket = null
                poller?.cancel()
                known.clear()
                _items.value = emptyList()
                _game.value = null
                _friends.value = emptyList()
                if (token != null) connect(token)
            }
        }
    }

    private fun connect(token: String) {
        val client = SocketClient(token, AniRoomApp.http)
        client.onJson<AppNotification>("notification") { item ->
            if (!known.add(item.id)) return@onJson
            _items.update { listOf(item) + it.filter { row -> row.id != item.id } }
            if (isForeground) _toasts.tryEmit(item) else showSystem(item)
        }
        client.onJson<SystemNoticePayload>("admin_broadcast") { payload ->
            val item = AppNotification(-System.currentTimeMillis(), "announcement", kotlinx.serialization.json.buildJsonObject {
                put("text", JsonPrimitive(payload.text))
            })
            if (isForeground) _toasts.tryEmit(item) else showSystem(item)
        }
        client.on("session_revoked") { raw ->
            val reason = when {
                raw?.contains("deleted") == true -> "Аккаунт удалён"
                raw?.contains("password") == true -> "Пароль изменён — войди с новым"
                else -> "Сессия завершена, войди снова"
            }
            if (Session.token.value == token) Session.signOut(reason)
        }
        client.on("account_banned") { Session.signOut("Аккаунт заблокирован") }
        client.on("connect_error") { raw ->
            if (raw != null && listOf("Сессия завершена", "Аккаунт заблокирован", "Пользователь не найден").any { raw.contains(it) }) {
                Session.signOut(raw.trim('"'))
            }
        }
        client.connect()
        socket = client
        poller = scope.launch {
            refreshUser()
            while (true) {
                if (isForeground) refresh()
                delay(45_000)
            }
        }
    }

    private suspend fun refreshUser() {
        api { me() }.onSuccess { Session.setUser(it) }.onFailure {
            if ((it as? ApiException)?.code == 401) Session.signOut("Сессия истекла, войди снова")
        }
        refreshGame()
        refreshFriends()
    }

    suspend fun refreshGame() { api { gameMe() }.onSuccess { _game.value = it } }
    suspend fun refreshFriends() { api { friends() }.onSuccess { _friends.value = it } }

    suspend fun refresh() {
        api { notifications() }.onSuccess { list ->
            val fresh = list.filter { !it.isRead && it.id !in known }.maxByOrNull { it.id }
            val first = known.isEmpty()
            known.addAll(list.map { it.id })
            _items.value = list
            if (!first && fresh != null && isForeground) _toasts.tryEmit(fresh)
        }
    }

    fun markRead(item: AppNotification) {
        if (item.isRead || item.id < 0) return
        _items.update { list -> list.map { if (it.id == item.id) it.copy(isRead = true) else it } }
        scope.launch { api { markRead(item.id) } }
    }

    fun markAllRead() {
        _items.update { list -> list.map { it.copy(isRead = true) } }
        scope.launch { api { markAllRead() } }
    }

    private fun showSystem(item: AppNotification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val copy = item.describe()
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            item.roomCode()?.let { data = Uri.parse("aniroom://room/$it") }
        }
        val pending = PendingIntent.getActivity(
            appContext, item.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, AniRoomApp.CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE2342A.toInt())
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { NotificationManagerCompat.from(appContext).notify(item.id.toInt(), notification) }
    }
}
