package space.aniroom.app.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.aniroom.app.BuildConfig
import space.aniroom.app.data.LinkStatus
import space.aniroom.app.data.Message
import space.aniroom.app.data.Session
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.EmptyState
import space.aniroom.app.ui.components.LoadingScreen
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.Mascot
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.RoundIcon
import space.aniroom.app.ui.components.UserAvatar
import space.aniroom.app.ui.components.nickColor
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.PipState
import space.aniroom.app.util.RoomLinks
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Crop169
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.ui.draw.blur
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.roundToInt
import space.aniroom.app.util.findActivity
import space.aniroom.app.util.formatTime
import space.aniroom.app.util.plural
import space.aniroom.app.util.rememberHaptic
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun RoomScreen(code: String, onBack: () -> Unit, openUser: (Int) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: RoomViewModel = viewModel(key = "room-$code", factory = viewModelFactory { initializer { RoomViewModel(app, code) } })
    val ui by vm.ui.collectAsState()
    val progress by vm.progress.collectAsState()
    val inPip by PipState.inPip.collectAsState()
    val activity = context.findActivity()
    val view = LocalView.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var lockedPortrait by rememberSaveable { mutableStateOf(false) }
    val isFull = landscape

    // Orientation / immersive handling
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { w -> WindowInsetsControllerCompat(w, w.decorView).show(WindowInsetsCompat.Type.systemBars()) }
            PipState.update(activity, false)
        }
    }
    LaunchedEffect(isFull) {
        val w = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(w, w.decorView)
        if (isFull) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else controller.show(WindowInsetsCompat.Type.systemBars())
    }
    // Keep screen awake while watching; allow PiP while playing.
    DisposableEffect(progress.isPlaying, vm.isOwner) {
        view.keepScreenOn = progress.isPlaying
        PipState.update(activity, progress.isPlaying && Session.appPrefs.value.autoPip, progress.isPlaying, vm.isOwner)
        onDispose { view.keepScreenOn = false }
    }
    // PiP window buttons → room actions
    DisposableEffect(vm) {
        PipState.onAction = { action ->
            when (action) {
                PipState.ACTION_TOGGLE -> if (!vm.togglePlay()) vm.syncToHost()
                PipState.ACTION_SYNC -> vm.syncToHost()
                PipState.ACTION_REACT -> vm.sendReaction("fire")
            }
        }
        onDispose { PipState.onAction = null }
    }
    // Leaving the app without PiP (or closing the PiP window) pauses locally; coming back re-syncs.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (activity?.isInPictureInPictureMode != true) vm.onBackground()
                Lifecycle.Event.ON_START -> vm.onForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(vm) { PipState.dismissed.collect { vm.onBackground() } }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event.startsWith("kicked:")) {
                Toast.makeText(context, event.removePrefix("kicked:"), Toast.LENGTH_LONG).show()
                onBack()
            }
        }
    }

    fun enterFull() {
        lockedPortrait = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    fun exitFull() {
        lockedPortrait = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    }
    LaunchedEffect(lockedPortrait, landscape) {
        // After an explicit exit, give the phone back its sensor once it's held upright again.
        if (lockedPortrait && !landscape) {
            delay(2500)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            lockedPortrait = false
        }
    }

    BackHandler(isFull) { exitFull() }
    val loadedTitle = ui.room?.anime?.title ?: ui.room?.title
    LaunchedEffect(loadedTitle) { if (loadedTitle != null) RoomLinks.pushShortcut(context, code, loadedTitle) }

    if (ui.loading) {
        LoadingScreen("Подключаю комнату…")
        return
    }
    if (ui.room == null) {
        Box(Modifier.fillMaxSize().background(Manga.colors.paper).statusBarsPadding(), contentAlignment = Alignment.Center) {
            EmptyState("Не удалось открыть комнату", ui.loadError ?: "Проверь код или соединение", mood = MascotMood.SHOCK) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MangaButton("Назад", onBack, kind = ButtonKind.PAPER)
                    MangaButton("Повторить", { vm.load() }, icon = Icons.Rounded.Refresh)
                }
            }
        }
        return
    }

    if (inPip) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PlayerSurface(vm, Modifier.fillMaxSize())
            FlyingLayer(vm, Modifier.fillMaxSize())
        }
        return
    }

    if (isFull) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PlayerArea(vm, ui, progress, isFull = true, onToggleFull = ::exitFull, onBack = ::exitFull, modifier = Modifier.fillMaxSize(), openUser = openUser)
        }
        return
    }

    val m = Manga.colors
    Column(Modifier.fillMaxSize().background(m.paper)) {
        Box(Modifier.background(Color.Black).statusBarsPadding()) {
            PlayerArea(
                vm, ui, progress, isFull = false, onToggleFull = ::enterFull, onBack = onBack,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Column(Modifier.weight(1f).navigationBarsPadding().imePadding()) {
            RoomHeader(vm, ui)
            ReactionBar(vm)
            EndCardPanel(vm, ui, progress)
            ResumePrompt(vm, ui)
            RoomTabs(vm, ui, openUser)
        }
    }
}

// --------------------------------------------------------------------------------------- player

@Composable
private fun PlayerSurface(
    vm: RoomViewModel,
    modifier: Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onTexture: (android.view.TextureView?) -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            (android.view.LayoutInflater.from(ctx).inflate(space.aniroom.app.R.layout.player_texture, null) as PlayerView).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                this.resizeMode = resizeMode
                player = vm.player
                onTexture(videoSurfaceView as? android.view.TextureView)
            }
        },
        update = {
            it.player = vm.player
            if (it.resizeMode != resizeMode) it.resizeMode = resizeMode
        },
        onRelease = { it.player = null; onTexture(null) },
    )
}

