package space.aniroom.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import space.aniroom.app.AniRoomApp
import space.aniroom.app.data.AnimeChoice
import space.aniroom.app.data.AnimeLoadingPayload
import space.aniroom.app.data.AppJson
import space.aniroom.app.data.ChatAck
import space.aniroom.app.data.EpisodeVoteResult
import space.aniroom.app.data.EpisodeVoteState
import space.aniroom.app.data.LevelUpEvent
import space.aniroom.app.data.LinkStatus
import space.aniroom.app.data.LiveReaction
import space.aniroom.app.data.Message
import space.aniroom.app.data.MessageDeleted
import space.aniroom.app.data.MessageReaction
import space.aniroom.app.data.Participant
import space.aniroom.app.data.ProfileUpdate
import space.aniroom.app.data.QuestEvent
import space.aniroom.app.data.ReactionsPayload
import space.aniroom.app.data.RatingsUpdate
import space.aniroom.app.data.Room
import space.aniroom.app.data.RoomRatings
import space.aniroom.app.data.Session
import space.aniroom.app.data.SimpleAck
import space.aniroom.app.data.SocketClient
import space.aniroom.app.data.Sticker
import space.aniroom.app.data.StreakEvent
import space.aniroom.app.data.SyncState
import space.aniroom.app.data.SystemNotice
import space.aniroom.app.data.SystemNoticePayload
import space.aniroom.app.data.Telemetry
import space.aniroom.app.data.TypingUser
import space.aniroom.app.data.UserHub
import space.aniroom.app.data.XpEvent
import space.aniroom.app.data.AchievementPayload
import space.aniroom.app.data.api
import space.aniroom.app.data.obj
import kotlin.math.abs
import kotlin.math.max

val LIVE_REACTIONS = listOf(
    Triple("laugh", "😂", "ХА-ХА-ХА!"),
    Triple("fire", "🔥", "ОГО-О-ОНЬ!"),
    Triple("cry", "😭", "НЕ-Е-ЕТ!"),
    Triple("shock", "😱", "ЧТО?!"),
    Triple("love", "❤️", "КЬЯ-А-А!"),
    Triple("skull", "💀", "ВСЁ. Я ВСЁ."),
)

fun reactionEmoji(kind: String) = LIVE_REACTIONS.firstOrNull { it.first == kind }?.second ?: "🔥"

data class FlyingReaction(val id: Long, val emoji: String, val mine: Boolean, val lane: Float)
data class Burst(val key: Long, val emoji: String, val text: String, val count: Int)
data class EndCard(val episode: Int, val isLast: Boolean, val dismissed: Boolean = false)

sealed interface PlayerToast {
    val id: Long
    data class Skip(override val id: Long, val by: String, val seconds: Double?) : PlayerToast
    data class Episode(override val id: Long, val from: Int?, val to: Int?, val loading: Boolean) : PlayerToast
    data class Info(override val id: Long, val text: String, val emoji: String = "✦") : PlayerToast
}

data class AnimeLoading(val reason: String, val episode: Int?, val title: String?)

data class RoomUi(
    val loading: Boolean = true,
    val loadError: String? = null,
    val room: Room? = null,
    val participants: List<Participant> = emptyList(),
    val messages: List<Message> = emptyList(),
    val typing: List<TypingUser> = emptyList(),
    val notices: List<SystemNotice> = emptyList(),
    val sync: SyncState? = null,
    val link: LinkStatus = LinkStatus.CONNECTING,
    val animeLoading: AnimeLoading? = null,
    val pingMs: Int = 0,
    val driftMs: Int = 0,
    val vote: EpisodeVoteState? = null,
    val voteResult: EpisodeVoteResult? = null,
    val clockOffsetMs: Long = 0,
    val ratings: RoomRatings? = null,
    val endCard: EndCard? = null,
    val stickers: List<Sticker>? = null,
    val watched: Set<Int> = emptySet(),
    val resumeAt: Double? = null,
    val ownerPaused: Boolean = false,
    val syncCooldown: Int = 0,
)

data class PlayerProgress(
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val bufferedSec: Double = 0.0,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val speed: Float = 1f,
    val videoHeight: Int = 0,
)