@Composable
private fun PlayerArea(
    vm: RoomViewModel,
    ui: RoomUi,
    progress: PlayerProgress,
    isFull: Boolean,
    onToggleFull: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
    openUser: (Int) -> Unit = {},
) {
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    var chatSeen by remember { mutableStateOf(ui.messages.size) }
    LaunchedEffect(chatOpen, ui.messages.size) { if (chatOpen) chatSeen = ui.messages.size }
    val chatUnread = (ui.messages.size - chatSeen).coerceAtLeast(0)
    BackHandler(chatOpen) { chatOpen = false }
    val m = Manga.colors
    val context = LocalContext.current
    val haptic = rememberHaptic()
    var controls by remember { mutableStateOf(true) }
    var seekFlash by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    var picker by remember { mutableStateOf(false) }
    val room = ui.room ?: return
    val anime = room.anime
    val owner = vm.isOwner
    val hasSource = !room.videoUrl.isNullOrBlank()

    LaunchedEffect(controls, progress.isPlaying) {
        if (controls && progress.isPlaying) {
            delay(3800)
            controls = false
        }
    }

    fun denied() {
        haptic(true)
        Toast.makeText(context, "Плеером управляет хост", Toast.LENGTH_SHORT).show()
    }

    val prefs by Session.appPrefs.collectAsState()
    var texture by remember { mutableStateOf<android.view.TextureView?>(null) }
    val fitMode = if (isFull) prefs.videoFit else "fit"
    val resize = when (fitMode) {
        "fill" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        "stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
    fun setFit(mode: String) {
        if (mode == prefs.videoFit) return
        Session.updatePrefs { it.copy(videoFit = mode) }
        haptic(false)
        vm.showToast(PlayerToast.Info(System.nanoTime(), when (mode) { "fill" -> "Заполнить экран"; "stretch" -> "Растянуть"; else -> "Вписать целиком" }, "⛶"), 1400)
    }

    BoxWithConstraints(modifier.background(Color.Black)) {
        val widthPx = constraints.maxWidth.toFloat()
        if (isFull && hasSource && prefs.ambient && fitMode == "fit") AmbientGlow(texture, vm, Modifier.fillMaxSize())
        PlayerSurface(vm, Modifier.fillMaxSize(), resize) { texture = it }

        // Gestures: tap toggles controls, double-tap seeks (host).
        val tapGestures = Modifier.pointerInput(owner) {
            detectTapGestures(
                onTap = { controls = !controls },
                onDoubleTap = { offset ->
                    val forward = offset.x > widthPx / 2
                    if (vm.seekBy(if (forward) 10.0 else -10.0)) {
                        haptic(false)
                        seekFlash = System.nanoTime() to (if (forward) 1 else -1)
                    } else denied()
                },
            )
        }
        val pinch = if (isFull) Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var zoom = 1f
                var multi = false
                do {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } >= 2) {
                        multi = true
                        zoom *= event.calculateZoom()
                        event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (multi) {
                    if (zoom > 1.12f) setFit("fill") else if (zoom < 0.9f) setFit("fit")
                }
            }
        } else Modifier
        if (isFull && prefs.gestures) SwipeAdjustLayer(Modifier.fillMaxSize(), pinch.then(tapGestures))
        else Box(Modifier.fillMaxSize().then(pinch).then(tapGestures))

        FlyingLayer(vm, Modifier.fillMaxSize())
        BurstLayer(vm, Modifier.align(Alignment.Center))

        // double-tap ripple
        seekFlash?.let { (key, dir) ->
            SeekRipple(key, dir, Modifier.align(if (dir > 0) Alignment.CenterEnd else Alignment.CenterStart)) { seekFlash = null }
        }

        // Loading / empty states
        val loadingInfo = ui.animeLoading
        when {
            loadingInfo != null -> SourceLoading(loadingInfo, Modifier.align(Alignment.Center))
            !hasSource -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(MascotMood.SLEEP, size = if (isFull) 110.dp else 84.dp)
                Spacer(Modifier.height(6.dp))
                Text(if (owner) "Выбери, что смотрим" else "Хост ещё выбирает тайтл", color = Color.White, fontWeight = FontWeight.Bold)
                if (owner) {
                    Spacer(Modifier.height(10.dp))
                    MangaButton("Выбрать аниме", { picker = true }, kind = ButtonKind.YELLOW, compact = true)
                }
            }
            progress.buffering -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(46.dp), color = m.red, strokeWidth = 4.dp)
        }

        // Manga pause screen (same as the site): whenever the video stands still
        val pausedNow = hasSource && loadingInfo == null && !progress.buffering && !progress.isPlaying &&
            !vm.player.playWhenReady && progress.durationSec > 0
        AnimatedVisibility(pausedNow, enter = fadeIn(tween(250)), exit = fadeOut(tween(200)), modifier = Modifier.fillMaxSize()) {
            val hostName = ui.participants.firstOrNull { it.isOwner }?.username
            PauseScreen(who = if (owner) "Ты" else hostName, isOwner = owner, compact = !isFull)
        }

        // Controls overlay
        AnimatedVisibility(controls, enter = fadeIn(tween(180)), exit = fadeOut(tween(250)), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(90.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(110.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                // top bar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).then(if (isFull) Modifier.statusBarsPadding() else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(anime?.title ?: room.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                        val sub = listOfNotNull(
                            anime?.episode?.takeIf { it > 0 }?.let { "Серия $it" + ((anime?.episodesTotal ?: 0).takeIf { t -> t > 0 }?.let { t -> " из $t" } ?: "") },
                            anime?.translationName,
                        ).joinToString(" · ")
                        if (sub.isNotEmpty()) Text(sub, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1)
                    }
                    SyncBadge(ui, owner) { vm.syncToHost() }
                    if (PipState.supported(LocalContext.current)) {
                        Spacer(Modifier.width(4.dp))
                        OverlayButton(Icons.Rounded.PictureInPictureAlt, "Картинка в картинке") { PipState.enter(context.findActivity(), progress.isPlaying, owner) }
                    }
                }
                // center play/pause
                if (hasSource && ui.animeLoading == null && !progress.buffering) {
                    val playing = progress.isPlaying || (vm.player.playWhenReady && !owner)
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(if (isFull) 74.dp else 62.dp)
                            .clip(CircleShape)
                            .background(m.red.copy(alpha = 0.92f))
                            .border(3.dp, Color.Black, CircleShape)
                            .clickable {
                                haptic(false)
                                if (!vm.togglePlay()) {
                                    if (ui.sync?.playing == true && !progress.isPlaying) vm.syncToHost() else denied()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(playing, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }, label = "pp") { p ->
                            Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (p) "Пауза" else "Play", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
                // bottom bar
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 4.dp)
                        .then(if (isFull) Modifier.navigationBarsPadding() else Modifier),
                ) {
                    SeekBar(vm, progress, owner, ::denied)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatTime(progress.positionSec)} / ${formatTime(progress.durationSec)}",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                        )
                        SpeedMenu(vm, progress)
                        QualityMenu(vm, progress)
                        if (isFull) OverlayButton(
                            when (prefs.videoFit) { "fill" -> Icons.Rounded.FitScreen; "stretch" -> Icons.Rounded.AspectRatio; else -> Icons.Rounded.Crop169 },
                            "Размер картинки",
                        ) { setFit(when (prefs.videoFit) { "fit" -> "fill"; "fill" -> "stretch"; else -> "fit" }) }
                        if (isFull) Box {
                            OverlayButton(Icons.AutoMirrored.Rounded.Chat, "Чат") { chatOpen = !chatOpen }
                            if (chatUnread > 0 && !chatOpen) Box(
                                Modifier.align(Alignment.TopEnd).clip(CircleShape).background(m.red).padding(horizontal = 5.dp),
                            ) { Text(if (chatUnread > 9) "9+" else "$chatUnread", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        }
                        OverlayButton(if (isFull) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, "Во весь экран", onToggleFull)
                    }
                }
            }
        }

        // Skip opening / next episode chips (host)
        val segments = anime?.skipSegments.orEmpty()
        val opening = segments.firstOrNull()
        val ending = segments.getOrNull(1)
        val pos = progress.positionSec
        val showSkip = owner && opening != null && opening.size >= 2 && pos >= opening[0] && pos < opening[1] - 1
        val showNext = owner && ending != null && ending.size >= 2 && pos >= ending[0] && pos <= ending[1] && (anime?.episodesTotal ?: 0) > (anime?.episode ?: 0)
        AnimatedVisibility(
            showSkip || showNext,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = if (controls) 74.dp else 18.dp),
            enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            MangaButton(
                if (showSkip) "Пропустить опенинг" else "Следующая серия",
                { haptic(false); if (showSkip) vm.skipOpening() else vm.nextEpisode() },
                kind = ButtonKind.YELLOW, compact = true, icon = Icons.Rounded.SkipNext,
            )
        }

        ActionBanners(vm, isFull, Modifier.fillMaxSize())
        PlayerToastLayer(vm, Modifier.align(Alignment.TopCenter).padding(top = if (isFull) 64.dp else 52.dp))

        if (isFull) {
            if (!chatOpen) ChatOverlay(vm, ui, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = if (controls) 96.dp else 24.dp).widthIn(max = 360.dp).navigationBarsPadding())
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FullscreenReactions(controls) { kind -> haptic(false); vm.sendReaction(kind) }
            }
            EndCardOverlay(vm, ui, progress, Modifier.align(Alignment.Center))
            FullscreenChat(vm, ui, chatOpen, { chatOpen = false }, openUser, Modifier.align(Alignment.CenterEnd))
        }
    }

    if (picker) AnimePickerDialog(onDismiss = { picker = false }, onConfirm = { vm.setAnime(it); picker = false })
}

@Composable
private fun OverlayButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(26.dp)) }
}

@Composable
private fun SeekBar(vm: RoomViewModel, progress: PlayerProgress, owner: Boolean, denied: () -> Unit) {
    val m = Manga.colors
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = progress.durationSec.toFloat().coerceAtLeast(1f)
    val value = if (dragging) dragValue else progress.positionSec.toFloat().coerceIn(0f, duration)
    val segments = vm.ui.collectAsState().value.room?.anime?.skipSegments.orEmpty()
    Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
        // buffered + opening marks
        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((progress.bufferedSec.toFloat() / duration).coerceIn(0f, 1f)).background(Color.White.copy(alpha = 0.35f)))
            segments.firstOrNull()?.takeIf { it.size >= 2 }?.let { seg ->
                Row(Modifier.fillMaxSize()) {
                    val start = (seg[0].toFloat() / duration).coerceIn(0f, 1f)
                    val len = ((seg[1] - seg[0]).toFloat() / duration).coerceIn(0f, 1f - start)
                    if (start > 0f) Spacer(Modifier.weight(start))
                    if (len > 0f) Box(Modifier.weight(len).fillMaxHeight().background(m.yellow.copy(alpha = 0.8f)))
                    if (1f - start - len > 0.001f) Spacer(Modifier.weight(1f - start - len))
                }
            }
        }
        Slider(
            value = value,
            onValueChange = {
                if (!owner) return@Slider
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                if (!owner) { denied(); return@Slider }
                vm.seekTo(dragValue.toDouble())
                dragging = false
            },
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = m.red,
                activeTrackColor = m.red,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (dragging) {
            Text(
                formatTime(dragValue.toDouble()),
                color = Color.White, fontFamily = DisplayFont, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-26).dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun SpeedMenu(vm: RoomViewModel, progress: PlayerProgress) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Speed, "Скорость", tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(3.dp))
            Text("${String.format(java.util.Locale.US, "%.2f", progress.speed).trimEnd('0').trimEnd('.')}×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                DropdownMenuItem(
                    text = { Text("${s}×", fontWeight = if (abs(progress.speed - s) < 0.01f) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { vm.setSpeed(s); open = false },
                )
            }
        }
    }
}