class RoomViewModel(app: Application, val roomCode: String) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(RoomUi())
    val ui: StateFlow<RoomUi> = _ui.asStateFlow()

    private val _progress = MutableStateFlow(PlayerProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private val _flying = MutableStateFlow<List<FlyingReaction>>(emptyList())
    val flying: StateFlow<List<FlyingReaction>> = _flying.asStateFlow()

    private val _burst = MutableStateFlow<Burst?>(null)
    val burst: StateFlow<Burst?> = _burst.asStateFlow()

    private val _toast = MutableStateFlow<PlayerToast?>(null)
    val toast: StateFlow<PlayerToast?> = _toast.asStateFlow()

    /** Fresh chat messages from others, for overlays in fullscreen. */
    private val _incoming = MutableSharedFlow<Message>(extraBufferCapacity = 16)
    val incoming: SharedFlow<Message> = _incoming.asSharedFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** One-off events for the screen: "kicked", "deleted". */
    val events: SharedFlow<String> = _events.asSharedFlow()

    val me get() = Session.user.value
    val isOwner: Boolean get() = _ui.value.room?.ownerId != null && _ui.value.room?.ownerId == me?.id

    val player: ExoPlayer = buildPlayer()

    private var socket: SocketClient? = null
    private var currentSource: String? = null
    private var sourceReady = false
    private var lastAlignedKey: String? = null
    private var correcting = false
    private var manualRate = false
    private var lastSeekNoticeAt = 0L
    private var streamRetries = 0
    private var endCardKey = ""
    private var voteRequestedKey = ""
    private var voteRetryAt = 0L
    private var watchedKey = ""
    private var autoSkippedKey = ""
    private var toastJob: Job? = null
    private var typingJob: Job? = null
    private var typingSent = false
    private var reactionSeq = 0L
    private val recentReactions = ArrayDeque<Pair<Long, String>>()
    private var lastBurstAt = 0L

    init {
        load()
        startLoops()
    }

    private fun buildPlayer(): ExoPlayer {
        val context = getApplication<Application>()
        val dataSource = OkHttpDataSource.Factory(AniRoomApp.http)
            .setUserAgent("AniRoom-Android")
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 60_000, 1_200, 2_500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setLoadControl(loadControl)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            if (!sourceReady) streamRetries = 0
                            sourceReady = true
                            applySync()
                        }
                        if (state == Player.STATE_ENDED) onEpisodeEnded()
                        publishProgress()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) = publishProgress()

                    override fun onPlayerError(error: PlaybackException) {
                        val anime = _ui.value.room?.anime
                        if (anime?.shikimoriId != null && streamRetries < 3) {
                            streamRetries++
                            showToast(PlayerToast.Info(System.nanoTime(), "Источник подвис — обновляю поток", "↻"))
                            retryStream()
                        } else {
                            pushNotice("Видео не загрузилось: ${error.errorCodeName}")
                        }
                    }
                })
                applyQualityCap(this)
            }
    }

    fun applyQualityCap(target: ExoPlayer = player) {
        val prefs = Session.appPrefs.value
        val manual = prefs.qualityMode.toIntOrNull()
        val saver = if (prefs.dataSaver) 480 else Int.MAX_VALUE
        val cap = minOf(manual ?: Int.MAX_VALUE, saver)
        target.trackSelectionParameters = target.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, cap)
            .build()
    }

    // ------------------------------------------------------------------ loading

    fun load() {
        _ui.update { it.copy(loading = it.room == null, loadError = null) }
        viewModelScope.launch {
            api { joinRoom(obj("room_code" to roomCode)) }
            val room = api { getRoom(roomCode) }
            room.onFailure { e ->
                _ui.update { it.copy(loading = false, loadError = e.message) }
                return@launch
            }
            val loaded = room.getOrThrow()
            val messages = api { messages(roomCode) }.getOrDefault(emptyList())
            val sync = SyncState(
                roomCode = loaded.roomCode,
                videoUrl = loaded.videoUrl,
                playing = loaded.playback.playing,
                currentTime = loaded.playback.currentTime,
                playbackRate = loaded.playback.playbackRate,
                serverTime = loaded.playback.serverTime,
                lastStateUpdateAt = loaded.playback.lastStateUpdateAt,
                anime = loaded.anime,
                reason = "load",
            )
            _ui.update {
                it.copy(
                    loading = false,
                    room = loaded,
                    participants = loaded.participants,
                    messages = messages.map(::normalize) + it.messages.filter { m -> m.deliveryState != null },
                    sync = sync,
                    watched = loaded.anime?.shikimoriId?.let(Session::watchedEpisodes) ?: emptySet(),
                )
            }
            ensureSource(sync.videoUrl)
            connect()
            loadExtras()
        }
    }

    private fun loadExtras() {
        val room = _ui.value.room ?: return
        if (room.anime?.shikimoriId != null) viewModelScope.launch {
            api { roomRatings(roomCode) }.onSuccess { r -> _ui.update { it.copy(ratings = r) } }
        }
        viewModelScope.launch {
            api { roomProgress(roomCode) }.onSuccess { result ->
                val saved = result.progress
                val anime = _ui.value.room?.anime
                val watched = result.episodes.filter { it.watched }.map { it.episode }
                _ui.update { it.copy(watched = it.watched + watched) }
                if (saved != null && isOwner && anime != null && saved.episode == anime.episode &&
                    saved.shikimoriId == anime.shikimoriId && saved.positionSec >= 30 &&
                    (saved.durationSec <= 0 || saved.positionSec < saved.durationSec - 60) &&
                    _ui.value.participants.count { p -> p.id != me?.id && p.online } == 0 &&
                    abs(currentTargetTime() - saved.positionSec) > 20
                ) {
                    _ui.update { it.copy(resumeAt = saved.positionSec) }
                }
            }
        }
    }

    fun loadStickers() {
        if (_ui.value.stickers != null) return
        viewModelScope.launch {
            api { stickers() }.onSuccess { list -> _ui.update { it.copy(stickers = list) } }
                .onFailure { _ui.update { it.copy(stickers = emptyList()) } }
        }
    }

    // ------------------------------------------------------------------ realtime

    private fun connect() {
        val token = Session.token.value ?: return
        socket?.disconnect()
        val s = SocketClient(token, AniRoomApp.http)
        socket = s
        s.on("connect") {
            val wasDown = _ui.value.link == LinkStatus.RECONNECTING
            _ui.update { it.copy(link = LinkStatus.CONNECTED) }
            s.emit("join_room", obj("room_code" to roomCode))
            _ui.value.messages.filter { it.deliveryState != null && it.clientMessageId != null }.forEach { transmit(it) }
            if (wasDown) showToast(PlayerToast.Info(System.nanoTime(), "Связь восстановлена", "⚡"))
        }
        s.on("disconnect") { _ui.update { it.copy(link = LinkStatus.RECONNECTING) } }
        s.on("connect_error") { raw ->
            _ui.update { it.copy(link = LinkStatus.RECONNECTING) }
            if (raw != null && listOf("Сессия завершена", "Аккаунт заблокирован", "Пользователь не найден").any { raw.contains(it) }) {
                Session.signOut(raw.trim('"'))
            }
        }
        s.on("error_message") { raw ->
            _ui.update { it.copy(animeLoading = null) }
            // Server sends {message: "..."}
            val message = raw?.let { runCatching { AppJson.parseToJsonElement(it) }.getOrNull() }
                ?.let { (it as? kotlinx.serialization.json.JsonObject)?.get("message") as? JsonPrimitive }?.content
            if (message != null) {
                pushNotice(message)
                if (message.contains("исключ", true) || message.contains("бан", true) || message.contains("удал", true)) {
                    _events.tryEmit("kicked:$message")
                }
            }
        }
        s.onJson<SyncState>("room_state") { applyPayload(it) }
        s.onJson<SyncState>("sync_state") { applyPayload(it) }
        s.onJson<AnimeLoadingPayload>("anime_loading") { p ->
            _ui.update { it.copy(animeLoading = AnimeLoading(p.reason ?: "anime", p.episode, p.title)) }
        }
        s.on("anime_ended") {
            _ui.update { it.copy(animeLoading = null) }
            pushNotice("Серии закончились — это была финальная")
        }
        s.on("participants") { raw ->
            val list = raw?.let { runCatching { AppJson.decodeFromString<List<Participant>>(it) }.getOrNull() } ?: return@on
            _ui.update { it.copy(participants = mergeParticipants(it.participants, list)) }
        }
        s.onJson<ProfileUpdate>("profile_updated") { p ->
            _ui.update { state ->
                state.copy(
                    participants = state.participants.map { if (it.id == p.userId) it.copy(username = p.username, avatar = p.avatar, nicknameColor = p.nicknameColor) else it },
                    messages = state.messages.map { if (it.userId == p.userId) it.copy(username = p.username, avatar = p.avatar, nicknameColor = p.nicknameColor) else it },
                )
            }
        }
        s.onJson<Telemetry>("telemetry") { t ->
            _ui.update { it.copy(participants = mergeParticipants(it.participants, t.participants)) }
        }
        s.on("typing_status") { raw ->
            val list = raw?.let { runCatching { AppJson.decodeFromString<List<TypingUser>>(it) }.getOrNull() } ?: emptyList()
            _ui.update { it.copy(typing = list.filter { u -> u.id != me?.id }) }
        }
        s.onJson<SystemNoticePayload>("system_notice") { pushNotice(it.text) }
        s.onJson<SystemNoticePayload>("admin_notice") { pushNotice("📣 Администрация: ${it.text}") }
        s.onJson<Message>("chat_message") { onChatMessage(it) }
        s.onJson<MessageDeleted>("chat_message_deleted") { d ->
            _ui.update { state ->
                state.copy(messages = state.messages.filter { it.id != d.messageId }.map { if (it.replyTo?.id == d.messageId) it.copy(replyTo = null) else it })
            }
        }
        s.onJson<ReactionsPayload>("message_reactions") { p ->
            _ui.update { state -> state.copy(messages = state.messages.map { if (it.id == p.messageId) it.copy(reactions = p.reactions) else it }) }
        }
        s.on("session_revoked") { Session.signOut("Сессия завершена, войди снова") }
        s.on("account_banned") { Session.signOut("Аккаунт заблокирован") }

        // party
        s.onJson<LiveReaction>("live_reaction") { r -> if (r.userId != me?.id) pushReaction(r.kind, false) }
        s.onJson<EpisodeVoteState>("episode_vote") { v ->
            if (v.roomCode.isNotEmpty() && !v.roomCode.equals(roomCode, true)) return@onJson
            val offset = v.serverNow?.let { (it * 1000).toLong() - System.currentTimeMillis() } ?: _ui.value.clockOffsetMs
            _ui.update { state ->
                val card = if (v.active && v.episode == state.room?.anime?.episode) state.endCard ?: EndCard(v.episode ?: 0, false) else state.endCard
                state.copy(vote = if (v.active) v else null, clockOffsetMs = offset, endCard = card)
            }
            if (v.active) {
                val key = "${_ui.value.room?.anime?.shikimoriId}:${v.episode}"
                endCardKey = key
                voteRequestedKey = key
            }
        }
        s.onJson<EpisodeVoteResult>("episode_vote_result") { r ->
            if (r.roomCode.isNotEmpty() && !r.roomCode.equals(roomCode, true)) return@onJson
            _ui.update { it.copy(voteResult = r, vote = null) }
            viewModelScope.launch {
                delay(9_000)
                _ui.update { if (it.voteResult == r) it.copy(voteResult = null) else it }
            }
        }
        s.onJson<RatingsUpdate>("ratings_update") { u ->
            val episode = u.episode ?: return@onJson
            _ui.update { state ->
                val current = state.ratings ?: return@update state
                if (current.shikimoriId != u.shikimoriId) return@update state
                val mine = current.episodes.firstOrNull { it.episode == episode.episode }?.mine
                val merged = episode.copy(mine = mine)
                val episodes = (current.episodes.filter { it.episode != episode.episode } + merged).sortedBy { it.episode }
                state.copy(ratings = current.copy(title = u.title, episodes = episodes))
            }
        }
        s.on("skip_opening") {
            if (!isOwner) {
                val owner = _ui.value.participants.firstOrNull { it.isOwner }?.username ?: "Хост"
                val seg = _ui.value.room?.anime?.skipSegments?.firstOrNull()
                showToast(PlayerToast.Skip(System.nanoTime(), owner, seg?.let { (it.getOrNull(1) ?: 0.0) - (it.getOrNull(0) ?: 0.0) }))
            }
        }
        s.onJson<AchievementPayload>("achievement:unlocked") { a ->
            if (me?.doNotDisturb != true) showToast(PlayerToast.Info(System.nanoTime(), "Достижение: ${a.title}", "🏆"), 4500)
        }
        s.onJson<XpEvent>("game:xp") { x ->
            if (x.gained > 0 && x.reasons.any { !it.quiet }) {
                showToast(PlayerToast.Info(System.nanoTime(), "+${x.gained} XP · ${x.reasons.firstOrNull { !it.quiet }?.label ?: ""}", "✦"))
            }
            viewModelScope.launch { UserHub.refreshGame() }
        }
        s.onJson<LevelUpEvent>("game:level_up") { l ->
            showToast(PlayerToast.Info(System.nanoTime(), "Новый уровень: ${l.level}!", "⬆"), 4500)
        }
        s.onJson<QuestEvent>("game:quest") { q -> showToast(PlayerToast.Info(System.nanoTime(), "Квест выполнен: ${q.title} · +${q.xp} XP", "✓"), 4000) }
        s.onJson<StreakEvent>("game:streak") { st -> showToast(PlayerToast.Info(System.nanoTime(), "Серия дней: ${st.days} 🔥", "🔥")) }
        s.connect()
    }

    private fun mergeParticipants(current: List<Participant>, incoming: List<Participant>): List<Participant> {
        val avatars = current.associate { it.id to it.avatar }
        return incoming.map { it.copy(avatar = it.avatar ?: avatars[it.id]) }
    }

    private fun normalize(message: Message): Message = message.copy(
        reactions = message.reactions.ifEmpty {
            listOf(MessageReaction("heart"), MessageReaction("like"), MessageReaction("dislike"))
        },
    )

    private fun onChatMessage(raw: Message) {
        val message = normalize(raw)
        var isNew = false
        _ui.update { state ->
            val index = state.messages.indexOfFirst { it.id == message.id || (message.clientMessageId != null && it.clientMessageId == message.clientMessageId) }
            isNew = index < 0
            val list = if (index >= 0) state.messages.toMutableList().also { it[index] = message } else state.messages + message
            state.copy(messages = list.takeLast(400))
        }
        if (isNew && message.userId != me?.id) _incoming.tryEmit(message)
    }

    private fun applyPayload(payload: SyncState) {
        if (payload.roomCode.isNotEmpty() && !payload.roomCode.equals(roomCode, ignoreCase = true)) return
        val previousEpisode = _ui.value.room?.anime?.episode
        val previousTitle = _ui.value.room?.anime?.shikimoriId
        _ui.update { state ->
            state.copy(
                sync = payload,
                animeLoading = null,
                room = state.room?.copy(
                    videoUrl = payload.videoUrl,
                    anime = payload.anime ?: state.room.anime,
                    playback = state.room.playback.copy(
                        playing = payload.playing,
                        currentTime = payload.currentTime,
                        playbackRate = payload.playbackRate,
                        serverTime = payload.serverTime,
                    ),
                ),
            )
        }
        val anime = _ui.value.room?.anime
        if (anime?.shikimoriId != previousTitle) {
            _ui.update { it.copy(watched = anime?.shikimoriId?.let(Session::watchedEpisodes) ?: emptySet(), ratings = null) }
            loadExtras()
        } else if (anime != null && previousEpisode != null && previousEpisode != anime.episode && anime.episode > 0) {
            showToast(PlayerToast.Episode(System.nanoTime(), previousEpisode, anime.episode, false))
        }
        ensureSource(payload.videoUrl)
        applySync()
    }

    private fun ensureSource(url: String?) {
        if (url == currentSource) return
        currentSource = url
        sourceReady = false
        lastAlignedKey = null
        correcting = false
        if (url.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
            return
        }
        val uri = when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> space.aniroom.app.data.Api.baseUrl + url
            else -> url
        }
        val item = MediaItem.Builder().setUri(uri).apply {
            val clean = uri.substringBefore('?').lowercase()
            when {
                clean.endsWith(".m3u8") || clean.contains("/hls") || clean.contains("m3u8") -> setMimeType(MimeTypes.APPLICATION_M3U8)
                clean.endsWith(".mp4") -> setMimeType(MimeTypes.VIDEO_MP4)
                clean.endsWith(".webm") -> setMimeType(MimeTypes.VIDEO_WEBM)
            }
        }.build()
        player.setMediaItem(item)
        player.prepare()
    }

    private fun currentTargetTime(): Double {
        val s = _ui.value.sync ?: return 0.0
        val elapsed = if (s.playing) max(System.currentTimeMillis() / 1000.0 - s.serverTime + max(0, _ui.value.pingMs) / 2000.0, 0.0) * s.playbackRate else 0.0
        return s.currentTime + elapsed
    }

    /** Aligns the local player with the authoritative room state (same rules as the web client). */
    fun applySync() {
        val s = _ui.value.sync ?: return
        if (s.videoUrl.isNullOrBlank() || s.videoUrl != currentSource || !sourceReady) return
        val mediaKey = "${s.videoUrl}:${s.anime?.shikimoriId}:${s.anime?.translationId}:${s.anime?.episode}"
        val sourceChanged = lastAlignedKey != mediaKey
        if (isOwner && !sourceChanged && s.reason != "stream_recovery") return

        val target = currentTargetTime()
        val current = player.currentPosition / 1000.0
        val diff = target - current
        val absDiff = abs(diff)
        _ui.update { it.copy(driftMs = (diff * 1000).toInt(), ownerPaused = !isOwner && !s.playing) }

        val rate = s.playbackRate.toFloat().coerceIn(0.25f, 4f)
        when {
            sourceChanged -> {
                player.seekTo((target * 1000).toLong().coerceAtLeast(0))
                player.playbackParameters = PlaybackParameters(rate)
                manualRate = false
                correcting = false
            }
            !s.playing -> {
                if (absDiff > 0.5) player.seekTo((target * 1000).toLong().coerceAtLeast(0))
                player.playbackParameters = PlaybackParameters(rate)
                correcting = false
            }
            player.playbackState == Player.STATE_BUFFERING || manualRate -> Unit
            absDiff > 3 -> {
                player.seekTo((target * 1000).toLong())
                player.playbackParameters = PlaybackParameters(rate)
                correcting = false
                if (System.currentTimeMillis() - lastSeekNoticeAt > 8_000) {
                    lastSeekNoticeAt = System.currentTimeMillis()
                    showToast(PlayerToast.Info(System.nanoTime(), "Синхронизировано с хостом", "⇄"))
                }
            }
            absDiff > 0.4 -> {
                val adjust = when {
                    diff > 1.5 -> 1.05f
                    diff > 0 -> 1.02f
                    diff < -1.5 -> 0.95f
                    else -> 0.98f
                }
                player.playbackParameters = PlaybackParameters(rate * adjust)
                correcting = true
            }
            absDiff < 0.15 && correcting -> {
                player.playbackParameters = PlaybackParameters(rate)
                correcting = false
            }
        }
        player.playWhenReady = s.playing
        lastAlignedKey = mediaKey
    }

    // ------------------------------------------------------------------ loops

    private fun startLoops() {
        // Fast UI progress + end-of-episode detection
        viewModelScope.launch {
            while (isActive) {
                publishProgress()
                checkEpisodeProgress()
                delay(250)
            }
        }
        // Viewer drift correction + heartbeat
        viewModelScope.launch {
            while (isActive) {
                delay(2_000)
                if (!isOwner && _ui.value.sync?.playing == true) applySync()
                heartbeat()
                if (_ui.value.syncCooldown > 0) _ui.update { it.copy(syncCooldown = max(0, it.syncCooldown - 2)) }
            }
        }
        // Ping
        viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                val s = socket ?: continue
                val started = System.nanoTime()
                val ack = s.emitAck("ping_check", obj("client_sent_at" to System.currentTimeMillis()), 5_000)
                if (ack != null) _ui.update { it.copy(pingMs = ((System.nanoTime() - started) / 1_000_000).toInt()) }
            }
        }
        // Resume progress
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                saveProgress()
            }
        }
    }

    private fun heartbeat() {
        val s = socket ?: return
        if (_ui.value.room == null) return
        val buffering = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
        s.emit(
            "client_heartbeat",
            obj(
                "room_code" to roomCode,
                "current_time" to player.currentPosition / 1000.0,
                "duration_sec" to max(0.0, player.duration.takeIf { it != C.TIME_UNSET }?.div(1000.0) ?: 0.0),
                "playback_rate" to player.playbackParameters.speed.toDouble(),
                "paused" to !player.playWhenReady,
                "buffering" to buffering,
                "visible" to UserHub.isForeground,
                "ping_ms" to _ui.value.pingMs,
                "downlink" to null,
            ),
        )
    }

    private fun saveProgress() {
        val anime = _ui.value.room?.anime ?: return
        if (anime.shikimoriId == null || !sourceReady) return
        val position = player.currentPosition / 1000.0
        if (position < 30) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET }?.div(1000.0) ?: 0.0
        viewModelScope.launch {
            api {
                saveProgress(
                    roomCode,
                    obj(
                        "shikimori_id" to anime.shikimoriId,
                        "translation_id" to anime.translationId,
                        "episode" to anime.episode,
                        "position_sec" to position,
                        "duration_sec" to duration,
                        "quality" to anime.quality,
                    ),
                )
            }
        }
    }

    private fun publishProgress() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }?.div(1000.0) ?: 0.0
        _progress.value = PlayerProgress(
            positionSec = player.currentPosition / 1000.0,
            durationSec = duration,
            bufferedSec = player.bufferedPosition / 1000.0,
            isPlaying = player.isPlaying,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            speed = player.playbackParameters.speed,
            videoHeight = player.videoSize.height,
        )
    }

    private fun checkEpisodeProgress() {
        val anime = _ui.value.room?.anime ?: return
        val sid = anime.shikimoriId ?: return
        val episode = anime.episode
        if (episode < 1 || !sourceReady) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET }?.div(1000.0) ?: return
        if (duration < 120) return
        val position = player.currentPosition / 1000.0
        val key = "$sid:$episode"
        val segments = anime.skipSegments

        // Auto skip opening (owner)
        val opening = segments.firstOrNull()
        if (isOwner && Session.appPrefs.value.autoSkipOpening && opening != null && opening.size >= 2 && autoSkippedKey != key && player.isPlaying) {
            if (position >= opening[0] && position < opening[1] - 3) {
                autoSkippedKey = key
                skipOpening()
            } else if (position >= opening[1]) autoSkippedKey = key
        }

        // Watched marking
        val ending = segments.firstOrNull { it.size >= 2 && it[0] >= duration * 0.6 && it[0] < duration }
        val watchThreshold = if (ending != null && ending[0] < duration * 0.9) max(duration * 0.6, ending[0] - 3) else duration * 0.9
        if (position >= watchThreshold && watchedKey != key) {
            watchedKey = key
            val set = Session.markWatched(sid, episode)
            _ui.update { it.copy(watched = it.watched + set) }
        }

        // End card + vote
        val endSeg = segments.firstOrNull { it.size >= 2 && it[0] >= duration * 0.7 && it[0] < duration }
        val threshold = endSeg?.get(0) ?: max(duration * 0.6, duration - 12)
        if (position >= threshold) triggerEndCard(key, episode, anime.episodesTotal)
    }

    private fun onEpisodeEnded() {
        val anime = _ui.value.room?.anime ?: return
        val sid = anime.shikimoriId ?: return
        if (anime.episode < 1) return
        triggerEndCard("$sid:${anime.episode}", anime.episode, anime.episodesTotal)
    }

    private fun triggerEndCard(key: String, episode: Int, total: Int) {
        val isLast = total > 0 && episode >= total
        if (endCardKey != key) {
            endCardKey = key
            _ui.update { it.copy(endCard = EndCard(episode, isLast)) }
        }
        if (isLast || voteRequestedKey == key || System.currentTimeMillis() < voteRetryAt) return
        voteRequestedKey = key
        val s = socket ?: return
        viewModelScope.launch {
            val raw = s.emitAck("episode_vote_start", obj("room_code" to roomCode, "episode" to episode), 5_000)
            val ack = raw?.let { runCatching { AppJson.decodeFromString<SimpleAck>(it) }.getOrNull() }
            if (raw == null || (ack?.ok == false && (ack.error == "too_early" || ack.error == "stale"))) {
                voteRequestedKey = ""
                voteRetryAt = System.currentTimeMillis() + 1_500
            }
        }
    }

    // ------------------------------------------------------------------ owner controls

    private fun control(action: String, playing: Boolean? = null) {
        socket?.emit(
            "control_playback",
            obj(
                "room_code" to roomCode,
                "action" to action,
                "current_time" to player.currentPosition / 1000.0,
                "playback_rate" to player.playbackParameters.speed.toDouble(),
            ).let { if (playing != null) kotlinx.serialization.json.JsonObject(it + ("playing" to JsonPrimitive(playing))) else it },
        )
    }

    /** Returns false when the viewer isn't allowed to control playback. */
    fun togglePlay(): Boolean {
        if (!isOwner) return false
        if (player.playWhenReady) {
            player.pause()
            control("pause")
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
            control("play")
        }
        publishProgress()
        return true
    }

    fun seekTo(seconds: Double): Boolean {
        if (!isOwner) return false
        val target = seconds.coerceAtLeast(0.0)
        player.seekTo((target * 1000).toLong())
        socket?.emit(
            "control_playback",
            obj(
                "room_code" to roomCode,
                "action" to "seek",
                "current_time" to target,
                "playback_rate" to player.playbackParameters.speed.toDouble(),
                "playing" to player.playWhenReady,
            ),
        )
        publishProgress()
        return true
    }

    fun seekBy(deltaSec: Double): Boolean = seekTo(player.currentPosition / 1000.0 + deltaSec)

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        if (isOwner) control("rate") else manualRate = speed != 1f
    }

    fun skipOpening() {
        val seg = _ui.value.room?.anime?.skipSegments?.firstOrNull() ?: return
        if (!isOwner || seg.size < 2) return
        val to = seg[1] + 0.5
        val from = player.currentPosition / 1000.0
        player.seekTo((to * 1000).toLong())
        socket?.emit(
            "control_playback",
            obj(
                "room_code" to roomCode,
                "action" to "skip_opening",
                "current_time" to to,
                "playback_rate" to player.playbackParameters.speed.toDouble(),
                "playing" to player.playWhenReady,
            ),
        )
        showToast(PlayerToast.Skip(System.nanoTime(), "Ты", to - max(0.0, from)))
    }

    fun nextEpisode() {
        val room = _ui.value.room ?: return
        val ep = room.anime?.episode ?: 0
        _ui.update { it.copy(animeLoading = AnimeLoading("episode", ep + 1, null)) }
        showToast(PlayerToast.Episode(System.nanoTime(), ep.takeIf { it > 0 }, ep + 1, true), 15_000)
        socket?.emit("next_episode", obj("room_code" to roomCode))
    }

    fun selectEpisode(episode: Int) {
        _ui.update { it.copy(animeLoading = AnimeLoading("episode", episode, null)) }
        socket?.emit("select_episode", obj("room_code" to roomCode, "episode" to episode))
    }

    fun retryStream() {
        _ui.update { it.copy(animeLoading = AnimeLoading("retry", it.room?.anime?.episode, null)) }
        socket?.emit("retry_anime_stream", obj("room_code" to roomCode))
    }

    fun setAnime(choice: AnimeChoice) {
        _ui.update { it.copy(animeLoading = AnimeLoading("anime", choice.episode, choice.title)) }
        val payload = AppJson.encodeToJsonElement(AnimeChoice.serializer(), choice) as kotlinx.serialization.json.JsonObject
        socket?.emit("set_anime", kotlinx.serialization.json.JsonObject(payload + ("room_code" to JsonPrimitive(roomCode))))
    }

    fun syncToHost() {
        val s = _ui.value.sync ?: return
        if (_ui.value.syncCooldown > 0) return
        val target = currentTargetTime()
        player.seekTo((target * 1000).toLong())
        player.playbackParameters = PlaybackParameters(s.playbackRate.toFloat())
        manualRate = false
        player.playWhenReady = s.playing
        _ui.update { it.copy(syncCooldown = 10) }
        socket?.emit("request_sync", obj("room_code" to roomCode))
        showToast(PlayerToast.Info(System.nanoTime(), "Догоняю хоста", "⇄"))
    }

    fun acceptResume() {
        val at = _ui.value.resumeAt ?: return
        _ui.update { it.copy(resumeAt = null) }
        if (!isOwner) return
        player.seekTo((at * 1000).toLong())
        player.play()
        socket?.emit(
            "control_playback",
            obj("room_code" to roomCode, "action" to "seek", "current_time" to at, "playback_rate" to player.playbackParameters.speed.toDouble(), "playing" to true),
        )
    }

    fun dismissResume() = _ui.update { it.copy(resumeAt = null) }

    // ------------------------------------------------------------------ chat

    fun sendMessage(text: String, replyTo: Message?) {
        val user = me ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val clientId = "${user.id}-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val pending = Message(
            id = -System.currentTimeMillis(),
            roomId = _ui.value.room?.id ?: 0,
            userId = user.id,
            username = user.username,
            nicknameColor = user.nicknameColor,
            avatar = user.avatar,
            replyToId = replyTo?.id,
            replyTo = replyTo?.let { space.aniroom.app.data.MessageReply(it.id, it.userId, it.username, it.nicknameColor, it.text, it.createdAt) },
            text = trimmed,
            createdAt = java.time.Instant.now().toString(),
            reactions = listOf(MessageReaction("heart"), MessageReaction("like"), MessageReaction("dislike")),
            deliveryState = "sending",
            clientMessageId = clientId,
        )
        _ui.update { it.copy(messages = it.messages + pending) }
        stopTyping()
        transmit(pending)
    }

    private fun transmit(pending: Message) {
        val s = socket ?: return
        val clientId = pending.clientMessageId ?: return
        if (!s.connected) {
            markDelivery(clientId, "failed")
            return
        }
        markDelivery(clientId, "sending")
        viewModelScope.launch {
            val raw = s.emitAck(
                "chat_message",
                obj("room_code" to roomCode, "text" to pending.text, "reply_to_id" to pending.replyToId, "client_message_id" to clientId),
                10_000,
            )
            val ack = raw?.let { runCatching { AppJson.decodeFromString<ChatAck>(it) }.getOrNull() }
            val canonical = ack?.message
            if (ack?.ok == true && canonical != null) {
                val fixed = normalize(canonical.copy(clientMessageId = clientId, deliveryState = null))
                _ui.update { state ->
                    val withoutDup = state.messages.filterNot { it.id == fixed.id && it.clientMessageId != clientId }
                    state.copy(messages = withoutDup.map { if (it.clientMessageId == clientId) fixed else it })
                }
            } else markDelivery(clientId, "failed")
        }
    }

    private fun markDelivery(clientId: String, state: String) {
        _ui.update { s -> s.copy(messages = s.messages.map { if (it.clientMessageId == clientId && it.deliveryState != null) it.copy(deliveryState = state) else it }) }
    }

    fun retryMessage(message: Message) = transmit(message)

    fun discardMessage(message: Message) {
        _ui.update { s -> s.copy(messages = s.messages.filterNot { it.clientMessageId != null && it.clientMessageId == message.clientMessageId }) }
    }

    fun sendSticker(sticker: Sticker) {
        socket?.emit("chat_message", obj("room_code" to roomCode, "sticker_id" to sticker.id))
    }

    fun toggleReaction(message: Message, reaction: String) {
        if (message.id < 0) return
        _ui.update { s ->
            s.copy(messages = s.messages.map { m ->
                if (m.id != message.id) m else m.copy(reactions = m.reactions.map { r ->
                    if (r.reaction != reaction) r else r.copy(reactedByMe = !r.reactedByMe, count = (r.count + if (r.reactedByMe) -1 else 1).coerceAtLeast(0))
                })
            })
        }
        socket?.emit("message_reaction", obj("room_code" to roomCode, "message_id" to message.id, "reaction" to reaction))
    }

    fun onTyping(text: String) {
        if (text.isBlank()) { stopTyping(); return }
        if (!typingSent) {
            typingSent = true
            socket?.emit("chat_typing", obj("room_code" to roomCode, "is_typing" to true))
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(1_600)
            stopTyping()
        }
    }

    private fun stopTyping() {
        typingJob?.cancel()
        if (typingSent) {
            typingSent = false
            socket?.emit("chat_typing", obj("room_code" to roomCode, "is_typing" to false))
        }
    }

    // ------------------------------------------------------------------ party

    fun sendReaction(kind: String) {
        if (socket?.connected != true) return
        pushReaction(kind, true)
        socket?.emit("live_reaction", obj("room_code" to roomCode, "kind" to kind))
    }

    private fun pushReaction(kind: String, mine: Boolean) {
        if (!mine && !Session.appPrefs.value.reactions) return
        val id = ++reactionSeq
        val emoji = reactionEmoji(kind)
        _flying.update { (it + FlyingReaction(id, emoji, mine, (0.15f + Math.random().toFloat() * 0.7f))).takeLast(30) }
        viewModelScope.launch {
            delay(3_200)
            _flying.update { list -> list.filterNot { it.id == id } }
        }
        val now = System.currentTimeMillis()
        recentReactions.addLast(now to kind)
        while (recentReactions.isNotEmpty() && now - recentReactions.first().first > 1_600) recentReactions.removeFirst()
        if (recentReactions.size >= 4 && now - lastBurstAt > 2_600) {
            lastBurstAt = now
            val top = recentReactions.groupingBy { it.second }.eachCount().maxByOrNull { it.value }?.key ?: kind
            val info = LIVE_REACTIONS.firstOrNull { it.first == top }
            val burst = Burst(now, info?.second ?: "🔥", info?.third ?: "!!!", recentReactions.size)
            _burst.value = burst
            viewModelScope.launch {
                delay(1_500)
                if (_burst.value == burst) _burst.value = null
            }
        }
    }

    fun castVote(choice: String) {
        val uid = me?.id ?: return
        _ui.update { s ->
            val v = s.vote ?: return@update s
            s.copy(vote = v.copy(
                yes = if (choice == "yes") (v.yes + uid).distinct() else v.yes - uid,
                no = if (choice == "no") (v.no + uid).distinct() else v.no - uid,
            ))
        }
        socket?.emit("episode_vote_cast", obj("room_code" to roomCode, "choice" to choice))
    }

    fun decideVote(decision: String) {
        socket?.emit("episode_vote_decide", obj("room_code" to roomCode, "decision" to decision))
    }

    fun dismissEndCard() = _ui.update { it.copy(endCard = it.endCard?.copy(dismissed = true), voteResult = null) }

    fun openRating(episode: Int) {
        val total = _ui.value.room?.anime?.episodesTotal ?: 0
        _ui.update { it.copy(endCard = EndCard(episode, total in 1..episode)) }
    }

    fun rateEpisode(episode: Int, score: Int) {
        _ui.update { s ->
            val r = s.ratings ?: return@update s
            val exists = r.episodes.any { it.episode == episode }
            val list = if (exists) r.episodes.map { if (it.episode == episode) it.copy(mine = score) else it }
            else r.episodes + space.aniroom.app.data.EpisodeRatingSummary(episode, null, 0, score)
            s.copy(ratings = r.copy(episodes = list.sortedBy { it.episode }))
        }
        val s = socket ?: return
        viewModelScope.launch {
            val raw = s.emitAck("rate_episode", obj("room_code" to roomCode, "episode" to episode, "score" to score), 6_000)
            val ack = raw?.let { runCatching { AppJson.decodeFromString<SimpleAck>(it) }.getOrNull() }
            if (ack?.ok != true) pushNotice(if (ack?.error == "rate_limited") "Слишком часто. Подожди минуту." else "Не получилось сохранить оценку")
            else showToast(PlayerToast.Info(System.nanoTime(), "Оценка сохранена", "★"))
        }
    }

    // ------------------------------------------------------------------ room admin

    fun toggleLock() = viewModelScope.launch {
        val room = _ui.value.room ?: return@launch
        api { setLock(roomCode, obj("is_locked" to !room.isLocked)) }
            .onSuccess { r -> _ui.update { it.copy(room = it.room?.copy(isLocked = r.isLocked)) }; requestSync() }
            .onFailure { pushNotice(it.message ?: "Не удалось изменить блокировку") }
    }

    fun togglePublic() = viewModelScope.launch {
        val room = _ui.value.room ?: return@launch
        api { updateRoom(roomCode, obj("is_public" to !room.isPublic)) }
            .onSuccess { r -> _ui.update { it.copy(room = it.room?.copy(isPublic = r.isPublic)) } }
            .onFailure { pushNotice(it.message ?: "Не удалось изменить комнату") }
    }

    fun transferOwner(userId: Int) = viewModelScope.launch {
        api { transferOwner(roomCode, obj("user_id" to userId)) }
            .onSuccess { r -> _ui.update { it.copy(room = it.room?.copy(ownerId = r.ownerId)) }; requestSync(); pushNotice("Права хоста переданы") }
            .onFailure { pushNotice(it.message ?: "Не удалось передать права") }
    }

    fun kick(userId: Int) = viewModelScope.launch {
        api { kick(roomCode, userId) }
            .onSuccess { r -> _ui.update { it.copy(participants = r.participants) } }
            .onFailure { pushNotice(it.message ?: "Не удалось исключить участника") }
    }

    fun invite(userId: Int, name: String) = viewModelScope.launch {
        api { inviteFriend(roomCode, obj("user_id" to userId)) }
            .onSuccess { pushNotice("Приглашение отправлено: $name") }
            .onFailure { pushNotice(it.message ?: "Не удалось пригласить") }
    }

    fun requestSync() { socket?.emit("request_sync", obj("room_code" to roomCode)) }

    // ------------------------------------------------------------------ helpers

    fun pushNotice(text: String) {
        _ui.update { it.copy(notices = (it.notices + SystemNotice("${System.nanoTime()}", text)).takeLast(20)) }
    }

    fun showToast(toast: PlayerToast, durationMs: Long = 2_900) {
        _toast.value = toast
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(durationMs)
            if (_toast.value?.id == toast.id) _toast.value = null
        }
    }

    override fun onCleared() {
        saveProgress()
        stopTyping()
        socket?.disconnect()
        socket = null
        player.release()
        super.onCleared()
    }

    @Suppress("unused")
    private fun jsonArray(list: List<Int>) = JsonArray(list.map { JsonPrimitive(it) })
}