@Composable
private fun QualityMenu(vm: RoomViewModel, progress: PlayerProgress) {
    var open by remember { mutableStateOf(false) }
    val prefs by Session.appPrefs.collectAsState()
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.HighQuality, "Качество", tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(3.dp))
            Text(if (progress.videoHeight > 0) "${progress.videoHeight}p" else "Авто", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            listOf("auto" to "Авто", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p").forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontWeight = if (prefs.qualityMode == key) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        Session.updatePrefs { it.copy(qualityMode = key) }
                        vm.applyQualityCap()
                        open = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(if (prefs.dataSaver) "Экономия трафика: вкл" else "Экономия трафика: выкл") },
                onClick = {
                    Session.updatePrefs { it.copy(dataSaver = !it.dataSaver) }
                    vm.applyQualityCap()
                    open = false
                },
            )
        }
    }
}

@Composable
private fun SyncBadge(ui: RoomUi, owner: Boolean, onSync: () -> Unit) {
    val m = Manga.colors
    val drift = abs(ui.driftMs)
    val (label, color) = when {
        ui.link != LinkStatus.CONNECTED -> "Связь…" to m.red
        owner -> "Хост" to m.yellow
        drift < 150 -> "Синхронно" to m.green
        drift <= 400 -> "±${drift} мс" to m.yellow
        else -> "Догоняем" to m.red
    }
    val t = rememberInfiniteTransition(label = "sync")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "spin")
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = !owner && ui.syncCooldown == 0, onClick = onSync)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (!owner && ui.link == LinkStatus.CONNECTED) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.Sync, "Синхронизировать", tint = Color.White.copy(alpha = if (ui.syncCooldown > 0) 0.4f else 1f),
                modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = if (ui.syncCooldown > 0) spin else 0f },
            )
        }
    }
}

@Composable
private fun SourceLoading(info: AnimeLoading, modifier: Modifier) {
    val m = Manga.colors
    MangaPanel(modifier, color = m.paper2, shadow = 4.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Mascot(MascotMood.SEARCH, size = 52.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    when (info.reason) {
                        "episode" -> info.episode?.let { "Загружаю серию $it" } ?: "Загружаю серию"
                        "retry" -> "Обновляю поток"
                        else -> info.title?.let { "Загружаю «$it»" } ?: "Загружаю тайтл"
                    },
                    color = m.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                space.aniroom.app.ui.components.LoaderBar(Modifier.width(140.dp))
            }
        }
    }
}

@Composable
private fun SeekRipple(key: Long, dir: Int, modifier: Modifier, onDone: () -> Unit) {
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        onDone()
    }
    Box(
        modifier
            .fillMaxHeight()
            .width(140.dp)
            .alpha(1f - anim.value)
            .background(Brush.horizontalGradient(if (dir > 0) listOf(Color.Transparent, Color.White.copy(alpha = 0.18f)) else listOf(Color.White.copy(alpha = 0.18f), Color.Transparent))),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (dir > 0) "10 ›› " else " ‹‹ 10", color = Color.White, fontFamily = DisplayFont, fontSize = 18.sp, modifier = Modifier.scale(0.8f + anim.value * 0.4f))
    }
}

@Composable
fun FlyingLayer(vm: RoomViewModel, modifier: Modifier) {
    val flying by vm.flying.collectAsState()
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        flying.forEach { r ->
            androidx.compose.runtime.key(r.id) {
                val anim = remember { Animatable(0f) }
                LaunchedEffect(Unit) { anim.animateTo(1f, tween(3000, easing = LinearEasing)) }
                val t = anim.value
                val x = w * r.lane + (sin(t * 9f + r.lane * 10f) * 18f).dp
                val y = h - (h * 0.85f * t) - 30.dp
                val popScale = if (t < 0.08f) t / 0.08f * 1.3f else 1.3f - (t - 0.08f) * 0.4f
                Text(
                    r.emoji,
                    fontSize = if (r.mine) 34.sp else 28.sp,
                    modifier = Modifier
                        .offset(x = x - 16.dp, y = y)
                        .graphicsLayer {
                            alpha = if (t > 0.75f) (1f - t) / 0.25f else 1f
                            scaleX = popScale
                            scaleY = popScale
                            rotationZ = sin(t * 7f) * 12f
                        },
                )
            }
        }
    }
}

@Composable
private fun BurstLayer(vm: RoomViewModel, modifier: Modifier) {
    val burst by vm.burst.collectAsState()
    val m = Manga.colors
    AnimatedVisibility(burst != null, modifier = modifier, enter = scaleIn(spring(dampingRatio = 0.35f, stiffness = 400f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
        val b = burst
        if (b != null) Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.rotate(-6f)) {
            Text(
                b.text, fontFamily = DisplayFont, fontSize = 30.sp, color = m.yellow,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
            )
            Text("${b.emoji} ×${b.count}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun PlayerToastLayer(vm: RoomViewModel, modifier: Modifier) {
    val all by vm.toast.collectAsState()
    val toast = all as? PlayerToast.Info
    val m = Manga.colors
    AnimatedContent(
        toast,
        modifier = modifier,
        transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
        contentKey = { it?.id },
        label = "toast",
    ) { t ->
        if (t != null) MangaPanel(color = m.paper2, shadow = 3.dp, radius = 14.dp) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t.emoji, fontSize = 15.sp, color = m.red, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text(t.text, color = m.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2)
            }
        } else Spacer(Modifier.height(1.dp))
    }
}

private val QUICK_REPLIES = listOf("ахаха 😂", "жесть 😱", "🔥🔥🔥", "согласен", "жду!", "ещё серию?")
private val CHAT_REACTIONS = listOf("heart" to "❤️", "like" to "👍", "dislike" to "👎")

@Composable
private fun ChatOverlay(vm: RoomViewModel, ui: RoomUi, modifier: Modifier) {
    val prefs by Session.appPrefs.collectAsState()
    val me = Session.user.collectAsState().value?.id
    val ids = remember { mutableStateListOf<Long>() }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    // Everything already in the room when fullscreen opened counts as read.
    val seen = remember { mutableSetOf<Long>().apply { ui.messages.forEach { add(it.id) } } }
    val hold = replyTo != null
    LaunchedEffect(ui.messages) {
        val fresh = ui.messages.filter { it.id > 0 && it.id !in seen }
        fresh.forEach { seen += it.id }
        if (!prefs.chatToasts) return@LaunchedEffect
        fresh.filter { it.userId != me }.forEach { msg ->
            ids.add(msg.id)
            while (ids.size > 4) ids.removeAt(0)
        }
    }
    // Each toast lives 9 s; the timer stops while you're replying.
    ids.forEach { id ->
        androidx.compose.runtime.key(id) {
            LaunchedEffect(hold) {
                if (hold) return@LaunchedEffect
                delay(9000)
                ids.remove(id)
            }
        }
    }
    Column(modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ids.forEach { id ->
            val msg = ui.messages.firstOrNull { it.id == id } ?: return@forEach
            androidx.compose.runtime.key(id) { ChatToast(msg, replyTo?.id == id, onReact = { r -> vm.toggleReaction(msg, r) }, onReply = { replyTo = msg }) }
        }
        AnimatedVisibility(replyTo != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            replyTo?.let { target ->
                QuickReplyBar(
                    target,
                    onSend = { text -> vm.sendMessage(text, target); replyTo = null; ids.remove(target.id) },
                    onClose = { replyTo = null },
                )
            }
        }
    }
}

@Composable
private fun ChatToast(msg: Message, active: Boolean, onReact: (String) -> Unit, onReply: () -> Unit) {
    val m = Manga.colors
    val haptic = rememberHaptic()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f)) }
    Column(
        Modifier
            .graphicsLayer { alpha = appear.value; translationX = (1f - appear.value) * -80f; scaleX = 0.9f + appear.value * 0.1f; scaleY = scaleX }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = if (active) 0.8f else 0.62f))
            .border(if (active) 2.dp else 1.5.dp, if (active) m.yellow else Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable { haptic(false); onReply() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(msg.avatar, msg.username, size = 26.dp, color = msg.nicknameColor, ring = false)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Text(msg.username, color = nickColor(msg.username, msg.nicknameColor), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                if (msg.stickerUrl != null) {
                    coil.compose.AsyncImage(model = space.aniroom.app.data.Api.media(msg.stickerUrl), contentDescription = "Стикер", modifier = Modifier.size(56.dp))
                } else {
                    Text(msg.text, color = Color.White, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            CHAT_REACTIONS.forEach { (key, emoji) ->
                val r = msg.reactions.firstOrNull { it.reaction == key }
                val mine = r?.reactedByMe == true
                val bump by animateFloatAsState(if (mine) 1.12f else 1f, spring(dampingRatio = 0.35f, stiffness = 600f), label = "r")
                Row(
                    Modifier.scale(bump).clip(RoundedCornerShape(12.dp))
                        .background(if (mine) m.yellow else Color.White.copy(alpha = 0.12f))
                        .clickable { haptic(false); onReact(key) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(emoji, fontSize = 13.sp)
                    if ((r?.count ?: 0) > 0) Text(" ${r?.count}", color = if (mine) m.onYellow else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(2.dp))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.12f)).clickable { haptic(false); onReply() }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Reply, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(" Ответить", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickReplyBar(target: Message, onSend: (String) -> Unit, onClose: () -> Unit) {
    val m = Manga.colors
    var text by remember(target.id) { mutableStateOf("") }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(target.id) { delay(150); runCatching { focus.requestFocus() } }
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xF0141210)).border(2.dp, m.yellow, RoundedCornerShape(16.dp)).padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.Reply, null, tint = m.yellow, modifier = Modifier.size(16.dp))
            Text(" Ответ ${target.username}", color = m.yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Box(Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Закрыть", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Row(Modifier.padding(vertical = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QUICK_REPLIES.forEach { q ->
                Text(
                    q, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.14f)).clickable { onSend(q) }.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (text.isEmpty()) Text("Быстрый ответ…", color = Color.White.copy(alpha = 0.45f), fontSize = 15.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = text, onValueChange = { text = it.take(360) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(m.yellow),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { if (text.isNotBlank()) onSend(text.trim()) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (text.isNotBlank()) m.red else Color.White.copy(alpha = 0.12f))
                    .clickable(enabled = text.isNotBlank()) { onSend(text.trim()) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.Send, "Отправить", tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

/** Live reactions in fullscreen: always reachable — a small button that fans out the six emojis. */
@Composable
private fun FullscreenReactions(controlsVisible: Boolean, onReact: (String) -> Unit) {
    val m = Manga.colors
    var open by remember { mutableStateOf(false) }
    var lastTap by remember { mutableStateOf(0L) }
    LaunchedEffect(open, lastTap) {
        if (open) { delay(4500); open = false }
    }
    val expanded = open || controlsVisible
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LIVE_REACTIONS.forEachIndexed { i, (kind, emoji, _) ->
            AnimatedVisibility(
                expanded,
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 500f), initialScale = 0.3f) + fadeIn(tween(120, delayMillis = i * 35)),
                exit = scaleOut(targetScale = 0.3f) + fadeOut(),
            ) {
                ReactionButton(emoji, 42.dp) { lastTap = System.currentTimeMillis(); onReact(kind) }
            }
        }
        AnimatedVisibility(!expanded, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).border(2.dp, m.yellow.copy(alpha = 0.8f), CircleShape)
                    .clickable { open = true; lastTap = System.currentTimeMillis() },
                contentAlignment = Alignment.Center,
            ) { Text("🔥", fontSize = 20.sp) }
        }
    }
}

/** Slide-in chat drawer for fullscreen: read and reply without leaving the video. */
@Composable
private fun FullscreenChat(vm: RoomViewModel, ui: RoomUi, open: Boolean, onClose: () -> Unit, openUser: (Int) -> Unit, modifier: Modifier) {
    val m = Manga.colors
    AnimatedVisibility(
        open, modifier = modifier,
        enter = slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 500f)) { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
    ) {
        Column(
            Modifier.fillMaxHeight().width(360.dp)
                .background(m.paper.copy(alpha = 0.96f))
                .border(2.dp, m.ink)
                .navigationBarsPadding().imePadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ЧАТ", style = Kicker, color = m.red, modifier = Modifier.weight(1f))
                Text("${ui.participants.count { it.online }} в комнате", color = m.muted, fontSize = 12.sp)
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Закрыть чат", tint = m.ink)
                }
            }
            Box(Modifier.weight(1f)) { ChatPanel(vm, ui, openUser) }
        }
    }
}

// --------------------------------------------------------------------------------------- below player

@Composable
private fun RoomHeader(vm: RoomViewModel, ui: RoomUi) {
    val m = Manga.colors
    val context = LocalContext.current
    val room = ui.room ?: return
    val online = ui.participants.filter { it.online }
    val inviteUrl = "${BuildConfig.SITE_URL}/rooms/${room.roomCode}"
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val linkColor = when (ui.link) {
                    LinkStatus.CONNECTED -> m.green
                    LinkStatus.CONNECTING, LinkStatus.RECONNECTING -> m.yellow
                    else -> m.red
                }
                val t = rememberInfiniteTransition(label = "dot")
                val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
                Box(Modifier.size(8.dp).clip(CircleShape).background(linkColor.copy(alpha = if (ui.link == LinkStatus.CONNECTED) 1f else a)))
                Spacer(Modifier.width(6.dp))
                Text(room.title, style = MaterialTheme.typography.titleMedium, color = m.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${online.size} ${plural(online.size, "зритель", "зрителя", "зрителей")} · " + if (vm.isOwner) "ты хост" else "хост: ${ui.participants.firstOrNull { it.isOwner }?.username ?: "—"}",
                color = m.muted, fontSize = 12.sp, maxLines = 1,
            )
        }
        Box(Modifier.width(((online.take(4).size - 1).coerceAtLeast(0) * 18 + 30).dp).height(30.dp)) {
            online.take(4).forEachIndexed { i, p ->
                UserAvatar(p.avatar, p.username, size = 30.dp, color = p.nicknameColor, modifier = Modifier.offset(x = (i * 18).dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        MangaPanel(color = m.yellow, radius = 12.dp, shadow = 3.dp, onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("AniRoom", inviteUrl))
            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        }) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(room.roomCode, fontFamily = DisplayFont, fontSize = 13.sp, color = m.onYellow)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.ContentCopy, "Скопировать", tint = m.onYellow, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        var inviteOpen by remember { mutableStateOf(false) }
        RoundIcon(Icons.Rounded.QrCode2, "Пригласить", { inviteOpen = true }, size = 36.dp)
        if (inviteOpen) InviteDialog(room.roomCode, room.title, room.anime?.title, inviteUrl) { inviteOpen = false }
    }
}

@Composable
private fun InviteDialog(code: String, title: String, anime: String?, url: String, onDismiss: () -> Unit) {
    val m = Manga.colors
    val context = LocalContext.current
    val qr = remember(url) { RoomLinks.qr(url).asImageBitmap() }
    val appear = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.5f)) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        MangaPanel(Modifier.fillMaxWidth().scale(appear.value), color = m.paper2, radius = 22.dp, shadow = 5.dp) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ПРИГЛАШЕНИЕ", style = Kicker, color = m.red)
                Text(title, style = MaterialTheme.typography.titleLarge, color = m.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                anime?.let { Text(it, color = m.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(14.dp))
                MangaPanel(Modifier.size(230.dp).rotate(-1.5f), color = m.paper2, radius = 16.dp, shadow = 4.dp) {
                    androidx.compose.foundation.Image(qr, "QR-код комнаты", Modifier.fillMaxSize().padding(10.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(code, fontFamily = DisplayFont, fontSize = 30.sp, color = m.ink, letterSpacing = 6.sp)
                Text("Наведи камеру друга на код или отправь ссылку", color = m.muted, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MangaButton("Копировать", {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("AniRoom", url))
                        Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                    }, kind = ButtonKind.PAPER, compact = true, icon = Icons.Rounded.ContentCopy, modifier = Modifier.weight(1f))
                    MangaButton("Отправить", {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Смотрим вместе в AniRoom${anime?.let { " — $it" } ?: ""}: $url")
                        }
                        context.startActivity(Intent.createChooser(send, "Пригласить в комнату"))
                    }, compact = true, icon = Icons.Rounded.Share, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReactionBar(vm: RoomViewModel) {
    val haptic = rememberHaptic()
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        LIVE_REACTIONS.forEach { (kind, emoji, _) ->
            ReactionButton(emoji, 44.dp) { haptic(false); vm.sendReaction(kind) }
        }
    }
}

@Composable
private fun ReactionButton(emoji: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val m = Manga.colors
    var bumps by remember { mutableStateOf(0) }
    val scale by animateFloatAsState(if (bumps % 2 == 1) 1.25f else 1f, spring(dampingRatio = 0.3f, stiffness = 800f), label = "bump", finishedListener = { if (bumps % 2 == 1) bumps++ })
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(m.paper2.copy(alpha = 0.92f))
            .border(2.dp, m.ink, CircleShape)
            .clickable { bumps = if (bumps % 2 == 0) bumps + 1 else bumps; onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * 0.48f).sp) }
}

@Composable
private fun ResumePrompt(vm: RoomViewModel, ui: RoomUi) {
    val m = Manga.colors
    AnimatedVisibility(ui.resumeAt != null) {
        MangaPanel(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), color = m.yellow, shadow = 3.dp) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Продолжить с ${formatTime(ui.resumeAt ?: 0.0)}?", color = m.onYellow, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                MangaButton("Нет", { vm.dismissResume() }, kind = ButtonKind.PAPER, compact = true)
                Spacer(Modifier.width(8.dp))
                MangaButton("Да", { vm.acceptResume() }, kind = ButtonKind.INK, compact = true)
            }
        }
    }
}

@Composable
private fun RoomTabs(vm: RoomViewModel, ui: RoomUi, openUser: (Int) -> Unit) {
    val m = Manga.colors
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    var seen by remember { mutableStateOf(ui.messages.size) }
    LaunchedEffect(pager.currentPage, ui.messages.size) { if (pager.currentPage == 0) seen = ui.messages.size }
    val unread = (ui.messages.size - seen).coerceAtLeast(0)
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(44.dp)) {
            val tabW = maxWidth / 3
            val indicator = (pager.currentPage + pager.currentPageOffsetFraction)
            Box(
                Modifier.offset(x = tabW * indicator).width(tabW).fillMaxHeight().padding(3.dp)
                    .clip(RoundedCornerShape(12.dp)).background(m.ink),
            )
            Row(Modifier.fillMaxSize()) {
                listOf("Чат", "Серии", "Комната").forEachIndexed { i, label ->
                    val selected = pager.currentPage == i
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable { scope.launch { pager.animateScrollToPage(i) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = if (selected) m.paper else m.ink, fontWeight = FontWeight.Bold)
                            if (i == 0 && unread > 0) {
                                Spacer(Modifier.width(5.dp))
                                Box(Modifier.clip(CircleShape).background(m.red).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                    Text("$unread", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(m.ink.copy(alpha = 0.08f)))
        HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            when (page) {
                0 -> ChatPanel(vm, ui, openUser)
                1 -> EpisodesPanel(vm, ui)
                else -> PeoplePanel(vm, ui, openUser)
            }
        }
    }
}


/** Fullscreen swipe gestures: left half = brightness, right half = volume. */
@Composable
private fun SwipeAdjustLayer(modifier: Modifier, tapGestures: Modifier) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var indicator by remember { mutableStateOf<Pair<Boolean, Float>?>(null) }
    var brightness by remember {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.6f)
    }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }
    LaunchedEffect(indicator) {
        if (indicator != null) {
            delay(900)
            indicator = null
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = -1f } }
        }
    }
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Box(
            Modifier.fillMaxSize().then(tapGestures).pointerInput(Unit) {
                var left = false
                detectVerticalDragGestures(
                    onDragStart = { offset -> left = offset.x < w / 2 },
                    onVerticalDrag = { change, drag ->
                        change.consume()
                        val delta = -drag / (h * 0.8f)
                        if (left) {
                            brightness = (brightness + delta).coerceIn(0.02f, 1f)
                            activity?.window?.let { win -> win.attributes = win.attributes.apply { screenBrightness = brightness } }
                            indicator = true to brightness
                        } else {
                            volume = (volume + delta).coerceIn(0f, 1f)
                            audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (volume * maxVolume).roundToInt(), 0)
                            indicator = false to volume
                        }
                    },
                )
            },
        )
        indicator?.let { (isBrightness, value) ->
            Row(
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (isBrightness) Icons.Rounded.BrightnessHigh else Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(140.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(value).background(Manga.colors.red))
                }
                Spacer(Modifier.width(10.dp))
                Text("${(value * 100).roundToInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * YouTube-style ambient mode: a tiny, constantly refreshed copy of the current frame is
 * stretched and blurred behind the video, so the side bars glow with the scene's colours
 * instead of staying black.
 */
@Composable
private fun AmbientGlow(texture: android.view.TextureView?, vm: RoomViewModel, modifier: Modifier) {
    var current by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var previous by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val fade = remember { Animatable(1f) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(texture) {
        while (true) {
            val tv = texture
            if (tv != null && tv.isAvailable && vm.player.videoSize.width > 0) {
                val frame = runCatching { tv.getBitmap(32, 18) }.getOrNull()
                if (frame != null) {
                    previous = current
                    current = frame.asImageBitmap()
                    if (appear.value < 1f) launch { appear.animateTo(1f, tween(900)) }
                    fade.snapTo(0f)
                    fade.animateTo(1f, tween(420))
                }
            }
            delay(if (vm.player.isPlaying) 350 else 1200)
        }
    }
    Box(modifier.graphicsLayer { alpha = appear.value }) {
        val blur = if (android.os.Build.VERSION.SDK_INT >= 31) Modifier.blur(48.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Rectangle) else Modifier
        previous?.let {
            androidx.compose.foundation.Image(it, null, contentScale = androidx.compose.ui.layout.ContentScale.FillBounds, modifier = Modifier.fillMaxSize().then(blur))
        }
        current?.let {
            androidx.compose.foundation.Image(
                it, null, contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().then(blur).graphicsLayer { alpha = fade.value },
            )
        }
        // keep it a soft glow, a bit darker toward the screen edges
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.18f to Color.Transparent, 0.82f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)),
            ),
        )
    }
}
